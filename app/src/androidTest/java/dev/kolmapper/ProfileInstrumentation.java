package dev.kolmapper;
import android.app.*;import android.os.*;import android.content.*;import java.util.*;
/** Storage migration and per-game persistence. No UiAutomation or production control hooks. */
public class ProfileInstrumentation extends LayoutInstrumentation {
 boolean seed,layoutOnly;
 int checks;void check(boolean b,String text){if(!b)throw new AssertionError(text);checks++;}
 @Override public void onCreate(Bundle b){seed="true".equals(b.getString("seed_profiles"));layoutOnly="true".equals(b.getString("layout_only"));super.onCreate(b);}
 @Override public void onStart(){if(layoutOnly){super.onStart();return;}Context c=getTargetContext();SharedPreferences p=Profiles.prefs(c);Map<String,?> original=new HashMap<>(p.getAll());Bundle out=new Bundle();try{
  if(seed){
   String a="dev.kolmapper.probe",b="dev.kolmapper.probe2";Profiles.select(c,a);Profiles.select(c,b);List<Binding> list=new ArrayList<>();int[] keys={21,22,96,99,105,-100},xs={200,420,650,850,1080,200};
   for(int i=0;i<keys.length;i++){Binding binding=new Binding(keys[i],xs[i],600,1280,800,80,i<2?1:0);binding.hold=i<2||i==5;binding.duration=25;binding.radius=40;list.add(binding);}Binding.save(c,a,list);
   Binding other=new Binding(96,850,600,1280,800,200,1);other.duration=25;Binding.save(c,b,Collections.singletonList(other));Profiles.orb(c,a,16,80);Profiles.orb(c,b,96,160);Profiles.ready(c,a,true);Profiles.ready(c,b,true);Profiles.select(c,a);p.edit().putBoolean("dark",true).putString("legacy_imported_to",a).commit();
   out.putString("stream","PROFILE FIXTURES SEEDED A=6 B=1\n");finish(Activity.RESULT_OK,out);return;
  }

  p.edit().clear().commit();Backups.validate(BackupCodec.decode(BackupCodec.encode(Backups.snapshot(c))));check(true,"empty installation produces a valid archive");String a="test.rota.gameA",b="test.rota.gameB";
  p.edit().putString("bindings","[{\"key\":96,\"x\":120,\"y\":200,\"w\":1280,\"h\":800,\"interval\":50,\"count\":0,\"duration\":25,\"hold\":false,\"radius\":40}]").putInt("orbX",321).putInt("orbY",123).commit();
  Profiles.select(c,a);check(Binding.load(c).isEmpty(),"old global mappings never auto-apply to A");Profiles.select(c,b);check(Binding.load(c).isEmpty(),"old global mappings never auto-apply to B");
  check(Profiles.importLegacy(c,a),"explicit legacy import succeeds");check(Binding.load(c,a).size()==1&&Binding.load(c,a).get(0).duration==25,"full old macro survives");check(Profiles.orb(c,a,"orbX",0)==321,"old orb position preserved only in A");check(Binding.load(c,b).isEmpty(),"import never fills B");check(!Profiles.importLegacy(c,b),"legacy import is one-time");check(p.getString("bindings","").contains("120"),"old backup not deleted");
  Binding ba=new Binding(96,650,600,1280,800,80,0);ba.duration=25;Binding bb=new Binding(96,850,600,1280,800,200,1);bb.duration=8;
  Binding.save(c,a,Collections.singletonList(ba));Binding.save(c,b,Collections.singletonList(bb));Profiles.orb(c,b,16,80);Profiles.ready(c,b,true);
  Profiles.select(c,a);check(Binding.load(c).get(0).x==650&&Binding.load(c).get(0).count==0,"A keeps own coordinate and macro");Profiles.select(c,b);check(Binding.load(c).get(0).x==850&&Binding.load(c).get(0).count==1,"B keeps different coordinate and macro");
  check(Profiles.orb(c,a,"orbX",0)==321&&Profiles.orb(c,b,"orbX",0)==16,"orb positions independent");check(Profiles.known(c).containsAll(Arrays.asList(a,b)),"game registry retained");
  Map<String,Object> snapshot=Backups.snapshot(c);byte[] binary=BackupCodec.encode(snapshot);Map<String,Object> restored=BackupCodec.decode(binary);Backups.validate(restored);check(snapshot.equals(restored),"all profile values survive binary archive");
  String child=a+".child",other="test.rota.other";Profiles.select(c,child);Binding.save(c,child,Collections.singletonList(ba));Profiles.orb(c,child,77,88);Profiles.select(c,other);Binding.save(c,other,Collections.singletonList(bb));Profiles.select(c,b);
  Binding.save(c,a,Collections.singletonList(bb));check(Binding.load(c,a).get(0).x==850,"profile changed before restore");Backups.restore(c,restored);check(Binding.load(c,a).get(0).x==650&&Binding.load(c,b).get(0).x==850,"restore recovers A and B separately");
  check(Binding.load(c,child).size()==1&&Binding.load(c,child).get(0).x==650&&Profiles.orb(c,child,"orbX",0)==77,"import preserves a different package sharing the imported package prefix");
  check(Binding.load(c,other).size()==1&&Binding.load(c,other).get(0).x==850,"import preserves unrelated existing profiles");
  Map<String,Object> rollback=BackupCodec.decode(Backups.read(new java.io.FileInputStream(new java.io.File(c.getFilesDir(),"before-restore.rota"))));check(((String)rollback.get(ProfileRules.key(a,"bindings"))).contains("850"),"pre-import rollback retains overwritten arrangement");
  p.edit().putString(ProfileRules.key(a,"bindings"),"broken").commit();check(!Binding.load(c,a).isEmpty(),"corrupt current profile recovers previous generation");check(Backups.snapshot(c).get(ProfileRules.key(a,"bindings")).equals(p.getString(ProfileRules.key(a,"previous"),"")),"export recovers a corrupt current profile from previous generation");
  Map<String,Object> bad=new HashMap<>(restored);bad.put(ProfileRules.key(a,"bindings"),"broken");Map<String,?> before=new HashMap<>(p.getAll());boolean rejected=false;try{Backups.restore(c,bad);}catch(java.io.IOException expected){rejected=true;}check(rejected&&p.getAll().equals(before),"malformed import does not modify current preferences");
  out.putString("stream","ALL PROFILE STORAGE CHECKS PASSED: "+checks+"\n");
 }catch(Throwable e){out.putString("stream","FAIL "+android.util.Log.getStackTraceString(e));}finally{
  if(!seed){SharedPreferences.Editor e=p.edit().clear();for(Map.Entry<String,?> item:original.entrySet()){String k=item.getKey();Object v=item.getValue();if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof Long)e.putLong(k,(Long)v);else if(v instanceof Float)e.putFloat(k,(Float)v);else if(v instanceof Set)e.putStringSet(k,new HashSet<>((Set<String>)v));}e.commit();}
 }finish(Activity.RESULT_OK,out);}
}
