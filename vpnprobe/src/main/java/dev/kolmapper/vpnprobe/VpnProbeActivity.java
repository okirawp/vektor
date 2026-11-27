package dev.kolmapper.vpnprobe;
import android.app.*;import android.os.*;import android.content.*;import android.net.VpnService;import android.widget.*;
/** Emulator-only regression fixture, never shipped in Rota. */
public class VpnProbeActivity extends Activity {
 @Override public void onCreate(Bundle b){super.onCreate(b);LinearLayout page=new LinearLayout(this);page.setOrientation(1);page.setPadding(48,48,48,48);TextView title=new TextView(this);title.setText("VPN lifecycle test — documentation network only");title.setTextSize(22);page.addView(title);Button connect=new Button(this);connect.setText("Connect test VPN");connect.setOnClickListener(v->{Intent consent=VpnService.prepare(this);if(consent==null)connect();else startActivityForResult(consent,1);});page.addView(connect);Button stop=new Button(this);stop.setText("Disconnect test VPN");stop.setOnClickListener(v->stopService(new Intent(this,ProbeVpn.class)));page.addView(stop);setContentView(page);}
 void connect(){startForegroundService(new Intent(this,ProbeVpn.class));}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==1&&result==RESULT_OK)connect();}
}
