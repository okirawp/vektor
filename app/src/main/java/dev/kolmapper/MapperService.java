package dev.kolmapper;
import android.accessibilityservice.*;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.hardware.input.InputManager;
import android.os.*;
import android.util.DisplayMetrics;
import android.view.*;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.*;
import java.util.*;

public class MapperService extends AccessibilityService implements InputManager.InputDeviceListener {
 private static java.lang.ref.WeakReference<MapperService> ref=new java.lang.ref.WeakReference<>(null);
 public static MapperService current(){return ref.get();}
 final Handler handler=new Handler(Looper.getMainLooper());
 final Map<Integer,Binding> bindings=new LinkedHashMap<>();
 final Map<Integer,RepeatState> states=new LinkedHashMap<>();
 final Map<Integer,GestureDescription.StrokeDescription> tapStrokes=new HashMap<>(),holdStarts=new HashMap<>();
 // Track physical device so releasing one controller does not affect another controller's hold.
 final InputLedger input=new InputLedger();
 final Map<Integer,float[]> vectors=new HashMap<>();
 final Map<Integer,Integer> axisOwners=new HashMap<>();
 LinearLayout emptyHelp; WindowManager wm; Context windowContext; IBinder overlayToken; View panel,picker,orb,backdrop; WindowManager.LayoutParams orbParams,panelParams; TextView label,coach; FrameLayout editorRoot;
 final Map<Integer,KeyBadge> badges=new LinkedHashMap<>();
 boolean editing,learningOverlay,pendingHold,orbTouching; int learningKey=-1; long scheduledAt; AlertDialog settingsDialog;
 boolean armed,busy,scheduled; int pendingKey=-1,pendingInterval=100,pendingCount,pendingDuration=8,pendingRadius=72;
 long observedControllerEvents,gesturesCompleted,gesturesCanceled;
 final Map<Integer,Integer> keyDowns=new HashMap<>(),keyUps=new HashMap<>();
 final List<Integer> keyOrder=new ArrayList<>();

 boolean sessionActive,closingEditor,connectionListenersRegistered; long transitionVersion; int editorPendingKey=-1,editorBuilds,editorOpens;
 String activePackage="",pendingPackage="";boolean foregroundAllowed;long profileEpoch;Set<String> knownProfiles=new HashSet<>();
 final Map<Integer,String> windowPackages=new LinkedHashMap<Integer,String>(32,.75f,true){@Override protected boolean removeEldestEntry(Map.Entry<Integer,String> eldest){return size()>256;}};
 final Runnable foregroundCheck=this::reconcileForeground;
 final ArrayDeque<String> focusTrace=new ArrayDeque<>();
 String stopReason="not_started";long lastEditorAttachMs,editorFocusGraceUntil;
 final Runnable sessionTick=this::checkSession;
 final Runnable showAfterMain=()->{if(canShowUi())showLauncher();};
 final Runnable tick=this::pump;
 final BroadcastReceiver screenReceiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){
  if(Intent.ACTION_SCREEN_OFF.equals(i.getAction()))endSession("screen_locked");
  else if(!screenSafe())endSession("screen_locked");
 }};
 @Override protected void onServiceConnected(){
  ref=new java.lang.ref.WeakReference<>(this);overlayToken=null;
  Display display=((WindowManager)super.getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
  windowContext=Build.VERSION.SDK_INT>=31?createWindowContext(display,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,null):Build.VERSION.SDK_INT>=30?createDisplayContext(display).createWindowContext(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,null):this;
  wm=(WindowManager)windowContext.getSystemService(WINDOW_SERVICE);activePackage=Profiles.selected(this);reload();
  if(!connectionListenersRegistered){
   ((InputManager)getSystemService(INPUT_SERVICE)).registerInputDeviceListener(this,handler);
   IntentFilter screens=new IntentFilter(Intent.ACTION_SCREEN_OFF);screens.addAction(Intent.ACTION_SCREEN_ON);screens.addAction(Intent.ACTION_USER_PRESENT);registerReceiver(screenReceiver,screens);connectionListenersRegistered=true;
  }
  syncMotionSources();
  if(SessionContinuity.canResume(this)){
   windowPackages.putAll(SessionContinuity.remembered());knownProfiles=Profiles.known(this);sessionActive=true;stopReason="reconnected";
   handler.removeCallbacks(sessionTick);handler.post(sessionTick);
  }else SessionContinuity.clear();
  MainActivity.connectionChanged();
 }
 public boolean sessionActive(){return sessionActive;}
 boolean screenSafe(){return ((PowerManager)getSystemService(POWER_SERVICE)).isInteractive()&&!((KeyguardManager)getSystemService(KEYGUARD_SERVICE)).isKeyguardLocked();}
 boolean canShowUi(){return foregroundAllowed&&SessionRules.show(sessionActive,((PowerManager)getSystemService(POWER_SERVICE)).isInteractive(),((KeyguardManager)getSystemService(KEYGUARD_SERVICE)).isKeyguardLocked(),MainActivity.isForeground());}
 public boolean openSession(){
  String selected=Profiles.selected(this);if(!screenSafe()||!ProfileRules.valid(selected))return false;
  knownProfiles=Profiles.known(this);if(!knownProfiles.contains(selected))return false;
  SessionContinuity.authorize(this);sessionActive=true;foregroundAllowed=false;stopReason="";
  try{startService(new Intent(this,MapperService.class));}catch(IllegalStateException ignored){/* Accessibility binding retains the user-authorized session. */}
  handler.removeCallbacks(sessionTick);handler.post(sessionTick);MainActivity.connectionChanged();return true;
 }
 boolean canEditGame(String pkg,long epoch){return epoch==profileEpoch&&pkg.equals(activePackage)&&editing&&canShowUi();}
 void pauseForeground(){
  foregroundAllowed=false;pendingPackage="";profileEpoch++;handler.removeCallbacks(showAfterMain);
  transitionVersion++;closingEditor=false;orbTouching=false;editing=false;learningOverlay=false;learningKey=-1;pendingKey=-1;
  stopAll();if(settingsDialog!=null){settingsDialog.dismiss();settingsDialog=null;}removePicker();removePanel();
  if(orb!=null){Motion.reset(orb);wm.removeViewImmediate(orb);orb=null;}
 }
 void activateGame(String pkg){
  if(foregroundAllowed&&activePackage.equals(pkg)||pendingPackage.equals(pkg))return;
  pauseForeground();pendingPackage=pkg;final long epoch=profileEpoch;
  handler.post(new Runnable(){@Override public void run(){
   if(epoch!=profileEpoch||!sessionActive||!pendingPackage.equals(pkg))return;
   if(!screenSafe()){endSession("screen_locked");return;}
   if(MainActivity.isForeground()){handler.postDelayed(this,16);return;}
   // Keep old bindings until the old gesture callback has lifted every existing pointer.
   if(busy||!contacts.isEmpty()){handler.postDelayed(this,16);return;}
   activePackage=pkg;pendingPackage="";reload();Profiles.select(MapperService.this,pkg);foregroundAllowed=true;
   armed=!bindings.isEmpty();showLauncher();updatePanel();
  }});
 }
 boolean keyboardWindow(AccessibilityEvent e){
  if(!editing||e.getPackageName()==null||e.getClassName()==null)return false;
  String pkg=e.getPackageName().toString(),name=e.getClassName().toString();
  android.view.inputmethod.InputMethodManager manager=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
  for(android.view.inputmethod.InputMethodInfo info:manager.getEnabledInputMethodList())if(info.getPackageName().equals(pkg))return name.equals("android.inputmethodservice.SoftInputWindow")||name.equals(info.getServiceName());
  return false;
 }
 void checkSession(){
  if(!sessionActive)return;
  if(!SessionRules.keep(sessionActive,screenSafe(),false)){endSession("screen_locked");return;}
  reconcileForeground();
  handler.postDelayed(sessionTick,1000);
 }
 public void endSession(String reason){
  SessionContinuity.clear();
  sessionActive=false;foregroundAllowed=false;pendingPackage="";profileEpoch++;stopReason=reason;handler.removeCallbacks(sessionTick);handler.removeCallbacks(showAfterMain);handler.removeCallbacks(foregroundCheck);
  transitionVersion++;closingEditor=false;orbTouching=false;editing=false;learningOverlay=false;learningKey=-1;pendingKey=-1;
  stopAll();if(settingsDialog!=null){settingsDialog.dismiss();settingsDialog=null;}removePicker();removePanel();
  if(orb!=null){Motion.reset(orb);wm.removeViewImmediate(orb);orb=null;}
  stopSelf();MainActivity.connectionChanged();
 }
 public void onMainForeground(){pauseForeground();}
 public void onMainBackground(){handler.removeCallbacks(showAfterMain);handler.postDelayed(showAfterMain,100);syncMotionSources();}
 @Override public int onStartCommand(Intent intent,int flags,int startId){return START_NOT_STICKY;}
 @Override public void onTaskRemoved(Intent rootIntent){super.onTaskRemoved(rootIntent);}
 static boolean isControllerKey(KeyEvent e){int k=e.getKeyCode();return KeyEvent.isGamepadButton(k)||k>=KeyEvent.KEYCODE_DPAD_UP&&k<=KeyEvent.KEYCODE_DPAD_CENTER;}
 public void reload(){stopAll();bindings.clear();states.clear();keyOrder.clear();tapStrokes.clear();holdStarts.clear();for(Binding b:Binding.load(this,activePackage)){bindings.put(b.key,b);states.put(b.key,new RepeatState(b.count));}keyOrder.addAll(states.keySet());refreshBadges();}
 @Override protected boolean onKeyEvent(KeyEvent e){
  if(!sessionActive||!foregroundAllowed||MainActivity.isForeground())return false;
  if(e.getAction()==KeyEvent.ACTION_DOWN&&e.getRepeatCount()==0&&!screenSafe()){endSession("screen_locked");return false;}
  int key=e.getKeyCode();if(isControllerKey(e)){observedControllerEvents++;if(e.getAction()==KeyEvent.ACTION_DOWN&&e.getRepeatCount()==0)keyDowns.merge(key,1,Integer::sum);if(e.getAction()==KeyEvent.ACTION_UP)keyUps.merge(key,1,Integer::sum);}
  if(key==learningKey){
   if(e.getAction()==KeyEvent.ACTION_UP){learningKey=-1;return true;}
   // A fresh press must work even if Android lost the learning press's release during reconfiguration.
   if(e.getAction()==KeyEvent.ACTION_DOWN&&e.getRepeatCount()==0)learningKey=-1;else return true;
  }
  if(learningOverlay&&isControllerKey(e)){
   if(e.getAction()==KeyEvent.ACTION_DOWN&&e.getRepeatCount()==0){learningOverlay=false;learningKey=key;pendingKey=key;pendingInterval=50;pendingCount=0;pendingDuration=8;pendingRadius=72;pendingHold=true;handler.post(this::pickPoint);}return true;
  }
  if(!armed||!bindings.containsKey(key)||!isControllerKey(e))return false;
  // ViewRoot's synthetic joystick arrows are superseded by our raw-axis owners.
  if(Build.VERSION.SDK_INT>=34&&key>=19&&key<=22&&(e.getFlags()&KeyEvent.FLAG_FALLBACK)!=0){InputDevice device=e.getDevice();if(device!=null&&device.supportsSource(InputDevice.SOURCE_JOYSTICK))return true;}

  if(e.getAction()==KeyEvent.ACTION_DOWN&&e.getRepeatCount()==0)setInput(key,e.getDeviceId(),0,true);
  else if(e.getAction()==KeyEvent.ACTION_UP)setInput(key,e.getDeviceId(),0,false);
  return true;
 }
 void setInput(int key,int device,int channel,boolean down){
  if(!armed||!states.containsKey(key))return;
  RepeatState state=states.get(key);boolean held=input.set(key,device,channel,down);
  if(held&&!state.held)state.down(SystemClock.uptimeMillis());
  else if(!held){state.up();Contact c=contacts.get(key);if(c!=null)c.closing=true;}
  markPressed(key,held);if(!busy)schedule(0);
 }
 float axis(MotionEvent e,int axis){return e.getAxisValue(axis);}
 float trigger(MotionEvent e,int first,int fallback){
  InputDevice device=e.getDevice();int chosen=first;
  InputDevice.MotionRange range=device==null?null:device.getMotionRange(chosen,e.getSource());
  if(range==null&&device!=null){chosen=fallback;range=device.getMotionRange(chosen,e.getSource());}
  return range==null?Float.NaN:AnalogInput.normalize(e.getAxisValue(chosen),range.getMin(),range.getMax());
 }
 @Override public void onMotionEvent(MotionEvent e){
  if(!sessionActive||!foregroundAllowed||MainActivity.isForeground()||!e.isFromSource(InputDevice.SOURCE_JOYSTICK))return;
  int device=e.getDeviceId();float[] left=AnalogInput.vector(axis(e,MotionEvent.AXIS_X),axis(e,MotionEvent.AXIS_Y),.18f);
  InputDevice rawDevice=e.getDevice();boolean rz=rawDevice!=null&&rawDevice.getMotionRange(MotionEvent.AXIS_Z,e.getSource())!=null;
  float[] right=AnalogInput.vector(axis(e,rz?MotionEvent.AXIS_Z:MotionEvent.AXIS_RX),axis(e,rz?MotionEvent.AXIS_RZ:MotionEvent.AXIS_RY),.18f);
  float lt=trigger(e,MotionEvent.AXIS_LTRIGGER,MotionEvent.AXIS_BRAKE),rt=trigger(e,MotionEvent.AXIS_RTRIGGER,MotionEvent.AXIS_GAS);
  if(learningOverlay){
   int key=AnalogInput.active(left[0],left[1])?AnalogInput.LEFT_STICK:AnalogInput.active(right[0],right[1])?AnalogInput.RIGHT_STICK:lt>.55f?AnalogInput.LEFT_TRIGGER:rt>.55f?AnalogInput.RIGHT_TRIGGER:0;
   if(key!=0){learningOverlay=false;pendingKey=key;pendingHold=true;pendingRadius=72;pendingInterval=50;pendingCount=0;pendingDuration=8;handler.post(this::pickPoint);}return;
  }
  if(!armed)return;
  if(bindings.containsKey(AnalogInput.LEFT_STICK))updateStick(AnalogInput.LEFT_STICK,device,left);else updateStickDirections(device,left);
  updateStick(AnalogInput.RIGHT_STICK,device,right);
  updateTrigger(AnalogInput.LEFT_TRIGGER,KeyEvent.KEYCODE_BUTTON_L2,device,lt);
  updateTrigger(AnalogInput.RIGHT_TRIGGER,KeyEvent.KEYCODE_BUTTON_R2,device,rt);
  // Hats may be axes rather than key pairs. Neutral must lift the previous direction.
  InputDevice d=e.getDevice();if(d!=null&&d.getMotionRange(MotionEvent.AXIS_HAT_X,e.getSource())!=null){
   float x=axis(e,MotionEvent.AXIS_HAT_X),y=axis(e,MotionEvent.AXIS_HAT_Y);
   int[] keys={21,22,19,20};boolean[] down={x<-.5f,x>.5f,y<-.5f,y>.5f};
   for(int i=0;i<keys.length;i++){
    // Releasing a hat must not release an independently held physical button.
    setInput(keys[i],device,1,down[i]);
   }
  }
 }
 void updateStickDirections(int device,float[] vector){
  int[] keys={21,22,19,20};float[] magnitude={-vector[0],vector[0],-vector[1],vector[1]};
  for(int i=0;i<keys.length;i++)setInput(keys[i],device,4,AnalogInput.direction(magnitude[i],input.owns(keys[i],device,4)));
 }
 void syncMotionSources(){
  if(orbTouching)return;
  AccessibilityServiceInfo info=getServiceInfo();if(info==null)return;
  boolean capture=sessionActive&&foregroundAllowed&&!MainActivity.isForeground();
  int flags=capture?info.flags|AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS:info.flags&~AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
  boolean changed=info.flags!=flags;info.flags=flags;
  if(Build.VERSION.SDK_INT>=34){int sources=capture&&(armed||learningOverlay)?InputDevice.SOURCE_JOYSTICK:0;if(info.getMotionEventSources()!=sources){info.setMotionEventSources(sources);changed=true;}}
  if(changed)setServiceInfo(info);
 }
 void updateStick(int key,int device,float[] vector){
  if(!bindings.containsKey(key))return;
  boolean active=AnalogInput.active(vector[0],vector[1]);Integer owner=axisOwners.get(key);
  if(owner!=null&&owner!=device)return;
  if(active){axisOwners.put(key,device);vectors.put(key,vector);}else{axisOwners.remove(key);vectors.remove(key);}
  setInput(key,device,2,active);
 }
 void updateTrigger(int analog,int digital,int device,float value){
  if(Float.isNaN(value))return;
  int key=bindings.containsKey(analog)?analog:digital;
  boolean down=AnalogInput.threshold(value,input.owns(key,device,3));setInput(key,device,3,down);
 }
 void schedule(long delay){
  long at=SystemClock.uptimeMillis()+delay;
  if(!scheduled||at<scheduledAt){handler.removeCallbacks(tick);scheduled=true;scheduledAt=at;handler.postAtTime(tick,at);}
 }
 static final class Contact {
  final GestureDescription.StrokeDescription stroke;final int x,y;boolean closing;
  Contact(GestureDescription.StrokeDescription stroke,int x,int y){this.stroke=stroke;this.x=x;this.y=y;}
 }
 final Map<Integer,Contact> contacts=new LinkedHashMap<>();
 boolean valid(Binding b){DisplayMetrics d=dimensions();return b.width==d.widthPixels&&b.height==d.heightPixels;}
 int[] target(Binding b,Contact previous,boolean keep){
  float[] vector=vectors.get(b.key);int x=b.x,y=b.y;
  if(keep&&AnalogInput.stick(b.key)&&vector!=null){x=Math.round(x+vector[0]*dp(b.radius));y=Math.round(y+vector[1]*dp(b.radius));}
  x=Math.max(0,Math.min(b.width-1,x));y=Math.max(0,Math.min(b.height-1,y));
  if(keep&&previous!=null&&previous.x==x&&previous.y==y)x=HoldPath.heartbeat(x,previous.x,b.width);
  else if(keep&&previous==null&&!AnalogInput.stick(b.key))x=HoldPath.heartbeat(x,x,b.width);
  return new int[]{x,y};
 }
 void pump(){
  scheduled=false;if(busy)return;
  long now=SystemClock.uptimeMillis(),nextDelay=Long.MAX_VALUE;
  List<Binding> taps=new ArrayList<>();List<Integer> keys=keyOrder;
  int capacity=Math.min(TouchStartPlan.CAPACITY,GestureDescription.getMaxStrokeCount());
  for(int key:keys){
   Binding b=bindings.get(key);RepeatState state=states.get(key);
   if(armed&&state.held&&!valid(b)){stopAll();toast("Ekran yönü değişti. Noktaları yeniden yerleştir.");return;}
   if(!armed||b.hold||AnalogInput.stick(key))continue;
   nextDelay=Math.min(nextDelay,state.delay(now));if(state.ready(now))taps.add(b);
  }
  // Android requires the first continuation sample to contain only existing pointers.
  // New fingers enter 1 ms later, together, so they do not cancel the held direction.
  int anchor=Integer.MIN_VALUE;
  if(contacts.isEmpty()){
   for(int key:keys){Binding b=bindings.get(key);if(armed&&states.get(key).held&&(b.hold||AnalogInput.stick(key))){anchor=key;break;}}
   if(anchor==Integer.MIN_VALUE&&!taps.isEmpty())anchor=taps.get(0).key;
  }
  int newStart=1;
  int frame=16;for(Binding b:taps)frame=Math.max(frame,b.duration+newStart);
  GestureDescription.Builder builder=new GestureDescription.Builder();
  Map<Integer,Contact> next=new LinkedHashMap<>();int count=0;
  for(Map.Entry<Integer,Contact> entry:contacts.entrySet()){
   int key=entry.getKey();Contact c=entry.getValue();Binding b=bindings.get(key);RepeatState state=states.get(key);
   boolean keep=ContactPolicy.shouldContinue(ContactPolicy.wantsHold(armed,state!=null&&state.held,b!=null&&(b.hold||AnalogInput.stick(key))),c.closing);
   int[] end=keep?target(b,c,true):new int[]{c.x,c.y};Path path=new Path();path.moveTo(c.x,c.y);path.lineTo(end[0],end[1]);
   GestureDescription.StrokeDescription stroke=c.stroke.continueStroke(path,0,keep?frame:8,keep);
   builder.addStroke(stroke);count++;if(keep)next.put(key,new Contact(stroke,end[0],end[1]));
  }
  for(int key:keys){
   Binding b=bindings.get(key);RepeatState state=states.get(key);
   if(!ContactPolicy.wantsHold(armed,state.held,b.hold||AnalogInput.stick(key))||contacts.containsKey(key)||count>=capacity)continue;
   // Reuse the initial stroke ID after release/repress. Android retains every stroke-to-pointer
   // mapping until the LAST finger lifts; allocating IDs per tap exhausts its pointer pool.
   int endX=HoldPath.heartbeat(b.x,b.x,b.width);
   int start=TouchStartPlan.start(!contacts.isEmpty(),key,anchor),identity=TouchStartPlan.cacheKey(key,start);
   GestureDescription.StrokeDescription stroke=holdStarts.get(identity);
   if(stroke==null){Path path=new Path();path.moveTo(b.x,b.y);path.lineTo(endX,b.y);stroke=new GestureDescription.StrokeDescription(path,start,16,true);holdStarts.put(identity,stroke);}
   builder.addStroke(stroke);count++;next.put(key,new Contact(stroke,endX,b.y));
  }
  List<Binding> dispatched=new ArrayList<>();
  // A, X and R2 share one multi-pointer gesture instead of taking turns.
  for(Binding b:taps){
   if(count>=capacity)break;
   int start=TouchStartPlan.start(!contacts.isEmpty(),b.key,anchor),identity=TouchStartPlan.cacheKey(b.key,start);
   GestureDescription.StrokeDescription stroke=tapStrokes.get(identity);
   if(stroke==null){Path path=new Path();path.moveTo(b.x,b.y);stroke=new GestureDescription.StrokeDescription(path,start,b.duration);tapStrokes.put(identity,stroke);}
   builder.addStroke(stroke);dispatched.add(b);count++;
  }
  if(count==0){if(armed&&nextDelay!=Long.MAX_VALUE)schedule(nextDelay);return;}
  busy=true;long sequence=++gestureSequence;
  boolean accepted=dispatchGesture(builder.build(),new GestureResultCallback(){
   @Override public void onCompleted(GestureDescription g){if(sequence!=gestureSequence)return;gesturesCompleted++;busy=false;schedule(0);}
   @Override public void onCancelled(GestureDescription g){
    if(sequence!=gestureSequence)return;gesturesCanceled++;busy=false;
    // Close any surviving continued pointers. Losing Java bookkeeping alone leaves fingers down.
    for(RepeatState state:states.values())state.up();input.clear();vectors.clear();axisOwners.clear();
    for(Contact c:contacts.values())c.closing=true;
    handler.removeCallbacks(tick);scheduled=false;
    if(!contacts.isEmpty())schedule(0);
   }
  },handler);
  if(accepted){contacts.clear();contacts.putAll(next);for(Binding b:dispatched)states.get(b.key).dispatched(now,b.interval);}
  else{busy=false;armed=false;contacts.clear();stopAll();if(sessionActive&&foregroundAllowed&&screenSafe())toast("Dokunma servisi yanıt vermedi. Kontrol durduruldu.");}
 }
 long gestureSequence;
 public void stopAll(){
  armed=false;handler.removeCallbacks(tick);scheduled=false;
  for(RepeatState state:states.values())state.up();input.clear();vectors.clear();axisOwners.clear();
  for(Contact c:contacts.values())c.closing=true;
  for(KeyBadge badge:badges.values()){badge.pressed=false;badge.invalidate();}
  updatePanel();if(!busy&&!contacts.isEmpty())pump();
 }
 DisplayMetrics dimensions(){DisplayMetrics current=new DisplayMetrics();wm.getDefaultDisplay().getRealMetrics(current);return current;}
 WindowManager.LayoutParams params(int width,int height){
  WindowManager.LayoutParams p=new WindowManager.LayoutParams(width,height,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
   WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,PixelFormat.TRANSLUCENT);
  p.gravity=Gravity.TOP|Gravity.LEFT;p.token=overlayToken;
  if(Build.VERSION.SDK_INT>=30)p.setFitInsetsTypes(0);
  p.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;return p;
 }
 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
 void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 void pauseForOverlayTouch(){stopAll();}
 Ui ui(){return new Ui(this);}
 public void showLauncher(){
  if(!canShowUi())return;
  if(orb!=null){orb.setVisibility(View.VISIBLE);orb.invalidate();return;}
  refreshOrbConnection();
  final String orbGame=activePackage;final long orbEpoch=profileEpoch;OrbView button=new OrbView(this);orb=button;button.setAlpha(editing?1f:.60f);orbParams=params(dp(Ui.ORB),dp(Ui.ORB));orbParams.x=Profiles.orb(this,activePackage,"orbX",dp(16));orbParams.y=Profiles.orb(this,activePackage,"orbY",dp(80));
  DisplayMetrics screen=dimensions();orbParams.x=Math.max(0,Math.min(screen.widthPixels-dp(Ui.ORB),orbParams.x));orbParams.y=Math.max(0,Math.min(screen.heightPixels-dp(Ui.ORB),orbParams.y));
  button.setContentDescription("Vektör: düzenlemeyi aç veya kapat");button.setOnClickListener(v->{if(orbEpoch!=profileEpoch||!orbGame.equals(activePackage)||!canShowUi())return;if(!editing)showPanel();else collapse();});
  button.setOnTouchListener(new View.OnTouchListener(){
   float x,y;int px,py;boolean drag,wasPlaying;
   @Override public boolean onTouch(View v,MotionEvent e){
    if(orbEpoch!=profileEpoch||!orbGame.equals(activePackage)||!canShowUi())return false;
    if(e.getAction()==MotionEvent.ACTION_DOWN){orbTouching=true;button.cancelDock();button.setAlpha(1);Motion.press(v,true);wasPlaying=armed;if(wasPlaying)pauseForOverlayTouch();x=e.getRawX();y=e.getRawY();px=orbParams.x;py=orbParams.y;drag=false;return true;}
    if(e.getAction()==MotionEvent.ACTION_MOVE){float dx=e.getRawX()-x,dy=e.getRawY()-y;if(Math.hypot(dx,dy)>ViewConfiguration.get(MapperService.this).getScaledTouchSlop())drag=true;
     if(drag){Motion.reset(v);DisplayMetrics d=dimensions();orbParams.x=Math.max(0,Math.min(d.widthPixels-dp(Ui.ORB),px+(int)dx));orbParams.y=Math.max(0,Math.min(d.heightPixels-dp(Ui.ORB),py+(int)dy));wm.updateViewLayout(orb,orbParams);positionPanel();}return true;}
    if(e.getAction()==MotionEvent.ACTION_UP){Motion.press(v,false);try{if(drag){button.dock(orbEpoch);if(wasPlaying){armed=canShowUi()&&!bindings.isEmpty();updatePanel();}}else v.performClick();}finally{orbTouching=false;button.setAlpha(editing?1f:.60f);syncMotionSources();}return true;}
    if(e.getAction()==MotionEvent.ACTION_CANCEL){Motion.reset(v);orbTouching=false;syncMotionSources();return true;}return true;
   }
  });wm.addView(orb,orbParams);overlayToken=orbParams.token;
 }
 boolean orbConnected;
 void refreshOrbConnection(){
  orbConnected=false;
  for(int id:InputDevice.getDeviceIds())if(ControllerIdentity.isPad(InputDevice.getDevice(id))){orbConnected=true;break;}
  if(orb!=null)orb.invalidate();
 }
 final class OrbView extends View {
  final Ui palette=ui();
  android.animation.ValueAnimator dockAnimation;
  void cancelDock(){if(dockAnimation!=null){dockAnimation.cancel();dockAnimation=null;}}
  @Override protected void onDetachedFromWindow(){cancelDock();super.onDetachedFromWindow();}
  void dock(long epoch){
   int limit=Math.max(0,dimensions().widthPixels-dp(Ui.ORB));
   int target=orbParams.x<limit/2?0:limit;
   Profiles.orb(MapperService.this,activePackage,target,orbParams.y);
   cancelDock();
   if(!Motion.enabled(MapperService.this)){orbParams.x=target;wm.updateViewLayout(this,orbParams);positionPanel();return;}
   dockAnimation=android.animation.ValueAnimator.ofInt(orbParams.x,target);
   dockAnimation.setDuration(Motion.COMPONENT);dockAnimation.setInterpolator(Motion.EASE);
   dockAnimation.addUpdateListener(animation->{
    if(epoch!=profileEpoch||orb!=this||!isAttachedToWindow()){animation.cancel();return;}
    orbParams.x=(int)animation.getAnimatedValue();wm.updateViewLayout(this,orbParams);positionPanel();
   });dockAnimation.start();
  }
  final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
  OrbView(Context context){super(context);}
  @Override protected void onDraw(Canvas canvas){
   float c=getWidth()/2f,r=c-dp(5);p.setStyle(Paint.Style.FILL);p.setColor(palette.orbSurface);canvas.drawCircle(c,c,r,p);
   p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1));p.setColor(palette.accent);canvas.drawCircle(c,c,r,p);
   BrandMark.draw(canvas,p,c-dp(12),c-dp(12),dp(24),0xE8E6EDF8);
   p.setStyle(Paint.Style.FILL);p.setColor(palette.orbSurface);canvas.drawCircle(getWidth()-dp(12),dp(12),dp(4),p);p.setColor(learningOverlay?palette.signalError:orbConnected?palette.success:palette.muted);canvas.drawCircle(getWidth()-dp(12),dp(12),dp(2),p);
  }
  @Override public boolean performClick(){super.performClick();return true;}
 }
 public void showPanel(){
  if(!canShowUi())return;long started=SystemClock.uptimeMillis();transitionVersion++;closingEditor=false;
  if(editorRoot!=null)Motion.reset(editorRoot);stopAll();editing=true;learningOverlay=false;editorOpens++;
  if(editorRoot!=null&&editorPendingKey!=pendingKey)removePanel();
  if(panel!=null){refreshBadges();attachEditor();updatePanel();animateEditorIn();lastEditorAttachMs=SystemClock.uptimeMillis()-started;return;}
  editorBuilds++;editorPendingKey=pendingKey;editorRoot=new FrameLayout(this);editorRoot.setClickable(true);showBackdrop();showLauncher();
  final String panelGame=activePackage;final long panelEpoch=profileEpoch;Ui u=ui();LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(14),dp(18),dp(16));box.setBackground(u.outline(u.surface,20));box.setElevation(dp(8));
  TextView brand=u.text(Profiles.label(this,activePackage),12,true);brand.setSingleLine();brand.setEllipsize(android.text.TextUtils.TruncateAt.END);brand.setTextColor(u.accentText);LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);
  ImageView appIcon=new ImageView(this);try{appIcon.setImageDrawable(getPackageManager().getApplicationIcon(activePackage));}catch(android.content.pm.PackageManager.NameNotFoundException ignored){}
  head.addView(appIcon,new LinearLayout.LayoutParams(dp(24),dp(24)));head.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
  Button close=u.button("×",false,this::collapse);close.setContentDescription("Düzenlemeyi kapat");head.addView(close,new LinearLayout.LayoutParams(dp(48),dp(48)));box.addView(head);
  label=u.chip("");box.addView(label,new LinearLayout.LayoutParams(-2,-2));coach=u.caption("");box.addView(coach);
  if(bindings.isEmpty()){
   emptyHelp=new LinearLayout(this);emptyHelp.setOrientation(LinearLayout.VERTICAL);
   int[] icons={ActionIcon.ADD,ActionIcon.PAD,ActionIcon.TARGET};
   String[] steps={"Kontrol ekle’ye dokun.","Koldaki tuşa bas.","Ekrandaki hedefi seç."};
   for(int i=0;i<steps.length;i++){
    LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
    ActionIcon icon=new ActionIcon(this,u,icons[i],false,"",()->{});
    icon.setClickable(false);icon.setFocusable(false);icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    row.addView(icon,new LinearLayout.LayoutParams(dp(28),dp(28)));row.addView(u.caption(steps[i]));emptyHelp.addView(row);
   }
   box.addView(emptyHelp);
  }
  Button add=u.button("＋  Kontrol ekle",true,()->{if(!canEditGame(panelGame,panelEpoch))return;learningOverlay=!learningOverlay;updatePanel();toast(learningOverlay?(Build.VERSION.SDK_INT>=34?"Kol tuşuna bas veya analog çubuğu oynat.":"Kol tuşuna bas. Ham analog yakalama Android 14+ gerektirir."):"Tuş ekleme iptal edildi.");});
  LinearLayout.LayoutParams addLp=new LinearLayout.LayoutParams(-1,dp(48));addLp.topMargin=dp(8);box.addView(add,addLp);
  if(!bindings.isEmpty()){
   LinearLayout items=new LinearLayout(this);items.setOrientation(LinearLayout.VERTICAL);items.setVisibility(View.GONE);
   Button list=u.button("Kontrolleri düzenle",false,()->{
    boolean open=items.getVisibility()!=View.VISIBLE;items.removeAllViews();
    if(open)for(Binding binding:bindings.values()){
     String type=AnalogInput.stick(binding.key)?"Analog":binding.hold?"Basılı":"Tekrar";
     u.addButton(items,KeyBadge.name(binding.key)+" · "+type+" · "+binding.x+", "+binding.y,false,()->{
      if(canEditGame(panelGame,panelEpoch)&&bindings.containsKey(binding.key))openBadgeSettings(bindings.get(binding.key));
     });
    }
    items.setVisibility(open?View.VISIBLE:View.GONE);box.post(this::positionPanel);
   });
   box.addView(list,new LinearLayout.LayoutParams(-1,dp(48)));box.addView(items);
  }
  if(pendingKey!=-1)u.addButton(box,"NOKTA SEÇ · "+KeyBadge.name(pendingKey),false,this::pickPoint);
  panelParams=params(Math.min(dp(260),dimensions().widthPixels-dp(16)),-2);
  BoundedScrollView clipped=new BoundedScrollView(this,Math.max(dp(64),dimensions().heightPixels-dp(24)));clipped.addView(box);panel=clipped;positionPanel();editorRoot.addView(panel,childParams(panelParams));updatePanel();refreshBadges();attachEditor();box.post(this::positionPanel);animateEditorIn();lastEditorAttachMs=SystemClock.uptimeMillis()-started;
 }
 void positionPanel(){
  if(panel==null||panelParams==null||orbParams==null)return;DisplayMetrics d=dimensions();
  int h=panel.getHeight()>0?panel.getHeight():dp(pendingKey!=-1?270:220);
  int[] pos=OverlayPlacement.panel(orbParams.x,orbParams.y,dp(Ui.ORB),panelParams.width,h,d.widthPixels,d.heightPixels,dp(8));
  panelParams.x=pos[0];panelParams.y=pos[1];if(panel.getParent()==editorRoot)panel.setLayoutParams(childParams(panelParams));
 }
 void showBackdrop(){
  if(backdrop!=null)return;View v=new View(this);v.setContentDescription("Düzenleme arka planı");v.setClickable(true);
  v.setBackgroundColor(ui().veil);
  backdrop=v;editorRoot.addView(v,new FrameLayout.LayoutParams(-1,-1));
 }
 FrameLayout.LayoutParams childParams(WindowManager.LayoutParams p){FrameLayout.LayoutParams child=new FrameLayout.LayoutParams(p.width,p.height);child.leftMargin=p.x;child.topMargin=p.y;return child;}
 void attachEditor(){
  if(!canShowUi()||editorRoot==null)return;
  if(!editorRoot.isAttachedToWindow())wm.addView(editorRoot,params(-1,-1));
  if(orb!=null){Motion.reset(orb);wm.removeViewImmediate(orb);wm.addView(orb,orbParams);}
 }
 void animateEditorIn(){
  if(editorRoot==null)return;Motion.reset(editorRoot);
  if(Motion.enabled(this)&&panel!=null){
   panel.setPivotX(panelParams.x>orbParams.x?0:panelParams.width);panel.setPivotY(0);
   panel.setScaleX(.97f);panel.setScaleY(.97f);panel.setAlpha(.80f);
   panel.animate().scaleX(1).scaleY(1).alpha(1).setDuration(Motion.COMPONENT).setInterpolator(Motion.EASE).start();
  }
 }
 void detachEditor(){if(editorRoot!=null){Motion.reset(editorRoot);if(editorRoot.isAttachedToWindow())wm.removeViewImmediate(editorRoot);}}
 void collapse(){
  if(!canShowUi())return;final long version=++transitionVersion;
  if(!bindings.isEmpty())GuideState.complete(this,activePackage);editing=false;closingEditor=true;learningOverlay=false;stopAll();
  Runnable finish=()->{
   if(version!=transitionVersion||!sessionActive)return;
   detachEditor();clearBadges();reload();closingEditor=false;
   if(canShowUi()){armed=!bindings.isEmpty();Profiles.ready(this,activePackage,armed);showLauncher();}updatePanel();
  };
  // Input must resume on this click; a fade-out would discard presses during its delay.
  finish.run();
 }
 public void restyle(){if(settingsDialog!=null)settingsDialog.dismiss();boolean expanded=editing;transitionVersion++;closingEditor=false;removePanel();if(orb!=null){orbTouching=false;syncMotionSources();wm.removeViewImmediate(orb);orb=null;}if(canShowUi()){showLauncher();if(expanded)showPanel();}}
 void updatePanel(){syncMotionSources();if(label!=null)label.setText(learningOverlay?"Kolundaki tuşa bas":bindings.isEmpty()?"Kontrol yok":bindings.size()+" kontrol");if(emptyHelp!=null){boolean visible=bindings.isEmpty()&&!learningOverlay;emptyHelp.setVisibility(visible?View.VISIBLE:View.GONE);coach.setVisibility(visible?View.GONE:View.VISIBLE);}if(coach!=null)coach.setText(learningOverlay||bindings.isEmpty()||GuideState.active(this,activePackage,bindings.isEmpty())?GuideState.editorText(learningOverlay,bindings.isEmpty()):"Etikete dokun: ayarlar. Sürükle: yer değiştir. Bitince yuvarlak simgeye dokun.");if(orb!=null){orb.setAlpha(editing||orbTouching?1f:.60f);orb.setContentDescription(editing?"Düzenlemeyi kapat ve oyna":"Vektör düzenleyicisini aç");orb.invalidate();}}
 void removePanel(){
  detachEditor();clearBadges();if(editorRoot!=null)editorRoot.removeAllViews();editorRoot=null;panel=null;backdrop=null;label=null;coach=null;emptyHelp=null;panelParams=null;
 }
 void removePicker(){if(picker!=null){wm.removeViewImmediate(picker);picker=null;}}
 void pickPoint(){
  if(!canShowUi()||!editing)return;final String pointGame=activePackage;final long pointEpoch=profileEpoch;transitionVersion++;closingEditor=false;
  stopAll();removePanel();clearBadges();if(orb!=null)orb.setVisibility(View.GONE);
  android.widget.FrameLayout layer=new android.widget.FrameLayout(this);layer.setBackgroundColor(0x227E9B83);
  TextView hint=new TextView(this);hint.setText(KeyBadge.name(pendingKey)+" için oyundaki düğmenin ortasına dokun\nÖrneğin bu tuşla şut çekmek istiyorsan şut düğmesini seç.");hint.setTextSize(18);hint.setTextColor(ui().text);hint.setBackground(ui().shape(ui().surface,18));hint.setPadding(dp(14),dp(14),dp(14),dp(14));
  FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL);hp.topMargin=dp(28);layer.addView(hint,hp);
  Button cancel=ui().button("Vazgeç",false,()->{});FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);cp.bottomMargin=dp(32);layer.addView(cancel,cp);
  cancel.setOnClickListener(v->{if(!canEditGame(pointGame,pointEpoch))return;removePicker();showPanel();});
  layer.setOnTouchListener((v,e)->{if(!canEditGame(pointGame,pointEpoch))return false;if(e.getAction()==MotionEvent.ACTION_UP){v.performClick();
   DisplayMetrics dm=dimensions();List<Binding> all=Binding.load(this,activePackage);all.removeIf(b->b.key==pendingKey);
   Binding added=new Binding(pendingKey,(int)e.getRawX(),(int)e.getRawY(),dm.widthPixels,dm.heightPixels,pendingInterval,pendingCount);added.duration=pendingDuration;added.radius=pendingRadius;added.hold=pendingHold||AnalogInput.stick(pendingKey);all.add(added);Binding.save(this,activePackage,all);
   pendingKey=-1;removePicker();reload();editing=true;showPanel();toast("Kontrol yerleştirildi.");}return true;});
  picker=layer;wm.addView(picker,params(-1,-1));
 }
 void markPressed(int key,boolean pressed){if(!editing)return;KeyBadge b=badges.get(key);if(b!=null){b.pressed=pressed;b.invalidate();}}
 void clearBadges(){for(KeyBadge b:badges.values())if(editorRoot!=null)editorRoot.removeView(b);badges.clear();}
 void refreshBadges(){
  if(wm==null)return;clearBadges();if(!editing||panel==null||picker!=null||editorRoot==null)return;
  DisplayMetrics dm=dimensions();int size=dp(52);final String badgeGame=activePackage;final long badgeEpoch=profileEpoch;
  for(Binding b:bindings.values()){
   if(b.width!=dm.widthPixels||b.height!=dm.heightPixels)continue;
   KeyBadge badge=new KeyBadge(this,b);badge.editing=editing;RepeatState state=states.get(b.key);badge.pressed=state!=null&&state.held;
   WindowManager.LayoutParams lp=params(size,size);lp.x=b.x-size/2;lp.y=b.y-size/2;
   if(!editing)lp.flags|=WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
   badge.setOnTouchListener(new View.OnTouchListener(){
    float downX,downY;int originX,originY;boolean dragged;
    @Override public boolean onTouch(View v,MotionEvent e){
     if(!canEditGame(badgeGame,badgeEpoch))return false;
     if(e.getAction()==MotionEvent.ACTION_DOWN){downX=e.getRawX();downY=e.getRawY();originX=b.x;originY=b.y;dragged=false;return true;}
     if(e.getAction()==MotionEvent.ACTION_MOVE){
      float dx=e.getRawX()-downX,dy=e.getRawY()-downY;
      if(Math.hypot(dx,dy)>ViewConfiguration.get(MapperService.this).getScaledTouchSlop())dragged=true;
      if(dragged){b.x=BadgePosition.center(originX+dx,dm.widthPixels,size);b.y=BadgePosition.center(originY+dy,dm.heightPixels,size);lp.x=b.x-size/2;lp.y=b.y-size/2;badge.setLayoutParams(childParams(lp));}return true;
     }
     if(e.getAction()==MotionEvent.ACTION_UP){
      v.performClick();if(dragged)Binding.save(MapperService.this,activePackage,new ArrayList<>(bindings.values()));else openBadgeSettings(b);return true;
     }
     if(e.getAction()==MotionEvent.ACTION_CANCEL){b.x=originX;b.y=originY;lp.x=b.x-size/2;lp.y=b.y-size/2;badge.setLayoutParams(childParams(lp));return true;}return true;
    }
   });
   badges.put(b.key,badge);editorRoot.addView(badge,childParams(lp));
  }
 }
 int number(EditText input,int fallback,int minimum,int maximum){try{return Math.max(minimum,Math.min(maximum,Integer.parseInt(input.getText().toString())));}catch(NumberFormatException e){return fallback;}}
 void openBadgeSettings(Binding b){
  if(!canShowUi()||!editing||settingsDialog!=null&&settingsDialog.isShowing())return;final String settingsGame=activePackage;final long settingsEpoch=profileEpoch;
  stopAll();learningOverlay=false;Ui u=ui();
  LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(8),dp(20),dp(8));
  Button delete=u.button("Bu kontrolü sil",false,()->{
   if(!canEditGame(settingsGame,settingsEpoch))return;
   bindings.remove(b.key);Binding.save(this,activePackage,new ArrayList<>(bindings.values()));
   if(settingsDialog!=null)settingsDialog.dismiss();reload();updatePanel();
  });delete.setTextColor(u.danger);delete.setBackground(u.ripple(u.shape(Color.TRANSPARENT,12)));
  boolean stick=AnalogInput.stick(b.key);content.addView(u.eyebrow(stick?"ANALOG ÇUBUK":"DAVRANIŞ"));final int holdId=View.generateViewId(),macroId=View.generateViewId();RadioGroup modes=new RadioGroup(this);modes.setOrientation(RadioGroup.HORIZONTAL);
  RadioButton hold=new RadioButton(this);hold.setId(holdId);hold.setText("Basılı tut");hold.setTextColor(u.text);hold.setButtonTintList(android.content.res.ColorStateList.valueOf(u.accent));
  RadioButton macro=new RadioButton(this);macro.setId(macroId);macro.setText("Tekrarla");macro.setTextColor(u.text);macro.setButtonTintList(android.content.res.ColorStateList.valueOf(u.accent));u.styleMode(hold);u.styleMode(macro);RadioGroup.LayoutParams holdLp=new RadioGroup.LayoutParams(0,-2,1);holdLp.rightMargin=dp(8);modes.addView(hold,holdLp);modes.addView(macro,new RadioGroup.LayoutParams(0,-2,1));content.addView(modes);modes.check(b.hold?holdId:macroId);if(stick)modes.setVisibility(View.GONE);
  TextView explanation=u.caption(b.hold?"Tuşu tuttuğun sürece basılı kalır. Bıraktığında durur.":"Tuşu tuttuğun sürece tekrarlar. Bıraktığında durur.");content.addView(explanation);if(stick)explanation.setText("Merkez noktasından çubuk yönüne kayar. Çubuğu bırakınca temas biter.");
  LinearLayout macroFields=new LinearLayout(this);macroFields.setOrientation(LinearLayout.VERTICAL);content.addView(macroFields);
  EditText interval=u.number(macroFields,"Tekrar aralığı · ms (16–60000)",b.interval);
  EditText count=u.number(macroFields,"Tekrar sayısı · 0 sınırsız, 1 tek dokunma",b.count);
  EditText duration=u.number(macroFields,"Dokunma süresi · ms (8–100)",b.duration);
  macroFields.setVisibility(b.hold||stick?View.GONE:View.VISIBLE);
  EditText radius=stick?u.number(content,"Çubuk hareket alanı · 16–300",b.radius):null;
  modes.setOnCheckedChangeListener((group,id)->{boolean held=id==holdId;macroFields.setVisibility(held?View.GONE:View.VISIBLE);explanation.setText(held?"Tuşu tuttuğun sürece basılı kalır. Bıraktığında durur.":"Tuşu tuttuğun sürece tekrarlar. Bıraktığında durur.");});
  Button relocate=u.button("Noktayı yeniden seç",false,()->{});content.addView(relocate);LinearLayout.LayoutParams deleteLp=new LinearLayout.LayoutParams(-1,dp(44));deleteLp.topMargin=dp(8);content.addView(delete,deleteLp);
  BoundedScrollView scroll=new BoundedScrollView(u.dialogContext(),Math.max(dp(110),Math.min(dp(360),dimensions().heightPixels-dp(210))));scroll.setVerticalScrollbarThumbDrawable(u.shape(u.muted,4));scroll.setScrollBarSize(dp(3));scroll.addView(content);FrameLayout frame=new FrameLayout(this);frame.addView(scroll,new FrameLayout.LayoutParams(-1,-2));
  settingsDialog=new AlertDialog.Builder(u.dialogContext()).setTitle(KeyBadge.name(b.key)+" · Kontrol ayarları").setView(frame)
   .setNegativeButton("Vazgeç",null)
   .setNeutralButton("Sil",(d,w)->{if(!canEditGame(settingsGame,settingsEpoch))return;bindings.remove(b.key);Binding.save(this,activePackage,new ArrayList<>(bindings.values()));reload();updatePanel();})
   .setPositiveButton("Kaydet",(d,w)->{if(!canEditGame(settingsGame,settingsEpoch))return;b.hold=stick||modes.getCheckedRadioButtonId()==holdId;if(radius!=null)b.radius=number(radius,b.radius,16,300);b.interval=number(interval,b.interval,16,60000);b.count=number(count,b.count,0,1000000);b.duration=number(duration,b.duration,8,100);Binding.save(this,activePackage,new ArrayList<>(bindings.values()));reload();updatePanel();}).create();
  settingsDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
  relocate.setOnClickListener(v->{if(!canEditGame(settingsGame,settingsEpoch))return;pendingKey=b.key;pendingRadius=radius==null?b.radius:number(radius,b.radius,16,300);pendingHold=modes.getCheckedRadioButtonId()==holdId;pendingInterval=number(interval,b.interval,16,60000);pendingCount=number(count,b.count,0,1000000);pendingDuration=number(duration,b.duration,8,100);settingsDialog.dismiss();pickPoint();});
  editorFocusGraceUntil=SystemClock.uptimeMillis()+750;
  final AlertDialog opened=settingsDialog;
  opened.setOnDismissListener(d->{if(settingsDialog==opened)settingsDialog=null;if(editing)editorFocusGraceUntil=SystemClock.uptimeMillis()+750;if(sessionActive)scheduleForegroundCheck();});
  settingsDialog.getWindow().getAttributes().token=overlayToken;settingsDialog.show();u.finishDialog(settingsDialog);settingsDialog.getWindow().setLayout(Math.min(dp(460),dimensions().widthPixels-dp(24)),-2);
 }

 /** Standard Android diagnostic dump; read-only and available through authorized adb only. */
 @Override protected void dump(java.io.FileDescriptor fd,java.io.PrintWriter out,String[] args){
  out.println("ROTA session="+sessionActive+" reason="+stopReason+" editorBuilds="+editorBuilds+" editorOpens="+editorOpens+" attachMs="+lastEditorAttachMs+" settings="+(settingsDialog!=null&&settingsDialog.isShowing()));
  out.println("PROFILE active="+activePackage+" foreground="+foregroundAllowed+" pending="+pendingPackage);
  out.println("ROTA armed="+armed+" editing="+editing+" busy="+busy+" events="+observedControllerEvents+" completed="+gesturesCompleted+" canceled="+gesturesCanceled);
  out.println("focus="+focusTrace);
  out.println("contacts="+contacts.keySet()+" tapCache="+tapStrokes.size()+" holdCache="+holdStarts.size()+" downs="+keyDowns+" ups="+keyUps);
  for(Map.Entry<Integer,RepeatState> entry:states.entrySet()){RepeatState state=entry.getValue();out.println("key="+entry.getKey()+" held="+state.held+" physical="+input.held(entry.getKey())+" sent="+state.sent+" limit="+state.limit);}
 }
 boolean ownsFocusedWindow(int id){
  return getPackageName().equals(windowPackages.get(id))
   ||settingsDialog!=null&&settingsDialog.isShowing()&&settingsDialog.getWindow().getDecorView().hasWindowFocus()
   ||editorRoot!=null&&editorRoot.isAttachedToWindow()&&editorRoot.hasWindowFocus()
   ||orb!=null&&orb.isAttachedToWindow()&&orb.hasWindowFocus();
 }
 void reconcileForeground(){
  if(!sessionActive)return;
  if(!screenSafe()){endSession("screen_locked");return;}
  if(MainActivity.isForeground()){if(foregroundAllowed)pauseForeground();return;}
  // Window metadata only: do not request a root node or read screen contents.
  List<AccessibilityWindowInfo> windows=getWindows();AccessibilityWindowInfo focused=null,application=null;
  for(AccessibilityWindowInfo window:windows){
   if(window.isFocused())focused=window;
   if(application==null&&window.getType()==AccessibilityWindowInfo.TYPE_APPLICATION)application=window;
  }
  String underneath=application==null?null:windowPackages.get(application.getId());
  // Android may publish an empty window list between creating/dismissing our
  // modal and assigning focus. External state events revoke editing immediately.
  if(editing&&foregroundAllowed&&focused==null&&(application==null||activePackage.equals(underneath))&&SystemClock.uptimeMillis()<editorFocusGraceUntil){scheduleForegroundCheck();return;}
  boolean dialogFocused=settingsDialog!=null&&settingsDialog.isShowing()&&settingsDialog.getWindow().getDecorView().hasWindowFocus();
  String trace="focused="+(focused==null?"none":focused.getId()+":"+focused.getType()+":"+windowPackages.get(focused.getId()))+" underneath="+(application==null?"none":application.getId()+":"+underneath);
  if(!trace.equals(focusTrace.peekLast())){if(focusTrace.size()>=8)focusTrace.removeFirst();focusTrace.addLast(trace);}
  boolean ownFocus=dialogFocused||focused!=null&&focused.getType()==AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY&&ownsFocusedWindow(focused.getId());
  if(dialogFocused&&getPackageName().equals(underneath))underneath=activePackage;
  boolean imeFocus=focused!=null&&focused.getType()==AccessibilityWindowInfo.TYPE_INPUT_METHOD;
  if(FocusRules.keepEditor(editing,foregroundAllowed,ownFocus,imeFocus,settingsDialog!=null&&settingsDialog.isShowing(),activePackage,underneath))return;
  if(focused!=null&&!ownFocus&&!imeFocus&&focused.getType()!=AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY){
   String pkg=windowPackages.get(focused.getId());
   if(getPackageName().equals(pkg)&&editing)return;
   if(pkg!=null&&knownProfiles.contains(pkg)){activateGame(pkg);return;}
  }
  // A VPN activity, consent screen, launcher or unknown window suspends input, not authorization.
  if(foregroundAllowed||!pendingPackage.isEmpty()||orb!=null)pauseForeground();
 }
 @Override public void onAccessibilityEvent(AccessibilityEvent e){
  int type=e.getEventType();String pkg=e.getPackageName()==null?null:e.getPackageName().toString();
  boolean state=type==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
  boolean known=pkg!=null&&(knownProfiles.contains(pkg)||getPackageName().equals(pkg));
  boolean changed=false;
  if(e.getWindowId()>=0&&pkg!=null&&(state||known)){
   // Some manufacturers attribute our modal event to the underlying game window.
   // Do not replace an already verified game identity with our own overlay package.
   String previous=windowPackages.get(e.getWindowId());
   if(!getPackageName().equals(pkg)||previous==null||!knownProfiles.contains(previous))changed=!pkg.equals(windowPackages.put(e.getWindowId(),pkg));
  }
  if(!sessionActive)return;
  if(keyboardWindow(e))return;
  // Revoke scope immediately on an external state event, including when a modal
  // temporarily occludes the underlying application in getWindows().
  if(state&&pkg!=null&&!getPackageName().equals(pkg)&&!knownProfiles.contains(pkg)){
   boolean external=false;
   for(AccessibilityWindowInfo window:getWindows())if(window.getId()==e.getWindowId()
     &&(window.getType()==AccessibilityWindowInfo.TYPE_APPLICATION||window.isFocused())){external=true;break;}
   // A non-focused system decoration event is not an application switch.
   if(external&&(foregroundAllowed||!pendingPackage.isEmpty()||orb!=null))pauseForeground();
   scheduleForegroundCheck();return;
  }
  // Content events only repair missing/replaced identity; never poll windows for every game frame.
  if(type==AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED&&!changed)return;
  scheduleForegroundCheck();
 }
 void scheduleForegroundCheck(){if(!handler.hasCallbacks(foregroundCheck))handler.postDelayed(foregroundCheck,32);}
 void releaseConnection(String reason){
  if(SessionContinuity.canResume(this)){
   SessionContinuity.remember(windowPackages);pauseForeground();sessionActive=false;stopReason=reason;
   handler.removeCallbacks(sessionTick);handler.removeCallbacks(foregroundCheck);
  }else endSession(reason);
 }
 void resetController(){boolean resume=armed;stopAll();armed=resume&&canShowUi()&&!editing&&!bindings.isEmpty();updatePanel();}
 @Override public void onInterrupt(){resetController();}
 @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);boolean expanded=editing,wasArmed=armed;transitionVersion++;closingEditor=false;stopAll();removePicker();removePanel();if(settingsDialog!=null)settingsDialog.dismiss();if(orb!=null){orbTouching=false;syncMotionSources();wm.removeViewImmediate(orb);orb=null;}if(canShowUi()){showLauncher();if(expanded)showPanel();else{armed=wasArmed&&!bindings.isEmpty();updatePanel();}}}
 @Override public void onInputDeviceAdded(int id){refreshOrbConnection();syncMotionSources();reconcileForeground();}
 @Override public void onInputDeviceChanged(int id){refreshOrbConnection();resetController();syncMotionSources();reconcileForeground();}
 @Override public void onInputDeviceRemoved(int id){refreshOrbConnection();resetController();syncMotionSources();}
 @Override public boolean onUnbind(Intent i){releaseConnection("service_unbound");if(current()==this){ref.clear();MainActivity.connectionChanged();}return super.onUnbind(i);}
 @Override public void onDestroy(){releaseConnection("service_destroyed");if(connectionListenersRegistered){((InputManager)getSystemService(INPUT_SERVICE)).unregisterInputDeviceListener(this);try{unregisterReceiver(screenReceiver);}catch(IllegalArgumentException ignored){}connectionListenersRegistered=false;}if(current()==this){ref.clear();MainActivity.connectionChanged();}super.onDestroy();}
}
