package dev.kolmapper;
import android.app.*;import android.os.*;import android.content.*;import android.view.*;import android.graphics.*;
import java.util.*;
/** Real Android output regression tests. Input state is driven directly, output is recorded by
 * the independent foreground probe app. Native HID routing is tested separately. */
public class EngineInstrumentation extends MapperInstrumentation {
 boolean seedOnly,seedStick=true;
 @Override public void onCreate(Bundle args){seedOnly="true".equals(args.getString("seed_only"));seedStick=!"false".equals(args.getString("seed_stick"));super.onCreate(args);}
 @Override String events(){try{return super.events();}catch(Exception e){throw new RuntimeException(e);}}
 void press(int key,boolean down){main(()->{long t=SystemClock.uptimeMillis();service().onKeyEvent(new KeyEvent(t,t,down?0:1,key,0,0,77,0,0,InputDevice.SOURCE_GAMEPAD));return null;});}
 int occurrences(String text,String needle){return (text.length()-text.replace(needle,"").length())/needle.length();}
 void reset()throws Exception{main(()->{service().stopAll();return null;});waitFor(()->!main(()->service().busy)&&main(()->service().contacts.isEmpty()),"output queue drained");shell("content delete --uri content://dev.kolmapper.probe/events");main(()->{service().armed=true;return null;});}
 void saveShot(String name)throws Exception{screenshot(name);}
 @Override public void onStart(){Bundle result=new Bundle();try{
  if(seedOnly){
   List<Binding> list=new ArrayList<>();int[] keys={21,22,96,99,105,-100},xs={200,420,650,850,1080,200};
   for(int i=0;i<keys.length-(seedStick?0:1);i++){Binding b=new Binding(keys[i],xs[i],600,1280,800,80,i<2?1:0);b.hold=i<2||i==5;b.duration=25;b.radius=40;list.add(b);}
   Profiles.select(getTargetContext(),"dev.kolmapper.probe");Profiles.ready(getTargetContext(),"dev.kolmapper.probe",true);
   Binding.save(getTargetContext(),list);
   boolean committed=getTargetContext().getSharedPreferences("mapper",0).edit().putBoolean("dark",true).putInt(ProfileRules.key("dev.kolmapper.probe","orbX"),16).putInt(ProfileRules.key("dev.kolmapper.probe","orbY"),80).commit();
   result.putString("stream","SEED committed="+committed+" loaded="+Binding.load(getTargetContext()).size()+"\n");finish(Activity.RESULT_OK,result);return;
  }

  for(int attempt=0;attempt<3&&automation==null;attempt++){automation=getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);if(automation==null)Thread.sleep(1000);}
  if(automation==null)throw new IllegalStateException("Emulator UiAutomation connection unavailable");
  android.accessibilityservice.AccessibilityServiceInfo info=automation.getServiceInfo();info.flags&=~android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;automation.setServiceInfo(info);
  shell("settings put secure immersive_mode_confirmations confirmed");
  shell("settings put secure enabled_accessibility_services ''");shell("settings put secure accessibility_enabled 0");
  shell("settings put secure enabled_accessibility_services dev.kolmapper/.MapperService");shell("settings put secure accessibility_enabled 1");
  waitFor(()->MapperService.current()!=null,"release accessibility service bound");
  main(()->{List<Binding> list=new ArrayList<>();int[] keys={21,22,96,99,105},xs={200,420,650,850,1080};for(int i=0;i<keys.length-(seedStick?0:1);i++){Binding b=new Binding(keys[i],xs[i],600,1280,800,80,i<2?1:0);b.duration=16;list.add(b);}Binding.save(getTargetContext(),list);service().reload();return null;});
  shell("am start -W -n dev.kolmapper.probe/.ProbeActivity");Thread.sleep(1000);reset();
  press(22,true);waitFor(()->events().contains("DOWN,RIGHT"),"right produces a real touch in separate app");Thread.sleep(250);
  check(main(()->service().states.get(22).held),"stationary hold survives repeated continuations");
  press(22,false);waitFor(()->events().contains("active=0"),"right release lifts finger (sticky direction regression)");
  check(events().contains("UP,RIGHT"),"right ends with UP rather than orphaned touch");
  reset();press(21,true);press(22,true);press(96,true);press(99,true);press(105,true);Thread.sleep(650);
  press(96,false);press(99,false);press(105,false);press(21,false);press(22,false);
  waitFor(()->events().contains("active=0"),"all five simultaneous buttons release");String trace=events();
  for(String name:new String[]{"A","X","R2"})check(occurrences(trace,",DOWN,"+name+",")>=2,name+" repeats alongside both directions");
  for(String name:new String[]{"LEFT","RIGHT"})check(occurrences(trace,",DOWN,"+name+",")==1&&occurrences(trace,",UP,"+name+",")==1&&!trace.contains(",CANCEL,"+name+","),name+" stays continuous while macros run");
  String settled=events();Thread.sleep(250);check(events().equals(settled),"no late macro after physical release");
  for(int i=0;i<5;i++){reset();press(21,true);Thread.sleep(70);press(21,false);waitFor(()->events().contains("active=0"),"left repress/release cycle "+i);}
  reset();press(21,true);press(22,true);Thread.sleep(100);press(21,false);waitFor(()->events().contains("active=1"),"releasing left leaves right independently held");press(22,false);waitFor(()->events().contains("active=0"),"right then releases independently");
  reset();press(22,true);Thread.sleep(100);main(()->{service().showPanel();return null;});waitFor(()->events().contains("active=0"),"opening editor releases held direction");
  check(main(()->!service().armed&&service().backdrop!=null),"editor pauses with separate visual layer");saveShot("rota-editor");
  main(()->{service().collapse();return null;});check(main(()->service().armed&&service().panel==null),"closing editor automatically starts controls");
  reset();main(()->{Binding b=new Binding(AnalogInput.LEFT_STICK,200,600,1280,800,80,0);b.hold=true;b.radius=40;List<Binding> list=Binding.load(getTargetContext());list.add(b);Binding.save(getTargetContext(),list);service().reload();service().armed=true;service().updateStick(AnalogInput.LEFT_STICK,77,new float[]{1,0});return null;});
  waitFor(()->events().contains("DOWN,LEFT"),"analog stick creates real touch");
  check(main(()->Math.abs(service().contacts.get(AnalogInput.LEFT_STICK).x-240)<=1),"analog position follows configured radius");
  main(()->{service().updateStick(AnalogInput.LEFT_STICK,77,new float[]{0,0});return null;});waitFor(()->events().contains("active=0"),"analog neutral lifts finger");
  check(!main(()->new Ui(getTargetContext()).dark)==!getTargetContext().getSharedPreferences("mapper",0).getBoolean("dark",true),"theme preference consistent");
  MainActivity activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Thread.sleep(500);saveShot("rota-main-dark");
  main(()->{activity.theme(false);return null;});Thread.sleep(800);saveShot("rota-main-light");
  result.putString("stream",report+"\nENGINE CHECKS PASSED\n");finish(Activity.RESULT_OK,result);
 }catch(Throwable e){result.putString("stream",report+"\nFAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
