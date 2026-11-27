#!/usr/bin/env python3
"""Release session checks on the dedicated API 36 emulator with seeded probe profiles."""
from pathlib import Path
import time,re,io
from PIL import Image
r=Path(__file__).resolve().parents[1];__file__=str(r/'scripts/session-experience-test.py')
source=(r/'scripts/session-experience-test.py').read_text();exec(source.split('\nsh("settings put secure enabled_accessibility_services',1)[0])
def until(fn,label,seconds=100):
 end=time.monotonic()+seconds
 while time.monotonic()<end:
  if fn():check(True,label);return
  time.sleep(.3)
 check(False,label)
def playing():return 'PROFILE active=dev.kolmapper.probe foreground=true' in state() and 'armed=true' in state()
def query():return sh('content query --uri content://dev.kolmapper.probe/events')
def device():
 path=None
 for line in sh('getevent -pl').splitlines():
  m=re.search(r'add device \d+: (/dev/input/event\d+)',line)
  if m:path=m.group(1)
  if '"KolMapper Test Gamepad"' in line:return path
 raise AssertionError('missing native gamepad')
def right(path,down):sh(f'sendevent {path} 1 106 {1 if down else 0} && sendevent {path} 0 0 0')
sh("settings put secure enabled_accessibility_services ''");sh('settings put secure accessibility_enabled 0');sh('settings put secure enabled_accessibility_services dev.kolmapper/.MapperService');sh('settings put secure accessibility_enabled 1')
main();wait('ROTA session=','final APK service connects',100);screenshot('rota08-main-dark-ready.png')
for attempt in range(3):
 shot=Image.open(io.BytesIO(call('exec-out','screencap','-p'))).convert('RGB');rows=[]
 for y in range(90,shot.height-40):
  xs=[x for x in range(20,shot.width-20) if shot.getpixel((x,y)) in [(255,137,96),(187,62,28)]]
  if len(xs)>120:rows.append((y,min(xs),max(xs)))
 if rows:
  y,x1,x2=rows[len(rows)//2];sh(f'input tap {(x1+x2)//2} {y}')
 time.sleep(1)
 if 'session=true' in state():break
until(playing,'final APK starts saved controls without orb touch');sh('content delete --uri content://dev.kolmapper.probe/events');path=device();right(path,True)
until(lambda:',DOWN,RIGHT,' in query(),'final APK receives real right down');sh('kill $(cat /data/local/tmp/kolmapper-gamepad.pid)');until(lambda:'active=0' in query(),'final APK unplug lifts right');sh('/data/local/tmp/kolmapper-gamepad');until(playing,'final APK remains armed after reconnect');path=device();right(path,True);until(lambda:query().count(',DOWN,RIGHT,')>=2,'final APK right works after reconnect');right(path,False);until(lambda:'active=0' in query(),'final APK right releases normally')
sh('input keyevent KEYCODE_HOME');wait('foreground=false','final APK hides outside game');check('session=true' in state() and overlay_count()==0,'final APK home keeps session without overlay');sh('am start -W -n dev.kolmapper.probe/.ProbeActivity');until(playing,'final APK returns automatically')
sh('input keyevent KEYCODE_SLEEP');wait('session=false','final APK screen off ends session');check(overlay_count()==0,'final APK screen off has no overlay');sh('input keyevent KEYCODE_WAKEUP')
(root/'dist/rota08-final-native-results.log').write_text('\n'.join(lines)+'\nALL FINAL APK NATIVE CHECKS PASSED\n');print('ALL FINAL APK NATIVE CHECKS PASSED',flush=True)
