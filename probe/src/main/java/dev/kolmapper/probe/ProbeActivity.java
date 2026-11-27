package dev.kolmapper.probe;
import android.app.*;import android.os.*;import android.graphics.*;import android.view.*;
import java.util.*;
public class ProbeActivity extends Activity {
 static final List<String> events=Collections.synchronizedList(new ArrayList<>());
 static final Map<Integer,String> active=Collections.synchronizedMap(new HashMap<>());
 static String name(float x){return x<300?"LEFT":x<520?"RIGHT":x<750?"A":x<960?"X":"R2";}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().getDecorView().setSystemUiVisibility(5894);setContentView(new Surface());}
 final class Surface extends View {
  Paint p=new Paint(3);Surface(){super(ProbeActivity.this);setContentDescription("Touch probe ready");setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);}
  @Override protected void onDraw(Canvas c){c.drawColor(0xFF142B35);p.setColor(Color.WHITE);p.setTextSize(32);c.drawText("TOUCH PROBE · gerçek Android dokunma olayları",40,70,p);int[] xs={200,420,650,850,1080};String[] names={"LEFT","RIGHT","A","X","R2"};for(int i=0;i<xs.length;i++){p.setColor(0xFF387B86);c.drawRoundRect(xs[i]-65,530,xs[i]+65,670,20,20,p);p.setColor(Color.WHITE);p.setTextSize(22);c.drawText(names[i],xs[i]-30,610,p);}}
  @Override public boolean onTouchEvent(MotionEvent e){int action=e.getActionMasked(),i=e.getActionIndex(),id=e.getPointerId(i);long time=SystemClock.uptimeMillis();
   if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){String n=name(e.getX(i));active.put(id,n);events.add(time+",DOWN,"+n+","+id);}
   if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP){events.add(time+",UP,"+active.remove(id)+","+id);}
   if(action==MotionEvent.ACTION_CANCEL){for(Map.Entry<Integer,String> a:active.entrySet())events.add(time+",CANCEL,"+a.getValue()+","+a.getKey());active.clear();}
   return true;
  }
 }
}
