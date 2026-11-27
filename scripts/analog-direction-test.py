#!/usr/bin/env python3
"""Final APK smoke test with native simultaneous input; requires emulator fixture from core test."""
import subprocess,pathlib,re,time
root=pathlib.Path(__file__).resolve().parents[1];adb='/workspace/tools/android-sdk/platform-tools/adb';report=[]
def call(*args,timeout=120):
 r=subprocess.run([adb,'-s','emulator-5554',*args],capture_output=True,text=True,timeout=timeout)
 if r.returncode:raise RuntimeError(r.stdout+r.stderr)
 return r.stdout

def sh(c):return call('shell',c)
def check(ok,label):
 if not ok:raise AssertionError(label)
 report.append('PASS '+label);print(report[-1],flush=True)
def wait(fn,label):
 end=time.monotonic()+100
 while time.monotonic()<end:
  if fn():check(True,label);return
  time.sleep(.1)
 check(False,label)
def state():return sh('dumpsys activity service dev.kolmapper/.MapperService')
def query():return sh('content query --uri content://dev.kolmapper.probe/events')
def clear():sh('content delete --uri content://dev.kolmapper.probe/events')
sh("settings put secure enabled_accessibility_services ''")
sh('settings put secure accessibility_enabled 0')
sh('settings put secure enabled_accessibility_services dev.kolmapper/.MapperService')
sh('settings put secure accessibility_enabled 1')
wait(lambda:'key=21 held=' in state() and 'key=-100 held=' not in state(),'legacy five-button profile loaded without a joystick-center mapping')
sh('am start -W -n dev.kolmapper.probe/.ProbeActivity -f 0x10008000')
raw=sh('getevent -pl');device=None;path=None
for line in raw.splitlines():
 match=re.search(r'add device \d+: (/dev/input/event\d+)',line)
 if match:path=match.group(1)
 if '"KolMapper Test Gamepad"' in line:device=path
check(device is not None,'native controller is connected')
def events(ops):sh(' && '.join(f'sendevent {device} {t} {c} {v}' for t,c,v in ops+[(0,0,0)]))
events([(1,k,0) for k in [105,106,304,307,313]]+[(3,0,0),(3,1,0)])
if 'editing=false' in state():sh('input tap 44 108')
wait(lambda:'editing=true' in state(),'direction editor opens')
sh('input tap 44 108');wait(lambda:'armed=true' in state(),'direction control starts')
clear();events([(3,0,32767)])
wait(lambda:',DOWN,RIGHT,' in query(),'left analog stick drives the existing RIGHT button')
events([(3,0,0)]);wait(lambda:'active=0' in query(),'analog neutral releases RIGHT')
clear();events([(3,0,-32768)])
wait(lambda:',DOWN,LEFT,' in query(),'left analog stick drives the existing LEFT button')
events([(3,0,0)]);wait(lambda:'active=0' in query(),'analog neutral releases LEFT')
clear();events([(1,106,1)]);wait(lambda:',DOWN,RIGHT,' in query(),'physical dpad holds RIGHT')
events([(3,0,32767)]);events([(3,0,0)])
wait(lambda:'active=1' in query(),'analog neutral preserves the independently held dpad button')
check(',UP,RIGHT,' not in query(),'analog release does not drop physical RIGHT')
events([(1,106,0)]);wait(lambda:'active=0' in query(),'physical release lifts the final RIGHT owner')
events([(1,106,1)]);wait(lambda:'active=1' in query(),'held direction exists before unplug')
pid=sh('cat /data/local/tmp/kolmapper-gamepad.pid').strip();check(bool(re.fullmatch(r'\d+',pid)) and 'kolmapper-gamepad' in sh('cat /proc/'+pid+'/cmdline'),'unplug targets only the emulator controller')
sh('kill '+pid);wait(lambda:'active=0' in query(),'unplug releases the touch with motion capture gating enabled')
wait(lambda:'armed=false' in state(),'unplug pauses control')
(root/'dist/analog-direction-results.log').write_text('\n'.join(report)+'\nALL CHECKS PASSED\n');print('ALL CHECKS PASSED',flush=True)
