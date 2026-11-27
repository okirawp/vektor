#!/usr/bin/env python3
"""Test the release app as a regular app, with Linux input events rather than UiAutomation keys."""
import subprocess,time,re,pathlib,xml.etree.ElementTree as ET,json,io,atexit
from PIL import Image
root=pathlib.Path(__file__).resolve().parents[1];adb='/workspace/tools/android-sdk/platform-tools/adb';report=[]
def command(*args,timeout=120,check=True):
 p=subprocess.run([adb,'-s','emulator-5554',*args],capture_output=True,text=True,timeout=timeout)
 if check and p.returncode:raise RuntimeError(p.stdout+p.stderr)
 return p.stdout

def shell(text,**kwargs):return command('shell',text,**kwargs)
def check(value,label):
 if not value:raise AssertionError(label)
 report.append('PASS '+label);print(report[-1],flush=True)
def shot(name):
 with open(root/'dist/screenshots'/name,'wb') as file:subprocess.run([adb,'-s','emulator-5554','exec-out','screencap','-p'],stdout=file,check=True)
def query():return shell('content query --uri content://dev.kolmapper.probe/events')
def clear():shell('content delete --uri content://dev.kolmapper.probe/events')
def wait_for(fn,label,seconds=60):
 end=time.monotonic()+seconds
 while time.monotonic()<end:
  if fn():check(True,label);return
  time.sleep(.15)
 check(False,label)

devices=shell('getevent -pl');device=None;current=None
for line in devices.splitlines():
 match=re.search(r'add device \d+: (/dev/input/event\d+)',line)
 if match:current=match.group(1)
 if '"KolMapper Test Gamepad"' in line:device=current
check(device is not None,'native Linux gamepad is present')
scans={'LEFT':105,'RIGHT':106,'A':304,'X':307,'R2':313}
def key(name,down):shell(f'sendevent {device} 1 {scans[name]} {1 if down else 0} && sendevent {device} 0 0 0')
def release_fixture():
 try:
  ops=[f'sendevent {device} 1 {code} 0' for code in scans.values()]
  ops += [f'sendevent {device} 3 0 0',f'sendevent {device} 3 1 0',f'sendevent {device} 0 0 0']
  command('shell','; '.join(ops),timeout=15,check=False)
 except Exception:pass
atexit.register(release_fixture)

# Restart only the emulator's unrelated System UI process to clear stale boot ANR dialogs.

shell("settings put secure enabled_accessibility_services ''")
shell('settings put secure accessibility_enabled 0')
shell('settings put secure enabled_accessibility_services dev.kolmapper/.MapperService')
shell('settings put secure accessibility_enabled 1');time.sleep(1)
shell('am start -W -n dev.kolmapper.probe/.ProbeActivity -f 0x10008000');time.sleep(.5)
shell('input keyevent KEYCODE_WAKEUP');shell('wm dismiss-keyguard');shell('svc power stayon true')
xml=ET.fromstring(shell('cat /data/user/0/dev.kolmapper/shared_prefs/mapper.xml'))
values={e.attrib['name']:e.attrib.get('value',e.text) for e in xml}
x=int(values.get('orbX',16))+28;y=int(values.get('orbY',80))+28
dark=values.get('dark','true')=='true'
surface=(29,35,33) if dark else (255,253,248)
accent=(255,137,96) if dark else (187,62,28)
print('orb center',x,y,flush=True)
def pixel(px,py):
 data=subprocess.run([adb,'-s','emulator-5554','exec-out','screencap','-p'],capture_output=True,check=True,timeout=30).stdout
 return Image.open(io.BytesIO(data)).convert('RGB').getpixel((px,py))
def near(actual,expected):return max(abs(a-b) for a,b in zip(actual,expected))<=4
tracking=100
def orb():
 global tracking
 tracking+=1;rx=round(x*32768/1280);ry=round(y*32768/800)
 ops=[(3,47,0),(3,57,tracking),(3,55,0),(3,53,rx),(3,54,ry),(3,58,512),(0,0,0)]
 shell(' && '.join(f'sendevent /dev/input/event2 {typ} {code} {val}' for typ,code,val in ops))
 time.sleep(.05);shell('sendevent /dev/input/event2 3 57 -1 && sendevent /dev/input/event2 0 0 0');time.sleep(.2)
wait_for(lambda:near(pixel(x+20,y),surface),'round launcher is visible automatically after service connection')
wait_for(lambda:near(pixel(10,400),(20,43,53)),'target application picture is unobstructed')
for n in scans:key(n,False)
orb();wait_for(lambda:not near(pixel(10,400),(20,43,53)),'editing adds a separate visual layer');shot('host-editor.png');clear();key('RIGHT',True);time.sleep(.25);key('RIGHT',False)
check('DOWN,RIGHT' not in query(),'editor pauses mapped controller touches')
orb();wait_for(lambda:near(pixel(x+20,y),accent),'closing the round icon starts play without a start button');wait_for(lambda:near(pixel(10,400),(20,43,53)),'play restores the game picture');shot('host-play.png');clear();key('RIGHT',True)
wait_for(lambda:'DOWN,RIGHT' in query(),'native gamepad generates a real touch in the target app')
key('A',True);key('X',True);key('R2',True)
def macros_ready():
 trace=query();return all(trace.count(',DOWN,'+n+',')>=100 for n in ['A','X','R2'])
wait_for(macros_ready,'A X R2 each repeat at least 100 times with the direction held',seconds=180)
for n in ['A','X','R2']:key(n,False)
key('RIGHT',False);wait_for(lambda:'active=0' in query(),'all contacts lift after the physical releases');mixed=query();(root/'dist/mixed-input-trace.txt').write_text(mixed)
for n in ['A','X','R2']:check(mixed.count(',DOWN,'+n+',')>=100,f'{n} produces repeated touches while held')
check(mixed.count(',DOWN,RIGHT,')==1 and mixed.count(',UP,RIGHT,')==1 and ',CANCEL,RIGHT,' not in mixed,'direction stays continuously down during all three macros')
check('active=0' in mixed,'releasing buttons lifts every touch')
settled=query();time.sleep(.3);check(query()==settled,'no repeated touches remain after release')
clear();key('LEFT',True);key('RIGHT',True);time.sleep(.15);key('LEFT',False)
wait_for(lambda:'active=1' in query(),'releasing left preserves the independently held right contact')
key('RIGHT',False);wait_for(lambda:'active=0' in query(),'right releases independently')
key('RIGHT',True);wait_for(lambda:'active=1' in query(),'active direction before opening the editor')
orb();wait_for(lambda:'active=0' in query(),'opening the editor releases the active contact');key('RIGHT',False)
orb();key('RIGHT',True);wait_for(lambda:'active=1' in query(),'active contact exists before controller disconnection')
key('RIGHT',False);wait_for(lambda:'active=0' in query(),'release before analog test')
# Exercise analog input through the kernel, InputReader and accessibility motion delivery.
clear();shell(f'sendevent {device} 3 0 32767 && sendevent {device} 0 0 0')
wait_for(lambda:'DOWN,LEFT' in query(),'native analog axis produces a real mapped joystick touch')
shell(f'sendevent {device} 3 0 0 && sendevent {device} 0 0 0')
wait_for(lambda:'active=0' in query(),'native analog neutral releases the joystick contact')
key('RIGHT',True);wait_for(lambda:'active=1' in query(),'active contact restored before disconnection')
pid=shell('cat /data/local/tmp/kolmapper-gamepad.pid').strip();check(bool(re.fullmatch(r'\d+',pid)),'virtual controller daemon PID is valid')
check('kolmapper-gamepad' in shell(f'cat /proc/{pid}/cmdline'),'disconnect targets only the test controller')
shell('kill '+pid);wait_for(lambda:'active=0' in query(),'native device removal releases the contact')
(root/'dist/host-native-results.txt').write_text('\n'.join(report)+'\nALL CHECKS PASSED\n')
print('ALL CHECKS PASSED',flush=True)
