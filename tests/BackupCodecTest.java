package dev.kolmapper;
import java.util.*;import java.io.*;
public class BackupCodecTest {
 static int checks;
 static void check(boolean ok){if(!ok)throw new AssertionError();checks++;}
 static void reject(byte[] bytes)throws Exception{try{BackupCodec.decode(bytes);throw new AssertionError("accepted damaged backup");}catch(IOException expected){checks++;}}
 public static void main(String[] args)throws Exception{
  Map<String,Object> data=new HashMap<>();data.put("games",new HashSet<>(Arrays.asList("game.one","game.two")));data.put("selected_game","game.one");data.put("game.one.bindings","Türkçe → A/X/R2");data.put("game.two.orbX",120);data.put("game.two.ready",true);
  byte[] bytes=BackupCodec.encode(data);check(BackupCodec.decode(bytes).equals(data));check(Arrays.equals(bytes,BackupCodec.encode(data)));
  for(int i=0;i<bytes.length;i++)reject(Arrays.copyOf(bytes,i));
  for(int i=0;i<44;i++){byte[] broken=bytes.clone();broken[i]^=1;reject(broken);}
  byte[] broken=bytes.clone();broken[bytes.length-2]^=16;reject(broken);
  reject(new byte[BackupCodec.LIMIT+1]);Map<String,Object> invalid=new HashMap<>();invalid.put("bad",new Object());try{BackupCodec.encode(invalid);throw new AssertionError();}catch(IOException expected){checks++;}
  Map<String,Object> empty=Collections.emptyMap();check(BackupCodec.decode(BackupCodec.encode(empty)).equals(empty));
  System.out.println("PASS "+checks+" archive checks: typed round trip, deterministic binary format, truncation, header/version/hash corruption, compressed corruption, size/type bounds");
 }
}
