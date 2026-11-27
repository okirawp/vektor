package dev.kolmapper;
import android.content.Context;
import android.content.SharedPreferences;
/** Optional local coaching state; never writes a mapping, backup or controller setting. */
final class GuideState {
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("experience",0);}
 static boolean active(Context c,String game,boolean empty){if(!ProfileRules.valid(game))return false;SharedPreferences p=prefs(c);return !p.getBoolean("hidden."+game,false)&&!p.getBoolean("done."+game,false)&&(empty||p.getBoolean("started."+game,false)||p.getBoolean("requested."+game,false));}
 static void begin(Context c,String game){if(ProfileRules.valid(game))prefs(c).edit().putBoolean("started."+game,true).apply();}
 static void request(Context c,String game){if(ProfileRules.valid(game))prefs(c).edit().putBoolean("requested."+game,true).putBoolean("hidden."+game,false).putBoolean("done."+game,false).apply();}
 static void skip(Context c,String game){if(ProfileRules.valid(game))prefs(c).edit().putBoolean("hidden."+game,true).putBoolean("requested."+game,false).apply();}
 static void complete(Context c,String game){if(ProfileRules.valid(game))prefs(c).edit().putBoolean("done."+game,true).putBoolean("requested."+game,false).putBoolean("started."+game,false).apply();}
 static String editorText(boolean learning,boolean empty){return learning?"Koldaki tuşa bir kez bas, ardından ekranda dokunacağı noktayı seç.":empty?"1   Kontrol ekle’ye dokun.\n2   Koldaki tuşa bas.\n3   Ekrandaki hedefi seç.":"Etiketi sürükleyerek yerleştir. Davranışını seçmek için etikete dokun.";}
}
