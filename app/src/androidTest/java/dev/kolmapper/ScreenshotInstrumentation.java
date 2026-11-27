package dev.kolmapper;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.Intent;
import android.graphics.Bitmap;
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

    @Override public void onCreate(Bundle args) {
        super.onCreate(args);
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

    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            output = new File(getTargetContext().getExternalFilesDir(null), "ci-screenshots");
            if (!output.mkdirs() && !output.isDirectory()) throw new IllegalStateException("Çıktı dizini açılamadı");
            automation = getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
            Intent intent = new Intent(getTargetContext(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity = (MainActivity) startActivitySync(intent);
            capture("01-ana-ekran", "Uygulamalarım");
            tap("Giriş testi  ›");
            capture("02-kol-onizleme", "Giriş geçmişi");
            tap("‹  Geri");
            tap("Ayarlar");
            tap("Gizlilik");
            capture("03-gizlilik", "Ağ erişimi");
            tap("‹  Geri");
            tap("Yedekler");
            capture("04-yedekler", "Dosyayı kaydet");
            tap("‹  Geri");
            tap("‹  Geri");
            onMain(() -> { activity.onBackPressed(); return null; });
            capture("05-cikis-diyalogu", "Uygulamadan çıkılsın mı?");
            onMain(() -> { activity.exitDialog.dismiss(); return null; });
            tap("Yedek paylaş");
            capture("06-paylasim-diyalogu", "Yedeği paylaş");
            result.putString("stream", "SCREENSHOTS_OK");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", android.util.Log.getStackTraceString(error));
            finish(Activity.RESULT_CANCELED, result);
        }
    }
}
