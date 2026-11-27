#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
mkdir -p ci-output/screenshots
app=dev.kolmapper.debug
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb shell wm size 768x1280
adb shell wm density 240
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
runner="$app.test/dev.kolmapper.ScreenshotInstrumentation"
remote="/sdcard/Android/data/$app/files/ci-screenshots"
adb shell rm -rf "$remote"
adb shell am instrument -w -r "$runner" | tr -d '\r' | tee ci-output/instrumentation.log
grep -Fq 'stream=SCREENSHOTS_OK' ci-output/instrumentation.log
grep -Fxq 'INSTRUMENTATION_CODE: -1' ci-output/instrumentation.log
for name in 01-ana-ekran 02-kol-onizleme 03-gizlilik 04-yedekler 05-cikis-diyalogu 06-paylasim-diyalogu; do
  adb pull "$remote/$name.png" "ci-output/screenshots/$name.png"
  test -s "ci-output/screenshots/$name.png"
done
