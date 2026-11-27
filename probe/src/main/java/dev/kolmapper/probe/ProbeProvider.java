package dev.kolmapper.probe;
import android.content.*;import android.database.*;import android.net.Uri;
public class ProbeProvider extends ContentProvider {
 public boolean onCreate(){return true;}
 public Cursor query(Uri u,String[] p,String s,String[] a,String order){MatrixCursor c=new MatrixCursor(new String[]{"events","active"});synchronized(ProbeActivity.events){c.addRow(new Object[]{String.join(";",ProbeActivity.events),ProbeActivity.active.size()});}return c;}
 public String getType(Uri u){return "vnd.android.cursor.item/events";}
 public int delete(Uri u,String s,String[] a){ProbeActivity.events.clear();ProbeActivity.active.clear();return 1;}
 public Uri insert(Uri u,ContentValues v){return null;}public int update(Uri u,ContentValues v,String s,String[] a){return 0;}
}
