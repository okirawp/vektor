package dev.kolmapper;
import android.content.Context;
import org.json.*;
import java.util.*;

final class Binding {
 int key, x, y, width, height, interval, count; int duration=8; boolean hold; int radius=72;
 Binding(int key,int x,int y,int w,int h,int interval,int count) {
  this.key=key;this.x=x;this.y=y;this.width=w;this.height=h;this.interval=interval;this.count=count;this.hold=ContactPolicy.legacyHold(key,count);
 }
 static List<Binding> load(Context c){return load(c,Profiles.selected(c));}
 static List<Binding> load(Context c,String pkg) {
  if(!ProfileRules.valid(pkg))return new ArrayList<>();
  android.content.SharedPreferences p=Profiles.prefs(c);
  try{return parse(p.getString(ProfileRules.key(pkg,"bindings"),"[]"));}
  catch(JSONException e){try{return parse(p.getString(ProfileRules.key(pkg,"previous"),"[]"));}catch(JSONException ignored){return new ArrayList<>();}}
 }
 static List<Binding> parse(String raw)throws JSONException{
  List<Binding> out=new ArrayList<>();JSONArray a=new JSONArray(raw);if(a.length()>128)throw new JSONException("Too many bindings");
   for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);out.add(new Binding(o.getInt("key"),o.getInt("x"),o.getInt("y"),o.getInt("w"),o.getInt("h"),Math.max(16,o.getInt("interval")),Math.max(0,o.getInt("count"))));out.get(out.size()-1).hold=o.optBoolean("hold",ContactPolicy.legacyHold(o.getInt("key"),o.getInt("count")));out.get(out.size()-1).radius=Math.max(16,Math.min(300,o.optInt("radius",72)));out.get(out.size()-1).duration=Math.max(8,Math.min(100,o.optInt("duration",8)));}
  Set<Integer> keys=new HashSet<>();for(Binding b:out){if(!keys.add(b.key)||b.key< -103||b.key>1000||b.width<1||b.height<1||b.width>100000||b.height>100000||b.x<0||b.y<0||b.x>b.width||b.y>b.height)throw new JSONException("Invalid binding coordinates/key");}
  return out;
 }
 static void save(Context c,List<Binding> list){save(c,Profiles.selected(c),list);}
 static void save(Context c,String pkg,List<Binding> list) {
  if(!ProfileRules.valid(pkg))throw new IllegalStateException("Select a game before saving");
  JSONArray a=new JSONArray();
  try {for(Binding b:list){JSONObject o=new JSONObject();o.put("key",b.key).put("x",b.x).put("y",b.y).put("w",b.width).put("h",b.height).put("interval",b.interval).put("count",b.count).put("duration",b.duration).put("hold",b.hold).put("radius",b.radius);a.put(o);}}
  catch(JSONException e){throw new IllegalStateException(e);}
  android.content.SharedPreferences p=Profiles.prefs(c);String key=ProfileRules.key(pkg,"bindings");
  android.content.SharedPreferences.Editor edit=p.edit();String old=p.getString(key,"[]");
  try{parse(old);edit.putString(ProfileRules.key(pkg,"previous"),old);}catch(JSONException ignored){}
  if(!edit.putString(key,a.toString()).commit())throw new IllegalStateException("Atamalar diske kaydedilemedi");
 }
}
