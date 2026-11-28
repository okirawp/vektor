package dev.kolmapper;

import android.graphics.Color;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import com.google.android.filament.Engine;
import com.google.android.filament.Renderer;
import com.google.android.filament.android.UiHelper;
import com.google.android.filament.gltfio.FilamentAsset;
import com.google.android.filament.utils.ModelViewer;
import com.google.android.filament.utils.Utils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/** The optional GLB surface. PadView owns gestures and live input independently. */
final class FilamentPadPreview implements PadView.ModelView {
 private final FrameLayout parent;
 private final PadView pad;
 private final String asset;
 private final Choreographer scheduler=Choreographer.getInstance();
 private final Choreographer.FrameCallback frame=this::render;
 private final float[] transform=new float[16];
 private TextureView surface;
 private ModelViewer viewer;
 private byte[] data;
 private float[] center;
 private float scale,basePitch;
 private boolean loading,queued,presented,failed;
 private long loadDeadline;

 FilamentPadPreview(FrameLayout parent,PadView pad,String asset){
  this.parent=parent;this.pad=pad;this.asset=asset;
  parent.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
   public void onViewAttachedToWindow(View view){
    if(viewer==null&&surface!=null){parent.removeView(surface);surface=null;}
    requestFrame();
   }
   public void onViewDetachedFromWindow(View view){setVisible(false);data=null;}
  });
 }
 @Override public void setVisible(boolean visible){
  if(visible)requestFrame();
  else{scheduler.removeFrameCallback(frame);queued=false;}
 }
 @Override public void requestFrame(){
  if(failed||queued||!parent.isAttachedToWindow()||!pad.isShown()||pad.getWindowVisibility()!=View.VISIBLE)return;
  if(viewer==null&&data==null){
   if(loading)return;
   loading=true;
   android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());
   new Thread(()->{
    byte[] bytes;
    try(InputStream stream=parent.getContext().getAssets().open(asset);
        ByteArrayOutputStream buffer=new ByteArrayOutputStream()){
     byte[] block=new byte[8192];
     int count;
     while((count=stream.read(block))!=-1)buffer.write(block,0,count);
     bytes=buffer.toByteArray();
    }
    catch(IOException e){bytes=null;}
    byte[] loaded=bytes;
    main.post(()->{
     loading=false;if(!parent.isAttachedToWindow())return;
     if(loaded==null){failed=true;return;}
     data=loaded;requestFrame();
    });
   },"Vektor-model").start();
   return;
  }
  queued=true;scheduler.postFrameCallback(frame);
 }
 private void render(long nanos){
  queued=false;
  if(!parent.isAttachedToWindow()||!pad.isShown()||pad.getWindowVisibility()!=View.VISIBLE)return;
  try{
   if(viewer==null){
    if(!validGlb(data))throw new IllegalArgumentException("Geçersiz GLB başlığı");
    initialize();
   }
   if(SystemClock.uptimeMillis()>loadDeadline&&!presented)throw new IllegalStateException("Model çizim süresi doldu");
   int root=viewer.getEngine().getTransformManager().getInstance(viewer.getAsset().getRoot());
   Matrix.setIdentityM(transform,0);
   Matrix.rotateM(transform,0,pad.yaw(),0,1,0);
   Matrix.rotateM(transform,0,basePitch+pad.pitch(),1,0,0);
   float zoom=scale*pad.zoom();
   Matrix.scaleM(transform,0,zoom,zoom,zoom);
   Matrix.translateM(transform,0,-center[0],-center[1],-center[2]);
   viewer.getEngine().getTransformManager().setTransform(root,transform);
   boolean drawn=viewer.render(nanos);
   if(drawn&&viewer.getProgress()>=1f&&!presented){presented=true;surface.setAlpha(1f);}
   if(!presented)requestFrame();
  }catch(RuntimeException|LinkageError error){
   android.util.Log.w("VektorPreview","Katmanlı önizleme kullanılacak",error);
   failed=true;
   if(surface!=null&&surface.getParent()==parent)parent.removeView(surface);
  }
 }
 private void initialize(){
  Utils.init();
  surface=new TextureView(parent.getContext());
  surface.setAlpha(0f);surface.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  surface.setClickable(false);surface.setFocusable(false);
  parent.addView(surface,new FrameLayout.LayoutParams(-1,-1));
  Engine engine=Engine.create();
  try{viewer=new ModelViewer(surface,engine,new UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK),null);}
  catch(RuntimeException|LinkageError error){engine.destroy();throw error;}
  surface.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
   public void onViewAttachedToWindow(View view){}
   public void onViewDetachedFromWindow(View view){viewer=null;presented=false;}
  });
  int background=pad.ui.surface;
  Renderer.ClearOptions clear=new Renderer.ClearOptions();
  clear.clear=true;
  clear.clearColor=new double[]{linear(Color.red(background)),linear(Color.green(background)),linear(Color.blue(background)),1};
  viewer.getRenderer().setClearOptions(clear);
  viewer.getCamera().lookAt(0,0,4,0,0,0,0,1,0);
  engine.getLightManager().setDirection(engine.getLightManager().getInstance(viewer.getLight()),-.35f,-.7f,-1f);
  ByteBuffer glb=ByteBuffer.allocateDirect(data.length);glb.put(data).flip();data=null;
  viewer.loadModelGlb(glb);
  FilamentAsset model=viewer.getAsset();
  if(model==null)throw new IllegalArgumentException("GLB okunamadı: "+asset);
  center=model.getBoundingBox().getCenter();
  float[] extent=model.getBoundingBox().getHalfExtent();
  float max=Math.max(extent[0],Math.max(extent[1],extent[2]));
  if(!(max>0))throw new IllegalArgumentException("Model sınırları geçersiz");
  scale=1.15f/max;
  basePitch=extent[1]<extent[2]?78f:-10f;
  loadDeadline=SystemClock.uptimeMillis()+Motion.MODEL_FRAME_TIMEOUT;
  surface.addOnLayoutChangeListener((view,l,t,r,b,ol,ot,or,ob)->requestFrame());
 }
 private static boolean validGlb(byte[] bytes){
  if(bytes==null||bytes.length<20)return false;
  ByteBuffer header=ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);
  return header.getInt()==0x46546c67&&header.getInt()==2
   &&header.getInt()==bytes.length&&header.getInt()>=4&&header.getInt()==0x4e4f534a;
 }
 private static double linear(int component){
  double srgb=component/255.0;
  return srgb<=.04045?srgb/12.92:Math.pow((srgb+.055)/1.055,2.4);
 }
}
