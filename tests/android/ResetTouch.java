import android.os.SystemClock;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.MotionEvent;
/** Emulator-only recovery after intentionally testing malformed pointer streams. Not in the APK. */
public final class ResetTouch {
 public static void main(String[] args)throws Exception {
  long now=SystemClock.uptimeMillis();
  MotionEvent.PointerProperties p=new MotionEvent.PointerProperties();p.id=0;p.toolType=MotionEvent.TOOL_TYPE_FINGER;
  MotionEvent.PointerCoords c=new MotionEvent.PointerCoords();c.x=1;c.y=1;
  MotionEvent.PointerProperties p2=new MotionEvent.PointerProperties();p2.id=10;p2.toolType=MotionEvent.TOOL_TYPE_FINGER;
  MotionEvent e=MotionEvent.obtain(now,now,MotionEvent.ACTION_CANCEL,2,new MotionEvent.PointerProperties[]{p,p2},new MotionEvent.PointerCoords[]{c,c},0,0,1,1,-1,0,InputDevice.SOURCE_TOUCHSCREEN,0);
  InputEvent.class.getMethod("setDisplayId",int.class).invoke(e,0);
  Class<?> type=Class.forName("android.hardware.input.InputManagerGlobal");Object manager=type.getMethod("getInstance").invoke(null);
  Object accepted=type.getMethod("injectInputEvent",InputEvent.class,int.class).invoke(manager,e,2);
  System.out.println("Emulator CANCEL device=-1 accepted="+accepted);e.recycle();
 }
}
