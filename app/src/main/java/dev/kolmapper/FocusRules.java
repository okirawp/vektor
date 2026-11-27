package dev.kolmapper;
/** Own focused modal windows may hide the underlying app on OEM Android builds.
 * An external application state event revokes the allowed scope before this rule.
 */
final class FocusRules {
 static boolean keepEditor(boolean editing,boolean allowed,boolean ownFocused,boolean imeFocused,boolean dialogShowing,String active,String underneath){
  if(!editing||!allowed||active==null)return false;
  boolean same=active.equals(underneath);
  return ownFocused&&(same||underneath==null)||imeFocused&&dialogShowing&&same;
 }
}
