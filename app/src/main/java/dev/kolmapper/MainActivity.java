package dev.kolmapper;
import android.app.*;
import android.os.Bundle;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.content.*;
import android.content.pm.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import android.text.*;
import java.util.*;
public class MainActivity extends Activity implements InputManager.InputDeviceListener {
 static java.lang.ref.WeakReference<MainActivity> visible=new java.lang.ref.WeakReference<>(null);
 static void connectionChanged(){MainActivity a=visible.get();if(a!=null)a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())a.refreshConnection();});}
 static boolean isForeground(){return visible.get()!=null;}
 PadView pad;
 View connectionLamp;
 AppPicker picker;
 Bundle restoredPicker;
 boolean pickerOpen,pickerMigration;
 View controllerHelp;
 TextView controllerTitle,controllerStatus,padFeedback;
 int padId=-1, identityVersion;
 String supportPage="";
 String supportParent="";
 String backupStage="";
 boolean backupBusy;
 TextView backupProgress;
 ProgressBar backupSpinner;
 final ArrayDeque<String> inputLog=new ArrayDeque<>();
 TextView readings,history;
 boolean hasLT,hasRT;
 boolean firstRender=true;

boolean listeningForControllers,launchGuideAfterPick;int helpPage;AlertDialog exitDialog;ActionIcon themeButton;
 Ui ui;LinearLayout body;boolean renderedReady,renderedSession,renderedLegacy;String renderedGame="";
 void refreshConnection(){if(pickerOpen&&body!=null)return;MapperService s=MapperService.current();if(body==null||renderedReady!=(s!=null)||renderedSession!=(s!=null&&s.sessionActive())||!renderedGame.equals(Profiles.selected(this))||renderedLegacy!=Profiles.hasLegacy(this))render();}
 @Override public void onCreate(Bundle state){if(state!=null){restoredPicker=state.getBundle("picker_state");pickerOpen=state.getBoolean("picker_open");pickerMigration=state.getBoolean("picker_migration");launchGuideAfterPick=state.getBoolean("picker_guided");profilePage=state.getString("profile_page","");supportPage=state.getString("support_page","");helpPage=state.getInt("guide_step",0);supportParent=state.getString("support_parent","");java.util.ArrayList<String> log=state.getStringArrayList("input_log");if(log!=null)inputLog.addAll(log);}ui=new Ui(this);setTheme(ui.dark?android.R.style.Theme_Material_NoActionBar:android.R.style.Theme_Material_Light_NoActionBar);super.onCreate(state);if(android.os.Build.VERSION.SDK_INT>=34)installBackPreview();else if(android.os.Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::requestExit);render();}
 @Override protected void onResume(){super.onResume();visible=new java.lang.ref.WeakReference<>(this);MapperService s=MapperService.current();if(s!=null)s.onMainForeground();refreshConnection();if(!listeningForControllers){getSystemService(InputManager.class).registerInputDeviceListener(this,new android.os.Handler(getMainLooper()));listeningForControllers=true;}updateControllerStatus();}
 @Override protected void onPause(){if(listeningForControllers){getSystemService(InputManager.class).unregisterInputDeviceListener(this);listeningForControllers=false;}if(visible.get()==this)visible.clear();MapperService s=MapperService.current();if(s!=null&&!isChangingConfigurations())s.onMainBackground();super.onPause();}
 @Override protected void onSaveInstanceState(Bundle state){
  super.onSaveInstanceState(state);
  if(picker!=null)state.putBundle("picker_state",picker.save());
  state.putBoolean("picker_open",pickerOpen);state.putBoolean("picker_migration",pickerMigration);state.putBoolean("picker_guided",launchGuideAfterPick);
  state.putStringArrayList("input_log",new ArrayList<>(inputLog));state.putString("profile_page",profilePage);
  state.putString("support_parent",supportParent);state.putString("support_page",supportPage);state.putInt("guide_step",helpPage);
 }
 @Override protected void onRestart(){super.onRestart();if(!pickerOpen)render();}
 String profilePage="";
 @android.annotation.TargetApi(34)
 private void installBackPreview(){
  getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
   new android.window.OnBackAnimationCallback(){
    boolean preview;
    public void onBackStarted(android.window.BackEvent e){
     preview=Motion.enabled(MainActivity.this)&&(!supportPage.isEmpty()||!profilePage.isEmpty());
     if(preview)Motion.reset(body);
    }
    public void onBackProgressed(android.window.BackEvent e){
     if(!preview)return;
     float progress=e.getProgress();
     int sign=e.getSwipeEdge()==android.window.BackEvent.EDGE_LEFT?1:-1;
     body.setTranslationX(sign*ui.dp(16)*progress);body.setAlpha(1-.12f*progress);
    }
    public void onBackCancelled(){if(preview)Motion.reset(body);preview=false;}
    public void onBackInvoked(){if(preview)Motion.reset(body);preview=false;requestExit();}
   });
 }
 void render(){
  if(body!=null)body.removeCallbacks(paintReadings);readingsQueued=false;
  readings=null;history=null;backupProgress=null;backupSpinner=null;
  pad=null;controllerStatus=null;controllerTitle=null;padFeedback=null;controllerHelp=null;padId=-1;identityVersion++;
  connectionLamp=null;ui=new Ui(this);getWindow().setStatusBarColor(ui.background);getWindow().setNavigationBarColor(ui.background);
  boolean compact=getResources().getConfiguration().screenHeightDp<420;
  getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(ui.background));body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setBackgroundColor(ui.background);body.setPadding(ui.dp(20),ui.dp(compact?6:14),ui.dp(20),ui.dp(compact?6:16));setContentView(body);applySystemBarStyle();
  body.setOnApplyWindowInsetsListener((v,insets)->{int left,top,right,bottom;if(android.os.Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());left=bars.left;top=bars.top;right=bars.right;bottom=pickerOpen?Math.max(bars.bottom,insets.getInsets(WindowInsets.Type.ime()).bottom):bars.bottom;WindowInsetsController controller=v.getWindowInsetsController();if(controller!=null)controller.setSystemBarsAppearance(ui.dark?0:WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);}else{left=insets.getSystemWindowInsetLeft();top=insets.getSystemWindowInsetTop();right=insets.getSystemWindowInsetRight();bottom=insets.getSystemWindowInsetBottom();int light=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;int flags=v.getSystemUiVisibility();v.setSystemUiVisibility(ui.dark?flags&~light:flags|light);}int margin=Math.max(ui.dp(16),(getResources().getDisplayMetrics().widthPixels-left-right-ui.dp(1120))/2);v.setPadding(left+margin,top+ui.dp(compact?6:14),right+margin,bottom+ui.dp(compact?6:16));return insets;});body.requestApplyInsets();
  if(pickerOpen){
   Bundle state=picker==null?restoredPicker:picker.save();
   restoredPicker=null;
   picker=new AppPicker(this,pickerMigration,launchGuideAfterPick,state);
   picker.load();
   return;
  }
  LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
  LinearLayout brand=new LinearLayout(this);brand.setGravity(Gravity.CENTER_VERTICAL);ImageView brandImage=new ImageView(this);brandImage.setImageResource(dev.kolmapper.R.drawable.brand_asset);brandImage.setBackground(ui.shape(0xFF0B1119,11));brandImage.setClipToOutline(true);brandImage.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);brand.addView(brandImage,new LinearLayout.LayoutParams(ui.dp(40),ui.dp(40)));
  TextView title=ui.text("Vektör",28,true);title.setLetterSpacing(-.035f);title.setSingleLine();title.setEllipsize(TextUtils.TruncateAt.END);LinearLayout.LayoutParams titleLp=new LinearLayout.LayoutParams(0,-2,1);titleLp.leftMargin=ui.dp(10);brand.addView(title,titleLp);header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
  header.addView(new ActionIcon(this,ui,ActionIcon.SHARE,false,"Yedek paylaş",this::shareBackup),new LinearLayout.LayoutParams(ui.dp(48),ui.dp(48)));
  themeButton=new ActionIcon(this,ui,ui.dark?ActionIcon.SUN:ActionIcon.MOON,false,ui.dark?"Açık temaya geç":"Koyu temaya geç",()->theme(!ui.dark));header.addView(themeButton,new LinearLayout.LayoutParams(ui.dp(48),ui.dp(48)));header.addView(new ActionIcon(this,ui,ActionIcon.SETTINGS,false,"Ayarlar",()->openSupport("settings")),new LinearLayout.LayoutParams(ui.dp(48),ui.dp(48)));body.addView(header,new LinearLayout.LayoutParams(-1,ui.dp(56)));
  MapperService service=MapperService.current();boolean ready=service!=null,active=ready&&service.sessionActive();String game=Profiles.selected(this);boolean legacy=Profiles.hasLegacy(this);renderedReady=ready;renderedSession=active;renderedGame=game;renderedLegacy=legacy;
  if(!supportPage.isEmpty()){
   renderSupport();
   return;
  }

  if(active) body.addView(footerLink("Oturumu bitir",()->{
   SessionContinuity.clear();
   MapperService current=MapperService.current();
   if(current!=null) current.endSession("user_stopped");
   render();
  }));
  if(legacy) body.addView(footerLink("Önceki düzeni taşı",()->chooseGame(true)));
  ScrollView viewport=new ScrollView(this);
  LinearLayout rows=new LinearLayout(this);
  rows.setOrientation(LinearLayout.VERTICAL);
  List<String> packages=new ArrayList<>(Profiles.known(this));
  if(!game.isEmpty()&&!packages.contains(game)) packages.add(game);
  java.text.Collator order=java.text.Collator.getInstance();
  packages.sort((a,b)->order.compare(Profiles.label(this,a),Profiles.label(this,b)));
  packages.removeIf(pkg->getPackageManager().getLaunchIntentForPackage(pkg)==null);
  if(!profilePage.isEmpty() && packages.contains(profilePage)){
   showProfile(rows,profilePage);
  }else{
   profilePage="";
   addController(rows);
   if(!ready){
    LinearLayout permissions=ui.card(rows);
    permissions.addView(ui.caption("Erişilebilirlik izni, kol girişini seçtiğin uygulamada ekran dokunuşuna çevirir."));
    ui.addButton(permissions,"Kontrol iznini aç",true,this::explainPermission);
   }
   rows.addView(ui.text("Uygulamalarım",22,true));
   LinearLayout pair=null;
   boolean wide=getResources().getConfiguration().screenWidthDp>=840;
   for(int index=0;index<packages.size();index++){
    if(!wide || index%2==0){
     pair=new LinearLayout(this);
     LinearLayout.LayoutParams line=new LinearLayout.LayoutParams(-1,-2);
     line.topMargin=ui.dp(8);rows.addView(pair,line);
    }
    LinearLayout.LayoutParams item=new LinearLayout.LayoutParams(0,-2,1);
    if(wide && index%2==1)item.leftMargin=ui.dp(12);
    View tile=homeGameTile(packages.get(index));
    pair.addView(tile,item);
    if(firstRender && index<3)Motion.enter(tile,index*36);
    if(wide && index%2==0 && index==packages.size()-1)pair.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));
   }
   if(packages.isEmpty()){
    rows.addView(ui.caption("Kontrollerini eşlemek için ilk uygulamanı ekle."));
   }
   TextView add=footerLink("＋  Yeni uygulama ekle",()->chooseGame(false));
   android.graphics.drawable.GradientDrawable dashed=ui.shape(ui.background,12);
   dashed.setStroke(ui.dp(1),ui.border,ui.dp(5),ui.dp(4));
   add.setBackground(ui.ripple(dashed));LinearLayout.LayoutParams addLp=new LinearLayout.LayoutParams(-1,-2);
   addLp.topMargin=ui.dp(12);rows.addView(add,addLp);
  }
  firstRender=false;
  viewport.addView(rows);
  body.addView(viewport,new LinearLayout.LayoutParams(-1,0,1));


 }
 void applySystemBarStyle(){
  if(android.os.Build.VERSION.SDK_INT>=30){WindowInsetsController c=getWindow().getInsetsController();if(c!=null)c.setSystemBarsAppearance(ui.dark?0:WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);}
  else{View decor=getWindow().getDecorView();int light=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;int flags=decor.getSystemUiVisibility();decor.setSystemUiVisibility(ui.dark?flags&~light:flags|light);}
 }
 LinearLayout homeGameTile(String pkg){
  LinearLayout tile=new LinearLayout(this);tile.setGravity(Gravity.CENTER_VERTICAL);tile.setMinimumHeight(ui.dp(72));tile.setPadding(ui.dp(10),ui.dp(8),ui.dp(10),ui.dp(8));tile.setBackground(ui.ripple(ui.outline(ui.surface,Ui.CARD)));tile.setClickable(true);tile.setFocusable(true);String name=Profiles.label(this,pkg);tile.setContentDescription(name+" kontrol düzeni");tile.setOnClickListener(v->navigate(pkg));

  android.graphics.drawable.Drawable image=null;
  try{image=getPackageManager().getApplicationIcon(pkg);}catch(PackageManager.NameNotFoundException ignored){}
  View mark;
  if(image!=null){ImageView icon=new ImageView(this);icon.setImageDrawable(image);mark=icon;}
  else{TextView initial=ui.text(name.isEmpty()?"·":name.substring(0,name.offsetByCodePoints(0,1)),18,true);
   initial.setGravity(Gravity.CENTER);initial.setBackground(ui.shape(ui.raised,10));mark=initial;}
  mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(ui.dp(40),ui.dp(40));ip.rightMargin=ui.dp(12);tile.addView(mark,ip);
  LinearLayout words=new LinearLayout(this);words.setOrientation(LinearLayout.VERTICAL);TextView title=ui.text(name,16,true);title.setSingleLine();title.setEllipsize(TextUtils.TruncateAt.END);words.addView(title);int count=Binding.load(this,pkg).size();TextView detail=ui.chip(count==0?"Atama yok":count+" kontrol",count>0);words.addView(detail,new LinearLayout.LayoutParams(-2,-2));tile.addView(words,new LinearLayout.LayoutParams(0,-2,1));
  Button open=ui.button(count>0?"Aç":"Kontrol ekle",count>0,()->{if(count==0)GuideState.request(this,pkg);openPackage(pkg);});
  open.setContentDescription(name+" uygulamasını aç");open.setPadding(0,0,0,0);tile.addView(open,new LinearLayout.LayoutParams(ui.dp(Ui.ACTION_WIDTH),ui.dp(Ui.ACTION_HEIGHT)));return tile;
 }
 void navigate(String pkg){
  boolean back=pkg.isEmpty();
  profilePage=pkg;
  switchLayout(back?-1:1);

 }
 void openPackage(String pkg){
  if(!pkg.equals(Profiles.selected(this))){
   SessionContinuity.clear();
   MapperService service=MapperService.current();
   if(service!=null) service.endSession("game_changed");
   Profiles.select(this,pkg);
  }
  launchGame();
 }
 void showProfile(LinearLayout rows,String pkg){
  rows.addView(footerLink("‹  Uygulamalarım",()->navigate("")));
  rows.addView(ui.text(Profiles.label(this,pkg),22,true));
  rows.addView(footerLink("Uygulamayı aç",()->openPackage(pkg)));
  for(Binding binding:Binding.load(this,pkg)){
   LinearLayout line=new LinearLayout(this);
   line.setGravity(Gravity.CENTER_VERTICAL);
   line.setMinimumHeight(ui.dp(48));
   line.addView(ui.text(KeyBadge.name(binding.key),16,true),new LinearLayout.LayoutParams(0,-2,1));
   String mode=AnalogInput.stick(binding.key)?"Analog":binding.hold?"Basılı tut":binding.count==1?"Tek dokunuş":"Tekrar · "+binding.interval+" ms";
   line.addView(ui.caption(mode));
   rows.addView(line);
   View rule=new View(this);rule.setBackgroundColor(ui.border);
   rows.addView(rule,new LinearLayout.LayoutParams(-1,1));
  }
 }
 void addController(LinearLayout rows){
  LinearLayout card=new LinearLayout(this);
  card.setOrientation(LinearLayout.VERTICAL);card.setPadding(ui.dp(16),ui.dp(10),ui.dp(16),ui.dp(8));
  card.setBackground(ui.shape(ui.surface,18));
  boolean expanded=supportPage.equals("controller");
  LinearLayout heading=new LinearLayout(this);
  heading.setGravity(Gravity.TOP);
  LinearLayout identity=new LinearLayout(this);identity.setOrientation(LinearLayout.VERTICAL);
  LinearLayout statusLine=new LinearLayout(this);statusLine.setGravity(Gravity.CENTER_VERTICAL);
  connectionLamp=new View(this);connectionLamp.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  LinearLayout.LayoutParams lampSize=new LinearLayout.LayoutParams(ui.dp(8),ui.dp(8));lampSize.rightMargin=ui.dp(8);
  statusLine.addView(connectionLamp,lampSize);
  controllerTitle=ui.text("",expanded?24:17,true);if(!expanded){controllerTitle.setMinHeight(ui.dp(Ui.TOUCH));controllerTitle.setGravity(Gravity.CENTER_VERTICAL);}statusLine.addView(controllerTitle);identity.addView(statusLine);
  controllerStatus=ui.caption("");
  heading.addView(identity,new LinearLayout.LayoutParams(0,-2,1));
  if(!expanded)heading.addView(footerLink("Giriş testi  ›",()->openSupport("controller")));
  card.addView(heading);
  card.addView(controllerStatus);
  pad=new PadView(this,ui,expanded);card.addView(pad,new LinearLayout.LayoutParams(-1,ui.dp(expanded?200:Ui.HOME_PAD_HEIGHT)));
  padFeedback=ui.caption("");padFeedback.setGravity(Gravity.CENTER);
  if(expanded){
   card.addView(padFeedback);
   card.addView(footerLink("Görünümü sıfırla",()->pad.resetView()));
  }
  LinearLayout panes=new LinearLayout(this);boolean wide=expanded&&getResources().getConfiguration().screenWidthDp>=840;
  panes.setOrientation(wide?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);
  panes.addView(card,new LinearLayout.LayoutParams(wide?0:-1,-2,wide?1:0));
  if(expanded){
   LinearLayout metrics=new LinearLayout(this);metrics.setOrientation(LinearLayout.VERTICAL);
   metrics.setPadding(ui.dp(wide?20:0),ui.dp(12),0,0);
   readings=ui.text("",14,false);readings.setFontFeatureSettings("tnum");metrics.addView(readings);
   LinearLayout logHeader=new LinearLayout(this);logHeader.setGravity(Gravity.CENTER_VERTICAL);
   logHeader.addView(ui.text("Giriş geçmişi",18,true),new LinearLayout.LayoutParams(0,-2,1));
   logHeader.addView(footerLink("Sıfırla",()->{inputLog.clear();refreshDiagnostics();}));metrics.addView(logHeader);
   history=ui.caption("");metrics.addView(history);panes.addView(metrics,new LinearLayout.LayoutParams(wide?0:-1,-2,wide?1:0));
  }
  rows.addView(panes);
  if(expanded){
   LinearLayout help=ui.card(rows);
   help.addView(ui.text("Kolu Bluetooth veya USB ile bağla",Ui.HELP_TITLE,true));
   help.addView(ui.caption("Bluetooth için Android ayarlarından kolunu eşleştir. Kablolu kullanım için veri aktarımını destekleyen bir USB kablosu kullan."));
   controllerHelp=help;
  }
  updateControllerStatus();
 }
 boolean dirtyReadings;
 boolean readingsQueued;
 
 final Runnable paintReadings=()->{
  readingsQueued=false;
  if(pad!=null&&readings!=null&&dirtyReadings)refreshDiagnostics();
 };
 void queueReadings(){
  if(readings==null || readingsQueued)return;
  dirtyReadings=true;readingsQueued=true;body.postOnAnimation(paintReadings);
 }
 String triggerReading(boolean analog,float value,int key){
  return analog?"%"+Math.round(100*value):pad.keys.get(key)?"Basılı":"Serbest";
 }
 void refreshDiagnostics(){
  dirtyReadings=false;
  if(readings==null||pad==null)return;
  StringBuilder down=new StringBuilder();
  for(int i=0;i<pad.keys.size();i++)if(pad.keys.valueAt(i)){
   if(down.length()>0)down.append(" + ");down.append(PadView.label(pad.keys.keyAt(i)));
  }
  padFeedback.setText(down.length()==0?(pad.connected?"Basılı tuş yok":"Kol bekleniyor"):"Basılı  ·  "+down);
  controllerHelp.setVisibility(pad.connected?View.GONE:View.VISIBLE);
  readings.setTextColor(pad.connected?ui.text:ui.muted);
  readings.setText(pad.connected?String.format(Locale.getDefault(),
   "L   X %+.2f    Y %+.2f     •     R   X %+.2f    Y %+.2f\nL2   %s         R2   %s%s",
   pad.lx,pad.ly,pad.rx,pad.ry,triggerReading(hasLT,pad.lt,KeyEvent.KEYCODE_BUTTON_L2),
   triggerReading(hasRT,pad.rt,KeyEvent.KEYCODE_BUTTON_R2),deadzone):"Kol bağlandığında eksen ve tetik değerleri burada görünür.");
  history.setText(inputLog.isEmpty()?"Henüz tuş olayı yok":TextUtils.join("\n",inputLog));
 }
 String deadzone="";
 void logKey(KeyEvent e){
  if(readings==null || e.getRepeatCount()!=0)return;
  long seconds=e.getEventTime()/1000;
  inputLog.addFirst(String.format(Locale.ROOT,"%02d:%02d.%03d   %s   %s",seconds/60%60,seconds%60,e.getEventTime()%1000,
   PadView.label(e.getKeyCode()),e.getAction()==KeyEvent.ACTION_DOWN?"Basıldı":"Bırakıldı"));
  while(inputLog.size()>8)inputLog.removeLast();
  queueReadings();
 }
 void updateControllerStatus(){
  if(pad==null)return;
  InputDevice selected=InputDevice.getDevice(padId);
  if(!ControllerIdentity.isPad(selected)){
   selected=null;
   for(int id:InputDevice.getDeviceIds()){
    InputDevice device=InputDevice.getDevice(id);
    if(ControllerIdentity.isPad(device)){selected=device;break;}
   }
  }
  selectController(selected);
 }
 void selectController(InputDevice device){
  int version=++identityVersion;
  padId=device==null?-1:device.getId();
  pad.connected=device!=null;if(device==null)pad.resetView();pad.reset();pad.setAlpha(device==null?.46f:1f);
  pad.ultimate=device!=null&&ControllerIdentity.ultimate2c(device.getVendorId(),device.getProductId());
  boolean[] additional=device==null?null:device.hasKeys(KeyEvent.KEYCODE_BUTTON_1,KeyEvent.KEYCODE_BUTTON_2);
  pad.hasExtra1=additional!=null&&additional.length>0&&additional[0];
  pad.hasExtra2=additional!=null&&additional.length>1&&additional[1];
  hasLT=device!=null&&(device.getMotionRange(MotionEvent.AXIS_LTRIGGER)!=null||device.getMotionRange(MotionEvent.AXIS_BRAKE)!=null);
  hasRT=device!=null&&(device.getMotionRange(MotionEvent.AXIS_RTRIGGER)!=null||device.getMotionRange(MotionEvent.AXIS_GAS)!=null);
  InputDevice.MotionRange range=device==null?null:device.getMotionRange(MotionEvent.AXIS_X);
  deadzone=range==null?"":String.format(Locale.getDefault(),"\nSol X ölü bölgesi   %.2f",range.getFlat());
  refreshDiagnostics();
  connectionLamp.setBackground(ui.shape(device==null?ui.muted:ui.success,4));
  controllerTitle.setText(device==null?"Kol bağlı değil":ControllerIdentity.name(device));
  controllerStatus.setText(device==null?"Bluetooth veya USB ile bağla":"Bağlantı denetleniyor…");

  boolean detailed=supportPage.equals("controller");
  if(device!=null)new Thread(()->{
   String transport=ControllerIdentity.connection(this,device);
   if(android.os.Build.VERSION.SDK_INT>=31){
    try{android.hardware.BatteryState battery=device.getBatteryState();
     float level=battery.getCapacity();
     if(battery.isPresent()&&level>=0&&level<=1)transport+="  ·  Pil %"+Math.round(level*100);
    }catch(SecurityException|UnsupportedOperationException ignored){}
   }
   String connection=detailed?transport+String.format(Locale.ROOT,"  ·  %04X:%04X",device.getVendorId(),device.getProductId()):transport;
   runOnUiThread(()->{if(version==identityVersion && !isDestroyed() && controllerStatus!=null)controllerStatus.setText(connection);});
  },"Vektor-controller-info").start();
 }
 boolean previewInput(InputEvent event){
  if(pad==null || !hasWindowFocus() || !ControllerIdentity.isPad(event.getDevice()))return false;
  if(event.getDeviceId()!=padId)selectController(event.getDevice());
  return supportPage.equals("controller");
 }
 @Override public boolean dispatchKeyEvent(KeyEvent e){
  if((e.getAction()==KeyEvent.ACTION_DOWN||e.getAction()==KeyEvent.ACTION_UP)
    && (KeyEvent.isGamepadButton(e.getKeyCode())||e.getKeyCode()>=KeyEvent.KEYCODE_DPAD_UP&&e.getKeyCode()<=KeyEvent.KEYCODE_DPAD_CENTER)
    && previewInput(e) && pad.key(e.getKeyCode(),e.getAction()==KeyEvent.ACTION_DOWN)){
   logKey(e);
   return true;
  }
  return super.dispatchKeyEvent(e);
 }
 @Override public boolean dispatchGenericMotionEvent(MotionEvent e){
  if(e.isFromSource(InputDevice.SOURCE_JOYSTICK) && e.getActionMasked()==MotionEvent.ACTION_MOVE && previewInput(e)){
   pad.motion(e);queueReadings();return true;
  }
  return super.dispatchGenericMotionEvent(e);
 }
 @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(!focus&&pad!=null){pad.reset();refreshDiagnostics();}}
 @Override public void onInputDeviceAdded(int id){updateControllerStatus();}
 @Override public void onInputDeviceChanged(int id){if(id==padId||padId==-1)updateControllerStatus();}
 @Override public void onInputDeviceRemoved(int id){if(id==padId)updateControllerStatus();}
 void launchGame(){
  String pkg=Profiles.selected(this);
  if(MapperService.current()!=null && GuideState.active(this,pkg,Binding.load(this,pkg).isEmpty())){
   AlertDialog hint=new AlertDialog.Builder(ui.dialogContext())
    .setTitle("İlk kontrolünü ekle")
    .setMessage("Uygulama açılınca kenardaki yuvarlak kol simgesine dokun. Kontrol ekle’yi seç, kolunda bir tuşa bas ve ekrandaki karşılığına dokun.")
    .setNegativeButton("Vazgeç",null)
    .setPositiveButton("Uygulamayı aç",(d,w)->{
     if(pkg.equals(Profiles.selected(this))){GuideState.skip(this,pkg);startSelectedApp();}
    }).create();
   hint.show();ui.finishDialog(hint);return;
  }
  startSelectedApp();
 }
 void startSelectedApp(){String pkg=Profiles.selected(this);Intent launch=getPackageManager().getLaunchIntentForPackage(pkg);MapperService s=MapperService.current();if(launch==null){Toast.makeText(this,"Uygulama bulunamadı. Başka bir oyun seç.",Toast.LENGTH_LONG).show();return;}if(s==null){explainPermission();return;}if(!s.openSession()){Toast.makeText(this,"Ekranın kilidi açıkken yeniden dene.",Toast.LENGTH_SHORT).show();return;}try{startActivity(launch);}catch(ActivityNotFoundException|SecurityException e){s.endSession("game_unavailable");Toast.makeText(this,"Bu uygulama açılamadı.",Toast.LENGTH_LONG).show();}}
 void chooseGame(boolean migration){
  pickerMigration=migration;pickerOpen=true;switchLayout(1);
 }
 void closePicker(){
  picker=null;pickerOpen=false;launchGuideAfterPick=false;
  ((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE))
   .hideSoftInputFromWindow(body.getWindowToken(),0);
  switchLayout(-1);
 }
 void confirmPicker(List<String> packages){
  if(packages.isEmpty()||!pickerOpen)return;
  String first=packages.get(0);
  SessionContinuity.clear();
  MapperService service=MapperService.current();
  if(service!=null)service.endSession("game_changed");
  if(pickerMigration&&!Profiles.importLegacy(this,first)){
   Toast.makeText(this,"Düzen aktarılamadı. Mevcut uygulama düzeni korundu.",Toast.LENGTH_LONG).show();
   return;
  }
  for(int i=packages.size()-1;i>=0;i--)Profiles.select(this,packages.get(i));
  boolean guided=launchGuideAfterPick;
  closePicker();
  if(guided){GuideState.request(this,first);launchGame();}
 }
 void switchLayout(int direction){
  View previous=body;
  ViewGroup parent=(ViewGroup)body.getParent();
  render();Motion.transition(parent,previous,body,direction);
 }
 void openSupport(String page){
  boolean back=page.isEmpty();
  if(back){
   supportPage=supportParent;supportParent="";
  }else{
   if(!page.equals(supportPage))supportParent=supportPage.equals("settings")&&!page.equals("settings")?"settings":"";
   supportPage=page;
  }
  switchLayout(back?-1:1);
 }
 void shareBackup(){
  if(backupBusy)return;
  AlertDialog dialog=new AlertDialog.Builder(ui.dialogContext()).setTitle("Yedeği paylaş")
   .setMessage("Önce .rota dosyasını kaydet. Ardından dosyayı göndereceğin uygulamayı seç.")
   .setNegativeButton("Vazgeç",null).setPositiveButton("Konum seç",(d,w)->createBackupFile(82)).create();
  dialog.getWindow().setWindowAnimations(0);
  dialog.show();
  ui.finishDialog(dialog);
 }
 void renderSupport(){
  TextView back=footerLink("‹  Geri",()->openSupport(""));
  body.addView(back,new LinearLayout.LayoutParams(-2,ui.dp(48)));
  ScrollView viewport=new ScrollView(this);
  LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
  SupportPages pages=new SupportPages(this,content);
  switch(supportPage){
   case "settings": pages.settings();break;
   case "controller": addController(content);break;
   case "guide": pages.guide(helpPage);break;
   case "privacy": pages.privacy();break;
   case "backups": pages.backups();break;
   default: supportPage="";render();return;
  }
  if(supportPage.equals("guide")){
   viewport.setFillViewport(true);content.setGravity(Gravity.CENTER_VERTICAL);
  }
  viewport.addView(content);body.addView(viewport,new LinearLayout.LayoutParams(-1,0,1));if(pages.actions()!=null)body.addView(pages.actions());
 }
 void showHelp(){showHelpPage(0);}
 void showHelpPage(int page){int next=Math.max(0,Math.min(4,page));int direction=Integer.compare(next,helpPage);helpPage=next;if(!supportPage.equals("guide"))openSupport("guide");else if(direction!=0)switchLayout(direction);}
 void launchGuidedGame(){String game=Profiles.selected(this);if(game.isEmpty()){launchGuideAfterPick=true;chooseGame(false);return;}GuideState.request(this,game);if(MapperService.current()==null)explainPermission();else launchGame();}
 TextView footerLink(String label,Runnable action){TextView t=ui.text(label,12,false);t.setTextColor(ui.muted);t.setGravity(Gravity.CENTER);t.setPadding(ui.dp(12),ui.dp(8),ui.dp(12),ui.dp(8));t.setMinimumHeight(ui.dp(48));t.setMinimumWidth(ui.dp(48));t.setBackground(ui.ripple(ui.shape(android.graphics.Color.TRANSPARENT,10)));t.setContentDescription(label);t.setClickable(true);t.setFocusable(true);t.setOnClickListener(v->action.run());return t;}
 void requestExit(){if(pickerOpen){closePicker();return;}if(!supportPage.isEmpty()){openSupport("");return;}if(!profilePage.isEmpty()){navigate("");return;}if(isFinishing()||exitDialog!=null&&exitDialog.isShowing())return;MapperService service=MapperService.current();String message=service!=null&&service.sessionActive()?"Vektör'den çıkarsan açık oyun kontrolü oturumu da durur. Atamaların ve makroların kayıtlı kalır.":"Vektör'den çıkmak istiyor musun? Atamaların ve ayarların kayıtlı kalır.";AlertDialog dialog=new AlertDialog.Builder(ui.dialogContext()).setTitle("Uygulamadan çıkılsın mı?").setMessage(message).setNegativeButton("Uygulamada kal",null).setPositiveButton("Çık",(d,w)->closeTask()).create();exitDialog=dialog;dialog.setOnDismissListener(d->{if(exitDialog==dialog)exitDialog=null;});dialog.show();ui.finishDialog(dialog);}
 void explainPermission(){AlertDialog d=new AlertDialog.Builder(ui.dialogContext()).setTitle("Oyun kontrolü izni").setMessage("Vektör, kol tuşlarını ve çubuk hareketlerini algılamak, açık oyunu ayırt etmek ve seçtiğin ekran noktalarına dokunmak için erişilebilirlik servisini kullanır.\n\nPencere kimliği, türü ve odak bilgisiyle oyuna dönmeni algılar. Pencere listesini almak için Android ekran içeriği erişim yeteneği ister; Vektör ekran metni veya görünüm ağacını istemez, şifreleri ve mesajları okumaz. Veri göndermez. Kontrol yalnızca tanımladığın oyunlarda açık oturum sırasında çalışır.\n\nİzni Android ayarlarından istediğin zaman kaldırabilirsin.").setNegativeButton("Vazgeç",null).setPositiveButton("Kabul et ve devam et",(dialog,w)->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))).create();d.show();ui.finishDialog(d);}
 void closeTask(){SessionContinuity.clear();MapperService s=MapperService.current();if(s!=null)s.endSession("app_closed");finishAndRemoveTask();}
 @Override public void onConfigurationChanged(android.content.res.Configuration config){
  super.onConfigurationChanged(config);render();
 }
 @Override public void onBackPressed(){requestExit();}
 @Override protected void onDestroy(){if(exitDialog!=null)exitDialog.dismiss();if(isFinishing()&&!isChangingConfigurations()){SessionContinuity.clear();MapperService s=MapperService.current();if(s!=null)s.endSession("app_closed");}super.onDestroy();}
 void showPrivacy(){openSupport("privacy");}
 void showBackups(){openSupport("backups");}
 void exportBackup(){createBackupFile(80);}
 void createBackupFile(int requestCode){if(backupBusy)return;
  String date=new java.text.SimpleDateFormat("yyyyMMdd-HHmm",Locale.ROOT).format(new Date());
  Intent request=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
   .setType("application/octet-stream").putExtra(Intent.EXTRA_TITLE,"Vektor-"+date+".rota");
  startActivityForResult(request,requestCode);
 }
 void importBackup(){if(backupBusy)return;
  startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"),81);
 }
 void updateBackupProgress(){
  if(backupProgress!=null){backupProgress.setText(backupStage);backupProgress.setVisibility(backupStage.isEmpty()?View.GONE:View.VISIBLE);}
  if(backupSpinner!=null)backupSpinner.setVisibility(backupBusy?View.VISIBLE:View.GONE);
 }
 void backupStep(String message,boolean busy){
  runOnUiThread(()->{backupStage=message;backupBusy=busy;if(!isDestroyed())updateBackupProgress();});
 }
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);
  if(result!=RESULT_OK||data==null||data.getData()==null)return;
  Uri uri=data.getData();
  if(request==81){readBackup(uri);return;}
  if(request!=80&&request!=82)return;
  backupStep("Dosya kaydediliyor…",true);
  new Thread(()->{
   try{
    byte[] bytes=BackupCodec.encode(Backups.snapshot(this));
    try(java.io.OutputStream out=getContentResolver().openOutputStream(uri,"wt")){
     if(out==null)throw new java.io.IOException("Dosya açılamadı");out.write(bytes);
    }
    getSharedPreferences("appearance",0).edit().putLong("last_export",System.currentTimeMillis()).apply();
    runOnUiThread(()->{
     if(isDestroyed())return;
     backupBusy=false;backupStage="Yedek kaydedildi.";render();backupMessage(backupStage,true);
     if(request==82){Intent send=new Intent(Intent.ACTION_SEND).setType("application/octet-stream")
      .putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      send.setClipData(ClipData.newRawUri("Vektör yedeği",uri));startActivity(Intent.createChooser(send,"Yedek paylaş"));}
    });
   }catch(Exception e){backupMessage("Yedek yazılamadı. Başka bir konum seç.");}
  },"Vektor-export").start();
 }
 void backupMessage(String message){backupMessage(message,false);}
 void backupMessage(String message,boolean success){
  runOnUiThread(()->{
   if(isDestroyed())return;backupBusy=false;backupStage=message;updateBackupProgress();
   TextView snack=ui.text(message,14,true);
   snack.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);snack.setPadding(ui.dp(16),ui.dp(12),ui.dp(16),ui.dp(12));snack.setBackground(ui.shape(ui.raised,12));
   LinearLayout notice=new LinearLayout(this);notice.setGravity(Gravity.CENTER_VERTICAL);notice.setBackground(ui.outline(ui.raised,12));
   if(success)notice.addView(new StoryArt(this,ui,StoryArt.SUCCESS),new LinearLayout.LayoutParams(ui.dp(36),ui.dp(32)));
   notice.addView(snack,new LinearLayout.LayoutParams(0,-2,1));
   LinearLayout owner=body;owner.addView(notice);Motion.enter(notice);
   notice.postDelayed(()->owner.removeView(notice),4000);
  });
 }
 String backupSummary(Map<String,Object> values){
  Set<?> apps=(Set<?>)values.get("games");int controls=0;
  for(Object pkg:apps){
   Object saved=values.get(ProfileRules.key((String)pkg,"bindings"));
   if(saved instanceof String)try{controls+=Binding.parse((String)saved).size();}catch(org.json.JSONException ignored){}
  }
  return apps.size()+" uygulama · "+controls+" kayıtlı kontrol\n\nAynı uygulamaların düzenleri değişir. Diğer düzenler korunur. İşlemden önce cihazında bir kurtarma kopyası saklanır.";
 }
 void readBackup(Uri uri){if(backupBusy)return;backupStep("Dosya okunuyor…",true);new Thread(()->{try{byte[] bytes=Backups.read(uri==null?new java.io.FileInputStream(new java.io.File(getFilesDir(),"before-restore.rota")):getContentResolver().openInputStream(uri));backupStep("İçerik doğrulanıyor…",true);Map<String,Object> values=BackupCodec.decode(bytes);Backups.validate(values);runOnUiThread(()->{if(isFinishing()||isDestroyed())return;backupStep("Dosya doğrulandı · İçe aktarmaya hazır",false);AlertDialog d=new AlertDialog.Builder(ui.dialogContext()).setTitle("Düzenleri geri yükle?").setMessage(backupSummary(values)).setOnCancelListener(dlg->backupStep("",false)).setNegativeButton("Vazgeç",(dlg,w)->backupStep("",false)).setPositiveButton("Geri yükle",(v,w)->{backupStep("Düzenler geri yükleniyor…",true);SessionContinuity.clear();MapperService service=MapperService.current();if(service!=null)service.endSession("restore");new Thread(()->{try{Backups.restore(this,values);runOnUiThread(()->{if(!isDestroyed()){render();backupMessage("Düzenlerin geri yüklendi.",true);}});}catch(Exception e){backupMessage("Geri yükleme tamamlanamadı. Kurtarma kopyası cihazında korunur.");}},"Vektor-restore").start();}).create();d.show();ui.finishDialog(d);});}catch(Exception e){backupMessage("Dosya geçerli bir Vektör / Rota yedeği değil veya bozulmuş. Atamaların değiştirilmedi.");}},"Vektor-import").start();}
 void theme(boolean dark){if(ui.dark==dark)return;Profiles.prefs(this).edit().putBoolean("dark",dark).apply();setTheme(dark?android.R.style.Theme_Material_NoActionBar:android.R.style.Theme_Material_Light_NoActionBar);MapperService service=MapperService.current();if(service!=null)service.restyle();switchLayout(0);Motion.themeIcon(themeButton);}
}
