package dev.kolmapper;
import java.util.Set;
import java.util.regex.Pattern;
/** Package identity is the boundary for both stored settings and foreground input. */
final class ProfileRules {
 static final int IGNORE=0, ACTIVATE=1, PAUSE=2;
 private static final Pattern PACKAGE=Pattern.compile("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+");
 static boolean valid(String pkg){return pkg!=null&&PACKAGE.matcher(pkg).matches();}
 static String key(String pkg,String setting){if(!valid(pkg))throw new IllegalArgumentException("Missing game package");return "game."+pkg+"."+setting;}
 static int event(boolean session,boolean main,String pkg,String own,boolean ime,Set<String> games){
  if(!session)return IGNORE;
  if(main)return PAUSE;
  if(own.equals(pkg)||ime)return IGNORE;
  return pkg!=null&&games.contains(pkg)?ACTIVATE:PAUSE;
 }
}
