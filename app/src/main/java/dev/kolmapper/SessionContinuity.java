package dev.kolmapper;
import android.content.*;
import android.os.PowerManager;
import android.app.KeyguardManager;
import android.provider.Settings;
import java.util.*;
/** Process-memory authorization for a temporary accessibility rebind. Never restored from disk. */
final class SessionContinuity {
 private static boolean authorized;
 private static Context app;
 private static BroadcastReceiver monitor;
 private static final Map<Integer,String> windows=new LinkedHashMap<>();
 static boolean safe(Context c){return ((PowerManager)c.getSystemService(Context.POWER_SERVICE)).isInteractive()&&!((KeyguardManager)c.getSystemService(Context.KEYGUARD_SERVICE)).isKeyguardLocked();}
 static void authorize(Context c){
  if(monitor==null){app=c.getApplicationContext();monitor=new BroadcastReceiver(){@Override public void onReceive(Context context,Intent event){
   clear();MapperService service=MapperService.current();if(service!=null&&service.sessionActive())service.endSession("screen_locked");
  }};IntentFilter filter=new IntentFilter(Intent.ACTION_SCREEN_OFF);filter.addAction(Intent.ACTION_USER_PRESENT);app.registerReceiver(monitor,filter);}
  authorized=true;
 }
 static boolean enabled(Context c){
  if(Settings.Secure.getInt(c.getContentResolver(),Settings.Secure.ACCESSIBILITY_ENABLED,0)!=1)return false;
  ComponentName own=new ComponentName(c,MapperService.class);String list=Settings.Secure.getString(c.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
  if(list!=null)for(String item:list.split(":"))if(own.equals(ComponentName.unflattenFromString(item)))return true;
  return false;
 }
 static boolean canResume(Context c){return authorized&&safe(c)&&enabled(c);}
 static void remember(Map<Integer,String> values){windows.clear();windows.putAll(values);}
 static Map<Integer,String> remembered(){return new LinkedHashMap<>(windows);}
 static void clear(){authorized=false;windows.clear();if(monitor!=null){try{app.unregisterReceiver(monitor);}catch(IllegalArgumentException ignored){}monitor=null;app=null;}}
}
