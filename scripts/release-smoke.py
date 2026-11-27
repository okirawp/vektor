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
wait(lambda:'key=-100' in state(),'final APK service loaded all six fixture mappings')
capture=subprocess.run([adb,'-s','emulator-5554','exec-out','screencap','-p'],capture_output=True,check=True,timeout=90)
(root/'dist/screenshots/rota-main-dark.png').write_bytes(capture.stdout)
sh('am start -W -n dev.kolmapper.probe/.ProbeActivity -f 0x10008000')
raw=sh('getevent -pl');device=None;path=None
for line in raw.splitlines():
 match=re.search(r'add device \d+: (/dev/input/event\d+)',line)
 if match:path=match.group(1)
 if '"KolMapper Test Gamepad"' in line:device=path
check(device is not None,'native test controller is connected')
scans=[106,304,307,313]
def events(ops):sh(' && '.join(f'sendevent {device} {t} {c} {v}' for t,c,v in ops+[(0,0,0)]))
events([(1,k,0) for k in [105]+scans]+[(3,0,0),(3,1,0)])
if 'editing=false' in state():sh('input tap 44 108')
wait(lambda:'editing=true' in state(),'final editor opens')
sh('input tap 44 108');wait(lambda:'armed=true' in state(),'final editor closes and starts control')
clear();events([(1,k,1) for k in scans])
def enough():
 s=query();return ',DOWN,RIGHT,' in s and all(s.count(',DOWN,'+n+',')>=10 for n in ['A','X','R2'])
wait(enough,'final APK accepts movement and all three macros from one native input report')
events([(1,k,0) for k in scans]);wait(lambda:'active=0' in query(),'final APK releases every finger')
trace=query();(root/'dist/final-simultaneous-trace.txt').write_text(trace)
check(trace.count(',DOWN,RIGHT,')==1 and trace.count(',UP,RIGHT,')==1 and ',CANCEL,RIGHT,' not in trace,'final movement remains continuous during simultaneous macros')
for name in ['A','X','R2']:check(trace.count(',DOWN,'+name+',')==trace.count(',UP,'+name+','),name+' has balanced DOWN/UP events')
clear();events([(3,0,32767),(1,304,1)])
wait(lambda:',DOWN,LEFT,' in query() and query().count(',DOWN,A,')>=2,'final analog stick works together with a macro')
events([(3,0,0),(1,304,0)]);wait(lambda:'active=0' in query(),'final analog neutral and button release leave no open finger')
(root/'dist/final-smoke-results.log').write_text('\n'.join(report)+'\nALL CHECKS PASSED\n');print('ALL CHECKS PASSED',flush=True)
