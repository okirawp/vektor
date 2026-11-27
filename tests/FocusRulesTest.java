package dev.kolmapper;
public class FocusRulesTest {
 static int checks;static void expect(boolean expected,boolean actual,String scenario){if(expected!=actual)throw new AssertionError(scenario);checks++;}
 public static void main(String[] args){
  expect(true,FocusRules.keepEditor(true,true,true,false,true,"game.a","game.a"),"own settings over game survives focus change");
  expect(true,FocusRules.keepEditor(true,true,false,true,true,"game.a","game.a"),"settings keyboard does not close editor");
  expect(false,FocusRules.keepEditor(true,true,true,false,true,"game.a","vpn.other"),"own dialog cannot remain above VPN activity");
  expect(true,FocusRules.keepEditor(true,true,true,false,true,"game.a",null),"focused own modal preserves previously verified game when OEM omits underneath");
  expect(false,FocusRules.keepEditor(true,true,false,true,false,"game.a","game.a"),"unrelated keyboard cannot authorize editor");
  expect(false,FocusRules.keepEditor(false,true,true,false,true,"game.a","game.a"),"closed editor cannot reopen from focus");
  expect(false,FocusRules.keepEditor(true,false,true,false,true,"game.a","game.a"),"paused scope cannot keep old dialog");
  expect(false,FocusRules.keepEditor(true,true,false,true,true,"game.a","game.b"),"another configured game does not inherit old editor");
  expect(false,FocusRules.keepEditor(true,true,true,false,true,null,null),"missing active game is never authorized");
  expect(false,FocusRules.keepEditor(true,false,true,false,true,"game.a",null),"external state event revokes modal scope even without underlying metadata");
  expect(false,FocusRules.keepEditor(true,true,false,false,true,"game.a",null),"showing dialog without own focus does not authorize unknown window");
  System.out.println("PASS "+checks+" editor focus regressions: settings, keyboard, VPN, other game and unknown window");
 }
}
