#!/usr/bin/env python3
"""Real overlay point-picking and first controller press after learning."""
import subprocess,pathlib,time,re,json,xml.etree.ElementTree as E,sys,io
from PIL import Image
root=pathlib.Path(__file__).resolve().parents[1];adb='/workspace/tools/android-sdk/platform-tools/adb';report=[]
def sh(command):return subprocess.check_output([adb,'-s','emulator-5554','shell',command],text=True,timeout=120)
def check(ok,label):
 if not ok:raise AssertionError(label)
 report.append('PASS '+label);print(report[-1],flush=True)
def wait(fn,label):
 end=time.monotonic()+90
 while time.monotonic()<end:
  if fn():check(True,label);return
  time.sleep(.1)
 check(False,label)
def state():return sh('dumpsys activity service dev.kolmapper/.MapperService')
def events():return sh('content query --uri content://dev.kolmapper.probe/events')
def snapshot(name):
 p=subprocess.run([adb,'-s','emulator-5554','exec-out','screencap','-p'],capture_output=True,check=True,timeout=90)
 (root/'dist/screenshots'/name).write_bytes(p.stdout)
if '--resume-picker' not in sys.argv:
 sh("settings put secure enabled_accessibility_services ''");sh('settings put secure accessibility_enabled 0')
 sh('settings put secure enabled_accessibility_services dev.kolmapper/.MapperService');sh('settings put secure accessibility_enabled 1')
 wait(lambda:'key=96 held=' in state(),'release service loaded the existing A mapping')
 sh('/data/local/tmp/kolmapper-gamepad')
 raw=sh('getevent -pl');device=None;path=None
 for line in raw.splitlines():
  m=re.search(r'add device \d+: (/dev/input/event\d+)',line)
  if m:path=m.group(1)
  if '"KolMapper Test Gamepad"' in line:device=path
 check(device is not None,'native controller for button learning connected')
 def key(down):sh(f'sendevent {device} 1 304 {1 if down else 0} && sendevent {device} 0 0 0')
 sh('am start -W -n dev.kolmapper.probe/.ProbeActivity -f 0x10008000')
 sh('input tap 44 108');wait(lambda:'editing=true' in state(),'point editor opens')
 sh('input tap 210 232');key(True);time.sleep(1);key(False);time.sleep(.3)
else:
 wait(lambda:'key=96 held=' in state(),'existing A mapping remains loaded')
 raw=sh('getevent -pl');device=None;path=None
 for line in raw.splitlines():
  m=re.search(r'add device \d+: (/dev/input/event\d+)',line)
  if m:path=m.group(1)
  if '"KolMapper Test Gamepad"' in line:device=path
 check(device is not None,'native learning controller remains connected')
 def key(down):sh(f'sendevent {device} 1 304 {1 if down else 0} && sendevent {device} 0 0 0')
def picker_ready():
 shot=subprocess.run([adb,'-s','emulator-5554','exec-out','screencap','-p'],capture_output=True,check=True,timeout=90).stdout
 color=Image.open(io.BytesIO(shot)).convert('RGB').getpixel((10,400))
 return max(abs(a-b) for a,b in zip(color,(34,58,63)))<=3
wait(picker_ready,'real point-picker layer is visible and ready before selection')
sh('input tap 650 500')
def saved_point():
 text=sh('cat /data/user/0/dev.kolmapper/shared_prefs/mapper.xml')
 xml=E.fromstring(text);value=next(e.text for e in xml if e.attrib.get('name')=='bindings')
 return any(b['key']==96 and b['x']==650 and b['y']==500 for b in json.loads(value))
wait(saved_point,'picked A point persisted at the exact new screen position')
wait(lambda:'editing=true' in state(),'point selection returns automatically to editing')
snapshot('learned-point.png')
# Exercise the settings window's accessibility token, then dismiss without changing the mapping.
sh('input tap 650 500');snapshot('settings-dark.png');sh('input keyevent KEYCODE_BACK')
sh('input tap 44 108');wait(lambda:'armed=true' in state(),'icon starts control after point learning')
sh('content delete --uri content://dev.kolmapper.probe/events');key(True)
wait(lambda:',DOWN,A,' in events(),'first fresh A press after learning generates a real touch')
key(False);wait(lambda:'active=0' in events(),'newly learned A releases normally')
sh('am start -W -n dev.kolmapper/.MainActivity');wait(lambda:'armed=false' in state(),'main screen pauses the learned control')
snapshot('rota-main-dark.png')
(root/'dist/learning-flow-results.log').write_text('\n'.join(report)+'\nALL CHECKS PASSED\n');print('ALL CHECKS PASSED',flush=True)
