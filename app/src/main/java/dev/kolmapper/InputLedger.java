package dev.kolmapper;
import java.util.*;
/** Independent owners for key, hat and analog inputs; one release cannot drop another contact. */
final class InputLedger {
 private final Map<Integer,Set<Long>> held=new HashMap<>();
 boolean set(int key,int device,int channel,boolean down){
  long owner=((long)device<<32)|(channel&0xffffffffL);
  Set<Long> set=held.computeIfAbsent(key,k->new HashSet<>());
  if(down)set.add(owner);else set.remove(owner);
  if(set.isEmpty()){held.remove(key);return false;}return true;
 }
 boolean held(int key){return held.containsKey(key);}
 boolean owns(int key,int device,int channel){Set<Long> set=held.get(key);return set!=null&&set.contains(((long)device<<32)|(channel&0xffffffffL));}
 void clear(){held.clear();}
}
