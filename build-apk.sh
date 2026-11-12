#!/usr/bin/env bash
# SDK-only alternative to Gradle. Produces a signed personal-testing debug APK.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
: "${SDK:?Set ANDROID_HOME to your Android SDK directory}"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}"
TOOLS="$SDK/build-tools/35.0.0"
ANDROID_JAR="$SDK/platforms/android-35/android.jar"
OUT="$ROOT/app/build/manual"
mkdir -p "$OUT/classes" "$OUT/dex" "$ROOT/dist"
python3 - "$ROOT" "$OUT" <<'PY'
import pathlib, sys
root, out = map(pathlib.Path, sys.argv[1:])
manifest = (root / 'app/src/main/AndroidManifest.xml').read_text()
manifest = manifest.replace('<manifest ', '<manifest package="dev.pocketcode.app" ', 1)
(out / 'AndroidManifest.xml').write_text(manifest)
PY
"$TOOLS/aapt2" compile --dir "$ROOT/app/src/main/res" -o "$OUT/resources.zip"
"$TOOLS/aapt2" link -o "$OUT/unsigned.apk" -I "$ANDROID_JAR" \
  --manifest "$OUT/AndroidManifest.xml" \
  --min-sdk-version 26 --target-sdk-version 35 \
  --version-code 15 --version-name 0.15.0 "$OUT/resources.zip"
"${JAVA}javac" -encoding UTF-8 -source 17 -target 17 \
  -classpath "$ANDROID_JAR" -d "$OUT/classes" \
  "$ROOT"/app/src/main/java/dev/pocketcode/app/*.java
mapfile -d '' CLASSES < <(find "$OUT/classes" -name '*.class' -print0)
"$TOOLS/d8" --lib "$ANDROID_JAR" --min-api 26 --output "$OUT/dex" "${CLASSES[@]}"
python3 - "$ROOT" "$OUT" <<'PY'
import pathlib, sys, zipfile
root, out = map(pathlib.Path, sys.argv[1:])
with zipfile.ZipFile(out / 'unsigned.apk', 'a', zipfile.ZIP_DEFLATED) as apk:
    for item in (out / 'dex').glob('*.dex'):
        apk.write(item, item.name)
    for item in (root / 'app/src/main/assets').rglob('*'):
        if item.is_file():
            apk.write(item, 'assets/' + str(item.relative_to(root / 'app/src/main/assets')))
PY
"$TOOLS/zipalign" -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
KEYSTORE="$ROOT/.debug-signing.keystore"
if [[ ! -f "$KEYSTORE" ]]; then
  "${JAVA}keytool" -genkeypair -keystore "$KEYSTORE" -storepass android \
    -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 \
    -validity 10000 -dname 'CN=Pocket Code Debug,O=Personal Testing,C=US'
fi
"$TOOLS/apksigner" sign --ks "$KEYSTORE" --ks-pass pass:android \
  --key-pass pass:android --out "$ROOT/dist/pocket-code-debug.apk" "$OUT/aligned.apk"
"$TOOLS/apksigner" verify --verbose "$ROOT/dist/pocket-code-debug.apk"
echo "APK: $ROOT/dist/pocket-code-debug.apk"
