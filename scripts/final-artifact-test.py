#!/usr/bin/env python3
"""Short check of exact signed deliverable and real task removal; dedicated emulator only."""
from pathlib import Path
import sys
source=Path(__file__).with_name('session-experience-test.py').read_text()
exec(source.split('\nsh("settings put secure enabled_accessibility_services',1)[0])
if '--resume-ui' not in sys.argv and '--resume-picker' not in sys.argv:
 if '--resume' not in sys.argv:
  sh("settings put secure enabled_accessibility_services ''");sh('settings put secure accessibility_enabled 0');sh('settings put secure enabled_accessibility_services dev.kolmapper/.MapperService');sh('settings put secure accessibility_enabled 1')
  main();wait('session=false','final signed APK starts with no session');check(overlay_count()==0,'final signed APK starts with no overlay')
  start_session();check(overlay_count()==1,'playing launcher uses exactly one overlay');tap();wait('editing=true','final editor opens');check(overlay_count()==2,'editor and all six controls share one window plus the orb');(root/'dist/rota06-ui-state.txt').write_text(state());tap();wait('armed=true','final editor closes and arms immediately');check(overlay_count()==1,'closing editor detaches its window')
  # Remove the real framework task while Home is foreground, exactly as Recents does.
  main();sh('input keyevent KEYCODE_HOME');wait('session=true','Home preserves the intentional session')
  recents=sh('dumpsys activity recents')
  match=re.search(r'Task\{[^}\n]*#(\d+)[^}\n]*A=\d+:dev\.kolmapper(?:\s|\})',recents)
  if not match:raise AssertionError('Rota own task missing: '+recents)
  removed=sh('CLASSPATH=/data/local/tmp/rota-remove-task.jar app_process /system/bin RemoveTask '+match.group(1))
  check('removed=true' in removed,'framework removes Rota task')
  wait('session=false','task removal ends the session');check(overlay_count()==0,'task removal removes every overlay')
 else:
  lines=[line for line in (root/'dist/rota06-task-results.log').read_text().splitlines() if line.startswith('PASS ')]
  sh("settings put secure enabled_accessibility_services ''");sh('settings put secure accessibility_enabled 0');sh('settings put secure enabled_accessibility_services dev.kolmapper/.MapperService');sh('settings put secure accessibility_enabled 1')
 start_session();sh('settings put global animator_duration_scale 0');tap();wait('editing=true','editor works when system animations are disabled');tap();wait('armed=true','play works when system animations are disabled');sh('settings put global animator_duration_scale 1')
 # Short native regression on the exact no-close-delay artifact.
 raw=sh('getevent -pl');device=None;path=None
 for line in raw.splitlines():
  m=re.search(r'add device \d+: (/dev/input/event\d+)',line)
  if m:path=m.group(1)
  if '\"KolMapper Test Gamepad\"' in line:device=path
 check(device is not None,'exact APK native controller connected')
 def keys(down):sh(' && '.join(f'sendevent {device} 1 {scan} {down}' for scan in [106,304,307,313])+f' && sendevent {device} 0 0 0')
 def query():return sh('content query --uri content://dev.kolmapper.probe/events')
 keys(0);sh('content delete --uri content://dev.kolmapper.probe/events');keys(1)
 end=time.monotonic()+90
 while time.monotonic()<end:
  trace=query()
  if ',DOWN,RIGHT,' in trace and all(trace.count(',DOWN,'+name+',')>=3 for name in ['A','X','R2']):break
  time.sleep(.2)
 else:raise AssertionError('native simultaneous input missing')
 check(True,'exact APK movement and three macros work together');keys(0)
 end=time.monotonic()+60
 while time.monotonic()<end:
  trace=query()
  if 'active=0' in trace:break
  time.sleep(.2)
 else:raise AssertionError('native release was not clean')
 check(trace.count(',DOWN,RIGHT,')==1 and trace.count(',UP,RIGHT,')==1,'exact APK movement remains continuous and releases')
 for name in ['A','X','R2']:check(trace.count(',DOWN,'+name+',')==trace.count(',UP,'+name+','),'exact APK '+name+' releases every touch')
 (root/'dist/rota06-exact-input-trace.txt').write_text(trace)
else:
 lines=[line for line in (root/'dist/rota06-ui-resume-prefix.log').read_text().splitlines() if line.startswith('PASS ')]
if '--resume-picker' not in sys.argv:
 # Lock during settings, including native dialog and editor, not just the playing orb.
 if 'session=true' not in state():start_session()
 if 'editing=true' not in state():tap();wait('editing=true','editor is open before dialog lock test')
 if overlay_count()!=3:
  # Wait for the actual badge layout, rather than just the editing state flag.
  from PIL import Image
  import io
  end=time.monotonic()+90
  while time.monotonic()<end:
   picture=Image.open(io.BytesIO(call('exec-out','screencap','-p'))).convert('RGB')
   if all(abs(a-b)<3 for a,b in zip(picture.getpixel((640,585)),(29,35,33))):break
   time.sleep(.2)
  else:raise AssertionError('A badge was not rendered')
  tap(650,600)
 end=time.monotonic()+60
 while time.monotonic()<end:
  if overlay_count()==3:break
  time.sleep(.2)
 else:raise AssertionError('settings dialog window did not appear')
 check(True,'settings dialog adds its own overlay window')
 sh('input keyevent KEYCODE_SLEEP');wait('session=false','lock ends final APK session');check(overlay_count()==0,'lock removes editor, icon and settings windows')
 unlock_fixture();check('session=false' in state() and overlay_count()==0,'unlock leaves the final APK inactive')
# Lock while the fullscreen point picker owns the screen.
start_session();tap();wait('editing=true','editor opens for point picker test');tap(210,230)
raw=sh('getevent -pl');device=None;path=None
for line in raw.splitlines():
 m=re.search(r'add device \d+: (/dev/input/event\d+)',line)
 if m:path=m.group(1)
 if '\"KolMapper Test Gamepad\"' in line:device=path
check(device is not None,'native controller available for learning')
sh(f'sendevent {device} 1 304 1 && sendevent {device} 0 0 0');sh(f'sendevent {device} 1 304 0 && sendevent {device} 0 0 0')
from PIL import Image
import io
end=time.monotonic()+60
while time.monotonic()<end:
 shot=call('exec-out','screencap','-p');color=Image.open(io.BytesIO(shot)).convert('RGB').getpixel((10,400))
 if all(abs(a-b)<=2 for a,b in zip(color,(34,58,63))):break
 time.sleep(.2)
else:raise AssertionError('point picker did not appear: '+str(color))
check(True,'native button learning opens point picker')
(root/'dist/screenshots/rota06-picker.png').write_bytes(shot)
sh('input keyevent KEYCODE_SLEEP');wait('session=false','lock ends point picker session');check(overlay_count()==0,'lock removes fullscreen point picker')
unlock_fixture();check('session=false' in state() and overlay_count()==0,'unlock after point picker remains inactive')
(root/'dist/rota06-final-artifact-results.log').write_text('\n'.join(lines)+'\nALL CHECKS PASSED\n');print('ALL CHECKS PASSED',flush=True)
