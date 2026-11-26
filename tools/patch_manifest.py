#!/usr/bin/env python3
"""Patch the engine's AndroidManifest.xml so GiftLauncherActivity is the launcher.
Only adds permissions that are not already declared. Safe to run twice."""
import re
import sys

path = sys.argv[1]
src = open(path, encoding="utf-8").read()

if "GiftLauncherActivity" in src:
    print("manifest already patched")
    sys.exit(0)

wanted = [
    "android.permission.MANAGE_EXTERNAL_STORAGE",
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE",
]
add = ""
for p in wanted:
    if ('android:name="%s"' % p) not in src:
        add += '    <uses-permission android:name="%s"/>\n' % p
if add:
    src, n = re.subn(r"(\s*<application)", "\n" + add + r"\1", src, count=1)
    if n != 1:
        sys.exit("could not find <application> in manifest")

src = re.sub(r'\s*<category android:name="android\.intent\.category\.LAUNCHER"\s*/>', "", src)
src = re.sub(r'\s*<category android:name="android\.intent\.category\.LEANBACK_LAUNCHER"\s*/>', "", src)

activity = """
        <activity
            android:name="su.xash.engine.gift.GiftLauncherActivity"
            android:exported="true"
            android:screenOrientation="sensorLandscape"
            android:theme="@android:style/Theme.NoTitleBar.Fullscreen">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
"""
src, n = re.subn(r"(\s*</application>)", activity + r"\1", src, count=1)
if n != 1:
    sys.exit("could not find </application> in manifest")

open(path, "w", encoding="utf-8").write(src)
print("manifest patched; added permissions:", add.strip() or "none (already present)")
