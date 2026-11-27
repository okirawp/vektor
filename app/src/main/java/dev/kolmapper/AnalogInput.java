package dev.kolmapper;
/** Axis identifiers are separate from Android key codes and survive profile upgrades. */
final class AnalogInput {
 static final int LEFT_STICK=-100, RIGHT_STICK=-101, LEFT_TRIGGER=-102, RIGHT_TRIGGER=-103;
 static boolean stick(int key){return key==LEFT_STICK||key==RIGHT_STICK;}
 static float normalize(float value,float min,float max){return max<=min?0:Math.max(0,Math.min(1,(value-min)/(max-min)));}
 static float[] vector(float x,float y,float deadzone){
  float length=(float)Math.hypot(x,y);if(length<=deadzone)return new float[]{0,0};
  float magnitude=Math.min(1,(length-deadzone)/(1-deadzone));return new float[]{x/length*magnitude,y/length*magnitude};
 }
 static boolean direction(float magnitude,boolean down){return magnitude>=(down?.15f:.25f);}
 static boolean active(float x,float y){return x!=0||y!=0;}
 static boolean threshold(float value,boolean down){return value>=(down?.35f:.55f);}
}
