package dev.kolmapper;
/** Stationary continued gestures produce no MotionEvents in Android's injector.
 * Alternate one physical pixel inside the button so every segment has a MOVE and a result. */
final class HoldPath {
 static int heartbeat(int center,int previous,int extent){
  int alternate=center<extent-1?center+1:Math.max(0,center-1);
  return previous==center?alternate:center;
 }
}
