package dev.kolmapper;

import android.content.Context;
import android.graphics.*;
import android.util.SparseBooleanArray;
import android.view.*;
import static android.view.KeyEvent.*;
import static android.view.MotionEvent.*;

/** Activity-only diagram. It never injects input or changes bindings. */
final class PadView extends View {
 interface Renderer {
  void draw(Canvas canvas,PadView state);
  default void setVisible(boolean visible){}
  default void release(){}
 }
 interface ModelFactory {
  Renderer create(Context context,String asset);
 }
 private final Renderer renderer;
 private final boolean interactive;
 private final ScaleGestureDetector pinch;
 private final Camera camera=new Camera();
 private final Matrix perspective=new Matrix();
 private android.animation.ValueAnimator settling;
 private float tiltX,tiltY,zoom=1,lastX,lastY;
 final Ui ui;
 final SparseBooleanArray keys=new SparseBooleanArray();
 final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 final Path shell=new Path();
 final Path switchShell=new Path();
 private enum Layout { GENERIC, PLAYSTATION, XBOX, SWITCH }
 private Layout layout=Layout.GENERIC;
 final int glow;
 final LinearGradient coating;
 final Typeface lettering=Typeface.create("sans-serif-medium",Typeface.NORMAL);
 float lx,ly,rx,ry,hx,hy,lt,rt;
 boolean connected;
 boolean ultimate,hasExtra1,hasExtra2;
 PadView(Context c,Ui ui,boolean interactive){
  this(c,ui,interactive,rendererFor(c));
 }
 private static Renderer rendererFor(Context context){
  try{
   String[] assets=context.getAssets().list("");
   if(assets!=null)for(String model:assets){
    if(!model.endsWith(".glb"))continue;
    for(ModelFactory factory:java.util.ServiceLoader.load(ModelFactory.class)){
     Renderer renderer=factory.create(context,model);
     if(renderer!=null)return renderer;
    }
   }
  }catch(java.io.IOException|java.util.ServiceConfigurationError|RuntimeException e){
   android.util.Log.w("VektorPreview","Model önizlemesi yüklenemedi",e);
  }
  return (canvas,state)->state.drawLayers(canvas);
 }
 float pitch(){return tiltX;}
 float yaw(){return tiltY;}
 float zoom(){return zoom;}
 // A GLB renderer can be supplied here without changing the input preview or mapper.
 PadView(Context c,Ui ui,boolean interactive,Renderer renderer){
  super(c);this.ui=ui;glow=ui.accent;this.interactive=interactive;this.renderer=renderer;
  pinch=new ScaleGestureDetector(c,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
   @Override public boolean onScale(ScaleGestureDetector detector){
    zoom=clamp(zoom*detector.getScaleFactor(),Motion.PAD_MIN_ZOOM,Motion.PAD_MAX_ZOOM);
    invalidate();return true;
   }
  });
  pinch.setQuickScaleEnabled(false);
  coating=new LinearGradient(0,32,25,136,ui.bodyTop,ui.bodyBottom,Shader.TileMode.CLAMP);
  shell.moveTo(81,32);shell.cubicTo(55,27,41,35,35,51);
  shell.cubicTo(27,72,16,114,22,127);shell.cubicTo(32,147,51,126,68,106);
  shell.cubicTo(79,94,88,98,103,99);shell.lineTo(177,99);
  shell.cubicTo(192,96,202,95,212,108);shell.cubicTo(233,131,248,146,257,126);
  shell.cubicTo(262,115,254,79,246,55);shell.cubicTo(239,32,224,27,202,32);
  shell.cubicTo(164,42,115,42,81,32);shell.close();
  switchShell.moveTo(79,31);switchShell.cubicTo(52,28,34,41,29,67);
  switchShell.cubicTo(22,90,18,129,29,137);switchShell.cubicTo(47,150,69,117,79,106);
  switchShell.cubicTo(87,97,107,99,122,100);switchShell.lineTo(161,100);
  switchShell.cubicTo(184,99,194,98,203,108);switchShell.cubicTo(220,129,244,150,254,135);
  switchShell.cubicTo(268,113,252,63,246,49);switchShell.cubicTo(234,27,216,29,199,32);
  switchShell.quadTo(139,34,79,31);switchShell.close();
  setContentDescription("Kolun canlı tuş ve çubuk görünümü");
 }
 private static float clamp(float value,float min,float max){return Math.max(min,Math.min(max,value));}
 private void cancelReturn(){if(settling!=null){settling.cancel();settling=null;}}
 void resetView(){cancelReturn();tiltX=tiltY=0;zoom=1;invalidate();}
 private boolean canRotate(){return interactive;}
 @Override public boolean onTouchEvent(MotionEvent event){
  if(!canRotate())return false;
  pinch.onTouchEvent(event);
  switch(event.getActionMasked()){
   case MotionEvent.ACTION_DOWN:
    cancelReturn();lastX=event.getX();lastY=event.getY();
    getParent().requestDisallowInterceptTouchEvent(true);break;
   case ACTION_MOVE:
    if(event.getPointerCount()==1&&!pinch.isInProgress()){
     tiltY=clamp(tiltY+(event.getX()-lastX)*2*Motion.PAD_TILT/getWidth(),-Motion.PAD_TILT,Motion.PAD_TILT);
     tiltX=clamp(tiltX-(event.getY()-lastY)*2*Motion.PAD_TILT/getHeight(),-Motion.PAD_TILT,Motion.PAD_TILT);
    }
    lastX=event.getX();lastY=event.getY();invalidate();break;
   case ACTION_POINTER_UP:
    int remaining=event.getActionIndex()==0?1:0;
    lastX=event.getX(remaining);lastY=event.getY(remaining);break;
   case MotionEvent.ACTION_UP:case ACTION_CANCEL:
    getParent().requestDisallowInterceptTouchEvent(false);returnToCenter();break;
   default:break;
  }
  return true;
 }
 private void returnToCenter(){
  float x=tiltX,y=tiltY;
  if(x==0&&y==0)return;
  if(!Motion.enabled(getContext())){tiltX=tiltY=0;invalidate();return;}
  settling=android.animation.ValueAnimator.ofFloat(1,0);
  settling.setDuration(Motion.PAD_RETURN);settling.setInterpolator(Motion.RETURN);
  settling.addUpdateListener(value->{float fraction=(Float)value.getAnimatedValue();tiltX=x*fraction;tiltY=y*fraction;invalidate();});
  settling.start();
 }
 @Override protected void onVisibilityChanged(View changedView,int visibility){
  super.onVisibilityChanged(changedView,visibility);
  if(visibility!=VISIBLE)resetView();
  if(renderer!=null)renderer.setVisible(isShown()&&getWindowVisibility()==VISIBLE);
 }
 @Override protected void onWindowVisibilityChanged(int visibility){
  super.onWindowVisibilityChanged(visibility);
  if(visibility!=VISIBLE)resetView();
  if(renderer!=null)renderer.setVisible(isShown()&&getWindowVisibility()==VISIBLE);
 }
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();renderer.setVisible(isShown());}
 @Override protected void onDetachedFromWindow(){resetView();renderer.release();super.onDetachedFromWindow();}
 void reset(){keys.clear();lx=ly=rx=ry=hx=hy=lt=rt=0;invalidate();}
 void layoutFor(String deviceName){
  String name=deviceName.toLowerCase(java.util.Locale.ROOT);
  if(name.contains("dualshock")||name.contains("dualsense")||name.contains("playstation")||name.contains("wireless controller")&&name.contains("sony"))layout=Layout.PLAYSTATION;
  else if(name.contains("xbox")||name.contains("x-box"))layout=Layout.XBOX;
  else if(name.contains("switch")||name.contains("nintendo")||name.equals("pro controller"))layout=Layout.SWITCH;
  else layout=Layout.GENERIC;
  invalidate();
 }
 String displayLabel(int code){
  if(layout==Layout.PLAYSTATION){
   switch(code){case KEYCODE_BUTTON_A:return "×";case KEYCODE_BUTTON_B:return "○";
    case KEYCODE_BUTTON_X:return "□";case KEYCODE_BUTTON_Y:return "△";default:break;}
  }
  return label(code);
 }
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
  boolean down=keys.get(key);
  circle(c,x,y+2,8,ui.bodyBottom);
  if(down){y+=2;circle(c,x,y,10,ui.accentSoft);}
  circle(c,x,y,down?6.5f:7.3f,down?glow:ui.background);
  text(c,label,x,y+2.8f,8,down?(ui.dark?Color.BLACK:Color.WHITE):ui.text);
 }
 void shoulder(Canvas c,int x,int y,String label,int key,float depth){
  boolean down=keys.get(key)||depth>.07f;y+=Math.round((keys.get(key)?1:depth)*4);color(ui.border);c.drawRoundRect(x,y,x+32,y+10,4,4,p);
  if(down){color(glow);c.drawRoundRect(x,y,x+32*(keys.get(key)?1:depth),y+10,4,4,p);}
  text(c,label,x+16,y+7.8f,7,down && (keys.get(key)||depth>.7f)?(ui.dark?Color.BLACK:Color.WHITE):ui.text);
 }
 void stick(Canvas c,int x,int y,float dx,float dy,int key){
  boolean active=keys.get(key)||Math.abs(dx)+Math.abs(dy)>.06f;
  circle(c,x,y+2,14,ui.bodyBottom);circle(c,x,y,13,ui.background);
  color(ui.muted);p.setStrokeWidth(3);c.drawLine(x,y,x+dx*5,y+dy*5,p);circle(c,x+dx*5,y+dy*5,9,active?glow:ui.muted);
  circle(c,x+dx*5,y+dy*5,7,active?glow:ui.border);
 }
 void direction(Canvas c,int x,int y,int key,boolean axis){
  color(keys.get(key)||axis?glow:ui.background);c.drawRoundRect(x-4,y-4,x+4,y+4,1.5f,1.5f,p);
 }
 @Override protected void onDraw(Canvas c){renderer.draw(c,this);}
 private void drawLayers(Canvas c){
  boolean depth=canRotate();
  float scale=Math.min(getWidth()/300f,getHeight()/164f)*(depth?zoom:1f);
  c.save();c.translate((getWidth()-280*scale)/2,(getHeight()-146*scale)/2);c.scale(scale,scale);
  color(ui.padShadow);c.drawOval(30,126,250,143,p);
  if(depth){
   camera.save();camera.rotateX(tiltX);camera.rotateY(tiltY);camera.getMatrix(perspective);camera.restore();
   perspective.preTranslate(-140,-73);perspective.postTranslate(140,73);c.concat(perspective);
  }
  boolean parallax=depth&&Motion.enabled(getContext());
  float parallaxX=parallax?tiltY*.12f:0,parallaxY=parallax?-tiltX*.12f:0;
  c.save();c.translate(-parallaxX,-parallaxY);
  shoulder(c,48,6,"L2",KEYCODE_BUTTON_L2,lt);shoulder(c,200,6,"R2",KEYCODE_BUTTON_R2,rt);
  shoulder(c,44,19,"L1",KEYCODE_BUTTON_L1,0);shoulder(c,204,19,"R1",KEYCODE_BUTTON_R1,0);
  c.restore();
  Path body=layout==Layout.SWITCH?switchShell:shell;
  c.save();c.translate(0,4);color(ui.bodyBottom);c.drawPath(body,p);c.restore();
  color(ui.raised);p.setShader(coating);c.drawPath(body,p);p.setShader(null);
  p.setColor(ui.padEdge);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.8f);c.drawPath(body,p);
  c.translate(parallaxX,parallaxY);
  boolean ps=layout==Layout.PLAYSTATION;
  stick(c,ps?108:73,ps?83:59,lx,ly,KEYCODE_BUTTON_THUMBL);
  stick(c,172,83,rx,ry,KEYCODE_BUTTON_THUMBR);
  int dx=ps?73:102,dy=ps?58:80;
  direction(c,dx,dy-8,KEYCODE_DPAD_UP,hy<-.5f);direction(c,dx,dy+8,KEYCODE_DPAD_DOWN,hy>.5f);
  direction(c,dx-8,dy,KEYCODE_DPAD_LEFT,hx<-.5f);direction(c,dx+8,dy,KEYCODE_DPAD_RIGHT,hx>.5f);
  boolean nintendo=layout==Layout.SWITCH;
  int top=nintendo?KEYCODE_BUTTON_X:KEYCODE_BUTTON_Y,bottom=nintendo?KEYCODE_BUTTON_B:KEYCODE_BUTTON_A;
  int left=nintendo?KEYCODE_BUTTON_Y:KEYCODE_BUTTON_X,right=nintendo?KEYCODE_BUTTON_A:KEYCODE_BUTTON_B;
  face(c,displayLabel(top),top,210,43);face(c,displayLabel(bottom),bottom,210,73);
  face(c,displayLabel(left),left,195,58);face(c,displayLabel(right),right,225,58);
  if(layout==Layout.XBOX)face(c,"X",KEYCODE_BUTTON_MODE,140,43);
  if(nintendo)face(c,"⌂",KEYCODE_BUTTON_MODE,160,76);
  face(c,"−",KEYCODE_BUTTON_SELECT,125,57);face(c,"+",KEYCODE_BUTTON_START,155,57);
  if(hasExtra1)face(c,"1",KEYCODE_BUTTON_1,100,39);
  if(hasExtra2)face(c,"2",KEYCODE_BUTTON_2,179,39);
  if(ultimate){color(ui.border);c.drawRoundRect(133,84,147,86,1,1,p);}
  if(layout!=Layout.XBOX&&!nintendo)circle(c,140,72,4,keys.get(KEYCODE_BUTTON_MODE)?glow:ui.background);
  circle(c,140,layout==Layout.XBOX?58:47,1.6f,connected?ui.success:ui.muted);c.restore();
 }
}
