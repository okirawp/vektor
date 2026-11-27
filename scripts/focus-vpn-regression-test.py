#!/usr/bin/env python3
"""Dedicated emulator-5554 only. No UiAutomation while inspecting the settings dialog."""
from pathlib import Path
import subprocess,time,re,io,json,sys,xml.etree.ElementTree as ET
from PIL import Image
root=Path(__file__).resolve().parents[1];adb='/workspace/tools/android-sdk/platform-tools/adb';log=root/'dist/rota101-focus-vpn-results.log'
def call(*args):
 p=subprocess.run([adb,'-s','emulator-5554',*args],capture_output=True,timeout=240)
 if p.returncode:raise RuntimeError(p.stdout.decode(errors='replace')+p.stderr.decode(errors='replace'))
 return p.stdout

def sh(cmd):return call('shell',cmd).decode()
def state():return sh('dumpsys activity service dev.kolmapper/.MapperService')
def check(ok,label):
 if not ok:raise AssertionError(label+'\n'+state())
 with log.open('a') as f:f.write('PASS '+label+'\n')
 print('PASS '+label,flush=True)
def wait(fn,label,seconds=120):
 end=time.monotonic()+seconds
 while time.monotonic()<end:
  if fn():check(True,label);return
  time.sleep(.4)
 check(False,label)
def tap(x,y):sh(f'input tap {x} {y}')
def screenshot(name):
 p=root/'dist/screenshots'/name;p.parent.mkdir(exist_ok=True);p.write_bytes(call('exec-out','screencap','-p'))
def playing():
 s=state();return 'PROFILE active=dev.kolmapper.probe foreground=true' in s and 'armed=true' in s

def bindings():
 # SharedPreferences atomically renames XML to .bak during a committed write.
 for attempt in range(10):
  try:
   data=ET.fromstring(sh('cat /data/user/0/dev.kolmapper/shared_prefs/mapper.xml'));break
  except RuntimeError as error:
   if 'No such file or directory' not in str(error) or attempt==9:raise
   time.sleep(1)
 return {x.attrib['name']:x.text for x in data if x.attrib.get('name','').endswith('.bindings')}
def keyA():return next(x for x in json.loads(bindings()['game.dev.kolmapper.probe.bindings']) if x['key']==96)
def launch_game():sh('am start -W -n dev.kolmapper.probe/.ProbeActivity')
if __name__=='__main__':
 phase=sys.argv[1]
 if phase=='setup':
  log.write_text('')
  sh('settings put system screen_off_timeout 2147483647');sh('svc power stayon true');sh('settings put secure immersive_mode_confirmations confirmed')
  sh("settings put secure enabled_accessibility_services ''");sh('settings put secure accessibility_enabled 0');sh('settings put secure enabled_accessibility_services dev.kolmapper/.MapperService');sh('settings put secure accessibility_enabled 1')
  sh('input keyevent KEYCODE_WAKEUP');sh('wm dismiss-keyguard');sh('am start -W -n dev.kolmapper/.MainActivity')
  wait(lambda:'ROTA session=' in state(),'patched accessibility service connects')
  for attempt in range(3):
   shot=Image.open(io.BytesIO(call('exec-out','screencap','-p'))).convert('RGB');rows=[]
   for y in range(100,shot.height-40):
    xs=[x for x in range(20,shot.width-20) if shot.getpixel((x,y)) in [(255,137,96),(187,62,28)]]
    if len(xs)>120:rows.append((y,min(xs),max(xs)))
   if rows:
    y,x1,x2=rows[len(rows)//2];tap((x1+x2)//2,y)
   time.sleep(1)
   if 'session=true' in state():break
  wait(playing,'game starts mapped controls')
  (root/'dist/rota101-bindings-baseline.txt').write_text(json.dumps(bindings()))
  tap(44,108);wait(lambda:'editing=true' in state(),'orb enters editing')
  tap(650,600);wait(lambda:'settings=true' in state(),'assigned A opens mode dialog')
  time.sleep(3);s=state();check('settings=true' in s and 'editing=true' in s and 'foreground=true' in s,'mode dialog stays open across focus events and session checks')
  screenshot('rota101-mode-dialog.png')
 elif phase=='verify_hold':
  wait(lambda:keyA()['hold'],'hold mode saved to the correct game profile')
  s=state();check('editing=true' in s and 'settings=false' in s,'saving mode returns to editor without closing editing')
  tap(650,600);wait(lambda:'settings=true' in state(),'same assigned key reopens saved settings');time.sleep(3);check('settings=true' in state(),'reopened settings remains stable');screenshot('rota101-hold-dialog.png')
 elif phase=='verify_macro':
  wait(lambda:not keyA()['hold'],'macro mode saved back to the same key')
  check(bindings()==json.loads((root/'dist/rota101-bindings-baseline.txt').read_text()),'all binding values preserved after hold/macro round trip')
  tap(44,108);wait(playing,'closing editor resumes mapped play')
 elif phase=='vpn':
  sh('am start -W -n dev.kolmapper.vpnprobe/.VpnProbeActivity')
  wait(lambda:'foreground=false' in state(),'VPN app suspends game scope');check('session=true' in state(),'opening VPN app keeps user session')
  check('ty=ACCESSIBILITY_OVERLAY' not in sh('dumpsys window windows'),'VPN app has no Rota overlay')
  tap(640,102);screenshot('rota101-vpn-consent.png')
 elif phase=='vpn_return':
  wait(lambda:'ProbeVpn' in sh('dumpsys activity services dev.kolmapper.vpnprobe'),'real test VPN service is running')
  launch_game();wait(playing,'return from actual VPN connection restores orb and controls without Rota launch')
  check(bindings()==json.loads((root/'dist/rota101-bindings-baseline.txt').read_text()),'VPN flow does not change any game binding')
 elif phase=='rebind':
  before=state();sh('uiautomator dump /sdcard/rota101-automation.xml')
  wait(playing,'accessibility reconnection resumes same authorized game session')
  check('reason=reconnected' in state(),'reconnection path actually restored authorization from RAM')
  check(bindings()==json.loads((root/'dist/rota101-bindings-baseline.txt').read_text()),'service reconnection preserves binding values')

 elif phase=='lock_disconnected':
  pid=sh('pidof dev.kolmapper').strip()
  remote=sh('nohup uiautomator events >/data/local/tmp/rota101-uiauto-events.txt 2>&1 </dev/null & echo $!').strip()
  check(remote.isdigit(),'temporary automation connection created')
  wait(lambda:'session=false' in state() or 'No services match' in state(),'accessibility service temporarily detached while authorization remains in process')
  try:sh('am stopservice -n dev.kolmapper/.MapperService')
  except RuntimeError as error:
   if 'was not running' not in str(error):raise
  wait(lambda:'No services match' in state(),'old service component destroyed before lock test')
  sh('input keyevent KEYCODE_SLEEP');time.sleep(1);sh('input keyevent KEYCODE_WAKEUP');sh('wm dismiss-keyguard')
  sh('kill '+remote)
  wait(lambda:'ROTA session=' in state(),'accessibility service reconnects after lock/unlock')
  time.sleep(2);check('session=false' in state(),'lock during disconnection revokes RAM authorization before returning to game')
  check(pid==sh('pidof dev.kolmapper').strip(),'lock protection verified in same process, not a cold restart')
  check('ty=ACCESSIBILITY_OVERLAY' not in sh('dumpsys window windows'),'reconnect after lock never restores orb or editor')
 elif phase=='lock':
  sh('input keyevent KEYCODE_SLEEP');wait(lambda:'session=false' in state(),'screen off ends restored session')
  check('ty=ACCESSIBILITY_OVERLAY' not in sh('dumpsys window windows'),'screen off has no editor or orb')
  sh('input keyevent KEYCODE_WAKEUP');time.sleep(2);check('session=false' in state(),'unlock never restarts restored session')
