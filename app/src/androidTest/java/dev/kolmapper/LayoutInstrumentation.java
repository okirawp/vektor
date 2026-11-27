package dev.kolmapper;
import android.app.*;import android.os.*;import android.content.*;import android.graphics.Rect;import android.view.*;import android.widget.*;
/** Existing native layout check, with optional QA screenshots. No production test hooks. */
public class LayoutInstrumentation extends Instrumentation {
 int clicks,icons;String failure;MainActivity activity;Bundle arguments;
 @Override public void onCreate(Bundle b){arguments=b==null?new Bundle():b;super.onCreate(b);start();}
 void inspect(View v){
  if(v instanceof HorizontalScrollView)failure="The library scrolls horizontally";
  if(v instanceof TextView){String t=((TextView)v).getText().toString();if(t.contains("ROTA  /")||t.contains("Atamaların cihazında kalır"))failure="Removed footer text still present";}
  if(v instanceof ActionIcon)icons++;
  if(v.isShown()&&v.isClickable()){Rect r=new Rect();if(v.getGlobalVisibleRect(r)){if(r.width()<v.getWidth())failure="Horizontally clipped control "+v.getContentDescription();clicks++;}}
  if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)inspect(g.getChildAt(i));}
 }
 View findAction(View v,String description){if(v.isShown()&&v.isClickable()&&description.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;for(int i=0;i<group.getChildCount();i++){View found=findAction(group.getChildAt(i),description);if(found!=null)return found;}}return null;}
 java.util.Map<String,Object> bindingsSnapshot(){java.util.Map<String,Object> result=new java.util.HashMap<>();for(java.util.Map.Entry<String,?> entry:Profiles.prefs(getTargetContext()).getAll().entrySet())if(entry.getKey().endsWith(".bindings")||entry.getKey().endsWith(".previous"))result.put(entry.getKey(),entry.getValue());return result;}
 void verifyProfileShortcuts(){
  String original=Profiles.selected(getTargetContext()),other=null;for(String pkg:Profiles.known(getTargetContext()))if(!pkg.equals(original)&&getTargetContext().getPackageManager().getLaunchIntentForPackage(pkg)!=null){other=pkg;break;}
  if(other==null)throw new AssertionError("Two installed QA game profiles required");java.util.Map<String,Object> before=bindingsSnapshot();final String target=other;
  runOnMainSync(()->{
   View tile=findAction(activity.body,Profiles.label(activity,target)+" kontrol düzeni");
   if(tile==null||!tile.performClick()||!target.equals(activity.profilePage))throw new AssertionError("Profile did not open");
   activity.onBackPressed();
   if(!activity.profilePage.isEmpty()||!original.equals(Profiles.selected(activity)))throw new AssertionError("Inspecting a profile changed the selected application");
  });
  if(!before.equals(bindingsSnapshot()))throw new AssertionError("UI profile switch changed saved binding values");
 }
 @Override public void onStart(){Bundle result=new Bundle();try{
  if(arguments.containsKey("theme"))Profiles.prefs(getTargetContext()).edit().putBoolean("dark",!"light".equals(arguments.getString("theme"))).commit();
  activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();SystemClock.sleep(400);
  boolean[] focused={false};long focusDeadline=SystemClock.uptimeMillis()+5000;
  do{runOnMainSync(()->focused[0]=activity.getWindow().getDecorView().hasWindowFocus());if(focused[0])break;SystemClock.sleep(100);}while(SystemClock.uptimeMillis()<focusDeadline);
  if(!focused[0])throw new AssertionError("QA main screen is obscured by another window");
  if("true".equals(arguments.getString("profile_switch"))){verifyProfileShortcuts();waitForIdleSync();}
  runOnMainSync(()->inspect(activity.getWindow().getDecorView()));if(failure!=null)throw new AssertionError(failure);if(clicks<6)throw new AssertionError("Main controls are not visible (locked or obscured screen)");if(icons!=3||findAction(activity.body,"Ayarlar")==null||findAction(activity.body,"Yedek paylaş")==null)throw new AssertionError("Home toolbar actions missing");
  String screen=arguments.getString("screen","main");
  if("picker".equals(screen))runOnMainSync(()->activity.chooseGame(false));
  if("editor".equals(screen)||"settings".equals(screen)){
   runOnMainSync(()->activity.launchGame());long until=SystemClock.uptimeMillis()+60000;boolean[] ready={false};
   do{runOnMainSync(()->{MapperService s=MapperService.current();ready[0]=s!=null&&s.canShowUi();});if(ready[0])break;SystemClock.sleep(200);}while(SystemClock.uptimeMillis()<until);
   if(!ready[0])throw new AssertionError("Configured QA game did not become active");
   runOnMainSync(()->{MapperService s=MapperService.current();s.showPanel();if("settings".equals(screen)){Binding b=s.bindings.get(96);if(b==null)throw new AssertionError("QA A binding absent");s.openBadgeSettings(b);}});
  }
  waitForIdleSync();SystemClock.sleep("picker".equals(screen)?2500:700);
  String name=arguments.getString("screenshot");
  if(name!=null){if(!name.matches("[a-zA-Z0-9_-]+"))throw new IllegalArgumentException("Invalid screenshot name");android.graphics.Bitmap shot=getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).takeScreenshot();if(shot==null)throw new AssertionError("No screenshot returned");java.io.File file=new java.io.File(getTargetContext().getExternalFilesDir(null),name+".png");try(java.io.FileOutputStream out=new java.io.FileOutputStream(file)){shot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}shot.recycle();result.putString("screenshot",file.getAbsolutePath());}
  result.putString("stream","LAYOUT CHECKS PASSED clickable="+clicks+" icons="+icons+" width="+activity.getResources().getDisplayMetrics().widthPixels+" height="+activity.getResources().getDisplayMetrics().heightPixels+"\n");
 }catch(Throwable e){result.putString("stream","FAIL "+android.util.Log.getStackTraceString(e));}finish(Activity.RESULT_OK,result);}
}
