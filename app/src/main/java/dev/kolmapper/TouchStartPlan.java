package dev.kolmapper;
/** Exactly one initial pointer starts at zero; other fingers join after it exists.
 * During a continuous session only its anchor can need two cached identities. */
final class TouchStartPlan {
 static final int CAPACITY=10;
 static int start(boolean continuing,int key,int anchor){return !continuing&&key==anchor?0:1;}
 static int cacheKey(int key,int start){return key*2+start;}
}
