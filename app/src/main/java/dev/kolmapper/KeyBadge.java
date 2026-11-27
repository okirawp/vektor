package dev.kolmapper;

import android.content.Context;
import android.graphics.*;
import android.view.*;

/** A small independent window; in play mode its entire window is touch-through. */
final class KeyBadge extends View {
 final Binding binding;
 final String title;
 final Typeface lettering=Typeface.create("sans-serif-medium",Typeface.NORMAL);
 final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 boolean editing,pressed; final Ui ui;
 KeyBadge(Context context,Binding binding){super(context);ui=new Ui(context);this.binding=binding;title=name(binding.key);setContentDescription(name(binding.key)+" tuş ataması");}
 static String name(int key){
  switch(key){case AnalogInput.LEFT_STICK:return "L ÇUBUK";case AnalogInput.RIGHT_STICK:return "R ÇUBUK";case AnalogInput.LEFT_TRIGGER:return "L2";case AnalogInput.RIGHT_TRIGGER:return "R2";}
  String s=KeyEvent.keyCodeToString(key).replace("KEYCODE_","").replace("BUTTON_","");
  switch(key){case KeyEvent.KEYCODE_DPAD_UP:return "↑";case KeyEvent.KEYCODE_DPAD_DOWN:return "↓";case KeyEvent.KEYCODE_DPAD_LEFT:return "←";case KeyEvent.KEYCODE_DPAD_RIGHT:return "→";default:return s;}
 }
 @Override protected void onDraw(Canvas c){
  float d=getResources().getDisplayMetrics().density,r=getWidth()/2f,cy=getHeight()/2f;
  paint.setStyle(Paint.Style.FILL);paint.setColor(pressed?ui.accent:ui.surface);c.drawCircle(r,cy,Math.min(r,cy)-2*d,paint);
  paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d);paint.setColor(ui.accent);c.drawCircle(r,cy,Math.min(r,cy)-2*d,paint);
  paint.setStyle(Paint.Style.FILL);paint.setColor(pressed?ui.onAccent:ui.text);paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(lettering);
  String name=title;paint.setTextSize(getResources().getDisplayMetrics().scaledDensity*(name.length()>4?10:17));c.drawText(name,r,cy-(paint.ascent()+paint.descent())/2f-3*d,paint);
  if(editing){paint.setColor(ui.muted);paint.setTextSize(getResources().getDisplayMetrics().scaledDensity*9);c.drawText(AnalogInput.stick(binding.key)?"ÇUBUK":binding.hold?"BASILI":"TEKRAR",r,cy+15*d,paint);}

 }
 @Override public boolean performClick(){super.performClick();return true;}
}
