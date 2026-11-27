package dev.kolmapper;

import android.provider.Settings;
import android.content.Intent;
import android.view.Gravity;
import android.widget.*;
import java.io.File;

final class SupportPages {
 private final MainActivity host;
 private final Ui ui;
 private final LinearLayout page;
 SupportPages(MainActivity host, LinearLayout page){this.host=host;ui=host.ui;this.page=page;}

 private LinearLayout navigation;
 LinearLayout actions(){return navigation;}
 private void cover(String title,int scene,String description){
  page.addView(ui.text(title,27,true));
  StoryArt art=new StoryArt(host,ui,scene);
  art.setBackground(ui.shape(ui.surface,18));
  LinearLayout.LayoutParams frame=new LinearLayout.LayoutParams(-1,ui.dp(scene<5 && host.getResources().getConfiguration().screenHeightDp>=640?190:136));
  frame.topMargin=ui.dp(10);frame.bottomMargin=ui.dp(12);page.addView(art,frame);
  page.addView(ui.caption(description));
  if(scene<5)art.setOnTouchListener(new android.view.View.OnTouchListener(){
   float downX,downY;
   public boolean onTouch(android.view.View v,android.view.MotionEvent e){
    if(e.getAction()==0){downX=e.getX();downY=e.getY();return true;}
    if(e.getAction()==1){
     float dx=e.getX()-downX;
     if(Math.abs(dx)>ui.dp(56)&&Math.abs(dx)>1.5*Math.abs(e.getY()-downY))
      host.showHelpPage(Math.max(0,Math.min(4,scene+(dx<0?1:-1))));
     else v.performClick();
    }
    return true;
   }
  });
 }
 private void entry(String title,String subtitle){
  LinearLayout section=new LinearLayout(host);
  section.setOrientation(LinearLayout.VERTICAL);section.setPadding(0,ui.dp(12),0,ui.dp(6));
  section.addView(ui.text(title,16,true));section.addView(ui.caption(subtitle));page.addView(section);
 }
 private void button(String title,boolean primary,Runnable click){
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,ui.dp(48));lp.topMargin=ui.dp(8);
  page.addView(ui.button(title,primary,click),lp);
 }
 void guide(int step){
  String[] titles={"Uygulamayı aç","Düzenleyiciyi aç","Koldaki tuşu tanıt","Hedefi yerleştir","Basılı tut veya tekrarla"};
  String[] copy={"Kütüphanede uygulamanın yanındaki Aç’a dokun. İlk kez kullanacaksan Yeni uygulama ekle’yi seç.",
   "Uygulama açıkken yuvarlak kol simgesine, ardından Kontrol ekle’ye dokun.",
   "Eşlemek istediğin fiziksel tuşa bir kez bas. Örneğin A tuşu, sonraki adımda seçeceğin noktayı kontrol edecek.",
   "Uygulamadaki hedef düğmenin ortasına dokun. A etiketi burada görünür. Konumunu düzeltmek için etiketi sürükle.",
   "Etikete dokunarak davranışını seç. Basılı tut, tuşu bırakana kadar teması sürdürür. Tekrarla art arda dokunur. Oynamaya dönmek için yuvarlak simgeye dokun."};
  page.addView(ui.eyebrow("NASIL KULLANILIR   /   "+(step+1)+" · 5"));
  cover(titles[step],step,copy[step]);
  LinearLayout track=new LinearLayout(host);
  for(int i=0;i<5;i++){android.view.View segment=new android.view.View(host);segment.setBackground(ui.shape(i<=step?ui.accent:ui.border,2));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,ui.dp(3),1);lp.rightMargin=ui.dp(4);track.addView(segment,lp);
   if(i==step && Motion.enabled(host)){segment.setPivotX(0);segment.setScaleX(.25f);segment.animate().scaleX(1).setDuration(Motion.COMPONENT).start();}}
  track.setPadding(0,ui.dp(18),0,ui.dp(10));page.addView(track);
  LinearLayout nav=new LinearLayout(host);nav.setGravity(Gravity.END);
  if(step<4)nav.addView(host.footerLink("Atla",()->host.openSupport("")));
  if(step>0)nav.addView(host.footerLink("Önceki",()->host.showHelpPage(step-1)));
  if(step==4)button("Uygulamada dene",false,host::launchGuidedGame);
  nav.addView(ui.button(step<4?"İleri":"Bitir",true,()->{if(step<4)host.showHelpPage(step+1);else host.openSupport("");}));
  navigation=nav;
 }
 void settings(){
  page.addView(ui.text("Ayarlar",28,true));
  Switch motion=new Switch(host);motion.setText("Hareketi azalt");motion.setTextColor(ui.text);motion.setMinHeight(ui.dp(48));
  motion.setChecked(host.getSharedPreferences("appearance",0).getBoolean("reduced",false));
  motion.setOnCheckedChangeListener((v,on)->host.getSharedPreferences("appearance",0).edit().putBoolean("reduced",on).apply());
  page.addView(motion);
  for(String title:new String[]{"Nasıl kullanılır","Gizlilik","Yedekler"}){
   int kind=title.equals("Gizlilik")?ActionIcon.PRIVACY:title.equals("Yedekler")?ActionIcon.BACKUP:ActionIcon.HELP;
   LinearLayout tile=ui.actionTile(title,kind,()->{if(kind==ActionIcon.HELP)host.showHelp();else if(kind==ActionIcon.PRIVACY)host.showPrivacy();else host.showBackups();});
   tile.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);tile.setPadding(ui.dp(16),0,0,0);
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,ui.dp(52));lp.topMargin=ui.dp(8);page.addView(tile,lp);
  }
 }
 void disclosure(String title,String summary,String details){
  LinearLayout card=ui.card(page);
  LinearLayout toggle=ui.actionTile(title, title.equals("Dosya ve depolama")?ActionIcon.BACKUP:
   title.equals("Ağ erişimi")?ActionIcon.NETWORK:ActionIcon.PAD,()->{});
  toggle.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);toggle.setMinimumHeight(ui.dp(48));toggle.setBackground(ui.ripple(ui.shape(ui.surface,10)));
  TextView state=ui.text("＋",18,false);state.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);toggle.addView(state,new LinearLayout.LayoutParams(0,-2,1));
  card.addView(toggle);card.addView(ui.caption(summary));TextView more=ui.caption(details);
  more.setVisibility(android.view.View.GONE);card.addView(more);
  toggle.setContentDescription(title+", ayrıntıları göster");
  toggle.setOnClickListener(v->{boolean open=more.getVisibility()!=android.view.View.VISIBLE;
   if(Motion.enabled(host))android.transition.TransitionManager.beginDelayedTransition(card,new android.transition.AutoTransition().setDuration(Motion.COMPONENT));
   more.setVisibility(open?android.view.View.VISIBLE:android.view.View.GONE);state.setText(open?"−":"＋");toggle.setContentDescription(title+(open?", ayrıntıları gizle":", ayrıntıları göster"));});
 }
 void privacy(){
  cover("Gizlilik",StoryArt.PRIVACY,"Atamalar, makrolar ve tercihler bu cihazda saklanır.");
  disclosure("Kol ve ekran","Seçtiğin uygulamaya dokunma gönderir.","Erişilebilirlik izni, seçili uygulamada kol girişini dokunuşa çevirir. Vektör pencerenin kimliğini, türünü ve odağını kullanır. Ekran görüntüsü almaz; mesaj, şifre veya ekran metni okumaz.");
  disclosure("Ağ erişimi","Ağ izni istenmez.","Vektör’ün internet izni yoktur. Hesap, reklam veya analiz servisi kullanmaz.");
  disclosure("Dosya ve depolama","Dışa aktarılan dosyanın konumunu sen seçersin.",".rota yedeği şifreli değildir. Bulut konumu seçersen dosyayı oraya seçtiğin sağlayıcı aktarır.");
  LinearLayout permissions=ui.actionTile("Android izin ayarları",ActionIcon.SETTINGS,()->host.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,ui.dp(48));lp.topMargin=ui.dp(12);page.addView(permissions,lp);
 }
 void backups(){
  int count=Profiles.known(host).size(),controls=0;
  for(String pkg:Profiles.known(host))controls+=Binding.load(host,pkg).size();
  long stamp=host.getSharedPreferences("appearance",0).getLong("last_export",0);
  cover("Yedekler",StoryArt.ARCHIVE,count+" uygulama   ·   "+controls+" kontrol");
  entry("Son başarılı yedek",stamp==0?"Bu sürümde henüz yedek kaydedilmedi":java.text.DateFormat.getDateTimeInstance().format(new java.util.Date(stamp)));
  host.backupSpinner=new ProgressBar(host);page.addView(host.backupSpinner,new LinearLayout.LayoutParams(ui.dp(24),ui.dp(24)));
  host.backupProgress=ui.caption(host.backupStage);host.backupProgress.setAccessibilityLiveRegion(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE);
  page.addView(host.backupProgress);host.updateBackupProgress();
  entry("Dışa aktar","Bir .rota dosyası oluştur ve saklayacağın konumu seç. Dosya şifreli değildir.");
  button("Dosyayı kaydet",true,host::exportBackup);
  entry("İçe aktar","Bir .rota dosyası seç. Yüklenecek düzenleri onaylamadan önce Vektör dosyanın bütünlüğünü denetler.");
  button("Yedek seç",false,host::importBackup);
  if(new File(host.getFilesDir(),"before-restore.rota").exists())button("Önceki düzeni kurtar",false,()->host.readBackup(null));
 }
}
