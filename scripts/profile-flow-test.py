#!/usr/bin/env python3
"""Two real app packages, genuine HID, and physical touchscreen. No production hooks."""
from pathlib import Path
import time,re,io,sys
from PIL import Image
source=Path(__file__).with_name('session-experience-test.py').read_text()
exec(source.split('\nsh("settings put secure enabled_accessibility_services',1)[0])
A='dev.kolmapper.probe';B='dev.kolmapper.probe2'
def launch(pkg):sh(f'am start -W -n {pkg}/dev.kolmapper.probe.ProbeActivity -f 0x10008000')
def focused(pkg):return f'PROFILE active={pkg} foreground=true' in state()
def wait_fn(fn,label,seconds=100):
 end=time.monotonic()+seconds
 while time.monotonic()<end:
  if fn():check(True,label);return
  time.sleep(.2)
 check(False,label)
def events(pkg):return sh(f'content query --uri content://{pkg}/events')
def clear(pkg):sh(f'content delete --uri content://{pkg}/events')
def start():
 unlock_fixture();main();wait('ROTA session=','profile mapper service bound',90)
 for attempt in range(3):
  shot=Image.open(io.BytesIO(call('exec-out','screencap','-p'))).convert('RGB');rows=[]
  for y in range(90,shot.height-40):
   xs=[x for x in range(20,shot.width-20) if shot.getpixel((x,y)) in [(255,137,96),(187,62,28)]]
   if len(xs)>120:rows.append((y,min(xs),max(xs)))
  if len(rows)>25:
   y,x1,x2=rows[len(rows)//2];tap((x1+x2)//2,y)
  end=time.monotonic()+15
  while time.monotonic()<end:
   if 'session=true' in state():wait_fn(lambda:focused(A),'selected game A activates its own profile');return
   time.sleep(.2)
 check(False,'main game launch opens a session')
raw=sh('getevent -pl');device=None;path=None
for line in raw.splitlines():
 m=re.search(r'add device \d+: (/dev/input/event\d+)',line)
 if m:path=m.group(1)
 if '"KolMapper Test Gamepad"' in line:device=path
check(device is not None,'native gamepad is available')
def send(ops):sh(' && '.join(f'sendevent {device} {t} {c} {v}' for t,c,v in ops+[(0,0,0)]))
if '--resume-b' not in sys.argv:
 sh("settings put secure enabled_accessibility_services ''");sh('settings put secure accessibility_enabled 0');sh('settings put secure enabled_accessibility_services dev.kolmapper/.MapperService');sh('settings put secure accessibility_enabled 1')
 start();wait('armed=true','saved A profile starts playing without an extra start button')
 send([(1,k,0) for k in [105,106,304,307,313]]+[(3,0,0),(3,1,0)])
 clear(A);clear(B);send([(1,k,1) for k in [106,304,307,313]])
 def enough():
  text=events(A);return all(text.count(',DOWN,'+n+',')>=3 for n in ['A','X','R2'])
 wait_fn(enough,'A movement and all three macros work together')
 send([(1,k,0) for k in [106,304,307,313]]);wait_fn(lambda:'active=0' in events(A),'A releases all contacts')
 trace=events(A);check(trace.count(',DOWN,RIGHT,')==1 and trace.count(',UP,RIGHT,')==1,'A movement stays continuous while macros run');(root/'dist/rota07-multi-input-trace.txt').write_text(trace)
 clear(A);clear(B);send([(1,106,1),(1,304,1)]);wait_fn(lambda:',DOWN,RIGHT,' in events(A) and ',DOWN,A,' in events(A),'A is held before switching apps')
 launch(B);wait_fn(lambda:focused(B) and 'armed=true' in state(),'switching to B automatically loads the B profile')
 wait_fn(lambda:'active=0' in events(A),'switching apps releases A contacts')
else:
 lines=[line for line in (root/'dist/rota07-scope-prefix.log').read_text().splitlines() if line.startswith('PASS ')]
 check(focused(B),'B profile is active after handoff')
# A public accessibility gesture already dispatched before an app switch cannot be targeted
# atomically at an app UID. Verify the handoff drains it, then no old input continues in B.
wait_fn(lambda:'active=0' in events(B),'B has no surviving contact after handoff')
baseline=events(B);time.sleep(.3);check(events(B)==baseline,'old input does not keep injecting after B profile activation')
send([(1,106,0),(1,304,0)]);clear(B);send([(1,304,1)])
wait_fn(lambda:',DOWN,X,' in events(B),'same physical A button uses B-specific coordinates')
time.sleep(.3);send([(1,304,0)]);wait_fn(lambda:'active=0' in events(B),'B single tap releases')
traceB=events(B);check(traceB.count(',DOWN,X,')==1 and ',DOWN,A,' not in traceB,'B keeps its own single-tap mode instead of A macro');(root/'dist/rota07-profile-b-trace.txt').write_text(traceB)
sh('am start -W -n com.android.settings/.Settings');wait('foreground=false','unregistered application pauses mapping');check(overlay_count()==0,'unregistered application has no Rota windows')
before=int(re.search(r'completed=(\d+)',state()).group(1));send([(1,304,1)]);time.sleep(.3);send([(1,304,0)]);after=int(re.search(r'completed=(\d+)',state()).group(1));check(before==after,'unregistered app receives no mapped gesture')
sh('input keyevent KEYCODE_HOME');wait('foreground=false','launcher stays outside game scope');check(overlay_count()==0,'launcher has no distracting overlay')
launch(A);wait_fn(lambda:focused(A) and 'armed=true' in state(),'returning to A restores A profile automatically');clear(A);send([(1,304,1)]);wait_fn(lambda:events(A).count(',DOWN,A,')>=3,'A original macro survives switching to B');send([(1,304,0)]);wait_fn(lambda:'active=0' in events(A),'A releases after returning')
shot=call('exec-out','screencap','-p');(root/'dist/screenshots/rota07-playing-orb.png').write_bytes(shot);pixel=Image.open(io.BytesIO(shot)).convert('RGB').getpixel((44,89));check(max(pixel)<120,'playing orb uses a subdued neutral fill')
tap();wait('editing=true','A editor opens');launch(B);wait_fn(lambda:focused(B) and 'editing=false' in state(),'switching away closes A editor instead of carrying its badges to B');check(overlay_count()==1,'B has only its own orb, no A editor or badges')
launch(A);wait_fn(lambda:focused(A),'A profile returns after leaving editor');check('armed=false' in state(),'interrupted editing does not silently start A mapping')
tap();wait('editing=true','A can resume editing its own controls');tap();wait('armed=true','orb close resumes A play immediately')
send([(1,106,1)]);wait_fn(lambda:'active=1' in events(A),'held direction exists before locking');sh('input keyevent KEYCODE_SLEEP');wait('session=false','screen lock ends the scoped session');check(overlay_count()==0,'lock removes the game orb and editor');send([(1,106,0)]);unlock_fixture();check('session=false' in state() and overlay_count()==0,'unlock never resumes game mapping')
(root/'dist/rota07-profile-flow-results.log').write_text('\n'.join(lines)+'\nALL PROFILE FLOW CHECKS PASSED\n');print('ALL PROFILE FLOW CHECKS PASSED',flush=True)
