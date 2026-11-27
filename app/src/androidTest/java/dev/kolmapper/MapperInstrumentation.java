package dev.kolmapper;
import android.app.*;import android.os.*;import android.content.*;import android.graphics.*;import android.view.*;import android.view.accessibility.*;
import java.io.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;import java.util.function.*;

/** Runs against the release build. No test hooks or exported commands are added to the user APK. */
public class MapperInstrumentation extends Instrumentation {
 UiAutomation automation;String inputDevice;boolean uiOnly;final StringBuilder report=new StringBuilder();final Map<Integer,Long> keyTimes=new HashMap<>();
 @Override public void onCreate(Bundle arguments){super.onCreate(arguments);inputDevice=arguments.getString("input_device");uiOnly="true".equals(arguments.getString("ui_only"));start();}
 <T>T main(Callable<T> fn){if(Looper.myLooper()==Looper.getMainLooper()){try{return fn.call();}catch(Exception e){throw new RuntimeException(e);}}AtomicReference<T> out=new AtomicReference<>();AtomicReference<Throwable> error=new AtomicReference<>();runOnMainSync(()->{try{out.set(fn.call());}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new RuntimeException(error.get());return out.get();}
 void check(boolean ok,String label){if(!ok)throw new AssertionError(label);android.util.Log.i("KolMapperTests","PASS "+label);report.append("PASS ").append(label).append('\n');}
 void waitFor(BooleanSupplier condition,String label)throws Exception{long until=SystemClock.uptimeMillis()+30000;while(!condition.getAsBoolean()&&SystemClock.uptimeMillis()<until)Thread.sleep(100);check(condition.getAsBoolean(),label);}
 String shell(String cmd)throws Exception{ParcelFileDescriptor file=automation.executeShellCommand(cmd);try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(file)){return new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}}
 MapperService service(){return main(MapperService::current);}
 void key(int code,boolean down){
  int scan;switch(code){case 21:scan=105;break;case 22:scan=106;break;case 96:scan=304;break;case 99:scan=307;break;case 105:scan=313;break;default:throw new AssertionError("unknown virtual button");}
  if(down)keyTimes.put(code,SystemClock.uptimeMillis());
  try{shell("su 0 /system/bin/sendevent "+inputDevice+" 1 "+scan+" "+(down?1:0));shell("su 0 /system/bin/sendevent "+inputDevice+" 0 0 0");}catch(Exception e){throw new RuntimeException(e);}
 }
 void touch(int action,float x,float y,long time){MotionEvent e=MotionEvent.obtain(time,SystemClock.uptimeMillis(),action,x,y,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);if(!automation.injectInputEvent(e,true))throw new AssertionError("touch rejected");e.recycle();}
 void tap(float x,float y)throws Exception{long t=SystemClock.uptimeMillis();touch(MotionEvent.ACTION_DOWN,x,y,t);Thread.sleep(30);touch(MotionEvent.ACTION_UP,x,y,t);Thread.sleep(200);}
 void orb()throws Exception{int[] p=main(()->new int[]{service().orbParams.x+service().dp(28),service().orbParams.y+service().dp(28)});tap(p[0],p[1]);}
 void screenshot(String name)throws Exception{Bitmap bitmap=automation.takeScreenshot();if(bitmap==null)throw new AssertionError("screenshot unavailable");File folder=new File(getTargetContext().getExternalFilesDir(null),"tests");folder.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(folder,name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}
 String events()throws Exception{return shell("content query --uri content://dev.kolmapper.probe/events");}
 int count(String source,String token){int n=0,start=0;while((start=source.indexOf(token,start))>=0){n++;start+=token.length();}return n;}
 void clearEvents()throws Exception{shell("content delete --uri content://dev.kolmapper.probe/events");}
 List<AccessibilityNodeInfo> roots(){List<AccessibilityNodeInfo> roots=new ArrayList<>();AccessibilityNodeInfo active=automation.getRootInActiveWindow();if(active!=null)roots.add(active);for(AccessibilityWindowInfo window:automation.getWindows()){AccessibilityNodeInfo root=window.getRoot();if(root!=null)roots.add(root);}return roots;}
 boolean hasExactText(String text){for(AccessibilityNodeInfo root:roots())for(AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText(text))if(text.contentEquals(node.getText()))return true;return false;}
 boolean hasText(String text){for(AccessibilityNodeInfo root:roots())if(!root.findAccessibilityNodeInfosByText(text).isEmpty())return true;return false;}
 String focusedPackage(){try{String dump=shell("dumpsys window displays");for(String line:dump.split("\n"))if(line.contains("mFocusedWindow=")){if(line.contains("dev.kolmapper.probe/"))return "dev.kolmapper.probe";if(line.contains("dev.kolmapper/"))return "dev.kolmapper";}}catch(Exception ignored){}return "";}
 void clickText(String text)throws Exception{for(AccessibilityNodeInfo root:roots())for(AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText(text))if(node.isClickable()&&node.performAction(AccessibilityNodeInfo.ACTION_CLICK)){Thread.sleep(300);return;}throw new AssertionError("button missing: "+text);}
 @Override public void onStart(){Bundle result=new Bundle();try{
  automation=getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);check(inputDevice!=null&&inputDevice.matches("/dev/input/event[0-9]+"),"kernel virtual gamepad configured");check(shell("su 0 id").contains("uid=0"),"test-only native driver has emulator privileges");
  android.accessibilityservice.AccessibilityServiceInfo info=automation.getServiceInfo();info.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;info.flags&=~android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;automation.setServiceInfo(info);
  if(hasText("System UI isn't responding")){clickText("Wait");android.util.Log.i("KolMapperTests","ENV: dismissed emulator System UI ANR");}
  MainActivity activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
  shell("settings put secure enabled_accessibility_services ''");shell("settings put secure accessibility_enabled 0");
  shell("settings put secure enabled_accessibility_services dev.kolmapper/.MapperService");shell("settings put secure accessibility_enabled 1");
  waitFor(()->MapperService.current()!=null,"accessibility service connected alongside instrumentation");main(()->{activity.render();return null;});
  check(getTargetContext().getSharedPreferences("mapper",0).getString("upgrade_marker","").equals("preserve-existing-data"),"signed upgrade preserves previous version app data");
  List<Binding> upgraded=Binding.load(getTargetContext());check(upgraded.size()==5,"all existing mappings survived signing-key rotation");
  for(Binding b:upgraded)if(b.key==96||b.key==99||b.key==105)check(!b.hold&&b.interval==50&&b.duration==25&&b.count==0,"macro settings preserved across upgrade: "+b.key);
  check(!getTargetContext().getApplicationInfo().isProfileableByShell(),"no shell profiling");
  check((getTargetContext().getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)==0,"release APK is not debuggable");
  Thread.sleep(300);
  waitFor(()->hasText("KolMapper"),"main window rendered without a system error modal");check(!hasText("System UI isn't responding"),"no System UI ANR blocks the test");
  check(!hasExactText("Tuşların"),"main screen has no mapping list");screenshot("main-dark");
  main(()->{activity.theme(false);return null;});Thread.sleep(700);waitFor(()->hasText("● Açık"),"light theme rendered in the window");check(!new Ui(getTargetContext()).dark,"light theme persists");screenshot("main-light");
  main(()->{getTargetContext().getSharedPreferences("mapper",0).edit().putBoolean("dark",true).putInt("orbX",service().dp(16)).putInt("orbY",service().dp(80)).commit();service().restyle();return null;});
  main(()->{
   List<Binding> bindings=new ArrayList<>();int[] codes={21,22,96,99,105},xs={200,420,650,850,1080};
   android.util.DisplayMetrics d=service().dimensions();for(int i=0;i<codes.length;i++){Binding b=new Binding(codes[i],xs[i],600,d.widthPixels,d.heightPixels,50,i<2?1:0);b.duration=25;bindings.add(b);}Binding.save(getTargetContext(),bindings);service().reload();service().showLauncher();return null;
  });
  shell("am start -n dev.kolmapper.probe/.ProbeActivity");waitFor(()->focusedPackage().equals("dev.kolmapper.probe"),"touch recorder has input focus before editing");Thread.sleep(400);orb();
  check(main(()->service().panel!=null&&service().backdrop!=null&&service().editing&&!service().armed),"orb opens editor and scrim and pauses controls");
  check(main(()->service().badges.size()==5),"all five mappings visible in editor");
  check(main(()->service().bindings.get(21).hold&&service().bindings.get(22).hold&&!service().bindings.get(96).hold&&!service().bindings.get(99).hold&&!service().bindings.get(105).hold),"legacy arrows become hold; A X R2 stay macros");
  int[] before=main(()->new int[]{service().orbParams.x,service().orbParams.y,service().panelParams.x,service().panelParams.y});
  long t=SystemClock.uptimeMillis();float x=before[0]+28,y=before[1]+28;touch(MotionEvent.ACTION_DOWN,x,y,t);for(int i=1;i<=5;i++){Thread.sleep(40);touch(MotionEvent.ACTION_MOVE,x+35*i,y+25*i,t);}touch(MotionEvent.ACTION_UP,x+175,y+125,t);Thread.sleep(300);
  check(main(()->service().orbParams.x!=before[0]&&service().panelParams.x!=before[2]),"dragging orb also moves the attached menu");
  check(main(()->service().panelParams.x>=0&&service().panelParams.y>=0),"attached menu remains on screen");screenshot("editor-dark");
  main(()->{getTargetContext().getSharedPreferences("mapper",0).edit().putBoolean("dark",false).commit();service().restyle();return null;});Thread.sleep(300);screenshot("editor-light");
  main(()->{getTargetContext().getSharedPreferences("mapper",0).edit().putBoolean("dark",true).commit();service().restyle();return null;});Thread.sleep(200);orb();
  check(main(()->service().armed&&service().panel==null&&service().backdrop==null&&service().badges.isEmpty()),"closing orb alone starts play and removes editor overlays");screenshot("play");
  if(uiOnly){main(()->{service().stopAll();return null;});result.putString("stream",report.toString()+"UI CHECKS PASSED\n");finish(Activity.RESULT_OK,result);return;}
  waitFor(()->focusedPackage().equals("dev.kolmapper.probe"),"touch recorder has input focus before controller injection");clearEvents();key(22,true);Thread.sleep(500);
  String diag=main(()->"keys="+service().observedControllerEvents+", complete="+service().gesturesCompleted+", canceled="+service().gesturesCanceled+", held="+service().states.get(22).held+", busy="+service().busy+", contacts="+service().contacts.size());report.append("DIAG ").append(diag).append("\n");check(events().contains("DOWN,RIGHT"),"injected controller key generates a real Android touch ("+diag+")");
  key(96,true);key(99,true);key(105,true);Thread.sleep(650);key(96,false);key(99,false);key(105,false);Thread.sleep(180);key(22,false);Thread.sleep(250);
  String mixed=events();File traceDir=new File(getTargetContext().getExternalFilesDir(null),"tests");traceDir.mkdirs();try(FileOutputStream trace=new FileOutputStream(new File(traceDir,"mixed-input-trace.txt"))){trace.write(mixed.getBytes(java.nio.charset.StandardCharsets.UTF_8));}check(count(mixed,",DOWN,RIGHT,")==1&&count(mixed,",UP,RIGHT,")==1&&!mixed.contains(",CANCEL,RIGHT,"),"movement remains one uninterrupted contact while three macros run");
  for(String name:new String[]{"A","X","R2"})check(count(mixed,",DOWN,"+name+",")>=2,"repeated Android touches for "+name);
  check(mixed.contains("active=0"),"all pointers released after physical key ups");
  String settled=events();Thread.sleep(200);check(events().equals(settled),"no queued repeats after releasing macro keys");
  clearEvents();key(21,true);Thread.sleep(100);key(22,true);Thread.sleep(200);key(21,false);Thread.sleep(150);check(events().contains("active=1"),"releasing one direction preserves the other held contact");key(22,false);Thread.sleep(150);check(events().contains("active=0"),"second direction releases independently");
  key(22,true);Thread.sleep(120);orb();Thread.sleep(150);check(!main(()->service().armed),"opening editor stops play during a held direction");check(events().contains("active=0"),"opening editor releases active touch");key(22,false);
  // Destroy the actual Linux input device and observe Android's native removal callback.
  orb();key(22,true);Thread.sleep(150);check(events().contains("active=1"),"active touch exists before native controller removal");String pid=shell("su 0 cat /data/local/tmp/kolmapper-gamepad.pid").trim();check(pid.matches("[0-9]+"),"virtual controller daemon identified");check(shell("su 0 cat /proc/"+pid+"/cmdline").contains("kolmapper-gamepad"),"only the test controller daemon is targeted");shell("su 0 kill "+pid);waitFor(()->!main(()->service().armed),"kernel gamepad removal stops the mapper");Thread.sleep(200);check(events().contains("active=0"),"device removal releases all touch pointers");
  check(main(()->service().backdrop==null&&service().badges.isEmpty()),"no leaked editor windows after play");
  automation.setRotation(UiAutomation.ROTATION_FREEZE_90);Thread.sleep(700);
  check(!main(()->service().armed),"rotation pauses injection");orb();Thread.sleep(250);
  check(main(()->service().panelParams.x>=0&&service().panelParams.y>=0&&service().panelParams.x+service().panel.getWidth()<=service().dimensions().widthPixels),"menu remains reachable after rotation");screenshot("editor-rotated");
  automation.setRotation(UiAutomation.ROTATION_FREEZE_0);Thread.sleep(600);
  main(()->{service().stopAll();return null;});
  result.putString("stream",report.toString()+"ALL CHECKS PASSED\n");finish(Activity.RESULT_OK,result);
 }catch(Throwable error){try{main(()->{if(service()!=null)service().stopAll();return null;});for(int code:new ArrayList<>(keyTimes.keySet()))key(code,false);screenshot("failure");}catch(Throwable ignored){}result.putString("stream",report+"FAIL "+android.util.Log.getStackTraceString(error));finish(Activity.RESULT_CANCELED,result);}}
}
