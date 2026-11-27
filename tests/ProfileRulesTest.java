package dev.kolmapper;
import java.util.*;
public class ProfileRulesTest {
 static void check(boolean b,String label){if(!b)throw new AssertionError(label);}
 public static void main(String[] args){
  String a="com.example.gameA",b="com.example.gameB",own="dev.kolmapper";Set<String> games=new HashSet<>(Arrays.asList(a,b));
  for(String setting:new String[]{"bindings","orbX","orbY","ready"})check(!ProfileRules.key(a,setting).equals(ProfileRules.key(b,setting)),"separate "+setting);
  check(!ProfileRules.valid("")&&!ProfileRules.valid(null)&&!ProfileRules.valid("../game"),"no missing/global profile fallback");
  check(ProfileRules.event(true,false,a,own,false,games)==1,"A activates its profile");check(ProfileRules.event(true,false,b,own,false,games)==1,"B activates its profile");
  for(String other:new String[]{"com.example.unknown","com.android.launcher3","com.android.systemui","",null})check(ProfileRules.event(true,false,other,own,false,games)==2,"foreign window pauses "+other);
  check(ProfileRules.event(true,false,own,own,false,games)==0,"own editor does not switch game");check(ProfileRules.event(true,false,"com.android.inputmethod.latin",own,true,games)==0,"editor keyboard stays usable");
  check(ProfileRules.event(true,true,a,own,false,games)==2,"main app always pauses");check(ProfileRules.event(false,false,a,own,false,games)==0,"no activation outside session");
  System.out.println("PASS: package-scoped settings, two games, unknown apps, launcher, system UI, main screen, keyboard and inactive session");
 }
}
