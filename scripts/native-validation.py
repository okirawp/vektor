#!/usr/bin/env python3
"""Resume the signed release test using a kernel-level virtual controller on the local emulator."""
import subprocess,time,re,pathlib
root=pathlib.Path(__file__).resolve().parents[1]
adb='/workspace/tools/android-sdk/platform-tools/adb'
def run(*args,timeout=30,check=True):
 p=subprocess.run([adb,'-s','emulator-5554',*args],capture_output=True,text=True,timeout=timeout)
 if check and p.returncode:raise RuntimeError(p.stdout+p.stderr)
 return p.stdout
for i in range(300):
 try:
  if run('get-state',timeout=6).strip()=='device':break
 except Exception:pass
 time.sleep(2)
else:raise RuntimeError('emulator did not become online')
print(run('root',timeout=30,check=False),flush=True)
for i in range(60):
 try:
  run('shell','setprop','ro.hw_timeout_multiplier','10',timeout=8,check=False)
  if 'uid=0' in run('shell','id',timeout=8):break
 except Exception:pass
 time.sleep(2)
for i in range(240):
 try:
  p=run('shell','service','check','package',timeout=8)
  w=run('shell','service','check','window',timeout=8)
  if 'found' in p and 'not found' not in p and 'found' in w and 'not found' not in w:
   run('shell','settings','put','global','watchdog_timeout_millis','600000')
   break
 except Exception:pass
 time.sleep(2)
else:raise RuntimeError('framework services not ready')
for i in range(120):
 try:
  if 'RUNNING_UNLOCKED' in run('shell','dumpsys','user',timeout=15):break
 except Exception:pass
 time.sleep(2)
else:raise RuntimeError('emulator user did not unlock')
print('Framework ready and user unlocked',flush=True)
run('shell','input','keyevent','KEYCODE_WAKEUP')
run('shell','wm','dismiss-keyguard')
run('shell','settings','put','secure','immersive_mode_confirmations','confirmed')
run('shell','settings','put','system','user_rotation','0')
run('push',str(root/'dist/virtual-gamepad'),'/data/local/tmp/kolmapper-gamepad')
run('shell','chmod','755','/data/local/tmp/kolmapper-gamepad')
run('shell','/data/local/tmp/kolmapper-gamepad')
path=None
for i in range(10):
 text=run('shell','getevent','-pl')
 current=None
 for line in text.splitlines():
  match=re.search(r'add device \d+: (/dev/input/event\d+)',line)
  if match:current=match.group(1)
  if '"KolMapper Test Gamepad"' in line:path=current
 if path:break
 time.sleep(1)
if not path:raise RuntimeError('virtual controller not found')
print('Kernel-level controller '+path,flush=True)
print(run('install','--no-incremental','-r',str(root/'dist/KolMapper-0.4-tests.apk'),timeout=180),flush=True)
result=run('shell','am','instrument','-w','-r','-e','input_device',path,'dev.kolmapper.test/dev.kolmapper.MapperInstrumentation',timeout=900)
(root/'dist/android-native-tests.log').write_text(result)
print(result,flush=True)
if 'ALL CHECKS PASSED' not in result:raise RuntimeError('Native controller test failed')
