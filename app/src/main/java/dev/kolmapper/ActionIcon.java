package dev.kolmapper;
import android.content.Context;

import android.graphics.*;

import android.view.*;
/** Small native icons: no emoji font differences or idle animations. */
final class ActionIcon extends View {
 static final int SUN=0,MOON=1,PRIVACY=2,HELP=3,BACKUP=4,SETTINGS=5,SHARE=6,NETWORK=7,PAD=8,TARGET=9,ADD=10,CHEVRON=11;
 final Path glyph=new Path();
 final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);final Ui ui;final int kind;final boolean selected;
 ActionIcon(Context c,Ui ui,int kind,boolean selected,String description,Runnable action){
  super(c);this.ui=ui;this.kind=kind;this.selected=selected;setContentDescription(description);setTooltipText(description);setClickable(true);setFocusable(true);
  setBackground(ui.ripple(ui.shape(selected?ui.accentSoft:Color.TRANSPARENT,12)));
  setOnClickListener(v->{
   v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
   if(kind==SHARE && Motion.enabled(c)){
    setTranslationY(-ui.dp(3));animate().translationY(0).setDuration(Motion.RELEASE).start();
   }
   action.run();
  });
 }
 @Override public void setPressed(boolean pressed){if(pressed!=isPressed())Motion.press(this,pressed);super.setPressed(pressed);}
 @Override public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.Button");}
 @Override public boolean performClick(){super.performClick();return true;}
 @Override protected void onDraw(Canvas c){
  c.save();float s=ui.dp(24);c.translate((getWidth()-s)/2,(getHeight()-s)/2);c.scale(s/24,s/24);p.setColor(selected?ui.accent:ui.muted);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.7f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
  if(kind==SUN){c.drawCircle(12,12,4,p);for(int i=0;i<8;i++){double a=Math.PI*i/4;c.drawLine(12+8*(float)Math.cos(a),12+8*(float)Math.sin(a),12+10*(float)Math.cos(a),12+10*(float)Math.sin(a),p);}}
  else if(kind==MOON){Path path=glyph;path.rewind();path.moveTo(19.8f,14.3f);path.cubicTo(14,17.6f,6.7f,12.4f,10.2f,4);path.cubicTo(3.8f,5.5f,2.5f,13.3f,6.8f,17.7f);path.cubicTo(10.9f,21.9f,17.7f,20.7f,19.8f,14.3f);c.drawPath(path,p);}
  else if(kind==PRIVACY){Path path=glyph;path.rewind();path.moveTo(12,3);path.lineTo(20,6);path.lineTo(19,13);path.cubicTo(18,17,15,19.5f,12,21);path.cubicTo(9,19.5f,6,17,5,13);path.lineTo(4,6);path.close();c.drawPath(path,p);c.drawLine(9,11.5f,11,14,p);c.drawLine(11,14,15.5f,9.5f,p);}
  else if(kind==NETWORK){c.drawCircle(12,12,9,p);c.drawOval(8,3,16,21,p);c.drawLine(3,12,21,12,p);}
  else if(kind==PAD){c.drawRoundRect(2,6,22,19,5,5,p);c.drawLine(5,12,11,12,p);c.drawLine(8,9,8,15,p);c.drawCircle(17,11,1,p);}
  else if(kind==TARGET){c.drawCircle(12,12,8,p);c.drawCircle(12,12,3,p);}
  else if(kind==ADD){c.drawLine(4,12,20,12,p);c.drawLine(12,4,12,20,p);}
  else if(kind==CHEVRON){c.drawLine(6,9,12,15,p);c.drawLine(12,15,18,9,p);}
  else if(kind==SETTINGS){
   c.drawLine(3,7,10,7,p);c.drawLine(16,7,21,7,p);c.drawCircle(13,7,3,p);
   c.drawLine(3,17,6,17,p);c.drawLine(12,17,21,17,p);c.drawCircle(9,17,3,p);
  }
  else if(kind==SHARE){
   c.drawCircle(5,12,2.5f,p);c.drawCircle(18,5,2.5f,p);c.drawCircle(18,19,2.5f,p);
   c.drawLine(7,11,16,6,p);c.drawLine(7,13,16,18,p);
  }
  else if(kind==BACKUP){c.drawLine(5,15,5,20,p);c.drawLine(5,20,19,20,p);c.drawLine(19,20,19,15,p);c.drawLine(12,3,12,15,p);c.drawLine(8,7,12,3,p);c.drawLine(12,3,16,7,p);}
  else {c.drawCircle(12,12,9,p);Path path=glyph;path.rewind();path.moveTo(9.5f,8.5f);path.cubicTo(10,5.5f,16.5f,6.5f,14.5f,10);path.cubicTo(14,11,12,11.5f,12,14);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);c.drawCircle(12,17,1,p);}
  c.restore();
 }
}
