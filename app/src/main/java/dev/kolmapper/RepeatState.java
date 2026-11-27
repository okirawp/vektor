package dev.kolmapper;
/** Pure state: physical release always cancels, even before a finite count completes. */
final class RepeatState {
 boolean held; int sent; final int limit; long nextAt;
 RepeatState(int limit){this.limit=limit;}
 void down(long now){if(!held){held=true;sent=0;nextAt=now;}}
 void up(){held=false;}
 boolean ready(long now){return held && (limit==0 || sent<limit) && now>=nextAt;}
 long delay(long now){return !held || limit>0 && sent>=limit ? Long.MAX_VALUE : Math.max(0,nextAt-now);}
 void dispatched(long now,int interval){sent++;nextAt=now+interval;}
}
