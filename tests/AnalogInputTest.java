package dev.kolmapper;
public final class AnalogInputTest {
 static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
 public static void main(String[] args){
  check(AnalogInput.direction(.3f,false)&&AnalogInput.direction(.2f,true)&&!AnalogInput.direction(.2f,false)&&!AnalogInput.direction(0,true),"digital direction activation, hysteresis and neutral");
  float[] center=AnalogInput.vector(.1f,-.1f,.18f);check(!AnalogInput.active(center[0],center[1]),"dead zone lifts contact");
  float[] diagonal=AnalogInput.vector(1,1,.18f);check(Math.abs(Math.hypot(diagonal[0],diagonal[1])-1)<.0001,"diagonal clamped to circle");
  float[] right=AnalogInput.vector(1,0,.18f);check(right[0]==1&&right[1]==0,"full right");
  check(AnalogInput.normalize(-1,-1,1)==0&&AnalogInput.normalize(1,-1,1)==1,"signed trigger calibration");
  check(!AnalogInput.threshold(.5f,false)&&AnalogInput.threshold(.5f,true),"trigger hysteresis");
  check(!AnalogInput.threshold(.1f,true),"trigger release");
  for(int extent:new int[]{2,800,1280})for(int centerX:new int[]{0,extent/2,extent-1}){
   int previous=centerX;
   for(int i=0;i<10000;i++){int end=HoldPath.heartbeat(centerX,previous,extent);check(end!=previous,"every continued frame generates MOVE");check(end>=0&&end<extent,"heartbeat stays on screen");check(Math.abs(end-centerX)<=1,"one physical pixel maximum");previous=end;}
  }
  System.out.println("PASS: analog center/diagonal/range/trigger hysteresis; 90000 non-empty hold continuations including edges");
 }
}
