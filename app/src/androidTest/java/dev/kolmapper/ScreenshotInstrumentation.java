package dev.kolmapper;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/** Runs only in the screenshot test APK, against an unlocked CI emulator. */
public final class ScreenshotInstrumentation extends Instrumentation {
    private MainActivity activity;
    private UiAutomation automation;
    private File output;
    private boolean large;

    @Override public void onCreate(Bundle args) {
        super.onCreate(args);
        large="true".equals(args.getString("large_font"));
        start();
    }

    private <T> T onMain(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        runOnMainSync(task);
        return task.get();
    }

    private View find(View view, String label) {
        if (!view.isShown()) return null;
        if (label.contentEquals(view.getContentDescription() == null ? "" : view.getContentDescription())
                || view instanceof TextView && label.contentEquals(((TextView) view).getText())) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View match = find(group.getChildAt(i), label);
                if (match != null) return match;
            }
        }
        return null;
    }

    private void tap(String description) throws Exception {
        boolean clicked = onMain(() -> {
            View action = find(activity.body, description);
            return action != null && action.performClick();
        });
        if (!clicked) throw new AssertionError("İşlem bulunamadı: " + description);
    }

    private void capture(String file, String expectedText) throws Exception {
        waitForIdleSync();
        long limit = SystemClock.uptimeMillis() + 15000;
        while (true) {
            AccessibilityNodeInfo root = automation.getRootInActiveWindow();
            if (root != null && getTargetContext().getPackageName().contentEquals(
                    root.getPackageName() == null ? "" : root.getPackageName())
                    && !root.findAccessibilityNodeInfosByText(expectedText).isEmpty()) break;
            if (SystemClock.uptimeMillis() >= limit) throw new AssertionError("Ekran açılmadı: " + expectedText);
            SystemClock.sleep(150);
        }
        automation.waitForIdle(300, 5000);
        Bitmap screenshot = automation.takeScreenshot();
        if (screenshot == null) throw new AssertionError("Ekran görüntüsü alınamadı: " + file);
        try (FileOutputStream stream = new FileOutputStream(new File(output, file + ".png"))) {
            if (!screenshot.compress(Bitmap.CompressFormat.PNG, 100, stream)) throw new AssertionError(file);
        } finally {
            screenshot.recycle();
        }
    }

    private void dragDisconnectedPad() throws Exception {
        onMain(() -> {
            PadView pad=activity.pad;
            if(pad==null||pad.connected||pad.getWidth()==0)throw new AssertionError("Bağlantısız önizleme bekleniyordu");
            Bitmap before=Bitmap.createBitmap(pad.getWidth(),pad.getHeight(),Bitmap.Config.ARGB_8888);
            Bitmap after=Bitmap.createBitmap(pad.getWidth(),pad.getHeight(),Bitmap.Config.ARGB_8888);
            try {
                pad.draw(new Canvas(before));
                long now=SystemClock.uptimeMillis();
                MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,pad.getWidth()*.35f,pad.getHeight()*.40f,0);
                MotionEvent move=MotionEvent.obtain(now,now+16,MotionEvent.ACTION_MOVE,pad.getWidth()*.65f,pad.getHeight()*.64f,0);
                try {
                    if(!pad.dispatchTouchEvent(down)||!pad.dispatchTouchEvent(move))throw new AssertionError("Sürükleme işlenmedi");
                } finally {down.recycle();move.recycle();}
                pad.draw(new Canvas(after));
                if(before.sameAs(after))throw new AssertionError("Sürükleme perspektifi değiştirmedi");
            } finally {before.recycle();after.recycle();}
            return null;
        });
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            output = new File(getTargetContext().getExternalFilesDir(null), "ci-screenshots");
            if (!output.mkdirs() && !output.isDirectory()) throw new IllegalStateException("Çıktı dizini açılamadı");
            automation = getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
            Intent intent = new Intent(getTargetContext(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity = (MainActivity) startActivitySync(intent);
            if(large){
                if(Math.abs(activity.getResources().getConfiguration().fontScale-1.5f)>.01f)
                    throw new AssertionError("1.5× yazı ölçeği uygulanmadı");
                capture("08-buyuk-yazi-ana-ekran","Uygulamalarım");
                onMain(() -> { activity.onBackPressed(); return null; });
                capture("09-buyuk-yazi-cikis","Uygulamada kal");
                onMain(() -> { activity.exitDialog.dismiss(); return null; });
                tap("Yedek paylaş");
                capture("10-buyuk-yazi-paylasim","Konum seç");
            } else {
                capture("01-ana-ekran","Uygulamalarım");
                tap("Döndürmek için dokun");
                capture("02-kol-onizleme","Giriş geçmişi");
                dragDisconnectedPad();
                capture("02b-kol-dondurulmus","Giriş geçmişi");
                tap("‹  Geri");
                tap("＋  Yeni uygulama ekle");
                tap("Tümü");
                tap("Sistem uygulamalarını göster");
                capture("07-uygulama-secici","Sistem uygulamalarını göster");
                tap("‹  Geri");
                tap("Ayarlar");
                tap("Gizlilik");
                capture("03-gizlilik","Ağ erişimi");
                tap("‹  Geri");
                tap("Yedekler");
                capture("04-yedekler","Dosyayı kaydet");
                tap("‹  Geri");
                tap("‹  Geri");
                onMain(() -> { activity.onBackPressed(); return null; });
                capture("05-cikis-diyalogu","Uygulamadan çıkılsın mı?");
                onMain(() -> { activity.exitDialog.dismiss(); return null; });
                tap("Yedek paylaş");
                capture("06-paylasim-diyalogu","Yedeği paylaş");
            }
            result.putString("stream", "SCREENSHOTS_OK");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", android.util.Log.getStackTraceString(error));
            finish(Activity.RESULT_CANCELED, result);
        }
    }
}
