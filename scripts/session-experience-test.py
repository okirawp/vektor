#!/usr/bin/env python3
"""Release lifecycle/real touchscreen QA. Only run on the dedicated emulator."""
import subprocess,time,pathlib,re,xml.etree.ElementTree as ET
root=pathlib.Path(__file__).resolve().parents[1];adb='/workspace/tools/android-sdk/platform-tools/adb';lines=[]
def call(*args):
 r=subprocess.run([adb,'-s','emulator-5554',*args],capture_output=True,timeout=120)
 if r.returncode:raise RuntimeError(r.stdout.decode(errors='replace')+r.stderr.decode(errors='replace'))
 return r.stdout

def sh(c):return call('shell',c).decode()
def state():return sh('dumpsys activity service dev.kolmapper/.MapperService')
def check(ok,label):
 if not ok:raise AssertionError(label+'\n'+state())
 lines.append('PASS '+label);print(lines[-1],flush=True)
def wait(text,label,seconds=45):
 end=time.monotonic()+seconds
 while time.monotonic()<end:
  if text in state():check(True,label);return
  time.sleep(.2)
 check(False,label)
def screenshot(name):
 p=root/'dist/screenshots'/name;p.parent.mkdir(exist_ok=True);p.write_bytes(call('exec-out','screencap','-p'))
def main():sh('am start -W -n dev.kolmapper/.MainActivity')
def click_text(text):
 # UiAutomation suppresses real accessibility services. Find the rendered CTA instead.
 if text!='Oyunda kullan':raise ValueError(text)
 from PIL import Image
 import io
 end=time.monotonic()+90
 while time.monotonic()<end:
  shot=Image.open(io.BytesIO(call('exec-out','screencap','-p'))).convert('RGB')
  colors=[(255,137,96),(187,62,28)]
  rows=[]
  for y in range(250,min(650,shot.height)):
   if any(all(all(abs(a-b)<4 for a,b in zip(shot.getpixel((x,y)),color)) for x in (400,880)) for color in colors):rows.append(y)
  if len(rows)>30:tap(640,rows[len(rows)//2]);return
  time.sleep(.2)
 raise AssertionError('Main screen CTA was not rendered; check emulator display/SystemUI')
tracking=700
def tap(x=44,y=108):
 global tracking
 tracking+=1;x=round(x*32768/1280);y=round(y*32768/800)
 ops=[(3,47,0),(3,57,tracking),(3,55,0),(3,53,x),(3,54,y),(3,58,512),(0,0,0)]
 sh(' && '.join(f'sendevent /dev/input/event2 {t} {c} {v}' for t,c,v in ops));time.sleep(.08)
 sh('sendevent /dev/input/event2 3 57 -1 && sendevent /dev/input/event2 0 0 0')
def unlock_fixture():
 # Swipe the unsecured emulator lock screen; never assume wm dismiss-keyguard worked.
 sh('input keyevent KEYCODE_WAKEUP')
 policy=sh('dumpsys window policy')
 if re.search(r'\bshowing=true\b',policy):
  global tracking
  tracking+=1;x=round(800*32768/1280)
  ops=[(3,47,0),(3,57,tracking),(3,55,0),(3,53,x),(3,54,round(700*32768/800)),(3,58,512),(0,0,0)]
  commands=[f'sendevent /dev/input/event2 {t} {c} {v}' for t,c,v in ops]
  for y in range(650,99,-50):commands.extend([f'sendevent /dev/input/event2 3 54 {round(y*32768/800)}','sendevent /dev/input/event2 0 0 0','sleep .04'])
  commands.extend(['sendevent /dev/input/event2 3 57 -1','sendevent /dev/input/event2 0 0 0'])
  sh(' && '.join(commands))
 end=time.monotonic()+90
 while time.monotonic()<end:
  if not re.search(r'\bshowing=true\b',sh('dumpsys window policy')):return
  time.sleep(.2)
 raise AssertionError('emulator keyguard is still showing')
def start_session():
 wait('ROTA session=','mapper service is connected',90)
 unlock_fixture();main()
 # A keyguard/task transition may still consume the first touch on slow TCG.
 for attempt in range(3):
  click_text('Oyunda kullan')
  end=time.monotonic()+15
  while time.monotonic()<end:
   if 'session=true' in state():
    check(True,'user explicitly opens a session')
    sh('am start -W -n dev.kolmapper.probe/.ProbeActivity -f 0x10008000');return
   time.sleep(.2)
 check(False,'user explicitly opens a session')
def overlay_count():
 # A11y windows in WindowManager dumps contain the service class plus type in attrs.
 return len(re.findall(r'ty=ACCESSIBILITY_OVERLAY',sh('dumpsys window windows')))
sh("settings put secure enabled_accessibility_services ''");sh('settings put secure accessibility_enabled 0')
sh('settings put secure enabled_accessibility_services dev.kolmapper/.MapperService');sh('settings put secure accessibility_enabled 1')
main();wait('session=false','service reconnect is inactive');check(overlay_count()==0,'inactive service draws no overlay')
screenshot('rota06-main-dark.png')
start_session();tap();wait('editing=true','real touchscreen opens editor');screenshot('rota06-editor.png')
tap();wait('armed=true','closing editor enables control')
initial=state();builds=int(re.search('editorBuilds=(\\d+)',initial).group(1))
for i in range(3):
 tap();wait('editing=true',f'warm editor open {i+1}');tap();wait('armed=true',f'warm editor close {i+1}')
check(int(re.search('editorBuilds=(\\d+)',state()).group(1))==builds,'warm editor opens reuse the menu instead of rebuilding it')
# Genuine evdev reports, not UiAutomation injection.
raw=sh('getevent -pl');device=None;path=None
for line in raw.splitlines():
 m=re.search(r'add device \d+: (/dev/input/event\d+)',line)
 if m:path=m.group(1)
 if '\"KolMapper Test Gamepad\"' in line:device=path
check(device is not None,'native controller is connected')
def events(ops):sh(' && '.join(f'sendevent {device} {t} {c} {v}' for t,c,v in ops+[(0,0,0)]))
def query():return sh('content query --uri content://dev.kolmapper.probe/events')
def wait_query(fn,label):
 end=time.monotonic()+90
 while time.monotonic()<end:
  if fn(query()):check(True,label);return
  time.sleep(.2)
 check(False,label)
events([(1,k,0) for k in [105,106,304,307,313]]+[(3,0,0),(3,1,0)])
sh('content delete --uri content://dev.kolmapper.probe/events')
events([(1,k,1) for k in [106,304,307,313]])
wait_query(lambda s:',DOWN,RIGHT,' in s and all(s.count(',DOWN,'+n+',')>=10 for n in ['A','X','R2']),'continuous movement and three macros work simultaneously')
events([(1,k,0) for k in [106,304,307,313]])
wait_query(lambda s:'active=0' in s,'all four contacts release')
trace=query();(root/'dist/rota06-input-trace.txt').write_text(trace)
check(trace.count(',DOWN,RIGHT,')==1 and trace.count(',UP,RIGHT,')==1 and ',CANCEL,RIGHT,' not in trace,'movement stays continuous while macros run')
for name in ['A','X','R2']:check(trace.count(',DOWN,'+name+',')==trace.count(',UP,'+name+','),name+' DOWN/UP balanced')
sh('content delete --uri content://dev.kolmapper.probe/events')
events([(3,0,32767),(1,304,1)])
wait_query(lambda s:',DOWN,LEFT,' in s and s.count(',DOWN,A,')>=2,'analog and macro work together')
events([(3,0,0),(1,304,0)]);wait_query(lambda s:'active=0' in s,'neutral analog releases')
# Lock while holding movement: no dangling contact and no silent resumption.
events([(1,106,1)]);wait_query(lambda s:'active=1' in s,'movement is held before screen off')
sh('input keyevent KEYCODE_SLEEP');wait('session=false','screen off ends session')
check(overlay_count()==0,'lock screen contains no Rota overlay')
events([(1,106,0)])
sh('input keyevent KEYCODE_WAKEUP');sh('wm dismiss-keyguard');time.sleep(.5)
check('session=false' in state() and overlay_count()==0,'unlock never resumes session or overlay')
wait_query(lambda s:'active=0' in s,'locking releases held contact')
start_session();tap();wait('editing=true','editor opens after explicit restart')
sh('input keyevent KEYCODE_SLEEP');wait('session=false','locking editor ends session');check(overlay_count()==0,'editor and orb both removed on lock')
sh('input keyevent KEYCODE_WAKEUP');sh('wm dismiss-keyguard')
start_session();main();sh('input keyevent KEYCODE_BACK');wait('session=false','Back closes app session');check(overlay_count()==0,'Back leaves no overlay')
start_session()
# Home is deliberately allowed to keep the game session. Task removal is tested separately.
sh('am start -W -n dev.kolmapper/.MainActivity');sh('input keyevent KEYCODE_HOME')
wait('session=true','Home keeps intentional game session')
(root/'dist/rota06-experience-results.log').write_text('\n'.join(lines)+'\nALL RECORDED CHECKS PASSED\n')
print('ALL RECORDED CHECKS PASSED',flush=True)
