package dev.kolmapper;
import android.app.*;import android.content.*;import android.graphics.*;import android.graphics.drawable.Drawable;import android.os.Build;import android.view.*;import android.widget.*;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;import org.robolectric.android.controller.ActivityController;import org.robolectric.shadows.ShadowAlertDialog;import java.util.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk={29,35},qualifiers="w1280dp-h800dp-land-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
public class ExperienceTest {
 Context context;
 @Before public void prepare(){context=RuntimeEnvironment.getApplication();Profiles.prefs(context).edit().clear().commit();GuideState.prefs(context).edit().clear().commit();}
 ActivityController<MainActivity> open(){return Robolectric.buildActivity(MainActivity.class).setup().visible();}
 View action(View view,String description){if(description.contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View found=action(group.getChildAt(i),description);if(found!=null)return found;}}return null;}
 View findText(View v,String label){
  if(v instanceof TextView && label.contentEquals(((TextView)v).getText()))return v;
  if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View hit=findText(g.getChildAt(i),label);if(hit!=null)return hit;}}
  return null;
 }
 int icons(View view){int n=view instanceof ActionIcon?1:0;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)n+=icons(group.getChildAt(i));}return n;}
 @Test public void freshInstallUsesDarkAndSingleSunAction(){assertTrue(new Ui(context).dark);try(ActivityController<MainActivity> controller=open()){MainActivity activity=controller.get();assertNotNull(action(activity.body,"Açık temaya geç"));assertNull(action(activity.body,"Koyu temaya geç"));assertEquals(3,icons(activity.body));
  assertNull(action(activity.body,"Gizlilik"));
  assertNotNull(action(activity.body,"Yedek paylaş"));
  action(activity.body,"Ayarlar").performClick();
  assertNotNull(action(activity.body,"Nasıl kullanılır"));
  action(activity.body,"Gizlilik").performClick();
  assertEquals("privacy",activity.supportPage);
  activity.onBackPressed();assertEquals("settings",activity.supportPage);
  Switch motion=(Switch)findText(activity.body,"Hareketi azalt");motion.setChecked(true);
  assertFalse(Motion.enabled(activity));
  activity.onBackPressed();assertTrue(activity.supportPage.isEmpty());}}
 @Test public void themeToggleIsReversibleWithoutChangingMappings(){Profiles.select(context,"test.game.a");Binding.save(context,"test.game.a",Collections.singletonList(new Binding(96,300,400,1280,800,50,0)));String before=Profiles.prefs(context).getString("game.test.game.a.bindings","");try(ActivityController<MainActivity> controller=open()){MainActivity activity=controller.get();action(activity.body,"Açık temaya geç").performClick();assertFalse(new Ui(context).dark);assertNotNull(action(activity.body,"Koyu temaya geç"));action(activity.body,"Koyu temaya geç").performClick();assertTrue(new Ui(context).dark);assertEquals(before,Profiles.prefs(context).getString("game.test.game.a.bindings",""));}}
 @Test public void backRequiresConfirmationAndCancelKeepsActivity(){try(ActivityController<MainActivity> controller=open()){MainActivity activity=controller.get();activity.onBackPressed();AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();assertTrue(dialog.isShowing());assertFalse(activity.isFinishing());dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertFalse(activity.isFinishing());assertFalse(dialog.isShowing());activity.onBackPressed();assertTrue(ShadowAlertDialog.getLatestAlertDialog().isShowing());}}
 @GraphicsMode(GraphicsMode.Mode.NATIVE)
 @Test public void startingWindowIsAlwaysDarkEvenWithStoredLightPreference(){Profiles.prefs(context).edit().putBoolean("dark",false).commit();Context themed=new ContextThemeWrapper(context,R.style.Theme_Vektor_Starting);android.content.res.TypedArray a=themed.obtainStyledAttributes(new int[]{android.R.attr.windowBackground});Drawable drawable=a.getDrawable(0);a.recycle();assertNotNull(drawable);Bitmap b=Bitmap.createBitmap(320,320,Bitmap.Config.ARGB_8888);drawable.setBounds(0,0,320,320);drawable.draw(new Canvas(b));assertEquals(0xFF0B0F14,b.getPixel(0,0));b.recycle();if(Build.VERSION.SDK_INT>=31){android.content.res.TypedArray splash=themed.obtainStyledAttributes(new int[]{android.R.attr.windowSplashScreenBackground});assertEquals(0xFF0B0F14,splash.getColor(0,0));splash.recycle();}assertFalse(new Ui(context).dark);}
 @Test public void tutorialProgressIsPerGameAndDoesNotEnterBackups()throws Exception {String a="test.game.a",b="test.game.b";Profiles.select(context,a);Profiles.select(context,b);Map<String,Object> before=Backups.snapshot(context);assertTrue(GuideState.active(context,a,true));assertFalse(GuideState.active(context,a,false));GuideState.begin(context,a);assertTrue(GuideState.active(context,a,false));GuideState.complete(context,a);assertFalse(GuideState.active(context,a,true));assertTrue(GuideState.active(context,b,true));GuideState.request(context,a);assertTrue(GuideState.active(context,a,false));GuideState.skip(context,a);assertFalse(GuideState.active(context,a,true));assertEquals(before,Backups.snapshot(context));}
 @Test public void helpHasConcretePagesAndCanReturn(){try(ActivityController<MainActivity> controller=open()){MainActivity activity=controller.get();activity.showHelp();assertEquals(0,activity.helpPage);((Button)findText(activity.body,"İleri")).performClick();assertEquals(1,activity.helpPage);action(activity.body,"Önceki").performClick();assertEquals(0,activity.helpPage);assertFalse(activity.isFinishing());}}
 @Test @GraphicsMode(GraphicsMode.Mode.NATIVE)
 @Config(sdk=35,qualifiers="w512dp-h853dp-port-hdpi")
 public void inspectProfileAndReturnWithoutChangingMappings() throws Exception {
  String pkg="sample.touch";
  android.content.pm.PackageInfo info=new android.content.pm.PackageInfo();
  info.packageName=pkg;
  info.applicationInfo=new android.content.pm.ApplicationInfo();
  info.applicationInfo.packageName=pkg;
  info.applicationInfo.nonLocalizedLabel="Touch Probe";
  org.robolectric.shadows.ShadowPackageManager pm=org.robolectric.Shadows.shadowOf(context.getPackageManager());
  pm.installPackage(info);
  Intent launcher=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(pkg);
  android.content.pm.ResolveInfo match=new android.content.pm.ResolveInfo();
  match.activityInfo=new android.content.pm.ActivityInfo();
  match.activityInfo.packageName=pkg;
  match.activityInfo.name="sample.touch.Main";
  match.activityInfo.applicationInfo=info.applicationInfo;
  pm.addResolveInfoForIntent(launcher,match);
  Profiles.select(context,pkg);
  Binding.save(context,pkg,Arrays.asList(new Binding(21,80,500,1000,600,50,1),new Binding(96,860,470,1000,600,50,0)));
  Map<String,Object> before=Backups.snapshot(context);
  try(ActivityController<MainActivity> controller=open()){
   MainActivity activity=controller.get();
   View row=action(activity.body,"Touch Probe kontrol düzeni");
   assertNotNull(row);
   capture(activity,"library");
   action(activity.body,"Açık temaya geç").performClick();capture(activity,"library-light");
   action(activity.body,"Koyu temaya geç").performClick();row=action(activity.body,"Touch Probe kontrol düzeni");
   row.performClick();
   assertEquals(pkg,activity.profilePage);
   assertNotNull(action(activity.body,"‹  Uygulamalarım"));
   capture(activity,"profile");
   activity.onBackPressed();
   assertEquals("",activity.profilePage);
   assertFalse(activity.isFinishing());
   Map<String,Object> after=Backups.snapshot(context);
   after.remove("dark");before.remove("dark");assertEquals(before,after);
  }
 }
 @Test @Config(sdk=35,qualifiers="w348dp-h470dp-port-mdpi")
 public void splitWindowGuideKeepsActionsReachableAfterResize(){
  try(ActivityController<MainActivity> controller=open()){
   MainActivity a=controller.get();a.showHelpPage(2);
   android.content.res.Configuration cfg=new android.content.res.Configuration(a.getResources().getConfiguration());
   cfg.fontScale=1.4f;a.getResources().updateConfiguration(cfg,null);a.onConfigurationChanged(cfg);
   a.body.measure(View.MeasureSpec.makeMeasureSpec(348,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(470,View.MeasureSpec.EXACTLY));a.body.layout(0,0,348,470);
   View next=findText(a.body,"İleri");assertNotNull(next);int[] xy=new int[2];next.getLocationInWindow(xy);
   assertTrue(next.getMeasuredHeight()>=a.ui.dp(48));assertTrue(xy[0]>=0&&xy[0]+next.getWidth()<=348);
   assertEquals(2,a.helpPage);next.performClick();assertEquals(3,a.helpPage);
  }
 }
 @Test public void restoredBackupPreservesOtherApplicationsAndSurfacePreferences()throws Exception{
  String a="sample.first",b="sample.second";
  Profiles.select(context,a);Binding.save(context,a,Collections.singletonList(new Binding(96,300,400,1280,800,50,0)));
  Map<String,Object> archive=Backups.snapshot(context);
  byte[] payload=BackupCodec.encode(archive);
  Profiles.select(context,b);Binding.save(context,b,Collections.singletonList(new Binding(99,440,200,1280,800,50,0)));
  String second=Profiles.prefs(context).getString(ProfileRules.key(b,"bindings"),"");
  context.getSharedPreferences("appearance",0).edit().putBoolean("reduced",true).commit();
  Backups.restore(context,BackupCodec.decode(payload));
  assertEquals(archive.get(ProfileRules.key(a,"bindings")),Profiles.prefs(context).getString(ProfileRules.key(a,"bindings"),""));
  assertEquals(second,Profiles.prefs(context).getString(ProfileRules.key(b,"bindings"),""));
  assertTrue(context.getSharedPreferences("appearance",0).getBoolean("reduced",false));
  assertTrue(new java.io.File(context.getFilesDir(),"before-restore.rota").isFile());
 }
 void capture(MainActivity activity,String name)throws Exception{
  View v=activity.body;
  v.animate().cancel();v.setAlpha(1);v.setTranslationX(0);
  v.measure(View.MeasureSpec.makeMeasureSpec(768,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1280,View.MeasureSpec.EXACTLY));
  v.layout(0,0,768,1280);
  Bitmap image=Bitmap.createBitmap(768,1280,Bitmap.Config.ARGB_8888);
  v.draw(new Canvas(image));
  java.io.File dir=new java.io.File("build/preview");dir.mkdirs();
  try(java.io.FileOutputStream stream=new java.io.FileOutputStream(new java.io.File(dir,name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,stream);}
  image.recycle();
 }
}
