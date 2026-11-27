package dev.kolmapper;
import android.content.*;
import android.content.pm.PackageManager;
import java.util.*;
import org.json.*;
final class Profiles {
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("mapper",0);}
 static String selected(Context c){return prefs(c).getString("selected_game","");}
 static Set<String> known(Context c){return new HashSet<>(prefs(c).getStringSet("games",Collections.emptySet()));}
 static void select(Context c,String pkg){
  if(!ProfileRules.valid(pkg)||pkg.equals(c.getPackageName()))throw new IllegalArgumentException("Invalid game");
  Set<String> all=known(c);all.add(pkg);prefs(c).edit().putStringSet("games",all).putString("selected_game",pkg).apply();
 }
 static String label(Context c,String pkg){try{return c.getPackageManager().getApplicationLabel(c.getPackageManager().getApplicationInfo(pkg,0)).toString();}catch(PackageManager.NameNotFoundException e){return pkg;}}
 static boolean ready(Context c,String pkg){return ProfileRules.valid(pkg)&&prefs(c).getBoolean(ProfileRules.key(pkg,"ready"),false);}
 static void ready(Context c,String pkg,boolean ready){if(ProfileRules.valid(pkg))prefs(c).edit().putBoolean(ProfileRules.key(pkg,"ready"),ready).apply();}
 static int orb(Context c,String pkg,String axis,int fallback){return prefs(c).getInt(ProfileRules.key(pkg,axis),fallback);}
 static void orb(Context c,String pkg,int x,int y){prefs(c).edit().putInt(ProfileRules.key(pkg,"orbX"),x).putInt(ProfileRules.key(pkg,"orbY"),y).apply();}
 static boolean hasLegacy(Context c){
  if(!prefs(c).getString("legacy_imported_to","").isEmpty())return false;
  try{return new JSONArray(prefs(c).getString("bindings","[]")).length()>0;}catch(JSONException e){return false;}
 }
 /** One atomic preferences edit assigns the old unscoped backup to an explicitly chosen game. */
 static boolean importLegacy(Context c,String pkg){
  if(!hasLegacy(c)||!ProfileRules.valid(pkg)||!Binding.load(c,pkg).isEmpty())return false;
  SharedPreferences p=prefs(c);Set<String> games=known(c);games.add(pkg);
  SharedPreferences.Editor edit=p.edit().putString(ProfileRules.key(pkg,"bindings"),p.getString("bindings","[]"))
   .putBoolean(ProfileRules.key(pkg,"ready"),true).putString("legacy_imported_to",pkg).putString("selected_game",pkg).putStringSet("games",games);
  if(p.contains("orbX"))edit.putInt(ProfileRules.key(pkg,"orbX"),p.getInt("orbX",16));
  if(p.contains("orbY"))edit.putInt(ProfileRules.key(pkg,"orbY"),p.getInt("orbY",80));
  edit.apply();return true;
 }
}
