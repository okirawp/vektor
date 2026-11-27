package dev.kolmapper.vpnprobe;
import android.app.*;import android.content.*;import android.net.VpnService;import android.os.*;import android.content.pm.ServiceInfo;
public class ProbeVpn extends VpnService {
 ParcelFileDescriptor tunnel;
 @Override public void onCreate(){super.onCreate();NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("test","Emulator VPN test",NotificationManager.IMPORTANCE_LOW));Notification n=new Notification.Builder(this,"test").setSmallIcon(android.R.drawable.ic_lock_lock).setContentTitle("VPN regression fixture").setContentText("Routes only 192.0.2.0/24; no production traffic").build();if(Build.VERSION.SDK_INT>=34)startForeground(77,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);else startForeground(77,n);}
 @Override public int onStartCommand(Intent i,int flags,int id){if(tunnel==null)tunnel=new Builder().setSession("Rota QA VPN").addAddress("192.0.2.2",32).addRoute("192.0.2.0",24).establish();return START_NOT_STICKY;}
 @Override public void onRevoke(){stopSelf();}
 @Override public void onDestroy(){if(tunnel!=null)try{tunnel.close();}catch(java.io.IOException ignored){}super.onDestroy();}
}
