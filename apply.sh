#!/usr/bin/env bash
# Builds the gift app: clones the Xash3D FWGS engine, drops our files in, patches the manifest,
# and builds the APK.
#   ./apply.sh            -> clone + patch + build (needs Android SDK/NDK + Java 17)
#   ./apply.sh --no-build -> clone + patch only (open engine/android in Android Studio)
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ENGINE="$HERE/engine"

if [ ! -d "$ENGINE/.git" ]; then
  git clone --recursive https://github.com/FWGS/xash3d-fwgs "$ENGINE"
fi

cd "$ENGINE"
mkdir -p 3rdparty
[ -d 3rdparty/SDL ] || git clone --depth 1 -b SDL2 https://github.com/libsdl-org/SDL 3rdparty/SDL
[ -d 3rdparty/hlsdk-portable ] || git clone --depth 1 -b mobile-hacks https://github.com/FWGS/hlsdk-portable 3rdparty/hlsdk-portable

# our code
APP_JAVA="android/app/src/main/java"
mkdir -p "$APP_JAVA/su/xash/engine/gift"
cp "$HERE/overlay/java/su/xash/engine/gift/"*.java "$APP_JAVA/su/xash/engine/gift/"

# manifest
MANIFEST="android/app/src/main/AndroidManifest.xml"
python3 "$HERE/tools/patch_manifest.py" "$MANIFEST"

echo
echo "== engine activity (check the extras the game launcher expects) =="
grep -rn "getStringExtra\|getBooleanExtra\|XashActivity" android/app/src/main/java 2>/dev/null | head -40 || true

if [ "${1:-}" = "--no-build" ]; then
  echo "Patched. Open $ENGINE/android in Android Studio."
  exit 0
fi

cd android
chmod +x gradlew
./gradlew assembleDebug
echo
find . -name "*.apk" -print
