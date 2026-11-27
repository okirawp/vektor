package dev.kolmapper;
/** No saved session can grant permission to draw over a locked or inactive screen. */
final class SessionRules {
 static boolean show(boolean active,boolean interactive,boolean locked,boolean ownScreen){return active&&interactive&&!locked&&!ownScreen;}
 static boolean keep(boolean active,boolean interactive,boolean locked){return active&&interactive&&!locked;}
}
