package dev.kolmapper;
public final class ContactPolicyTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args){
  check(ContactPolicy.legacyHold(21,1)&&ContactPolicy.legacyHold(22,1),"existing single-tap arrows migrate to hold");
  for(int key:new int[]{96,99,105})for(int n:new int[]{0,1,100})check(!ContactPolicy.legacyHold(key,n),"A X R2 macro remains a macro");
  check(!ContactPolicy.legacyHold(21,0),"explicit repeated arrow is preserved");
  check(ContactPolicy.wantsHold(true,true,true),"press begins contact");
  for(int i=0;i<10000;i++)check(ContactPolicy.shouldContinue(ContactPolicy.wantsHold(true,true,true),false),"hold stays down across macro gesture frames");
  check(!ContactPolicy.wantsHold(true,false,true),"release ends contact");
  check(!ContactPolicy.wantsHold(false,true,true),"stop ends contact even if physically held");
  check(!ContactPolicy.wantsHold(true,true,false),"macro uses short taps, never persistent touch");
  check(!ContactPolicy.shouldContinue(true,true),"quick repress cannot swallow preceding release");
  System.out.println("PASS: left/right migration, A/X/R2 preservation, continuous contact across 10000 frames, release, stop, quick repress");
 }
}
