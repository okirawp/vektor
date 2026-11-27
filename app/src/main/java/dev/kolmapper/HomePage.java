package dev.kolmapper;
import android.content.Context;
import android.view.*;
import android.widget.*;
/** A bounded page, never a scroll container; secondary instructions yield to the game controls. */
final class HomePage extends ViewGroup {
 final Ui ui;final LinearLayout left,guide;final View hero,message,migration;final java.util.List<View> descriptions;
 boolean measuring;
 @Override public void requestLayout(){if(!measuring)super.requestLayout();}
 boolean wide,showGuide;int column,gap;
 HomePage(Context c,Ui ui,LinearLayout left,View hero,View message,View migration,LinearLayout guide,java.util.List<View> descriptions){super(c);this.ui=ui;this.left=left;this.hero=hero;this.message=message;this.migration=migration;this.guide=guide;this.descriptions=descriptions;addView(left);addView(guide);}
 void natural(View v,int width){v.measure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));}
 @Override protected void onMeasure(int ws,int hs){
  measuring=true;
  int w=MeasureSpec.getSize(ws),h=MeasureSpec.getSize(hs);gap=ui.dp(40);wide=w>=ui.dp(720)&&w>h;
  column=wide?(Math.min(w,ui.dp(1040))-gap)/2:Math.min(w,ui.dp(520));
  hero.setVisibility(VISIBLE);message.setVisibility(VISIBLE);if(migration!=null)migration.setVisibility(VISIBLE);
  natural(left,column);
  if(left.getMeasuredHeight()>h){hero.setVisibility(GONE);natural(left,column);}
  if(left.getMeasuredHeight()>h){message.setVisibility(GONE);natural(left,column);}
  if(left.getMeasuredHeight()>h&&migration!=null){migration.setVisibility(GONE);natural(left,column);}
  guide.setVisibility(VISIBLE);for(View v:descriptions)v.setVisibility(wide?VISIBLE:GONE);natural(guide,column);
  if(wide&&guide.getMeasuredHeight()>h){for(View v:descriptions)v.setVisibility(GONE);natural(guide,column);}
  showGuide=wide||left.getMeasuredHeight()+gap+guide.getMeasuredHeight()<=h;
  if(!showGuide)guide.setVisibility(GONE);
  setMeasuredDimension(w,h);measuring=false;
 }
 @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
  int w=r-l,h=b-t;
  if(wide){int x=(w-column*2-gap)/2;left.layout(x,Math.max(0,(h-left.getMeasuredHeight())/2),x+column,Math.max(0,(h-left.getMeasuredHeight())/2)+left.getMeasuredHeight());int gx=x+column+gap,gy=Math.max(0,(h-guide.getMeasuredHeight())/2);guide.layout(gx,gy,gx+column,gy+guide.getMeasuredHeight());}
  else{int total=left.getMeasuredHeight()+(showGuide?gap+guide.getMeasuredHeight():0),x=(w-column)/2,y=Math.max(0,(h-total)/2);left.layout(x,y,x+column,y+left.getMeasuredHeight());if(showGuide){int gy=y+left.getMeasuredHeight()+gap;guide.layout(x,gy,x+column,gy+guide.getMeasuredHeight());}}
 }
}
