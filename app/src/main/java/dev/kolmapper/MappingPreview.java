package dev.kolmapper;
import android.content.Context;
import android.graphics.*;
import android.view.View;
/** Static onboarding illustration, never represents a live controller connection. */
final class MappingPreview extends View {
 final Ui ui;final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 MappingPreview(Context c,Ui ui){super(c);this.ui=ui;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
 @Override protected void onDraw(Canvas c){
  super.onDraw(c);float scale=Math.min(getWidth()/420f,getHeight()/152f);c.save();c.translate((getWidth()-420*scale)/2,(getHeight()-152*scale)/2);c.scale(scale,scale);
  p.setStyle(Paint.Style.FILL);p.setColor(ui.accentSoft);c.drawRoundRect(4,16,164,136,28,28,p);
  p.setColor(ui.surface);c.drawRoundRect(234,12,414,140,20,20,p);p.setStyle(Paint.Style.STROKE);p.setColor(ui.border);p.setStrokeWidth(1.4f);c.drawRoundRect(234,12,414,140,20,20,p);
  p.setColor(ui.accent);p.setStrokeWidth(3);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(42,77,76,77,p);c.drawLine(59,60,59,94,p);c.drawCircle(118,62,5,p);c.drawCircle(135,79,5,p);c.drawCircle(101,79,5,p);c.drawCircle(118,96,5,p);
  p.setColor(ui.muted);p.setStrokeWidth(1.6f);c.drawLine(179,76,219,76,p);c.drawLine(212,69,219,76,p);c.drawLine(212,83,219,76,p);
  p.setColor(ui.border);p.setStrokeWidth(1);c.drawLine(258,43,337,43,p);c.drawLine(258,52,311,52,p);c.drawRoundRect(255,69,389,119,10,10,p);
  p.setColor(ui.accent);p.setStrokeWidth(2);c.drawCircle(355,95,14,p);c.drawLine(355,74,355,81,p);c.drawLine(355,109,355,116,p);c.drawLine(334,95,341,95,p);c.drawLine(369,95,376,95,p);p.setStyle(Paint.Style.FILL);c.drawCircle(355,95,3,p);c.restore();
 }
}
