package dev.kolmapper;
import android.content.*;
import java.io.*;
import java.util.*;
import org.json.*;
final class Backups {
 static Map<String,Object> snapshot(Context c)throws IOException{
  Map<String,Object> out=new HashMap<>();for(Map.Entry<String,?> e:Profiles.prefs(c).getAll().entrySet())if(allowed(e.getKey()))out.put(e.getKey(),e.getValue());
  out.putIfAbsent("games",new HashSet<String>());out.putIfAbsent("selected_game","");
  for(String key:new ArrayList<>(out.keySet())){
   if(key.endsWith(".previous")){try{Binding.parse((String)out.get(key));}catch(Exception e){out.remove(key);}}
   else if(key.endsWith(".bindings")){try{Binding.parse((String)out.get(key));}catch(Exception e){String previous=key.substring(0,key.length()-8)+"previous";try{String raw=(String)out.get(previous);if(raw==null)throw new IOException("Kurtarma kopyası yok");Binding.parse(raw);out.put(key,raw);}catch(Exception failure){throw new IOException("Atama kurtarılamadı",failure);}}}
  }
  validate(out);return out;
 }
 static boolean allowed(String k){return k.equals("games")||k.equals("selected_game")||k.equals("dark")||k.equals("bindings")||k.equals("orbX")||k.equals("orbY")||k.equals("legacy_imported_to")||k.startsWith("game.");}
 static void validate(Map<String,Object> values)throws IOException{
  Object games=values.get("games");if(!(games instanceof Set))throw new IOException("Oyun listesi eksik");
  for(Object pkg:(Set<?>)games)if(!(pkg instanceof String)||!ProfileRules.valid((String)pkg)||pkg.equals("dev.kolmapper"))throw new IOException("Geçersiz oyun");
  try{for(Map.Entry<String,Object> e:values.entrySet()){
   String k=e.getKey();Object v=e.getValue();
   if(k.equals("games"))continue;
   if(k.equals("selected_game")||k.equals("legacy_imported_to")){if(!(v instanceof String)||!((String)v).isEmpty()&&!((Set<?>)games).contains(v))throw new IOException("Geçersiz oyun seçimi");continue;}
   if(k.equals("dark")){if(!(v instanceof Boolean))throw new IOException("Geçersiz tema");continue;}
   String field=k;
   if(k.startsWith("game.")){int split=k.lastIndexOf('.');String pkg=k.substring(5,split);if(!((Set<?>)games).contains(pkg))throw new IOException("Bilinmeyen oyun");field=k.substring(split+1);}
   if(field.equals("bindings")||field.equals("previous")){if(!(v instanceof String))throw new IOException("Geçersiz atamalar");Binding.parse((String)v);}
   else if(field.equals("orbX")||field.equals("orbY")){if(!(v instanceof Integer)||Math.abs((long)(Integer)v)>100000)throw new IOException("Geçersiz ikon konumu");}
   else if(field.equals("ready")){if(!(v instanceof Boolean))throw new IOException("Geçersiz durum");}
   else throw new IOException("Bilinmeyen ayar");
  }}catch(JSONException|IllegalArgumentException e){throw new IOException("Geçersiz atama verisi",e);}
 }
 static byte[] read(InputStream in)throws IOException{if(in==null)throw new IOException("Dosya açılamadı");try(InputStream stream=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=stream.read(b))!=-1){if(out.size()+n>BackupCodec.LIMIT)throw new IOException("Dosya çok büyük");out.write(b,0,n);}return out.toByteArray();}}
 static void restore(Context c,Map<String,Object> imported)throws IOException{
  validate(imported);
  // Keep a complete local rollback before one atomic preferences transaction.
  android.util.AtomicFile rollback=new android.util.AtomicFile(new File(c.getFilesDir(),"before-restore.rota"));FileOutputStream stream=null;
  try{stream=rollback.startWrite();stream.write(BackupCodec.encode(snapshot(c)));rollback.finishWrite(stream);}catch(IOException e){if(stream!=null)rollback.failWrite(stream);throw e;}
  SharedPreferences p=Profiles.prefs(c);SharedPreferences.Editor edit=p.edit();Set<String> games=Profiles.known(c);games.addAll((Set<String>)imported.get("games"));
  for(String pkg:(Set<String>)imported.get("games"))for(String field:Arrays.asList("bindings","previous","orbX","orbY","ready"))edit.remove(ProfileRules.key(pkg,field));
  for(Map.Entry<String,Object> e:imported.entrySet()){String k=e.getKey();Object v=e.getValue();if(k.equals("games")||k.equals("dark"))continue;
   if(v instanceof String)edit.putString(k,(String)v);else if(v instanceof Integer)edit.putInt(k,(Integer)v);else if(v instanceof Boolean)edit.putBoolean(k,(Boolean)v);
  }
  if(!edit.putStringSet("games",games).commit())throw new IOException("Yedek kaydedilemedi");
 }
}
