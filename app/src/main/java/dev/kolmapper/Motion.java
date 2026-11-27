package dev.kolmapper;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.*;
import android.view.animation.*;

final class Motion {
 static final int PRESS=80, RELEASE=140, COMPONENT=190, SCREEN=260, THEME=300, SUCCESS=380;
 static final Interpolator EASE=new PathInterpolator(.2f,0f,.2f,1f);
 private static final Interpolator RETURN=new OvershootInterpolator(.55f);
 static boolean enabled(Context context){
  return ValueAnimator.areAnimatorsEnabled() && !context.getSharedPreferences("appearance",0).getBoolean("reduced",false);
 }
 static void press(View view,boolean down){
  view.animate().cancel();
  if(!enabled(view.getContext())){view.setScaleX(1);view.setScaleY(1);return;}
  float scale=down?.96f:1f;
  view.animate().scaleX(scale).scaleY(scale).setDuration(down?PRESS:RELEASE).setInterpolator(down?EASE:RETURN).start();
 }
 static void enter(View view){enter(view,0);}
 static void enter(View view,int delay){
  if(!enabled(view.getContext()))return;
  view.setAlpha(.85f);view.setTranslationY(4*view.getResources().getDisplayMetrics().density);
  view.animate().alpha(1).translationY(0).setDuration(COMPONENT).setStartDelay(delay).setInterpolator(EASE)
   .withEndAction(()->view.animate().setStartDelay(0)).start();
 }
 static void transition(ViewGroup root,View previous,View next,int direction){
  if(!enabled(next.getContext())||previous==null||previous.getParent()!=null||previous.getWidth()==0)return;
  float distance=16*next.getResources().getDisplayMetrics().density*direction;
  int duration=direction==0?THEME:SCREEN;
  root.getOverlay().add(previous);
  previous.animate().setStartDelay(0).alpha(0).translationX(-distance).setDuration(duration).setInterpolator(EASE)
    .withEndAction(()->root.getOverlay().remove(previous)).start();
  next.setAlpha(direction==0?0:.35f);next.setTranslationX(distance);
  next.animate().alpha(1).translationX(0).setDuration(duration).setInterpolator(EASE).start();
 }
 static void themeIcon(View view){if(enabled(view.getContext())){view.setRotation(-30);view.animate().rotation(0).setDuration(THEME).setInterpolator(EASE).start();}}
 static void reset(View view){view.animate().setListener(null).cancel();view.animate().setStartDelay(0);view.setScaleX(1);view.setScaleY(1);view.setAlpha(1);view.setTranslationY(0);view.setTranslationX(0);view.setRotation(0);}
}
