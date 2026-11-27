package dev.kolmapper;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.view.InputDevice;
import java.util.Locale;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

final class ControllerIdentity {
 static boolean isPad(InputDevice device) {
  return device != null && !device.isVirtual() && device.isEnabled()
    && (device.supportsSource(InputDevice.SOURCE_GAMEPAD) || device.supportsSource(InputDevice.SOURCE_JOYSTICK));
 }

 // The same Ultimate 2C VID/PID can be exposed by more than one transport.
 // Model IDs cross-checked against mdqinc/SDL_GameControllerDB (Ultimate 2C).
 static boolean ultimate2c(int vendor, int product) {
  return vendor == 0x2dc8 && (product == 0x301b || product == 0x301c
    || product == 0x301d || product == 0x310a);
 }

 static String name(InputDevice device) {
  String name = device.getName();
  if (ultimate2c(device.getVendorId(), device.getProductId())) return "8BitDo Ultimate 2C";
  if (device.getVendorId() == 0x2dc8 && !name.toLowerCase(Locale.ROOT).contains("8bitdo"))
   return "8BitDo · " + name;
  return name;
 }

 private static String attribute(File directory,String relative) throws IOException {
  try(BufferedReader reader=new BufferedReader(new FileReader(new File(directory,relative)))){
   String line=reader.readLine();
   return line==null?"":line.trim();
  }
 }

 // Some OEMs expose input metadata to apps. Never open the raw input event devices.
 static String bus(File root,String name,int vendor,int product) {
  File[] devices=root.listFiles(f->f.getName().matches("input[0-9]+"));
  if(devices==null)return "";
  String result="";
  for(File node:devices){
   try{
    if(!name.equalsIgnoreCase(attribute(node,"name"))
       || vendor!=Integer.parseInt(attribute(node,"id/vendor"),16)
       || product!=Integer.parseInt(attribute(node,"id/product"),16))continue;
    String bus=attribute(node,"id/bustype");
    if(!result.isEmpty()&&!result.equals(bus))return "ambiguous";
    result=bus;
   }catch(IOException|SecurityException|NumberFormatException ignored){ }
  }
  return result;
 }

 static String connection(Context context, InputDevice device) {
  String name = device.getName().toLowerCase(Locale.ROOT);
  try{
   String bus=bus(new File("/sys/class/input"),device.getName(),device.getVendorId(),device.getProductId());
   if(bus.equals("0005"))return "Bluetooth";
   if(bus.equals("0003"))return "USB · kablo / alıcı";
   if(bus.equals("ambiguous"))return "Bağlantı türü ayırt edilemedi";
  }catch(SecurityException ignored){ }

  UsbManager manager = context.getSystemService(UsbManager.class);
  if (manager != null && device.getVendorId() != 0) {
   try {
    for (UsbDevice usb : manager.getDeviceList().values()) {
     if (usb.getVendorId() == device.getVendorId() && usb.getProductId() == device.getProductId())
      return "USB · kablo / alıcı";
    }
    if(name.contains("bluetooth"))return "Bluetooth (cihaz adı)";
   } catch (SecurityException ignored) { /* USB enumeration is restricted on some devices. */ }
  }
  return "Bağlantı türü bildirilmedi";
 }
}
