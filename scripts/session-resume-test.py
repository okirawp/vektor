#!/usr/bin/env python3
"""0.8 regression: genuine HID reconnection and scoped system-UI resume."""
from pathlib import Path
import re,time,io
from PIL import Image
source=Path(__file__).with_name('session-experience-test.py').read_text()
exec(source.split('\nsh("settings put secure enabled_accessibility_services',1)[0])
A='dev.kolmapper.probe';B='dev.kolmapper.probe2'
def launch(pkg):sh(f'am start -W -n {pkg}/dev.kolmapper.probe.ProbeActivity')
def until(fn,label,seconds=100):
 end=time.monotonic()+seconds
 while time.monotonic()<end:
  if fn():check(True,label);return
  time.sleep(.4)
 check(False,label)
def active(pkg):return f'PROFILE active={pkg} foreground=true' in state() and 'armed=true' in state()
def query():return sh(f'content query --uri content://{A}/events')
def clear():sh(f'content delete --uri content://{A}/events')
def device():
 path=None
 for line in sh('getevent -pl').splitlines():
  m=re.search(r'add device \d+: (/dev/input/event\d+)',line)
  if m:path=m.group(1)
  if '"KolMapper Test Gamepad"' in line:return path
 return None
def send(keys,value):
 path=device();check(path is not None,'native controller exists')
 sh(' && '.join([f'sendevent {path} 1 {key} {value}' for key in keys]+[f'sendevent {path} 0 0 0']))
sh("settings put secure enabled_accessibility_services ''");sh('settings put secure accessibility_enabled 0')
sh('settings put secure enabled_accessibility_services dev.kolmapper/.MapperService');sh('settings put secure accessibility_enabled 1')
unlock_fixture();main();wait('ROTA session=','service connected',100)
for attempt in range(3):
 shot=Image.open(io.BytesIO(call('exec-out','screencap','-p'))).convert('RGB');rows=[]
 for y in range(90,shot.height-40):
  xs=[x for x in range(20,shot.width-20) if shot.getpixel((x,y)) in [(255,137,96),(187,62,28)]]
  if len(xs)>120:rows.append((y,min(xs),max(xs)))
 if rows:
  y,x1,x2=rows[len(rows)//2];tap((x1+x2)//2,y)
 time.sleep(2)
 if 'session=true' in state():break
until(lambda:active(A),'saved A profile starts armed without touching orb')
def saved_bindings():
 xml=ET.fromstring(sh('cat /data/user/0/dev.kolmapper/shared_prefs/mapper.xml'));return {e.attrib['name']:e.text for e in xml if e.attrib.get('name','').endswith('.bindings')}
before_bindings=saved_bindings();(root/"dist/rota08-bindings-baseline.txt").write_text(__import__("json").dumps(before_bindings))
if device() is None:sh('/data/local/tmp/kolmapper-gamepad')
until(lambda:device() is not None,'native gamepad hot-plug detected')
clear();send([106,304,307,313],1)
until(lambda:all(query().count(',DOWN,'+key+',')>=2 for key in ['A','X','R2']),'direction and A/X/R2 work together without opening editor')
send([106,304,307,313],0);until(lambda:'active=0' in query(),'all fingers release')
# Genuine removal while held, followed by a new evdev device.
clear();send([106],1);until(lambda:',DOWN,RIGHT,' in query(),'right held before unplug')
sh('kill $(cat /data/local/tmp/kolmapper-gamepad.pid)');until(lambda:'active=0' in query(),'unplug lifts held right')
sh('/data/local/tmp/kolmapper-gamepad');until(lambda:active(A) and device() is not None,'replug stays armed without orb touch')
clear();send([106],1);until(lambda:',DOWN,RIGHT,' in query(),'fresh right works after reconnection');send([106],0);until(lambda:'active=0' in query(),'right releases after reconnection')
# Back invokes the probe normal system navigation; session must survive on launcher.
sh('input keyevent KEYCODE_HOME');wait('foreground=false','home hides game overlay');check('session=true' in state() and overlay_count()==0,'home preserves session but shows no overlay')
launch(A);until(lambda:active(A),'returning game automatically restores orb and control');check(overlay_count()==1,'only game orb returns')
sh('cmd statusbar expand-notifications');time.sleep(1);check('session=true' in state(),'notification shade does not end session')
sh('cmd statusbar collapse');until(lambda:active(A),'closing notification shade restores play automatically')
sh('input keyevent KEYCODE_APP_SWITCH');time.sleep(1);check('session=true' in state(),'recent-app gesture retains session');tap(640,360);until(lambda:active(A),'returning from recents restores play')
sh('am start -W -n com.android.settings/.Settings');wait('foreground=false','unregistered app suspends controls');check(overlay_count()==0 and 'session=true' in state(),'unregistered app has no overlay but retains session')
launch(A);until(lambda:active(A),'return from another app needs no Rota relaunch')
# Interrupted editing must not poison persistent readiness.
tap();wait('editing=true','editor opens');sh('input keyevent KEYCODE_HOME');wait('foreground=false','leaving editor hides all editor windows');launch(A);until(lambda:active(A) and 'editing=false' in state(),'saved mapping resumes even after interrupted edit')
launch(B);until(lambda:active(B),'B uses its own profile');launch(A);until(lambda:active(A),'A mapping survives repeated app handoffs')
clear();send([304],1);until(lambda:query().count(',DOWN,A,')>=3,'A macro preserved after reconnect and app switching');send([304],0);until(lambda:'active=0' in query(),'macro stops on release')
check(saved_bindings()==before_bindings,'all saved bindings byte-for-byte unchanged after reconnect, gestures and profile switches')
screenshot('rota08-game-orb.png')
(root/'dist/rota08-session-results.log').write_text('\n'.join(lines)+'\nALL SESSION RESUME CHECKS PASSED\n');print('ALL SESSION RESUME CHECKS PASSED',flush=True)
