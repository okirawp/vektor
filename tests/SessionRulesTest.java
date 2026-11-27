package dev.kolmapper;
public class SessionRulesTest {
 public static void main(String[] args){
  for(int bits=0;bits<16;bits++){
   boolean active=(bits&1)!=0,interactive=(bits&2)!=0,locked=(bits&4)!=0,other=(bits&8)!=0;
   if(SessionRules.show(active,interactive,locked,other)!=(bits==3))throw new AssertionError("unsafe overlay "+bits);
   if(SessionRules.keep(active,interactive,locked)!=((bits&7)==3))throw new AssertionError("unsafe session "+bits);
  }
  System.out.println("PASS: all 32 session/visibility combinations stop on lock/screen off; home/recents do not end the session");
 }
}
