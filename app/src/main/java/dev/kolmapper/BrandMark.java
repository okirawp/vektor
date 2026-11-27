package dev.kolmapper;
import android.content.Context;
import android.graphics.*;
import android.view.View;
/** Quiet controller/orbit companion mark for the floating control, independent of the launcher bitmap. */
final class BrandMark extends View {
 final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);final int color;
 BrandMark(Context c,int color){super(c);this.color=color;setContentDescription("Vektör oyun kolu simgesi");}
 private static final Path OUTLINE=outline();
 private static Path outline(){Path b=new Path();b.moveTo(5,22);b.lineTo(8,12);b.quadTo(9,8,13,11);b.lineTo(19,11);b.quadTo(23,8,24,12);b.lineTo(27,22);b.quadTo(28,26,24,24);b.lineTo(20,20);b.lineTo(12,20);b.lineTo(8,24);b.quadTo(4,27,5,22);return b;}
 static void draw(Canvas c,Paint p,float x,float y,float size,int color){c.save();c.translate(x,y);c.scale(size/32,size/32);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.8f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
  c.drawPath(OUTLINE,p);c.drawLine(10,13,10,18,p);c.drawLine(7.5f,15.5f,12.5f,15.5f,p);p.setStyle(Paint.Style.FILL);c.drawCircle(21,13,1,p);c.drawCircle(24,16,1,p);c.drawCircle(18,16,1,p);c.drawCircle(21,19,1,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.4f);c.drawArc(2,3,29,28,203,128,false,p);c.drawArc(1,7,31,27,0,146,false,p);c.drawLine(12,26,16,30,p);c.drawLine(16,30,21,25,p);c.restore();}
 @Override protected void onDraw(Canvas c){float size=Math.min(getWidth(),getHeight());draw(c,paint,(getWidth()-size)/2,(getHeight()-size)/2,size,color);}
}
