package dev.kolmapper;
public final class RepeatStateTest {
 static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 public static void main(String[] args){
  RepeatState s=new RepeatState(0);check(!s.ready(0),"not active initially");s.down(0);
  for(int i=0;i<10000;i++){check(s.ready(i*100L),"unlimited repeat");s.dispatched(i*100L,100);check(!s.ready(i*100L+99),"respect interval");}
  s.up();check(!s.ready(Long.MAX_VALUE),"release cancels infinite repeat");
  RepeatState finite=new RepeatState(100);finite.down(0);
  for(int i=0;i<100;i++){check(finite.ready(i*50L),"finite action due");finite.dispatched(i*50L,50);}
  check(!finite.ready(100000),"finite repeat stops at limit");finite.down(100000);check(!finite.ready(100000),"duplicate down does not restart");
  finite.up();finite.down(100000);check(finite.ready(100000),"new physical press restarts");finite.dispatched(100000,50);finite.up();check(!finite.ready(200000),"release cancels pending finite repeats");
  RepeatState single=new RepeatState(1);single.down(0);single.dispatched(0,100);check(!single.ready(1000),"single click");
  RepeatState timing=new RepeatState(0);check(timing.delay(0)==Long.MAX_VALUE,"no timer while idle");timing.down(500);
  check(timing.delay(500)==0,"first press immediately due");timing.dispatched(500,16);
  check(timing.delay(501)==15,"exact next deadline, no polling rounding");check(timing.delay(516)==0,"deadline reached");
  timing.up();check(timing.delay(501)==Long.MAX_VALUE,"release removes pending deadline");
  check(single.delay(1000)==Long.MAX_VALUE,"completed finite press has no idle wakeups");
  for(int width:new int[]{800,1200,2000})for(int raw:new int[]{-100,0,26,400,9999}){
   int center=BadgePosition.center(raw,width,52);check(center>=26&&center<=width-26,"drag badge remains fully on screen");
  }
  check(BadgePosition.center(400,1200,52)==400,"badge center equals mapped touch coordinate");
  System.out.println("PASS: scheduling deadlines, immediate first press, no idle timer, release cancels deadline, drag bounds and exact center");
  System.out.println("PASS: infinite 10000 repeats, finite limit, immediate release, duplicate down, repress, interval, single tap");
 }
}
