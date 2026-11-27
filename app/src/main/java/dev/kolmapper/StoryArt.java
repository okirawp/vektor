package dev.kolmapper;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Miniature examples for the guide, local data flow and backup file. */
final class StoryArt extends View {
 static final int PRIVACY=5, ARCHIVE=6, SUCCESS=7;
 private static final float[] TARGET_X={235,52,199,221},TARGET_Y={73,36,63,81};
 final int scene;
 final Ui ui;
 float progress=1;
 android.animation.ValueAnimator reveal;
 final Typeface lettering=Typeface.create("sans-serif-medium",Typeface.NORMAL);
 final Path shield=new Path();
 final Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG);
 StoryArt(Context c,Ui ui,int scene){super(c);this.ui=ui;this.scene=scene;
  shield.moveTo(149,30);shield.lineTo(171,38);shield.lineTo(169,57);
  shield.quadTo(167,68,149,78);shield.quadTo(130,68,129,57);shield.lineTo(127,38);shield.close();
  setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
 @Override protected void onAttachedToWindow(){
  super.onAttachedToWindow();
  if(!Motion.enabled(getContext())||getWindowVisibility()!=VISIBLE)return;
  reveal=android.animation.ValueAnimator.ofFloat(0,1);
  reveal.setDuration(scene==SUCCESS?Motion.SUCCESS:Motion.SCREEN);
  reveal.setInterpolator(Motion.EASE);
  reveal.addUpdateListener(a->{progress=(float)a.getAnimatedValue();invalidate();});
  reveal.start();
 }
 private void finishMotion(){
  if(reveal!=null){reveal.cancel();reveal=null;}progress=1;
 }
 @Override protected void onWindowVisibilityChanged(int visibility){
  super.onWindowVisibilityChanged(visibility);if(visibility!=VISIBLE)finishMotion();
 }
 @Override protected void onDetachedFromWindow(){finishMotion();super.onDetachedFromWindow();}
 @Override public boolean performClick(){return super.performClick();}
 void box(Canvas c,float x,float y,float w,float h,float r,int color){ink.setColor(color);c.drawRoundRect(x,y,x+w,y+h,r,r,ink);}
 void label(Canvas c,String s,int x,int y,int size,int color){ink.setColor(color);ink.setTextSize(size);ink.setTypeface(lettering);c.drawText(s,x,y,ink);}
 void screen(Canvas c,int x,int y,int w,int h){box(c,x,y,w,h,9,ui.border);box(c,x+2,y+2,w-4,h-4,8,ui.background);}
 @Override protected void onDraw(Canvas c){
  if(scene==SUCCESS){
   float unit=Math.min(getWidth(),getHeight())/32f;
   c.save();c.translate((getWidth()-32*unit)/2,(getHeight()-32*unit)/2);c.scale(unit,unit);
   ink.setStyle(Paint.Style.STROKE);ink.setStrokeCap(Paint.Cap.ROUND);ink.setStrokeWidth(2.4f);ink.setColor(ui.accentText);
   float first=Math.min(1,progress*3);c.drawLine(6,17,6+6*first,17+6*first,ink);
   if(progress>1f/3){float second=(progress-1f/3)*1.5f;c.drawLine(12,23,12+14*second,23-15*second,ink);}
   ink.setStyle(Paint.Style.FILL);c.restore();return;
  }
  float scale=Math.min(getWidth()/300f,getHeight()/128f);
  c.save();c.translate((getWidth()-300*scale)/2,(getHeight()-128*scale)/2);c.scale(scale,scale);
  if(scene==ARCHIVE){
   for(int i=0;i<3;i++){box(c,29+i*4-(1-progress)*(2-i)*6,30+i*9,86,30,5,i==2?ui.accentSoft:ui.border);}
   label(c,"Düzenler",48,66,11,ui.accentText);label(c,"→",137,68,21,ui.accentText);
   box(c,186,17,70,89,8,ui.background);box(c,196,30,27,4,2,ui.muted);box(c,196,41,43,3,2,ui.border);label(c,".rota",196,85,21,ui.accentText);
  }else if(scene==PRIVACY){
   screen(c,85,12,130,104);box(c,139,18,22,3,2,ui.muted);
   ink.setColor(ui.accentSoft);c.drawPath(shield,ink);
   box(c,140,49,18,14,3,ui.accent);ink.setColor(ui.accent);ink.setStyle(Paint.Style.STROKE);ink.setStrokeWidth(2);c.drawArc(144,42-5*(1-progress),154,56-5*(1-progress),180,180,false,ink);ink.setStyle(Paint.Style.FILL);
   label(c,"Cihaz belleği",115,98,12,ui.text);
  }else{
   screen(c,26,12,248,104);
   c.save();
   if(scene==1){c.translate(3*(1-progress),-2*(1-progress));}
   if(scene==0){
    label(c,"Uygulamalarım",43,37,12,ui.text);label(c,"+",243,37,14,ui.accentText);
    box(c,42,51,215,41,5,ui.surface);box(c,51,61,21,21,5,ui.accentSoft);label(c,"Uygulama",83,77,12,ui.text);label(c,"Aç",229,77,11,ui.accentText);
   }else if(scene==1){
    box(c,40,24,24,24,12,ui.accentSoft);label(c,"V",48,41,12,ui.accentText);
    box(c,72,27,123,62,8,ui.surface);label(c,"Kontroller",84,47,12,ui.text);box(c,81,56,104,23,5,ui.accentSoft);label(c,"+ Kontrol ekle",89,72,10,ui.accentText);
   }else if(scene==2){
    box(c,62,46,61,35,14,ui.accentSoft);label(c,"+",72,69,17,ui.text);label(c,"A",103,66,12,ui.accentText);
    label(c,"→",142,72,20,ui.muted);box(c,184,47,32,32,16,ui.accent);label(c,"A",195,69,15,ui.onAccent);
   }else if(scene==3){
    box(c,60,76,31,18,7,ui.surface);box(c,98,76,31,18,7,ui.surface);
    box(c,200,65,42,32,10,ui.accentSoft);ink.setColor(ui.accent);ink.setStyle(Paint.Style.STROKE);ink.setStrokeWidth(1.4f);c.drawCircle(221,81,12,ink);ink.setStyle(Paint.Style.FILL);label(c,"A",217,85,12,ui.text);
   }else{
    label(c,"A",50,54,14,ui.accentText);label(c,"Basılı tut",76,54,11,ui.text);box(c,153,47,83,5,2,ui.accent);
    label(c,"X",50,82,14,ui.accentText);label(c,"Tekrarla",76,82,11,ui.text);for(int i=0;i<6;i++)box(c,153+i*14,75,7,5,2,i<=progress*6?ui.accent:ui.border);
   }
   if(scene<4){
    float x=TARGET_X[scene],y=TARGET_Y[scene];
    if(scene==3){x-=40*(1-progress);y-=18*(1-progress);}
    ink.setStyle(Paint.Style.STROKE);ink.setStrokeWidth(1.4f);
    ink.setColor(ui.accentText);ink.setAlpha((int)(160*(1-progress)));
    c.drawCircle(x,y,5+13*progress,ink);ink.setStyle(Paint.Style.FILL);
   }
   c.restore();
  }
  c.restore();
 }
}
