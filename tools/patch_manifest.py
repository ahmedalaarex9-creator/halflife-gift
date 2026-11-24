#!/usr/bin/env python3
"""Patch the engine's AndroidManifest.xml so GiftLauncherActivity is the launcher.

usage: patch_manifest.py <path/to/AndroidManifest.xml>
Safe to run twice.
"""
import re
import sys

path = sys.argv[1]
src = open(path, encoding="utf-8").read()

if "GiftLauncherActivity" in src:
    print("manifest already patched")
    sys.exit(0)

# 1) permissions
perms = (
    '    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"/>\n'
    '    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>\n'
    '    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" '
    'android:maxSdkVersion="29"/>\n'
)
src, n = re.subn(r"(\s*<application)", "\n" + perms + r"\1", src, count=1)
if n != 1:
    sys.exit("could not find <application> in manifest")

# 2) old launcher categories go away (the engine activity stays, just not as an icon)
src = re.sub(r'\s*<category android:name="android\.intent\.category\.LAUNCHER"\s*/>', "", src)
src = re.sub(r'\s*<category android:name="android\.intent\.category\.LEANBACK_LAUNCHER"\s*/>', "", src)

# 3) our activity
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
print("manifest patched")
m = re.findall(r'android:name="([^"]*Activity[^"]*)"', src)
print("activities now:", m)
