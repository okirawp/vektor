package dev.kolmapper;
/** Pixel coordinates shared by initial layout, dragging, and rotation. */
final class OverlayPlacement {
 static int clamp(int value,int minimum,int maximum){return Math.max(minimum,Math.min(Math.max(minimum,maximum),value));}
 static int[] panel(int orbX,int orbY,int orbSize,int panelWidth,int panelHeight,int width,int height,int gap){
  int x=orbX+orbSize+gap;
  if(x+panelWidth>width-gap)x=orbX-panelWidth-gap;
  return new int[]{clamp(x,gap,width-panelWidth-gap),clamp(orbY,gap,height-panelHeight-gap)};
 }
}
