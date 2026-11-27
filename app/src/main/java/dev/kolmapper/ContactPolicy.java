package dev.kolmapper;
/** Decisions independent of Android: hold contacts survive other taps and close on physical release. */
final class ContactPolicy {
 static boolean wantsHold(boolean armed,boolean physicalDown,boolean holdMode){return armed&&physicalDown&&holdMode;}
 static boolean shouldContinue(boolean wanted,boolean closing){return wanted&&!closing;}
 static boolean legacyHold(int key,int count){return count==1&&(key==21||key==22);}
}
