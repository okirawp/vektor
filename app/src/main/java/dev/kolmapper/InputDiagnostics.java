package dev.kolmapper;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayDeque;
import java.util.Locale;

/** Read-only presentation of live values supplied by the diagnostic preview. */
final class InputDiagnostics extends LinearLayout {
 static final int MAX_EVENTS=6;
 static final class Event {
  final long time;
  final String name;
  final boolean down;
  Event(long time,String name,boolean down){this.time=time;this.name=name;this.down=down;}
 }
 private final Ui ui;
 private final Axis left,right;
 private final Meter l2,r2;
 private final LinearLayout history;
 private final ArrayDeque<Event> events;
 private final java.util.ArrayList<TextView> times=new java.util.ArrayList<>(MAX_EVENTS);
 private final Runnable updateTimes=this::refreshTimes;

 InputDiagnostics(MainActivity host,Ui ui,ArrayDeque<Event> events){
  super(host);this.ui=ui;this.events=events;setOrientation(VERTICAL);
  LinearLayout axes=new LinearLayout(host);
  left=new Axis("Sol analog");right=new Axis("Sağ analog");
  axes.addView(left,new LayoutParams(0,ui.dp(Ui.AXIS_SIZE),1));
  axes.addView(right,new LayoutParams(0,ui.dp(Ui.AXIS_SIZE),1));addView(axes);
  LinearLayout triggers=new LinearLayout(host);
  l2=new Meter("L2");r2=new Meter("R2");
  triggers.addView(l2,new LayoutParams(0,-2,1));triggers.addView(r2,new LayoutParams(0,-2,1));addView(triggers);
  LinearLayout header=new LinearLayout(host);header.setGravity(Gravity.CENTER_VERTICAL);
  header.addView(ui.text("Giriş geçmişi",Ui.HELP_TITLE,true),new LayoutParams(0,-2,1));
  header.addView(host.footerLink("Sıfırla",()->{events.clear();updateHistory();}));addView(header);
  history=new LinearLayout(host);history.setOrientation(VERTICAL);addView(history);
 }
 void update(PadView pad,boolean hasLT,boolean hasRT){
  left.update(pad.lx,pad.ly,pad.connected);right.update(pad.rx,pad.ry,pad.connected);
  l2.update(pad.lt,hasLT,pad.keys.get(KeyEvent.KEYCODE_BUTTON_L2),pad.connected);
  r2.update(pad.rt,hasRT,pad.keys.get(KeyEvent.KEYCODE_BUTTON_R2),pad.connected);
 }
 void updateHistory(){
  removeCallbacks(updateTimes);
  history.removeAllViews();times.clear();
  if(events.isEmpty()){history.addView(ui.caption("Henüz tuş olayı yok"));return;}
  for(Event event:events){
   LinearLayout row=new LinearLayout(getContext());row.setGravity(Gravity.CENTER_VERTICAL);
   TextView key=ui.chip(event.name,event.down);
   row.addView(key);
   TextView state=ui.caption(event.down?"↓ Basıldı":"↑ Bırakıldı");
   state.setPaddingRelative(ui.dp(Ui.PICKER_GAP),0,0,0);
   row.addView(state,new LayoutParams(0,-2,1));
   TextView time=ui.caption("");times.add(time);row.addView(time);
   history.addView(row);
  }
  refreshTimes();
 }
 private void refreshTimes(){
  long now=SystemClock.uptimeMillis();
  int index=0;
  for(Event event:events){
   long seconds=Math.max(0,(now-event.time)/1000);
   String age=seconds==0?"Şimdi":seconds<60?seconds+" sn önce":seconds/60+" dk önce";
   TextView time=times.get(index++);if(!age.contentEquals(time.getText()))time.setText(age);
  }
  if(!events.isEmpty()&&isShown()&&getWindowVisibility()==VISIBLE)postDelayed(updateTimes,Motion.INPUT_CLOCK);
 }
 @Override protected void onWindowVisibilityChanged(int visibility){
  super.onWindowVisibilityChanged(visibility);
  if(history==null)return;
  if(visibility==VISIBLE)updateHistory();else removeCallbacks(updateTimes);
 }
 @Override protected void onDetachedFromWindow(){removeCallbacks(updateTimes);super.onDetachedFromWindow();}

 private final class Axis extends LinearLayout {
  private final TextView label;
  private final Plot plot;
  private final String name;
  private boolean detailed;
  private float x,y;
  Axis(String name){
   super(InputDiagnostics.this.getContext());this.name=name;
   setOrientation(VERTICAL);setGravity(Gravity.CENTER);
   label=ui.caption(name);label.setGravity(Gravity.CENTER);
   plot=new Plot(InputDiagnostics.this.getContext());
   addView(plot,new LayoutParams(ui.dp(Ui.AXIS_PLOT),ui.dp(Ui.AXIS_PLOT)));addView(label);
   setFocusable(true);setOnClickListener(view->{detailed=!detailed;display();});
   setContentDescription(name+" değerleri");
  }
  void update(float x,float y,boolean connected){
   this.x=x;this.y=y;
   plot.x=x;plot.y=y;plot.active=connected;plot.invalidate();if(detailed)display();
  }
  void display(){label.setText(detailed?String.format(Locale.getDefault(),"%s\nX %+.2f  Y %+.2f",name,x,y):name);}
 }
 private final class Meter extends LinearLayout {
  private final TextView label;
  private final Plot bar;
  private final String name;
  Meter(String name){
   super(InputDiagnostics.this.getContext());this.name=name;
   setOrientation(VERTICAL);setPadding(ui.dp(Ui.PICKER_GAP),0,ui.dp(Ui.PICKER_GAP),0);
   label=ui.caption(name);addView(label);
   bar=new Plot(InputDiagnostics.this.getContext());bar.meter=true;
   addView(bar,new LayoutParams(-1,ui.dp(Ui.TRIGGER_BAR)));
  }
  void update(float value,boolean analog,boolean pressed,boolean connected){
   label.setText(name+"  "+(!connected?"—":analog?"%"+Math.round(value*100):pressed?"Basılı":"Serbest"));
   bar.x=analog?value:pressed?1:0;bar.active=connected;bar.invalidate();
  }
 }
 private final class Plot extends View {
  final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
  float x,y;
  boolean active,meter;
  Plot(android.content.Context context){super(context);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
  @Override protected void onDraw(Canvas c){
   paint.setColor(ui.raised);
   if(meter){
    float radius=getHeight()/2f;
    c.drawRoundRect(0,0,getWidth(),getHeight(),radius,radius,paint);
    if(active){paint.setColor(ui.accent);c.drawRoundRect(0,0,getWidth()*x,getHeight(),radius,radius,paint);}
   }else{
    float cx=getWidth()/2f,cy=getHeight()/2f,radius=Math.min(cx,cy);
    c.drawCircle(cx,cy,radius,paint);paint.setColor(active?ui.accent:ui.muted);
    float length=Math.max(1f,(float)Math.hypot(x,y));
    c.drawCircle(cx+x*radius*.72f/length,cy+y*radius*.72f/length,radius*.15f,paint);
   }
  }
 }
}
