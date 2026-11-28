package dev.kolmapper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Activity-owned cache. Worker threads query package metadata and load only requested icons. */
final class AppCatalog implements AutoCloseable {
 static final class Entry {
  final String pkg,name,searchKey;
  final boolean game,system;
  final ApplicationInfo app;
  Entry(ApplicationInfo app,String name,Locale locale){
   this.app=app;pkg=app.packageName;this.name=name;
   searchKey=(name+" "+pkg).toLowerCase(locale);
   game=app.category==ApplicationInfo.CATEGORY_GAME||(app.flags&ApplicationInfo.FLAG_IS_GAME)!=0;
   system=(app.flags&ApplicationInfo.FLAG_SYSTEM)!=0;
  }
 }
 private final Context context;
 private final Handler main=new Handler(Looper.getMainLooper());
 private final ExecutorService index=Executors.newSingleThreadExecutor();
 private final ExecutorService icons=Executors.newSingleThreadExecutor();
 private final LruCache<String,Drawable.ConstantState> iconCache=new LruCache<>(64);
 private final Map<String,List<Consumer<Drawable.ConstantState>>> pending=new HashMap<>();
 private final BroadcastReceiver changes=new BroadcastReceiver(){
  @Override public void onReceive(Context owner,Intent event){refresh();}
 };
 private List<Entry> snapshot;
 private Consumer<List<Entry>> listener;
 private int revision;
 private boolean indexing,indexed,closed;

 AppCatalog(Context activity){
  context=activity.getApplicationContext();
  IntentFilter filter=new IntentFilter();
  filter.addAction(Intent.ACTION_PACKAGE_ADDED);
  filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
  filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
  filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
  filter.addDataScheme("package");
  if(Build.VERSION.SDK_INT>=33)context.registerReceiver(changes,filter,Context.RECEIVER_NOT_EXPORTED);
  else context.registerReceiver(changes,filter);
  refresh();
 }
 void observe(Consumer<List<Entry>> consumer){
  listener=consumer;
  if(consumer!=null&&indexed)consumer.accept(snapshot);
 }
 private void refresh(){
  if(closed)return;
  revision++;
  iconCache.evictAll();pending.clear();
  if(!indexing)startIndex();
 }
 private void startIndex(){
  indexing=true;int expected=revision;
  index.execute(()->{
   List<Entry> entries;
   try{entries=query();}
   catch(RuntimeException e){android.util.Log.w("VektorCatalog","Uygulama listesi okunamadı",e);entries=null;}
   List<Entry> result=entries;
   main.post(()->{
    if(closed)return;
    indexing=false;
    if(expected!=revision){startIndex();return;}
    if(result!=null)snapshot=result;
    indexed=true;
    if(listener!=null)listener.accept(snapshot);
   });
  });
 }
 private List<Entry> query(){
  PackageManager pm=context.getPackageManager();
  Locale locale=context.getResources().getConfiguration().getLocales().get(0);
  Intent launch=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
  Map<String,Entry> unique=new HashMap<>();
  for(ResolveInfo match:pm.queryIntentActivities(launch,0)){
   if(match.activityInfo==null)continue;
   ApplicationInfo app=match.activityInfo.applicationInfo;
   if(app==null||app.packageName.equals(context.getPackageName())||!match.activityInfo.enabled||!app.enabled||unique.containsKey(app.packageName))continue;
   unique.put(app.packageName,new Entry(app,app.loadLabel(pm).toString(),locale));
  }
  ArrayList<Entry> result=new ArrayList<>(unique.values());
  Collator sort=Collator.getInstance(locale);
  result.sort((a,b)->{int order=sort.compare(a.name,b.name);return order==0?a.pkg.compareTo(b.pkg):order;});
  return result;
 }
 void icon(Entry entry,Consumer<Drawable.ConstantState> show){
  if(closed)return;
  Drawable.ConstantState cached=iconCache.get(entry.pkg);
  if(cached!=null){show.accept(cached);return;}
  List<Consumer<Drawable.ConstantState>> waiting=pending.get(entry.pkg);
  if(waiting!=null){waiting.add(show);return;}
  waiting=new ArrayList<>();waiting.add(show);pending.put(entry.pkg,waiting);
  int expected=revision;
  icons.execute(()->{
   Drawable.ConstantState loaded=null;
   try{loaded=entry.app.loadIcon(context.getPackageManager()).getConstantState();}
   catch(RuntimeException e){android.util.Log.d("VektorCatalog","Simge okunamadı: "+entry.pkg,e);}
   Drawable.ConstantState state=loaded;
   main.post(()->{
    if(closed||revision!=expected)return;
    List<Consumer<Drawable.ConstantState>> callbacks=pending.remove(entry.pkg);
    if(callbacks==null||state==null)return;
    iconCache.put(entry.pkg,state);for(Consumer<Drawable.ConstantState> callback:callbacks)callback.accept(state);
   });
  });
 }
 @Override public void close(){
  if(closed)return;
  closed=true;listener=null;pending.clear();iconCache.evictAll();
  context.unregisterReceiver(changes);
  index.shutdownNow();icons.shutdownNow();
 }
}
