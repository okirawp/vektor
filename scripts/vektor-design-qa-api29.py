#!/usr/bin/env python3
"""Native release UI checks on the dedicated local QA emulator; never user hardware."""
from pathlib import Path
import subprocess,time,sys
root=Path(__file__).resolve().parents[1];adb='/workspace/tools/android-sdk/platform-tools/adb'
def call(*args,timeout=240):
 p=subprocess.run([adb,'-s','emulator-5556',*args],capture_output=True,timeout=timeout)
 if p.returncode:raise RuntimeError(p.stdout.decode(errors='replace')+p.stderr.decode(errors='replace'))
 return p.stdout.decode(errors='replace')
def sh(cmd,timeout=240):return call('shell',cmd,timeout=timeout)
def report(msg):print(msg,flush=True)
report('Waiting for software-rendered API 29 emulator startup')
until=time.monotonic()+1500
while time.monotonic()<until:
 try:
  if sh('getprop sys.boot_completed',timeout=8).strip()=='1':break
 except Exception:pass
 time.sleep(2)
else:raise RuntimeError('Emulator startup did not complete; no UI checks claimed')
report('Boot complete')
call('root');time.sleep(2)
sh('''settings put system screen_off_timeout 2147483647
svc power stayon true
settings put global hide_error_dialogs 1
settings put global window_animation_scale 0
settings put global transition_animation_scale 0
settings put global animator_duration_scale 0
settings put secure immersive_mode_confirmations confirmed
input keyevent KEYCODE_WAKEUP
wm dismiss-keyguard''')
report(call('install','-r',str(root/'dist/Vektor-1.1.0.apk'),timeout=360).strip())
report(call('install','-r',str(root/'dist/Vektor-1.1.0-test.apk'),timeout=360).strip())
result=sh('am instrument -w -r dev.kolmapper.test/dev.kolmapper.ProfileInstrumentation',timeout=360)
(root/'dist/vektor11-api29-storage-results.log').write_text(result)
if 'ALL PROFILE STORAGE CHECKS PASSED' not in result:raise AssertionError(result)
report('PASS 22 profile/storage checks on signed release')
(root/'dist/screenshots').mkdir(exist_ok=True)
for module in ['probe','probe2']:
 report(call('install','-r',str(root/f'{module}/build/outputs/apk/debug/{module}-debug.apk'),timeout=360).strip())
result=sh('am instrument -w -r -e seed_profiles true dev.kolmapper.test/dev.kolmapper.ProfileInstrumentation',timeout=360)
if 'PROFILE FIXTURES SEEDED' not in result:raise AssertionError(result)
sh('settings put secure enabled_accessibility_services dev.kolmapper/.MapperService\nsettings put secure accessibility_enabled 1')
report('QA games installed and profile fixtures seeded')
configs=[('tablet-dark',1280,800,1.0,'dark'),('tablet-light',1280,800,1.0,'light'),('phone-dark',393,852,1.0,'dark'),('phone-large-text',393,852,1.3,'light'),('compact-landscape',800,360,1.0,'dark')]
for name,w,h,scale,theme in configs:
 sh(f'wm size {w}x{h}\nwm density 160\nsettings put system font_scale {scale}')
 result=sh(f'am instrument -w -r -e layout_only true -e theme {theme} -e screenshot vektor-{name} dev.kolmapper.test/dev.kolmapper.ProfileInstrumentation',timeout=360)
 (root/f'dist/vektor11-api29-{name}-results.log').write_text(result)
 if 'LAYOUT CHECKS PASSED' not in result:raise AssertionError(result)
 call('pull',f'/sdcard/Android/data/dev.kolmapper/files/vektor-{name}.png',str(root/f'dist/screenshots/vektor-api29-{name}.png'))
 report('PASS '+name+' unclipped controls and non-scrolling main page')
sh('wm size 1280x800\nsettings put system font_scale 1.0\nsettings put secure enabled_accessibility_services dev.kolmapper/.MapperService\nsettings put secure accessibility_enabled 1')
for screen in ['picker','editor','settings']:
 result=sh(f'am instrument -w -r -e layout_only true -e theme dark -e screen {screen} -e screenshot vektor-{screen} dev.kolmapper.test/dev.kolmapper.ProfileInstrumentation',timeout=360)
 (root/f'dist/vektor11-api29-{screen}-results.log').write_text(result)
 if 'LAYOUT CHECKS PASSED' not in result:raise AssertionError(result)
 call('pull',f'/sdcard/Android/data/dev.kolmapper/files/vektor-{screen}.png',str(root/f'dist/screenshots/vektor-api29-{screen}.png'))
 report('PASS '+screen+' native UI capture')
report('ALL DESIGN QA CHECKS PASSED')
