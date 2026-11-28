package dev.kolmapper;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.content.res.ColorStateList;
import android.view.*;
import android.widget.*;
final class Ui {
 static final int HEADER_LOGO=40;
 static final int SECTION_GAP=24, PICKER_SECTION_GAP=12, TAB_GAP=8, LIBRARY_ROW=64;
 static final int AXIS_SIZE=104, AXIS_PLOT=44, TRIGGER_BAR=4;
 static final int PICKER_ROW=60, PICKER_ICON=40, PICKER_GAP=8, PICKER_LABEL=15, PICKER_SUBTITLE=12;
 static final int DIALOG_BUTTON_HEIGHT=40, BUTTON_HORIZONTAL_PADDING=20, DIALOG_BUTTON_GAP=8, DIALOG_GUTTER=24;
 final int padEdge=0x50ffffff, padShadow=0x35000000;
 static final int DIALOG_RADIUS=24, ACTION_MIN_WIDTH=124, ACTION_LABEL_SPACE=120, HOME_PAD_HEIGHT=124;
 static final int DISCLOSURE_TITLE=17, DISCLOSURE_ARROW=24, HELP_TITLE=16;
 final Context context;final boolean dark;final int background,surface,text,muted,border,accent,accentSoft,onAccent,danger;
 static final int TOUCH=48,ORB=52,CARD=16,PANEL=20,ICON=24;
 final int accentText,raised,success,warning,signalError,bodyTop,bodyBottom,orbSurface,veil;
 Ui(Context c){context=c;dark=c.getSharedPreferences("mapper",0).getBoolean("dark",true);
  background=dark?0xFF0B0F14:0xFFF4F7FA;surface=dark?0xFF131B24:Color.WHITE;
  text=dark?0xFFF1F5F9:0xFF101820;muted=dark?0xFF94A3B8:0xFF526373;
  border=dark?0xFF263442:0xFFD7E0E8;accent=dark?0xFF00D4E8:0xFF007F91;
  accentSoft=dark?0xFF12333B:0xFFE1F3F5;onAccent=dark?0xFF071216:Color.WHITE;danger=dark?0xFFFFA2AC:0xFFB83048;
  accentText=dark?accent:0xff006674;
  raised=dark?0xFF1B2632:0xFFE8EFF4; success=0xff22c55e;warning=0xfff59e0b;signalError=0xffef4444;
  bodyTop=dark?0xff465764:0xffd8e4e9;bodyBottom=dark?0xff23313d:0xffa7b8c3;orbSurface=0xe0131b24;veil=dark?0x780b0f14:0x6b263442;
 }
 int dp(int n){return (int)(n*context.getResources().getDisplayMetrics().density+.5f);}
 GradientDrawable shape(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
 GradientDrawable outline(int color,int radius){GradientDrawable g=shape(color,radius);g.setStroke(dp(1),border);return g;}
 Drawable ripple(Drawable base){android.graphics.drawable.StateListDrawable states=new android.graphics.drawable.StateListDrawable();
  for(int state:new int[]{android.R.attr.state_pressed,android.R.attr.state_focused}){
   Drawable layer=base.getConstantState()==null?base:base.getConstantState().newDrawable().mutate();
   layer.setColorFilter((accent&0x00ffffff)|0x35000000,android.graphics.PorterDuff.Mode.SRC_ATOP);
   states.addState(new int[]{state},layer);
  }
  states.addState(new int[]{},base);return states;}
 Context dialogContext(){return new ContextThemeWrapper(context,dark?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert);}
 TextView text(String value,int size,boolean bold){TextView t=new TextView(context);t.setText(value);t.setTextColor(text);t.setTextSize(size);t.setFontFeatureSettings("kern, tnum");t.setPadding(0,dp(3),0,dp(3));t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",Typeface.NORMAL));return t;}
 TextView caption(String value){TextView t=text(value,14,false);t.setTextColor(muted);t.setLineSpacing(dp(3),1);return t;}
 TextView eyebrow(String value){TextView t=text(value,10,true);t.setTextColor(muted);t.setLetterSpacing(.12f);return t;}
 TextView chip(String value){return chip(value,true);}
 TextView chip(String value,boolean active){TextView t=text(value,11,true);t.setTextColor(active?accentText:muted);t.setBackground(shape(active?accentSoft:raised,7));t.setPadding(dp(9),dp(5),dp(9),dp(5));t.setSingleLine();return t;}
 Button button(String title,boolean primary,Runnable action){
  Button b=new Button(context){
   @Override public void setPressed(boolean pressed){if(pressed!=isPressed())Motion.press(this,pressed);super.setPressed(pressed);}
  };b.setText(title);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(primary?onAccent:text);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
  b.setBackground(ripple(primary?shape(accent,14):outline(surface,14)));b.setMinHeight(dp(48));b.setMinimumHeight(dp(TOUCH));b.setMinimumWidth(dp(TOUCH));b.setSingleLine(false);b.setEllipsize(null);b.setPadding(dp(BUTTON_HORIZONTAL_PADDING),dp(8),dp(BUTTON_HORIZONTAL_PADDING),dp(8));b.setStateListAnimator(null);
  b.setOnClickListener(v->{v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);action.run();});return b;
 }
 Button textButton(String title,Runnable action){
  Button button=button(title,false,action);
  button.setTextColor(accentText);button.setBackground(ripple(shape(Color.TRANSPARENT,CARD)));
  return button;
 }
 void pickerTab(RadioButton tab){
  styleMode(tab);
  tab.setPadding(dp(TAB_GAP),dp(TAB_GAP),dp(TAB_GAP),dp(TAB_GAP));
  StateListDrawable states=new StateListDrawable();
  states.addState(new int[]{android.R.attr.state_checked},shape(accentSoft,TOUCH/2));
  states.addState(new int[]{},shape(surface,TOUCH/2));
  tab.setBackground(ripple(states));
 }
 void styleMode(RadioButton b){
  b.setButtonDrawable((Drawable)null);b.setGravity(Gravity.CENTER);b.setMinHeight(dp(48));b.setMinimumHeight(dp(TOUCH));b.setMinimumWidth(dp(TOUCH));b.setSingleLine(false);b.setEllipsize(null);b.setTextSize(14);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setPadding(dp(BUTTON_HORIZONTAL_PADDING),dp(10),dp(BUTTON_HORIZONTAL_PADDING),dp(10));
  StateListDrawable states=new StateListDrawable();GradientDrawable checked=shape(accentSoft,11);checked.setStroke(dp(1),accent);states.addState(new int[]{android.R.attr.state_checked},checked);states.addState(new int[]{},outline(background,11));b.setBackground(ripple(states));
  b.setTextColor(new ColorStateList(new int[][]{{android.R.attr.state_checked},{}},new int[]{accentText,muted}));
 }
 LinearLayout actionTile(String label,int kind,Runnable action){
  LinearLayout tile=new LinearLayout(context);tile.setGravity(Gravity.CENTER);tile.setPadding(dp(6),dp(6),dp(6),dp(6));tile.setBackground(ripple(outline(surface,13)));tile.setClickable(true);tile.setFocusable(true);tile.setContentDescription(label);tile.setOnClickListener(v->action.run());
  ActionIcon icon=new ActionIcon(context,this,kind,false,label,action);icon.setClickable(false);icon.setFocusable(false);icon.setBackground(null);icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);tile.addView(icon,new LinearLayout.LayoutParams(dp(22),dp(22)));
  TextView name=text(label,12,true);name.setSingleLine(false);name.setEllipsize(null);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.leftMargin=dp(5);tile.addView(name,lp);return tile;
 }
 void addButton(LinearLayout parent,String title,boolean primary,Runnable action){Button b=button(title,primary,action);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(12);parent.addView(b,lp);}
 LinearLayout card(LinearLayout parent){LinearLayout c=new LinearLayout(context);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(20),dp(20),dp(20),dp(20));c.setBackground(outline(surface,22));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(20);parent.addView(c,lp);return c;}
 EditText number(LinearLayout parent,String label,int value){parent.addView(caption(label));EditText e=new EditText(context);e.setSingleLine();e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);e.setText(Integer.toString(value));e.setTextColor(text);e.setTextSize(16);e.setBackground(outline(background,12));e.setPadding(dp(14),dp(10),dp(14),dp(10));e.setSelectAllOnFocus(true);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(48));lp.bottomMargin=dp(12);parent.addView(e,lp);return e;}
 void finishDialog(android.app.AlertDialog dialog){
  Window window=dialog.getWindow();
  window.setWindowAnimations(0);
  window.setBackgroundDrawable(outline(surface,DIALOG_RADIUS));
  window.setDimAmount(.48f);
  window.setLayout(Math.min(dp(560),context.getResources().getDisplayMetrics().widthPixels-dp(32)),-2);
  TextView message=dialog.findViewById(android.R.id.message);
  if(message!=null)message.setTextColor(muted);
  java.util.ArrayList<Button> buttons=new java.util.ArrayList<>();
  for(int id:new int[]{-1,-2,-3}){
   Button button=dialog.getButton(id);
   if(button==null)continue;
   boolean primary=id==android.app.AlertDialog.BUTTON_POSITIVE;
   button.setAllCaps(false);
   button.setTextColor(primary?onAccent:id==android.app.AlertDialog.BUTTON_NEUTRAL?danger:text);
   button.setTextSize(14);
   button.setMinimumHeight(dp(TOUCH));button.setMinimumWidth(dp(TOUCH));
   button.setMinHeight(dp(TOUCH));
   button.setSingleLine(false);
   button.setEllipsize(null);
   int inset=dp((TOUCH-DIALOG_BUTTON_HEIGHT)/2);
   button.setBackground(new InsetDrawable(ripple(primary?shape(accent,16):outline(surface,16)),0,inset,0,inset));
   button.setPadding(dp(BUTTON_HORIZONTAL_PADDING),inset,dp(BUTTON_HORIZONTAL_PADDING),inset);
   buttons.add(button);
   Motion.dialogButton(button);
  }
  if(!buttons.isEmpty()&&buttons.get(0).getParent() instanceof LinearLayout){
   LinearLayout panel=(LinearLayout)buttons.get(0).getParent();
   panel.setPaddingRelative(dp(DIALOG_GUTTER),panel.getPaddingTop(),dp(DIALOG_GUTTER),panel.getPaddingBottom());
   int needed=dp(2*DIALOG_GUTTER+DIALOG_BUTTON_GAP*(buttons.size()-1));
   for(Button button:buttons)needed+=buttonWidth(button);
   final int requiredWidth=needed;
   layoutDialogActions(panel,buttons,requiredWidth>window.getAttributes().width);
   panel.addOnLayoutChangeListener((v,left,top,right,bottom,oldLeft,oldTop,oldRight,oldBottom)->{
    if(right-left!=oldRight-oldLeft)layoutDialogActions(panel,buttons,requiredWidth>right-left);
   });
  }
  Motion.dialog(window.getDecorView());
 }

 int buttonWidth(Button button){
  return Math.max(dp(TOUCH),(int)Math.ceil(android.text.Layout.getDesiredWidth(button.getText(),button.getPaint()))
   +button.getPaddingStart()+button.getPaddingEnd());
 }
 private void layoutDialogActions(LinearLayout panel,java.util.List<Button> buttons,boolean stack){
  panel.setOrientation(stack?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);
  panel.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
  int visible=0;
  for(int i=0;i<panel.getChildCount();i++){
   View child=panel.getChildAt(i);
   if(!(child instanceof Button)){child.setVisibility(View.GONE);continue;}
   if(!buttons.contains(child))continue;
   LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(stack?-1:-2,-2);
   if(visible++>0){if(stack)params.topMargin=dp(DIALOG_BUTTON_GAP);else params.setMarginStart(dp(DIALOG_BUTTON_GAP));}
   child.setLayoutParams(params);
  }
 }
}
