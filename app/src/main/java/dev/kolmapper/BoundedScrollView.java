package dev.kolmapper;
import android.content.Context;
import android.view.View;
import android.widget.ScrollView;
/** A compact form grows with its content and scrolls only at the window's safe limit. */
final class BoundedScrollView extends ScrollView {
 final int maximum;
 BoundedScrollView(Context context,int maximum){super(context);this.maximum=maximum;setFillViewport(false);setScrollbarFadingEnabled(false);}
 @Override protected void onMeasure(int widthSpec,int heightSpec){
  int mode=View.MeasureSpec.getMode(heightSpec),available=View.MeasureSpec.getSize(heightSpec);
  int limit=mode==View.MeasureSpec.UNSPECIFIED?maximum:Math.min(maximum,available);
  super.onMeasure(widthSpec,View.MeasureSpec.makeMeasureSpec(Math.max(0,limit),View.MeasureSpec.AT_MOST));
 }
}
