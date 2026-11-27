#!/usr/bin/env bash
set -euo pipefail
# Private signing material stays outside the source archive.
: "${KOLMAPPER_SIGNING_DIR:?Set KOLMAPPER_SIGNING_DIR to your signing directory}"
: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT}"
: "${KOLMAPPER_LEGACY_KEYSTORE:?Set path to the existing debug keystore}"
input_apk=$1
output_apk=$2
"$ANDROID_SDK_ROOT/build-tools/36.0.0/apksigner" sign \
 --ks "$KOLMAPPER_LEGACY_KEYSTORE" --ks-key-alias androiddebugkey --ks-pass pass:android \
 --next-signer --ks "$KOLMAPPER_SIGNING_DIR/kolmapper-release.p12" --ks-key-alias kolmapper \
 --ks-pass "file:$KOLMAPPER_SIGNING_DIR/release.pass" \
 --lineage "$KOLMAPPER_SIGNING_DIR/kolmapper.lineage" --rotation-min-sdk-version 28 \
 --out "$output_apk" "$input_apk"
