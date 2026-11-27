#!/usr/bin/env python3
"""Run genuine Android release instrumentation using a separate touch-recorder APK."""
import json,subprocess,os,pathlib,html,time,re
root=pathlib.Path(__file__).resolve().parents[1]
adb=os.environ.get('ADB','/workspace/tools/android-sdk/platform-tools/adb')
serial=os.environ.get('ANDROID_SERIAL','emulator-5554')
if not serial.startswith('emulator-'):raise RuntimeError('This destructive fixture test runs only on an emulator')
def call(*args,timeout=120):
 r=subprocess.run([adb,'-s',serial,*args],capture_output=True,text=True,timeout=timeout)
 if r.returncode:raise RuntimeError(r.stdout+r.stderr)
 return r.stdout
# Android 16's boot property can survive a framework restart. Verify the actual services too.
for attempt in range(240):
 try:
  ready=all('found' in call('shell','service','check',name,timeout=12) and 'not found' not in call('shell','service','check',name,timeout=12) for name in ['package','window','display'])
  if ready and call('shell','getprop','sys.boot_completed',timeout=12).strip()=='1':break
 except Exception:pass
 time.sleep(3)
else:raise RuntimeError('Android framework services did not become ready')
print('Android framework services ready',flush=True)
installed=call('shell','pm','list','packages','dev.kolmapper')
if 'package:dev.kolmapper' in installed.splitlines():call('uninstall','dev.kolmapper')
# Native input events are required: UiAutomation.injectInputEvent bypasses InputFilter.
subprocess.run(['gcc','-O2','-static','-o',str(root/'dist/virtual-gamepad'),str(root/'tests/virtual-gamepad.c')],check=True)
call('root');call('wait-for-device')
call('push',str(root/'dist/virtual-gamepad'),'/data/local/tmp/kolmapper-gamepad')
call('shell','chmod','755','/data/local/tmp/kolmapper-gamepad')
input_device=None
for attempt in range(10):
 devices=call('shell','getevent','-pl')
 current=None
 for line in devices.splitlines():
  match=re.search(r'add device \d+: (/dev/input/event\d+)',line)
  if match:current=match.group(1)
  if '"KolMapper Test Gamepad"' in line:input_device=current
 if input_device:break
 if attempt==0:call('shell','/data/local/tmp/kolmapper-gamepad')
 time.sleep(1)
if not input_device:raise RuntimeError('Native test gamepad not registered')
print('Native controller: '+input_device,flush=True)
# The first install is deliberately the previous published debug-signed version.
print(call('install','--no-incremental','-r','/workspace/library-files/kolmapper-v03/KolMapper-0.3-debug.apk'),flush=True)
call('shell','am','start','-W','-n','dev.kolmapper/.MainActivity',timeout=180)
call('shell','am','force-stop','dev.kolmapper')
call('shell','run-as','dev.kolmapper','mkdir','-p','shared_prefs')
bindings=[dict(key=k,x=x,y=600,w=1280,h=800,interval=50,count=1 if i<2 else 0,duration=25) for i,(k,x) in enumerate(zip([21,22,96,99,105],[200,420,650,850,1080]))]
seed=root/'dist/seed.xml';seed.write_text('<?xml version="1.0" encoding="utf-8"?><map><string name="upgrade_marker">preserve-existing-data</string><boolean name="dark" value="true"/><string name="bindings">'+html.escape(json.dumps(bindings))+'</string></map>')
call('push',str(seed),'/data/local/tmp/kolmapper-seed.xml')
call('shell','run-as','dev.kolmapper','cp','/data/local/tmp/kolmapper-seed.xml','shared_prefs/mapper.xml')
print(call('install','--no-incremental','-r',str(root/'dist/Rota-0.5.apk')),flush=True)
print(call('install','--no-incremental','-r',str(root/'probe/build/outputs/apk/debug/probe-debug.apk')),flush=True)
print(call('install','--no-incremental','-r',str(root/'dist/Rota-0.5-tests.apk')),flush=True)
call('shell','settings','put','secure','enabled_accessibility_services','dev.kolmapper/.MapperService')
call('shell','settings','put','secure','accessibility_enabled','1')
call('shell','settings','put','secure','immersive_mode_confirmations','confirmed')
call('shell','settings','put','system','accelerometer_rotation','0')
call('shell','settings','put','system','user_rotation','0')
call('shell','input','keyevent','KEYCODE_WAKEUP')
call('shell','wm','dismiss-keyguard')
result=call('shell','am','instrument','-w','-r','dev.kolmapper.test/dev.kolmapper.EngineInstrumentation',timeout=600)
(root/'dist/ui-tests.log').write_text(result)
print(result,flush=True)
if 'ENGINE CHECKS PASSED' not in result:raise RuntimeError('Android instrumentation did not pass')
