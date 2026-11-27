package dev.kolmapper;
import java.io.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;
/** Versioned Rota archive, typed binary payload with SHA-256 integrity. Not encryption. */
final class BackupCodec {
 static final int LIMIT=4*1024*1024,MAGIC=0x524F5441,VERSION=1;
 static byte[] encode(Map<String,Object> values)throws IOException{
  ByteArrayOutputStream raw=new ByteArrayOutputStream();
  try(DataOutputStream d=new DataOutputStream(raw)){
   if(values.size()>4096)throw new IOException("Too many settings");d.writeInt(values.size());
   for(String k:new TreeSet<>(values.keySet())){d.writeUTF(k);Object v=values.get(k);
    if(v instanceof String){d.writeByte(1);d.writeUTF((String)v);}
    else if(v instanceof Integer){d.writeByte(2);d.writeInt((Integer)v);}
    else if(v instanceof Boolean){d.writeByte(3);d.writeBoolean((Boolean)v);}
    else if(v instanceof Set){d.writeByte(4);Set<?> set=(Set<?>)v;if(set.size()>1024)throw new IOException("Too many profiles");d.writeInt(set.size());for(Object item:new TreeSet<Object>((Set<?>)v))d.writeUTF((String)item);}
    else throw new IOException("Unsupported setting");
   }
  }
  byte[] payload=raw.toByteArray();if(payload.length>LIMIT)throw new IOException("Archive too large");
  ByteArrayOutputStream out=new ByteArrayOutputStream();DataOutputStream header=new DataOutputStream(out);header.writeInt(MAGIC);header.writeInt(VERSION);header.writeInt(payload.length);header.write(digest(payload));
  try(DeflaterOutputStream zip=new DeflaterOutputStream(out)){zip.write(payload);}return out.toByteArray();
 }
 static Map<String,Object> decode(byte[] file)throws IOException{
  if(file.length>LIMIT)throw new IOException("Archive too large");DataInputStream h=new DataInputStream(new ByteArrayInputStream(file));
  if(h.readInt()!=MAGIC||h.readInt()!=VERSION)throw new IOException("Not a supported Rota archive");int length=h.readInt();if(length<4||length>LIMIT)throw new IOException("Invalid archive size");byte[] hash=new byte[32];h.readFully(hash);
  ByteArrayOutputStream raw=new ByteArrayOutputStream();try(InflaterInputStream z=new InflaterInputStream(h)){byte[] buf=new byte[8192];int n;while((n=z.read(buf))!=-1){if(raw.size()+n>length)throw new IOException("Invalid expanded size");raw.write(buf,0,n);}}
  byte[] data=raw.toByteArray();if(data.length!=length||!MessageDigest.isEqual(hash,digest(data)))throw new IOException("Damaged archive");
  Map<String,Object> values=new LinkedHashMap<>();DataInputStream d=new DataInputStream(new ByteArrayInputStream(data));int count=d.readInt();if(count<0||count>4096)throw new IOException("Invalid settings count");
  for(int i=0;i<count;i++){String key=d.readUTF();if(values.containsKey(key))throw new IOException("Duplicate setting");Object v;switch(d.readUnsignedByte()){
   case 1:v=d.readUTF();break;case 2:v=d.readInt();break;case 3:v=d.readBoolean();break;
   case 4:int n=d.readInt();if(n<0||n>1024)throw new IOException("Invalid profiles count");Set<String> set=new HashSet<>();for(int j=0;j<n;j++)if(!set.add(d.readUTF()))throw new IOException("Duplicate profile");v=set;break;
   default:throw new IOException("Invalid setting type");}values.put(key,v);
  }if(d.available()!=0)throw new IOException("Trailing data");return values;
 }
 static byte[] digest(byte[] bytes)throws IOException{try{return MessageDigest.getInstance("SHA-256").digest(bytes);}catch(NoSuchAlgorithmException e){throw new IOException(e);}}
}
