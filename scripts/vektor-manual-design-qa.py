#!/usr/bin/env python3
"""Native UI through adb input, without an instrumentation lifecycle or UiAutomation."""
from pathlib import Path
import subprocess,time,xml.etree.ElementTree as ET,io,hashlib
from PIL import Image
root=Path(__file__).resolve().parents[1];adb='/workspace/tools/android-sdk/platform-tools/adb';log=root/'dist/vektor11-manual-ui-results.log';log.write_text('')
def call(*args):
 p=subprocess.run([adb,'-s','emulator-5556',*args],capture_output=True,timeout=180)
 if p.returncode:raise RuntimeError(p.stdout.decode(errors='replace')+p.stderr.decode(errors='replace'))
 return p.stdout
def sh(cmd):return call('shell',cmd).decode()
def check(ok,msg):
 if not ok:raise AssertionError(msg)
 with log.open('a') as f:f.write('PASS '+msg+'\n')
 print('PASS '+msg,flush=True)
def prefs():
 for i in range(10):
  try:return {e.attrib['name']:e.text or e.attrib.get('value') for e in ET.fromstring(sh('cat /data/user/0/dev.kolmapper/shared_prefs/mapper.xml'))}
  except RuntimeError as err:
   if 'No such file' not in str(err) or i==9:raise
   time.sleep(.5)
def data():return {k:v for k,v in prefs().items() if k.endswith(('.bindings','.previous'))}
def state():return sh('dumpsys activity service dev.kolmapper/.MapperService')
def wait(fn,msg,seconds=90):
 end=time.monotonic()+seconds
 while time.monotonic()<end:
  if fn():check(True,msg);return
  time.sleep(.5)
 check(False,msg)
def tap(x,y):sh(f'input tap {x} {y}')
def shot(name,theme=None):
 for i in range(5):
  raw=call('exec-out','screencap','-p');img=Image.open(io.BytesIO(raw)).convert('RGB')
  if theme is None or img.getpixel((10,150))==((15,19,27) if theme=='dark' else (244,246,250)):break
  time.sleep(1)
 else:raise AssertionError('Main UI not visible with requested theme: '+name)
 (root/'dist/screenshots'/f'vektor-final-{name}.png').write_bytes(raw)
 print('Captured '+name+' '+str(img.size),flush=True)
 return img.size
baseline=data();original=prefs()['selected_game']
sh('wm size 1280x800\nsettings put system font_scale 1.0\nam start -W -n dev.kolmapper/.MainActivity')
wait(lambda:'PROFILE active=dev.kolmapper.probe' in state(),'real accessibility service is connected outside instrumentation')
tap(370,620);wait(lambda:prefs()['selected_game']=='dev.kolmapper.probe2','real touch selects the other saved game')
tap(370,620);wait(lambda:prefs()['selected_game']==original,'real touch restores original saved game')
check(data()==baseline,'saved-game card round trip preserves every binding/recovery value')
configs=[('tablet-dark',1280,800,1.0,'dark'),('tablet-light',1280,800,1.0,'light'),('phone-dark',393,852,1.0,'dark'),('phone-large-text',393,852,1.3,'light'),('compact-landscape',800,360,1.0,'dark')]
for name,w,h,scale,theme in configs:
 sh(f'wm size {w}x{h}\nsettings put system font_scale {scale}\nam start -W -n dev.kolmapper/.MainActivity')
 tap(w-(95 if theme=='light' else 47),58 if h<420 else 70);time.sleep(1)
 check(shot(name,theme)==(w,h),name+' native viewport and theme')
sh('wm size 1280x800\nsettings put system font_scale 1.0\nam start -W -n dev.kolmapper/.MainActivity');tap(1233,70);time.sleep(1)
tap(370,360);time.sleep(3);shot('picker');sh('input keyevent KEYCODE_BACK');time.sleep(1)
tap(370,470);wait(lambda:'foreground=true' in state() and 'armed=true' in state(),'normal launch activates saved game controls')
shot('playing');tap(44,108);wait(lambda:'editing=true' in state(),'orb opens real editor');shot('editor')
tap(650,600);wait(lambda:'settings=true' in state(),'A opens real control settings');time.sleep(3);check('settings=true' in state(),'restyled settings stays open');shot('settings')
check(data()==baseline,'all binding/recovery values preserved after visual QA')
paths=sh('pm path dev.kolmapper').splitlines();path=next(x[8:] for x in paths if x.startswith('package:') and x.endswith('/base.apk'))
call('pull',path,str(root/'dist/vektor11-native-tested.apk'))
digest=hashlib.sha256((root/'dist/vektor11-native-tested.apk').read_bytes()).hexdigest();check(digest==hashlib.sha256((root/'dist/Vektor-1.1.0.apk').read_bytes()).hexdigest(),'native tested APK exactly matches delivery APK')
(root/'dist/vektor11-native-tested-apk-sha.txt').write_text(digest+'  Vektor-1.1.0.apk\n')
print('ALL MANUAL DESIGN QA CHECKS PASSED',flush=True)
