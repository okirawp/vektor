#!/usr/bin/env python3
"""Dedicated emulator-5556 test only. Requires compiled/signed test APK and root uinput fixture."""
from pathlib import Path
import subprocess,time,re,io
from PIL import Image
r=Path(__file__).resolve().parents[1];a='/workspace/tools/android-sdk/platform-tools/adb';lines=[]
def call(*args):
 p=subprocess.run([a,'-s','emulator-5556',*args],capture_output=True,timeout=180)
 if p.returncode:raise RuntimeError(p.stdout.decode(errors='replace')+p.stderr.decode(errors='replace'))
 return p.stdout
def sh(s):return call('shell',s).decode()
def state():return sh('dumpsys activity service dev.kolmapper/.MapperService')
def check(ok,label):
 if not ok:raise AssertionError(label+'\n'+state())
 lines.append('PASS '+label);print(lines[-1],flush=True)
def until(fn,label,seconds=120):
 end=time.monotonic()+seconds
 while time.monotonic()<end:
  if fn():check(True,label);return
  time.sleep(.5)
 check(False,label)
def playing():return 'PROFILE active=dev.kolmapper.probe foreground=true' in state() and 'armed=true' in state()
def overlay():return bool(re.search(r'ty=(?:ACCESSIBILITY_OVERLAY|2032)\b',sh('dumpsys window windows')))
def query():return sh('content query --uri content://dev.kolmapper.probe/events')
def device():
 path=None
 for line in sh('getevent -pl').splitlines():
  m=re.search(r'add device \d+: (/dev/input/event\d+)',line)
  if m:path=m.group(1)
  if '"KolMapper Test Gamepad"' in line:return path
 raise AssertionError('native controller missing')
def keys(path,down):sh(' && '.join([f'sendevent {path} 1 {k} {1 if down else 0}' for k in [106,304]]+[f'sendevent {path} 0 0 0']))
check(sh('getprop ro.build.version.sdk').strip()=='29','dedicated Android10 emulator confirmed')
print(call('install','--no-streaming','--no-incremental','-r',str(r/'probe/build/outputs/apk/debug/probe-debug.apk')).decode(),flush=True)
result=sh('am instrument -w -r -e seed_profiles true dev.kolmapper.test/dev.kolmapper.ProfileInstrumentation');check('PROFILE FIXTURES SEEDED' in result,'Android10 profiles seeded')
call('push','/tmp/rota10-gamepad','/data/local/tmp/rota-gamepad');sh('chmod 755 /data/local/tmp/rota-gamepad');sh('/data/local/tmp/rota-gamepad')
sh("settings put secure enabled_accessibility_services ''");sh('settings put secure accessibility_enabled 0');sh('settings put secure enabled_accessibility_services dev.kolmapper/.MapperService');sh('settings put secure accessibility_enabled 1');sh('input keyevent KEYCODE_WAKEUP');sh('wm dismiss-keyguard');sh('am start -W -n dev.kolmapper/.MainActivity')
until(lambda:'ROTA session=' in state(),'Android10 accessibility service connects')
for attempt in range(3):
 shot=Image.open(io.BytesIO(call('exec-out','screencap','-p'))).convert('RGB');rows=[]
 for y in range(90,shot.height-40):
  xs=[x for x in range(20,shot.width-20) if shot.getpixel((x,y)) in [(255,137,96),(187,62,28)]]
  if len(xs)>120:rows.append((y,min(xs),max(xs)))
 if rows:
  y,x1,x2=rows[len(rows)//2];sh(f'input tap {(x1+x2)//2} {y}')
 time.sleep(1)
 if 'session=true' in state():break
until(playing,'Android10 game starts saved controls without touching orb')
check(overlay(),'Android10 accessibility overlay attaches')
sh('content delete --uri content://dev.kolmapper.probe/events');path=device();keys(path,True)
until(lambda:query().count(',DOWN,A,')>=3 and ',DOWN,RIGHT,' in query(),'Android10 real HID right hold and A macro run together')
keys(path,False);until(lambda:'active=0' in query(),'Android10 releasing keys lifts every contact')
trace=query();check(trace.count(',DOWN,RIGHT,')==1 and trace.count(',UP,RIGHT,')==1,'Android10 held direction remains continuous alongside macro');(r/'dist/rota10-api29-touch-trace.txt').write_text(trace)
sh('input keyevent KEYCODE_HOME');until(lambda:'foreground=false' in state(),'Android10 home suspends controls');check('session=true' in state() and not overlay(),'Android10 home retains session but hides overlay');sh('am start -W -n dev.kolmapper.probe/.ProbeActivity');until(playing,'Android10 returning to game restores controls')
sh('input keyevent KEYCODE_SLEEP');until(lambda:'session=false' in state(),'Android10 screen off ends session');check(not overlay(),'Android10 screen off has no overlay');sh('input keyevent KEYCODE_WAKEUP')
(r/'dist/rota10-api29-input-results.log').write_text('\n'.join(lines)+'\nALL ANDROID10 INPUT CHECKS PASSED\n');print('ALL ANDROID10 INPUT CHECKS PASSED',flush=True)
