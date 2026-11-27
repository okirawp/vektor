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
font_scale=$(adb shell settings get system font_scale | tr -d '\r')
restore_scale(){
  if [[ "$font_scale" == null ]]; then adb shell settings delete system font_scale; else adb shell settings put system font_scale "$font_scale"; fi
}
trap restore_scale EXIT
capture(){
  adb shell settings put system font_scale "$1"
  adb shell am force-stop "$app"
  adb shell am instrument -w -r -e large_font "$2" "$runner" | tr -d '\r' | tee "ci-output/instrumentation.log"
  grep -Fq 'stream=SCREENSHOTS_OK' "ci-output/instrumentation.log"
  grep -Fxq 'INSTRUMENTATION_CODE: -1' "ci-output/instrumentation.log"
}
capture 1.0 false
capture 1.5 true
for name in 01-ana-ekran 02-kol-onizleme 02b-kol-dondurulmus 03-gizlilik 04-yedekler 05-cikis-diyalogu 06-paylasim-diyalogu 07-uygulama-secici 08-buyuk-yazi-ana-ekran 09-buyuk-yazi-cikis 10-buyuk-yazi-paylasim 11-dolu-koyu 12-dolu-acik 13-giris-testi-acik; do
  adb pull "$remote/$name.png" "ci-output/screenshots/$name.png"
  test -s "ci-output/screenshots/$name.png"
done
