package dev.kolmapper;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.text.Collator;
import java.util.*;

/** The application library picker. Selection is staged until the user confirms. */
final class AppPicker {
 private static final int RECOMMENDED=0, GAMES=1, ALL=2;
 private final MainActivity host;
 private final Ui ui;
 private final LinearLayout root;
 private final boolean single,migration;
 private final Set<String> added;
 private final Set<String> selected=new LinkedHashSet<>();
 private final List<Entry> entries=new ArrayList<>(),results=new ArrayList<>();
 private final RadioGroup tabs;
 private final ListView list;
 private final EditText search;
 private final TextView empty;
 private final LinearLayout skeleton,rail;
 private final Button confirm;
 private final Switch system;
 private final Rows adapter=new Rows();
 private final Alphabet sections=new Alphabet();
 private boolean loaded;
 private int filter;

 AppPicker(MainActivity host,boolean migration,boolean guided,Bundle state){
  this.host=host;ui=host.ui;root=host.body;this.migration=migration;single=migration||guided;
  added=Profiles.known(host);
  LinearLayout header=new LinearLayout(host);header.setGravity(Gravity.CENTER_VERTICAL);
  header.addView(host.footerLink("‹  Geri",host::closePicker));
  header.addView(ui.text(migration?"Önceki düzenin uygulamasını seç":"Uygulama seç",Ui.HELP_TITLE,true),
   new LinearLayout.LayoutParams(0,-2,1));
  root.addView(header);
  search=new EditText(host);
  search.setSingleLine();search.setHint("Uygulama veya paket ara");search.setTextSize(Ui.PICKER_LABEL);
  search.setTextColor(ui.text);search.setHintTextColor(ui.muted);
  search.setBackground(ui.outline(ui.surface,Ui.CARD));
  search.setPadding(ui.dp(Ui.PICKER_GAP),0,ui.dp(Ui.PICKER_GAP),0);
  root.addView(search,new LinearLayout.LayoutParams(-1,ui.dp(Ui.TOUCH)));
  tabs=new RadioGroup(host);tabs.setOrientation(LinearLayout.HORIZONTAL);
  String[] labels={"Önerilen","Oyunlar","Tümü"};
  for(int i=0;i<labels.length;i++){
   RadioButton tab=new RadioButton(host);tab.setId(View.generateViewId());tab.setText(labels[i]);
   ui.styleMode(tab);tabs.addView(tab,new RadioGroup.LayoutParams(0,-2,1));
  }
  root.addView(tabs);tabs.check(tabs.getChildAt(RECOMMENDED).getId());
  system=new Switch(host);system.setText("Sistem uygulamalarını göster");system.setTextColor(ui.muted);
  system.setMinHeight(ui.dp(Ui.TOUCH));root.addView(system);

  FrameLayout resultsArea=new FrameLayout(host);root.addView(resultsArea,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout resultRow=new LinearLayout(host);
  resultsArea.addView(resultRow,new FrameLayout.LayoutParams(-1,-1));
  list=new ListView(host);list.setAdapter(adapter);
  list.setFastScrollEnabled(true);list.setFastScrollAlwaysVisible(false);
  list.setOnItemClickListener((parent,view,index,id)->toggle(results.get(index)));
  list.setOnItemLongClickListener((parent,view,index,id)->{toggle(results.get(index));return true;});
  resultRow.addView(list,new LinearLayout.LayoutParams(0,-1,1));
  ScrollView railScroll=new ScrollView(host);
  railScroll.setVerticalScrollBarEnabled(false);
  rail=new LinearLayout(host);rail.setOrientation(LinearLayout.VERTICAL);
  railScroll.addView(rail);
  resultRow.addView(railScroll,new LinearLayout.LayoutParams(ui.dp(Ui.TOUCH),-1));
  empty=ui.caption("Eşleşen uygulama yok.");empty.setGravity(Gravity.CENTER);
  resultsArea.addView(empty,new FrameLayout.LayoutParams(-1,-1));empty.setVisibility(View.GONE);
  skeleton=new LinearLayout(host);skeleton.setOrientation(LinearLayout.VERTICAL);
  skeleton.setContentDescription("Uygulamalar yükleniyor");
  for(int i=0;i<3;i++){
   View placeholder=new View(host);placeholder.setBackground(ui.shape(ui.raised,Ui.CARD));
   LinearLayout.LayoutParams item=new LinearLayout.LayoutParams(-1,ui.dp(Ui.TOUCH));
   item.bottomMargin=ui.dp(Ui.PICKER_GAP);skeleton.addView(placeholder,item);
  }
  resultsArea.addView(skeleton,new FrameLayout.LayoutParams(-1,-2));
  confirm=ui.button("",true,()->host.confirmPicker(new ArrayList<>(selected)));
  root.addView(confirm);confirm.setVisibility(View.GONE);
  search.addTextChangedListener(new TextWatcher(){
   public void beforeTextChanged(CharSequence value,int start,int count,int after){}
   public void onTextChanged(CharSequence value,int start,int before,int count){refresh();}
   public void afterTextChanged(Editable value){}
  });
  system.setOnCheckedChangeListener((button,on)->refresh());
  tabs.setOnCheckedChangeListener((group,id)->{
   filter=group.indexOfChild(group.findViewById(id));refresh();
  });
  if(state!=null){
   ArrayList<String> saved=state.getStringArrayList("selected");
   if(saved!=null)selected.addAll(saved);
   search.setText(state.getString("query",""));
   system.setChecked(state.getBoolean("system"));
   tabs.check(tabs.getChildAt(state.getInt("filter",RECOMMENDED)).getId());
   updateConfirm();
  }
  root.setFocusableInTouchMode(true);root.requestFocus();
 }

 Bundle save(){
  Bundle state=new Bundle();
  state.putStringArrayList("selected",new ArrayList<>(selected));
  state.putString("query",search.getText().toString());
  state.putBoolean("system",system.isChecked());
  state.putInt("filter",filter);
  return state;
 }

 void load(){
  new Thread(()->{
   PackageManager pm=host.getPackageManager();
   Map<String,Entry> unique=new HashMap<>();
   Intent intent=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
   for(ResolveInfo candidate:pm.queryIntentActivities(intent,0)){
    ApplicationInfo app=candidate.activityInfo.applicationInfo;
    String pkg=app.packageName;
    if(pkg.equals(host.getPackageName())||!candidate.activityInfo.enabled||!app.enabled||unique.containsKey(pkg))continue;
    boolean game=app.category==ApplicationInfo.CATEGORY_GAME||(app.flags&ApplicationInfo.FLAG_IS_GAME)!=0;
    boolean systemApp=(app.flags&ApplicationInfo.FLAG_SYSTEM)!=0;
    unique.put(pkg,new Entry(pkg,app.loadLabel(pm).toString(),app.loadIcon(pm),game,systemApp));
   }
   List<Entry> discovered=new ArrayList<>(unique.values());
   Collator collator=Collator.getInstance();
   discovered.sort((a,b)->{int order=collator.compare(a.name,b.name);return order==0?a.pkg.compareTo(b.pkg):order;});
   host.runOnUiThread(()->{
    if(host.picker!=this||host.isDestroyed())return;
    entries.addAll(discovered);loaded=true;refresh();
   });
  },"Vektor-app-picker").start();
 }

 private void refresh(){
  if(!loaded)return;
  String query=search.getText().toString().trim().toLowerCase(Locale.getDefault());
  boolean showSystem=system.isChecked();
  results.clear();
  for(Entry entry:entries){
   if(!showSystem&&entry.system||filter==GAMES&&!entry.game)continue;
   if(query.isEmpty()||entry.searchKey.contains(query))results.add(entry);
  }
  if(filter==RECOMMENDED)results.sort(Comparator.comparing(entry->!entry.game));
  sections.rebuild();
  rail.removeAllViews();
  Map<String,Integer> firstPositions=new TreeMap<>(Collator.getInstance());
  for(int i=0;i<sections.labels.size();i++)firstPositions.putIfAbsent(sections.labels.get(i),sections.starts.get(i));
  ((View)rail.getParent()).setVisibility(firstPositions.size()>1?View.VISIBLE:View.GONE);
  if(firstPositions.size()>1)for(Map.Entry<String,Integer> section:firstPositions.entrySet()){
   TextView letter=host.footerLink(section.getKey(),()->list.setSelection(section.getValue()));
   letter.setPadding(0,0,0,0);letter.setContentDescription(section.getKey()+" harfine git");
   rail.addView(letter,new LinearLayout.LayoutParams(-1,ui.dp(Ui.TOUCH)));
  }
  adapter.notifyDataSetChanged();
  skeleton.setVisibility(View.GONE);
  empty.setVisibility(results.isEmpty()?View.VISIBLE:View.GONE);
 }

 private void toggle(Entry entry){
  if(!migration&&added.contains(entry.pkg))return;
  boolean remove=selected.contains(entry.pkg);
  if(single)selected.clear();
  if(remove)selected.remove(entry.pkg);else selected.add(entry.pkg);
  adapter.notifyDataSetChanged();
  updateConfirm();
 }

 private void updateConfirm(){
  confirm.setText(migration?"Düzeni taşı":selected.size()+" uygulama ekle");
  confirm.setVisibility(selected.isEmpty()?View.GONE:View.VISIBLE);
 }

 private static final class Entry {
  final String pkg,name,searchKey;
  final Drawable icon;
  final boolean game,system;
  Entry(String pkg,String name,Drawable icon,boolean game,boolean system){
   this.pkg=pkg;this.name=name;this.icon=icon;this.game=game;this.system=system;
   searchKey=(name+" "+pkg).toLowerCase(Locale.getDefault());
  }
 }
 private final class Row extends LinearLayout {
  final ImageView icon=new ImageView(host);
  final TextView name=ui.text("",Ui.PICKER_LABEL,true),pkg=ui.caption(""),badge=ui.chip("",false);
  final CheckBox check=new CheckBox(host);
  Row(){
   super(host);setGravity(Gravity.CENTER_VERTICAL);setMinimumHeight(ui.dp(Ui.PICKER_ROW));
   setPaddingRelative(ui.dp(Ui.PICKER_GAP),0,ui.dp(Ui.PICKER_GAP),0);
   addView(icon,new LinearLayout.LayoutParams(ui.dp(Ui.PICKER_ICON),ui.dp(Ui.PICKER_ICON)));
   LinearLayout labels=new LinearLayout(host);labels.setOrientation(VERTICAL);
   labels.setPaddingRelative(ui.dp(Ui.PICKER_GAP),0,0,0);
   name.setSingleLine();name.setEllipsize(TextUtils.TruncateAt.END);
   pkg.setTextSize(Ui.PICKER_SUBTITLE);pkg.setSingleLine();pkg.setEllipsize(TextUtils.TruncateAt.END);
   labels.addView(name);labels.addView(pkg);addView(labels,new LinearLayout.LayoutParams(0,-2,1));
   addView(badge);
   check.setButtonTintList(new ColorStateList(new int[][]{{android.R.attr.state_checked},{}},new int[]{ui.accent,ui.muted}));
   check.setClickable(false);check.setFocusable(false);
   check.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
   addView(check);
  }
  void bind(Entry entry){
   boolean exists=added.contains(entry.pkg),checked=selected.contains(entry.pkg);
   name.setText(entry.name);pkg.setText(entry.pkg);icon.setImageDrawable(entry.icon);
   badge.setText("Eklendi");badge.setVisibility(exists?VISIBLE:GONE);
   check.setVisibility(!exists||migration?VISIBLE:GONE);
   check.setChecked(checked);setBackgroundColor(checked?ui.accentSoft:ui.background);
   setContentDescription(entry.name+", "+entry.pkg+(exists?", Eklendi":"")+(checked?", seçili":""));
  }
 }
 private final class Rows extends BaseAdapter implements SectionIndexer {
  public int getCount(){return results.size();}
  public Object getItem(int position){return results.get(position);}
  public long getItemId(int position){return position;}
  public boolean areAllItemsEnabled(){return false;}
  public boolean isEnabled(int position){return migration||!added.contains(results.get(position).pkg);}
  public View getView(int position,View recycled,ViewGroup parent){
   Row row=recycled instanceof Row?(Row)recycled:new Row();row.bind(results.get(position));return row;
  }
  public Object[] getSections(){return sections.labels.toArray();}
  public int getPositionForSection(int section){return sections.starts.isEmpty()?0:sections.starts.get(Math.max(0,Math.min(section,sections.starts.size()-1)));}
  public int getSectionForPosition(int position){
   int index=Collections.binarySearch(sections.starts,position);return index>=0?index:Math.max(0,-index-2);
  }
 }
 private final class Alphabet {
  final List<String> labels=new ArrayList<>();
  final List<Integer> starts=new ArrayList<>();
  void rebuild(){
   labels.clear();starts.clear();String previous="";
   for(int i=0;i<results.size();i++){
    String name=results.get(i).name;
    String label=name.isEmpty()?"#":new String(Character.toChars(name.codePointAt(0))).toUpperCase(Locale.getDefault());
    if(!label.equals(previous)){labels.add(label);starts.add(i);previous=label;}
   }
  }
 }
}
