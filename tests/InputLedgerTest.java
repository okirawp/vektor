package dev.kolmapper;
public final class InputLedgerTest {
 static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
 public static void main(String[] args){
  InputLedger l=new InputLedger();int[] keys={21,22,96,99,105};
  for(int i=0;i<10000;i++){
   for(int key:keys)check(l.set(key,7,0,true),"all five simultaneous inputs down");
   check(!l.set(22,7,0,false),"right physically releases");
   for(int key:new int[]{21,96,99,105})check(l.held(key),"right release leaves other buttons held");
   for(int key:keys)l.set(key,7,0,false);
   for(int key:keys)check(!l.held(key),"no sticky button after release");
  }
  l.set(21,1,0,true);l.set(21,2,0,true);check(l.set(21,1,0,false),"independent controllers");check(!l.set(21,2,0,false),"last controller releases");
  l.set(105,1,0,true);l.set(105,1,3,true);check(l.set(105,1,0,false),"key release preserves analog owner");check(!l.set(105,1,3,false),"trigger neutral releases");
  l.set(105,1,0,true);l.set(105,1,3,true);check(l.set(105,1,3,false),"trigger neutral preserves an independently held button");check(!l.set(105,1,0,false),"last trigger/button owner releases");
  l.set(22,1,0,true);l.set(22,1,4,true);check(l.owns(22,1,4),"analog direction is independently owned");check(l.set(22,1,4,false),"analog neutral preserves held dpad button");check(!l.set(22,1,0,false),"dpad release lifts last contact");
  l.set(21,1,0,true);l.clear();check(!l.held(21),"screen off / interruption clears inputs");
  System.out.println("PASS: 10000 simultaneous five-button press/release sequences; devices, axis/key owners and reset");
 }
}
