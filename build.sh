#!/bin/bash
# Manual APK build for StudyQuiz (Gradle daemon is broken in this sandbox).
set -e
export JAVA_HOME=$HOME/buildtools/jdk-17.0.11+9
export PATH=$JAVA_HOME/bin:$PATH

SDK=$HOME/buildtools/android-sdk
BT=$SDK/build-tools/34.0.0
PLATFORM=$SDK/platforms/android-34/android.jar
KOTLINC=$HOME/buildtools/kotlinc/bin/kotlinc
STDLIB=$HOME/buildtools/kotlinc/lib/kotlin-stdlib.jar
PROJ=$HOME/workspace/studyquiz/app/src/main
OUT=$HOME/workspace/studyquiz/build
KEYSTORE=$HOME/workspace/android-toolchain/debug.keystore

VER_CODE=53
VER_NAME=1.53
APK_NAME="StudyQuiz-$VER_NAME.apk"

rm -rf "$OUT"
mkdir -p "$OUT/gen" "$OUT/classes" "$OUT/dex" "$OUT/genkt"

echo "== version info =="
cat > "$OUT/genkt/Version.kt" <<EOF
package com.anandu.studyquiz
/** Generated at build time — do not edit. Keep in sync via VER_CODE/VER_NAME above. */
object AppVersion { const val CODE = $VER_CODE; const val NAME = "$VER_NAME" }
EOF

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

cp "$OUT/$APK_NAME" "$HOME/workspace/your_files/$APK_NAME"
ls -la "$HOME/workspace/your_files/$APK_NAME"
echo "BUILD OK: $APK_NAME"
