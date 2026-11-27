package dev.kolmapper;
import java.util.*;
/** Model the Android injector's retained stroke->pointer table (including its clamp at ID 10). */
public final class TouchStartPlanTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static final class PlatformPool {
  final Map<Integer,Integer> ids=new HashMap<>();
  int pointer(int stroke){Integer cached=ids.get(stroke);if(cached!=null)return cached;int pointer=0;while(ids.containsValue(pointer)&&pointer<10)pointer++;ids.put(stroke,pointer);return pointer;}
 }
 static void unique(Map<Integer,Integer> active){check(new HashSet<>(active.values()).size()==active.size(),"no two active fingers share an Android pointer ID");}
 public static void main(String[] args){
  int[] keys={21,22,96,99,105,-100,-101,-102,-103,108};int sequences=0;check(keys.length==TouchStartPlan.CAPACITY,"exercise full supported pointer capacity");
  for(int anchor:keys){
   PlatformPool pool=new PlatformPool();Map<Integer,Integer> active=new HashMap<>();int zero=0;
   for(int key:keys){int start=TouchStartPlan.start(false,key,anchor);if(start==0)zero++;active.put(key,pool.pointer(TouchStartPlan.cacheKey(key,start)));}
   check(zero==1,"initial gesture has exactly one anchor at time zero");unique(active);
   for(int i=0;i<10000;i++){
    int key=keys[i%keys.length];active.remove(key);
    int start=TouchStartPlan.start(true,key,anchor);check(start==1,"new fingers stay out of the first continuation sample");
    active.put(key,pool.pointer(TouchStartPlan.cacheKey(key,start)));unique(active);sequences++;
   }
   check(new HashSet<>(pool.ids.values()).size()<=keys.length+1,"retained identities fit Android's pointer pool indefinitely");
  }
  PlatformPool broken=new PlatformPool();for(int i=0;i<20;i++)broken.pointer(i);
  check(broken.ids.get(10).equals(broken.ids.get(19)),"model reproduces the original per-tap allocation failure");
  System.out.println("PASS: "+sequences+" multi-pointer release/repress sequences with retained Android pointer IDs; unique fingers and initial anchor");
 }
}
