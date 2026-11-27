package dev.kolmapper;
public class OverlayPlacementTest {
 static void check(boolean condition){if(!condition)throw new AssertionError("panel outside display");}
 public static void main(String[] args){
  int n=0;
  for(int w:new int[]{320,800,1280,2000})for(int h:new int[]{320,800,1200})for(int x:new int[]{-10,0,100,600,1270,2500})for(int y:new int[]{0,10,400,2000}){
   int pw=Math.min(260,w-16),ph=Math.min(240,h-16);int[] p=OverlayPlacement.panel(x,y,56,pw,ph,w,h,8);
   check(p[0]>=8&&p[1]>=8&&p[0]+pw<=w-8&&p[1]+ph<=h-8);n++;
  }
  int[] a=OverlayPlacement.panel(16,80,56,260,170,1280,800,8),b=OverlayPlacement.panel(300,200,56,260,170,1280,800,8);
  check(a[0]!=b[0]&&a[1]!=b[1]);
  System.out.println("PASS: attached panel follows the orb and stays in bounds in "+n+" edge/rotation cases");
 }
}
