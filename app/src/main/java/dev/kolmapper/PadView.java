package dev.kolmapper;

import android.content.Context;
import android.graphics.*;
import android.util.SparseBooleanArray;
import android.view.*;
import static android.view.KeyEvent.*;
import static android.view.MotionEvent.*;

/** Activity-only diagram. It never injects input or changes bindings. */
final class PadView extends View {
 final Ui ui;
 final SparseBooleanArray keys=new SparseBooleanArray();
 final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 final Path shell=new Path();
 final int glow;
 final LinearGradient coating;
 final Typeface lettering=Typeface.create("sans-serif-medium",Typeface.NORMAL);
 float lx,ly,rx,ry,hx,hy,lt,rt;
 boolean connected;
 boolean ultimate,hasExtra1,hasExtra2;
 PadView(Context c, Ui ui){
  super(c);this.ui=ui;glow=ui.accent;
  coating=new LinearGradient(0,32,25,136,ui.bodyTop,ui.bodyBottom,Shader.TileMode.CLAMP);
  shell.moveTo(81,32);shell.cubicTo(55,27,41,35,35,51);
  shell.cubicTo(27,72,16,114,22,127);shell.cubicTo(32,147,51,126,68,106);
  shell.cubicTo(79,94,88,98,103,99);shell.lineTo(177,99);
  shell.cubicTo(192,96,202,95,212,108);shell.cubicTo(233,131,248,146,257,126);
  shell.cubicTo(262,115,254,79,246,55);shell.cubicTo(239,32,224,27,202,32);
  shell.cubicTo(164,42,115,42,81,32);shell.close();
  setContentDescription("Kolun canlı tuş ve çubuk görünümü");
 }
 void reset(){keys.clear();lx=ly=rx=ry=hx=hy=lt=rt=0;invalidate();}
 static String label(int code){
  switch(code){
   case KEYCODE_BUTTON_A:return "A";case KEYCODE_BUTTON_B:return "B";
   case KEYCODE_BUTTON_X:return "X";case KEYCODE_BUTTON_Y:return "Y";
   case KEYCODE_BUTTON_THUMBL:return "L3";case KEYCODE_BUTTON_THUMBR:return "R3";
   default:return KeyBadge.name(code);
  }
 }
 boolean key(int code,boolean down){
  if(!KeyEvent.isGamepadButton(code) && !(code>=KEYCODE_DPAD_UP && code<=KEYCODE_DPAD_CENTER))return false;
  if(keys.get(code)!=down){keys.put(code,down);invalidate();}
  return true;
 }
 static float stickAxis(MotionEvent e,int axis){
  float v=e.getAxisValue(axis);
  InputDevice d=e.getDevice();
  InputDevice.MotionRange r=d==null?null:d.getMotionRange(axis,e.getSource());
  if(r==null)return Math.max(-1,Math.min(1,v));
  if(Math.abs(v)<=r.getFlat())return 0;
  return Math.max(-1,Math.min(1,v/(v<0?Math.max(.001f,-r.getMin()):Math.max(.001f,r.getMax()))));
 }
 static float trigger(MotionEvent e,int axis){
  InputDevice d=e.getDevice();
  InputDevice.MotionRange r=d==null?null:d.getMotionRange(axis,e.getSource());
  return r==null?0:AnalogInput.normalize(e.getAxisValue(axis),r.getMin(),r.getMax());
 }
 void motion(MotionEvent e){
  lx=stickAxis(e,AXIS_X);ly=stickAxis(e,AXIS_Y);
  InputDevice d=e.getDevice();
  boolean z=d!=null && d.getMotionRange(AXIS_Z,e.getSource())!=null && d.getMotionRange(AXIS_RZ,e.getSource())!=null;
  rx=stickAxis(e,z?AXIS_Z:AXIS_RX);ry=stickAxis(e,z?AXIS_RZ:AXIS_RY);
  hx=e.getAxisValue(AXIS_HAT_X);hy=e.getAxisValue(AXIS_HAT_Y);
  lt=Math.max(trigger(e,AXIS_LTRIGGER),trigger(e,AXIS_BRAKE));
  rt=Math.max(trigger(e,AXIS_RTRIGGER),trigger(e,AXIS_GAS));postInvalidateOnAnimation();
 }
 void color(int color){p.setStyle(Paint.Style.FILL);p.setColor(color);}
 void text(Canvas c,String s,float x,float y,float size,int color){
  color(color);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(lettering);p.setTextSize(size);c.drawText(s,x,y,p);
 }
 void circle(Canvas c,float x,float y,float r,int color){color(color);c.drawCircle(x,y,r,p);}
 void face(Canvas c,String label,int key,int x,int y){
  boolean down=keys.get(key);if(down)circle(c,x,y,9,ui.accentSoft);circle(c,x,y,down?7:7.3f,down?glow:ui.background);
  text(c,label,x,y+2.8f,8,down?(ui.dark?Color.BLACK:Color.WHITE):ui.text);
 }
 void shoulder(Canvas c,int x,int y,String label,int key,float depth){
  boolean down=keys.get(key)||depth>.07f;color(ui.border);c.drawRoundRect(x,y,x+32,y+10,4,4,p);
  if(down){color(glow);c.drawRoundRect(x,y,x+32*(keys.get(key)?1:depth),y+10,4,4,p);}
  text(c,label,x+16,y+7.8f,7,down && (keys.get(key)||depth>.7f)?(ui.dark?Color.BLACK:Color.WHITE):ui.text);
 }
 void stick(Canvas c,int x,int y,float dx,float dy,int key){
  boolean active=keys.get(key)||Math.abs(dx)+Math.abs(dy)>.06f;
  circle(c,x,y,13,ui.background);circle(c,x+dx*5,y+dy*5,9,active?glow:ui.muted);
  circle(c,x+dx*5,y+dy*5,7,active?glow:ui.border);
 }
 void direction(Canvas c,int x,int y,int key,boolean axis){
  color(keys.get(key)||axis?glow:ui.background);c.drawRoundRect(x-4,y-4,x+4,y+4,1.5f,1.5f,p);
 }
 @Override protected void onDraw(Canvas c){
  float scale=Math.min(getWidth()/280f,getHeight()/146f);
  c.save();c.translate((getWidth()-280*scale)/2,(getHeight()-146*scale)/2);c.scale(scale,scale);
  shoulder(c,48,6,"L2",KEYCODE_BUTTON_L2,lt);shoulder(c,200,6,"R2",KEYCODE_BUTTON_R2,rt);
  shoulder(c,44,19,"L1",KEYCODE_BUTTON_L1,0);shoulder(c,204,19,"R1",KEYCODE_BUTTON_R1,0);
  color(ui.raised);p.setShader(coating);c.drawPath(shell,p);p.setShader(null);
  p.setColor(ui.muted);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.8f);c.drawPath(shell,p);
  stick(c,73,59,lx,ly,KEYCODE_BUTTON_THUMBL);stick(c,170,83,rx,ry,KEYCODE_BUTTON_THUMBR);
  direction(c,102,72,KEYCODE_DPAD_UP,hy<-.5f);direction(c,102,88,KEYCODE_DPAD_DOWN,hy>.5f);
  direction(c,94,80,KEYCODE_DPAD_LEFT,hx<-.5f);direction(c,110,80,KEYCODE_DPAD_RIGHT,hx>.5f);
  face(c,"Y",KEYCODE_BUTTON_Y,210,43);face(c,"A",KEYCODE_BUTTON_A,210,73);
  face(c,"X",KEYCODE_BUTTON_X,195,58);face(c,"B",KEYCODE_BUTTON_B,225,58);
  face(c,"−",KEYCODE_BUTTON_SELECT,125,57);face(c,"+",KEYCODE_BUTTON_START,155,57);
  if(hasExtra1)face(c,"1",KEYCODE_BUTTON_1,100,39);
  if(hasExtra2)face(c,"2",KEYCODE_BUTTON_2,179,39);
  if(ultimate){color(ui.border);c.drawRoundRect(133,84,147,86,1,1,p);}
  circle(c,140,72,4,keys.get(KEYCODE_BUTTON_MODE)?glow:ui.background);
  circle(c,140,47,1.6f,connected?ui.success:ui.muted);c.restore();
 }
}
