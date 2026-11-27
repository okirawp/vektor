#!/usr/bin/env python3
"""Verify real touchscreen icon taps across motion-capture mode changes."""
import subprocess,pathlib,time
root=pathlib.Path(__file__).resolve().parents[1];adb='/workspace/tools/android-sdk/platform-tools/adb';lines=[]
def sh(command):return subprocess.check_output([adb,'-s','emulator-5554','shell',command],text=True,timeout=90)
def state():return sh('dumpsys activity service dev.kolmapper/.MapperService')
def wait(text,label):
 end=time.monotonic()+30
 while time.monotonic()<end:
  if text in state():lines.append('PASS '+label);print(lines[-1],flush=True);return
  time.sleep(.1)
 raise AssertionError(label+'\n'+state())
tracking=300

def tap():
 global tracking
 tracking+=1;x=round(44*32768/1280);y=round(108*32768/800)
 ops=[(3,47,0),(3,57,tracking),(3,55,0),(3,53,x),(3,54,y),(3,58,512),(0,0,0)]
 sh(' && '.join(f'sendevent /dev/input/event2 {t} {c} {v}' for t,c,v in ops));time.sleep(.05)
 sh('sendevent /dev/input/event2 3 57 -1 && sendevent /dev/input/event2 0 0 0')
# Wait for the preceding UI capture to finish before taking over the foreground.
for attempt in range(180):
 if (root/'dist/screenshots/rota-main-dark.png').stat().st_mtime>(root/'dist/analog-direction-results.log').stat().st_mtime:break
 time.sleep(1)
else:raise RuntimeError('Previous UI capture is still pending')
sh('am start -W -n dev.kolmapper.probe/.ProbeActivity -f 0x10008000')
# Clear a physical pointer left by the previously failing prototype in this emulator.
subprocess.run([adb,'-s','emulator-5554','shell','input','motionevent','CANCEL','0','0'],capture_output=True,timeout=90)
if 'editing=true' in state():tap()
# First establish edit then play, including public source reconfiguration.
tap();wait('editing=true','physical touchscreen tap opens the editor')
tap();wait('armed=true','physical touchscreen tap closes editor and starts play')
tap();wait('editing=true','single physical tap from playing enters editor despite motion-source gating')
tap();wait('armed=true','single physical tap exits editor again')
sh('am start -W -n dev.kolmapper/.MainActivity');wait('armed=false','returning to the main app pauses input')
(root/'dist/physical-orb-results.log').write_text('\n'.join(lines)+'\nALL CHECKS PASSED\n');print('ALL CHECKS PASSED',flush=True)
