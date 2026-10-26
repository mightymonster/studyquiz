#!/bin/bash
# CI build for StudyQuiz (GitHub Actions) — mirrors build.sh's manual toolchain
# (aapt2 + kotlinc + d8, no Gradle) so the repo build can never drift from it.
# Version is read from build.sh, so VER_CODE/VER_NAME stay in one place.
#
# Env inputs:
#   JAVA_HOME        (required, set by actions/setup-java)
#   ANDROID_SDK_ROOT or ANDROID_HOME (Android SDK location)
#   KOTLINC_BIN      (required, path to the kotlinc binary)
#   KEYSTORE_B64     (optional, base64 of the debug keystore; keeps APK
#                     signatures identical to local builds)
#   KEYSTORE         (optional, path to a keystore file; used if KEYSTORE_B64
#                     is unset)
#   OUT              (optional, output dir; default: <repo>/ci-build)
# If neither KEYSTORE_B64 nor KEYSTORE is given, a fresh debug keystore is
# generated (APK will still install, but will NOT update over builds signed
# with a different key without an uninstall first).
set -e
export PATH="$JAVA_HOME/bin:$PATH"

ROOT="$(cd "$(dirname "$0")" && pwd)"
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:?ANDROID_SDK_ROOT or ANDROID_HOME must be set}}"
BT="$SDK/build-tools/34.0.0"
PLATFORM="$SDK/platforms/android-34/android.jar"
KOTLINC="${KOTLINC_BIN:?KOTLINC_BIN must be set}"
STDLIB="$(cd "$(dirname "$KOTLINC")/../lib" && pwd)/kotlin-stdlib.jar"
PROJ="$ROOT/app/src/main"
OUT="${OUT:-$ROOT/ci-build}"

VER_CODE="$(grep '^VER_CODE=' "$ROOT/build.sh" | cut -d= -f2)"
VER_NAME="$(grep '^VER_NAME=' "$ROOT/build.sh" | cut -d= -f2)"
APK_NAME="StudyQuiz-$VER_NAME.apk"

rm -rf "$OUT"
mkdir -p "$OUT/gen" "$OUT/classes" "$OUT/dex" "$OUT/genkt"

echo "== version info =="
cat > "$OUT/genkt/Version.kt" <<EOF
package com.anandu.studyquiz
/** Generated at build time — do not edit. Keep in sync via VER_CODE/VER_NAME above. */
object AppVersion { const val CODE = $VER_CODE; const val NAME = "$VER_NAME" }
EOF

if [ -n "$KEYSTORE_B64" ]; then
  echo "$KEYSTORE_B64" | base64 -d > "$OUT/debug.keystore"
  KEYSTORE="$OUT/debug.keystore"
  echo "== keystore: from secret =="
elif [ -n "$KEYSTORE" ]; then
  echo "== keystore: $KEYSTORE =="
else
  echo "== keystore: generating fresh debug keystore (signatures will differ from local builds) =="
  keytool -genkeypair -keystore "$OUT/debug.keystore" \
    -storepass android -keypass android -alias androiddebugkey \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Android Debug, O=Android, C=US" >/dev/null 2>&1
  KEYSTORE="$OUT/debug.keystore"
fi

echo "== aapt2 compile =="
"$BT/aapt2" compile --dir "$PROJ/res" -o "$OUT/res.zip"

echo "== aapt2 link =="
"$BT/aapt2" link -o "$OUT/base.apk" \
  -I "$PLATFORM" \
  --manifest "$PROJ/AndroidManifest.xml" \
  -A "$PROJ/assets" \
  --version-code "$VER_CODE" --version-name "$VER_NAME" \
  --java "$OUT/gen" \
  "$OUT/res.zip"

echo "== kotlinc =="
# shellcheck disable=SC2046
"$KOTLINC" $(find "$PROJ/java" -name "*.kt") "$OUT/genkt/Version.kt" "$OUT/gen/com/anandu/studyquiz/R.java" \
  -cp "$PLATFORM" -d "$OUT/classes" -jvm-target 17 -nowarn

echo "== d8 =="
# shellcheck disable=SC2046
"$BT/d8" --lib "$PLATFORM" --min-api 24 \
  --output "$OUT/dex" \
  $(find "$OUT/classes" -name "*.class") \
  "$STDLIB"

echo "== package =="
cp "$OUT/base.apk" "$OUT/unsigned.apk"
(cd "$OUT/dex" && zip -q "$OUT/unsigned.apk" classes.dex)

echo "== align + sign =="
"$BT/zipalign" -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
"$BT/apksigner" sign --ks "$KEYSTORE" \
  --ks-pass pass:android --key-pass pass:android \
  --out "$OUT/$APK_NAME" "$OUT/aligned.apk"

echo "== verify =="
"$BT/apksigner" verify --print-certs "$OUT/$APK_NAME" | head -5

ls -la "$OUT/$APK_NAME"
echo "BUILD OK: $APK_NAME"
