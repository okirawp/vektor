#!/usr/bin/env python3
"""Create emulator-only fixtures through the signed test APK and Android preferences API."""
import subprocess,pathlib
root=pathlib.Path(__file__).resolve().parents[1];adb='/workspace/tools/android-sdk/platform-tools/adb'
def call(*args):
 r=subprocess.run([adb,'-s','emulator-5554',*args],capture_output=True,text=True,timeout=180)
 if r.returncode:raise RuntimeError(r.stdout+r.stderr)
 print(r.stdout.strip(),flush=True);return r.stdout.strip()
if not call('get-serialno').startswith('emulator-'):raise RuntimeError('emulator only')
call('shell','am','force-stop','dev.kolmapper')
uid=call('shell','stat','-c','%u','/data/user/0/dev.kolmapper')
if not uid.isdigit():raise RuntimeError('invalid fixture UID')
call('shell',f'chown {uid}:{uid} /data/user/0/dev.kolmapper/shared_prefs && chmod 700 /data/user/0/dev.kolmapper/shared_prefs && restorecon -RF /data/user/0/dev.kolmapper/shared_prefs')
result=call('shell','am','instrument','-w','-r','-e','seed_only','true','dev.kolmapper.test/dev.kolmapper.EngineInstrumentation')
if 'SEED committed=true loaded=6' not in result:raise RuntimeError('Android fixture persistence failed')
call('shell','input','keyevent','KEYCODE_WAKEUP');call('shell','wm','dismiss-keyguard')
