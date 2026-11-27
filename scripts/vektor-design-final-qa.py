#!/usr/bin/env python3
"""Final native screenshots/layout checks after preparing the dedicated API 29 QA device."""
from pathlib import Path
import subprocess,xml.etree.ElementTree as ET,time
root=Path(__file__).resolve().parents[1];adb='/workspace/tools/android-sdk/platform-tools/adb'
def call(*args,timeout=360):
 p=subprocess.run([adb,'-s','emulator-5556',*args],capture_output=True,timeout=timeout)
 if p.returncode:raise RuntimeError(p.stdout.decode(errors='replace')+p.stderr.decode(errors='replace'))
 return p.stdout.decode(errors='replace')
def sh(cmd):return call('shell',cmd)
def bindings():
 for attempt in range(10):
  try:return {e.attrib['name']:e.text for e in ET.fromstring(sh('cat /data/user/0/dev.kolmapper/shared_prefs/mapper.xml')) if e.attrib.get('name','').endswith(('.bindings','.previous'))}
  except RuntimeError as err:
   if 'No such file' not in str(err) or attempt==9:raise
   time.sleep(.5)
baseline=bindings()
print(call('install','--no-incremental','-r',str(root/'dist/Vektor-1.1.0.apk')).strip(),flush=True)
print(call('install','--no-incremental','-r',str(root/'dist/Vektor-1.1.0-test.apk')).strip(),flush=True)
assert bindings()==baseline;print('PASS bindings preserved after signed UI update',flush=True)
sh('''stop vendor.ril-daemon
stop netmgr
stop emu_hostapd
settings put global watchdog_timeout_millis 600000
settings put system screen_off_timeout 2147483647
svc power stayon true
wm size 1280x800
wm density 160
settings put system font_scale 1.0
settings put secure enabled_accessibility_services dev.kolmapper/.MapperService
settings put secure accessibility_enabled 1
input keyevent KEYCODE_WAKEUP
wm dismiss-keyguard''')
configs=[('tablet-dark',1280,800,1.0,'dark'),('tablet-light',1280,800,1.0,'light'),('phone-dark',393,852,1.0,'dark'),('phone-large-text',393,852,1.3,'light'),('compact-landscape',800,360,1.0,'dark')]
for name,w,h,scale,theme in configs:
 sh(f'wm size {w}x{h}\nsettings put system font_scale {scale}')
 extra=' -e profile_switch true' if name=='tablet-dark' else ''
 result=sh(f'am instrument -w -r -e layout_only true -e theme {theme} -e screenshot vektor-{name}{extra} dev.kolmapper.test/dev.kolmapper.ProfileInstrumentation')
 (root/f'dist/vektor11-api29-{name}-results.log').write_text(result)
 if 'LAYOUT CHECKS PASSED' not in result:raise AssertionError(result)
 call('pull',f'/sdcard/Android/data/dev.kolmapper/files/vektor-{name}.png',str(root/f'dist/screenshots/vektor-api29-{name}.png'))
 print('PASS '+name+' visible focused UI and unclipped controls'+('; saved-game shortcuts round trip without binding changes' if extra else ''),flush=True)
sh('wm size 1280x800\nsettings put system font_scale 1.0')
for screen in ['picker','editor','settings']:
 result=sh(f'am instrument -w -r -e layout_only true -e theme dark -e screen {screen} -e screenshot vektor-{screen} dev.kolmapper.test/dev.kolmapper.ProfileInstrumentation')
 (root/f'dist/vektor11-api29-{screen}-results.log').write_text(result)
 if 'LAYOUT CHECKS PASSED' not in result:raise AssertionError(result)
 call('pull',f'/sdcard/Android/data/dev.kolmapper/files/vektor-{screen}.png',str(root/f'dist/screenshots/vektor-api29-{screen}.png'))
 print('PASS '+screen+' UI capture',flush=True)
assert bindings()==baseline;print('PASS all binding and recovery values unchanged after design QA',flush=True)
print('ALL FINAL DESIGN QA CHECKS PASSED',flush=True)
