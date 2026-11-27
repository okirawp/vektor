package dev.kolmapper;
/** Keep the complete drag target visible while keeping its center equal to the touch point. */
final class BadgePosition {
 static int center(float raw,int length,int size){int half=size/2;return Math.max(half,Math.min(length-(size-half),Math.round(raw)));}
}
