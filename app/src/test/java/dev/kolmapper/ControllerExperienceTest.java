package dev.kolmapper;

import android.content.*;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.InputDeviceBuilder;
import org.robolectric.shadows.ShadowInputDevice;
import java.nio.file.*;
import java.io.File;
import java.io.FileOutputStream;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="w512dp-h853dp-port-hdpi",shadows=ControllerExperienceTest.Devices.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ControllerExperienceTest {
 @Implements(InputDevice.class)
 public static class Devices extends ShadowInputDevice {
  static InputDevice pad;
  @Implementation protected static InputDevice getDevice(int id){return id==7?pad:null;}
  @Implementation protected static int[] getDeviceIds(){return pad==null?new int[0]:new int[]{7};}
  @Implementation protected boolean isEnabled(){return true;}
 }
 InputDevice fixture(){
  return InputDeviceBuilder.newBuilder().setId(7).setExternal(true).setDescriptor("test-controller")
    .setName("Test pad").setVendorId(0x2dc8).setProductId(0x301b)
    .setSources(InputDevice.SOURCE_JOYSTICK|InputDevice.SOURCE_GAMEPAD)
    .addMotionRange(MotionEvent.AXIS_X,InputDevice.SOURCE_JOYSTICK,-1,1,.01f,0,0)
    .addMotionRange(MotionEvent.AXIS_RTRIGGER,InputDevice.SOURCE_JOYSTICK,0,1,0,0,0).build();
 }
 KeyEvent key(int action,int key){
  KeyEvent event=new KeyEvent(1,2,action,key,0,0,7,0,0,InputDevice.SOURCE_GAMEPAD);
  Shadows.shadowOf(event).setDevice(Devices.pad);
  return event;
 }
 void capture(MainActivity a,String name)throws Exception{
  View v=a.body;v.animate().cancel();v.setAlpha(1);v.setTranslationY(0);
  v.measure(View.MeasureSpec.makeMeasureSpec(768,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1280,View.MeasureSpec.EXACTLY));
  v.layout(0,0,768,1280);Bitmap b=Bitmap.createBitmap(768,1280,Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));
  File dir=new File("build/preview");dir.mkdirs();
  try(FileOutputStream f=new FileOutputStream(new File(dir,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,f);}b.recycle();
 }
 private boolean hasText(View view,String wanted){
  if(view instanceof android.widget.TextView&&wanted.contentEquals(((android.widget.TextView)view).getText()))return true;
  if(view instanceof ViewGroup){
   ViewGroup group=(ViewGroup)view;
   for(int i=0;i<group.getChildCount();i++)if(hasText(group.getChildAt(i),wanted))return true;
  }
  return false;
 }
 @Test public void simultaneousInputsMotionAndDisconnect()throws Exception{
  Devices.pad=fixture();
  try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup().visible()){
   MainActivity a=c.get();c.windowFocusChanged(true);
   assertEquals("8BitDo Ultimate 2C",a.controllerTitle.getText());
   a.openSupport("controller");c.windowFocusChanged(true);

   assertTrue(a.dispatchKeyEvent(key(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BUTTON_A)));
   assertTrue(a.dispatchKeyEvent(key(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BUTTON_X)));
   assertTrue(a.pad.keys.get(KeyEvent.KEYCODE_BUTTON_A));assertTrue(a.pad.keys.get(KeyEvent.KEYCODE_BUTTON_X));
   a.dispatchKeyEvent(key(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BUTTON_A));
   assertFalse(a.pad.keys.get(KeyEvent.KEYCODE_BUTTON_A));assertTrue(a.pad.keys.get(KeyEvent.KEYCODE_BUTTON_X));
   MotionEvent.PointerCoords coords=new MotionEvent.PointerCoords();coords.x=-.75f;coords.setAxisValue(MotionEvent.AXIS_RTRIGGER,.86f);
   MotionEvent.PointerProperties pointer=new MotionEvent.PointerProperties();pointer.id=0;
   MotionEvent e=MotionEvent.obtain(1,2,MotionEvent.ACTION_MOVE,1,new MotionEvent.PointerProperties[]{pointer},new MotionEvent.PointerCoords[]{coords},0,0,1,1,7,0,InputDevice.SOURCE_JOYSTICK,0);
   Shadows.shadowOf(e).setDevice(Devices.pad);
   try{assertTrue(a.dispatchGenericMotionEvent(e));assertEquals(-.75f,a.pad.lx,.01f);assertEquals(.86f,a.pad.rt,.01f);}finally{e.recycle();}
   a.refreshDiagnostics();assertTrue(hasText(a.diagnostics,"R2  %86"));
   assertTrue(hasText(a.diagnostics,"↑ Bırakıldı"));
   capture(a,"controller-test");
   for(int i=0;i<12;i++) a.dispatchKeyEvent(key(i%2,KeyEvent.KEYCODE_BUTTON_A));
   assertEquals(6,a.inputLog.size());
   java.util.Map<String,Object> snapshot=Backups.snapshot(a);
   a.inputLog.clear();a.diagnostics.updateHistory();assertTrue(hasText(a.diagnostics,"Henüz tuş olayı yok"));
   assertEquals(snapshot,Backups.snapshot(a));
   Devices.pad=null;a.onInputDeviceRemoved(7);assertEquals("Kol bağlı değil",a.controllerTitle.getText());assertFalse(a.pad.keys.get(KeyEvent.KEYCODE_BUTTON_X));assertEquals(0,a.pad.rt,0);
  }finally{Devices.pad=null;}
 }
 @Test public void backupAndPrivacyHaveOwnPagesAndDocumentPickers()throws Exception{
  Devices.pad=null;
  try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup().visible()){
   MainActivity a=c.get();a.showBackups();assertEquals("backups",a.supportPage);capture(a,"backup-screen");
   a.exportBackup();assertEquals(Intent.ACTION_CREATE_DOCUMENT,Shadows.shadowOf(a).getNextStartedActivity().getAction());
   a.importBackup();assertEquals(Intent.ACTION_OPEN_DOCUMENT,Shadows.shadowOf(a).getNextStartedActivity().getAction());
   a.onBackPressed();assertTrue(a.supportPage.isEmpty());a.showPrivacy();assertEquals("privacy",a.supportPage);capture(a,"privacy-screen");
   a.onBackPressed();a.showHelpPage(3);capture(a,"guide-screen");
  }
 }
 void write(Path path,String value)throws Exception{Files.write(path,value.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
 @Test public void transportUsesMatchingIdentityAndHandlesAmbiguity()throws Exception{
  Path root=Files.createTempDirectory("pad-bus");
  for(int i=0;i<2;i++){
   Path input=root.resolve("input"+i);Files.createDirectories(input.resolve("id"));
   write(input.resolve("name"),"Test");
   write(input.resolve("id/vendor"),"2dc8");write(input.resolve("id/product"),"301b");
   write(input.resolve("id/bustype"),i==0?"0005":"0003");
   assertEquals(i==0?"0005":"ambiguous",ControllerIdentity.bus(root.toFile(),"Test",0x2dc8,0x301b));
  }
  assertEquals("",ControllerIdentity.bus(root.toFile(),"Another controller",0x2dc8,0x301b));
 }
}
