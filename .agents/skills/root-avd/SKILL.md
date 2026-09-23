---
name: root-avd
description: "This skill should be used when the user needs to root an existing Android Studio AVD (Android Virtual Device) with Magisk using the rootAVD tool. Trigger phrases include 'root AVD', 'install Magisk on emulator', 'rootAVD'. This skill covers downloading Magisk, patching ramdisk, and verifying root."
---

# Root AVD Skill

Root an existing, running Android Studio AVD with Magisk using the rootAVD tool.

## Prerequisites

- A running AVD with `adb shell` accessible
- Android SDK with `platform-tools` (adb) in PATH
- rootAVD repository cloned locally: **https://github.com/NKU100/rootAVD** (our fork of newbit1/rootAVD, with `MAGISK_CHOICE` env var support and Android 16 compatibility)
- Recommended image type: `google_apis` (not `google_apis_playstore`) for easier rooting
- Tested with: **Magisk v30.7** + **Android 7 ~ Android 16 (API 36)**

## Workflow

### 1. Ensure AVD is Running with Cold Boot

Root patches only take effect after a cold boot. Always start the emulator with `-no-snapshot-load`:

```bash
nohup $ANDROID_HOME/emulator/emulator -avd {AVD_NAME} -no-snapshot-load &
$ANDROID_HOME/platform-tools/adb wait-for-device
```

### 2. Download Latest Magisk

The `stable.json` in topjohnwu/magisk-files may lag behind actual releases. Check the latest release at https://github.com/topjohnwu/Magisk/releases and download if needed:

```bash
curl -L -o {ROOTAVD_DIR}/Magisk.zip \
  "https://github.com/topjohnwu/Magisk/releases/download/v{VERSION}/Magisk-v{VERSION}.apk"
```

> As of 2026-02, the latest stable is **Magisk v30.7**.

### 3. Run rootAVD

Use `MAGISK_CHOICE` env var for non-interactive selection:
- `1` = local (whatever Magisk.zip is present)
- `2` = stable (from online)
- `3` = canary (from online)

```bash
cd {ROOTAVD_DIR}
export PATH=$ANDROID_HOME/platform-tools:$PATH
MAGISK_CHOICE=1 ./rootAVD.sh system-images/android-{API}/{IMAGE_TYPE}/{ARCH}/ramdisk.img
```

Use `ListAllAVDs` to find the correct ramdisk path:
```bash
./rootAVD.sh ListAllAVDs
```

For **Android 14+**, you may need the `FAKEBOOTIMG` parameter to create a fake Boot.img for Magisk APP patching:
```bash
MAGISK_CHOICE=1 ./rootAVD.sh system-images/android-{API}/{IMAGE_TYPE}/{ARCH}/ramdisk.img FAKEBOOTIMG
```

### 4. Cold Boot After Patching

The script shuts down the AVD after patching. Restart with cold boot:

```bash
nohup $ANDROID_HOME/emulator/emulator -avd {AVD_NAME} -no-snapshot-load &
$ANDROID_HOME/platform-tools/adb wait-for-device
```

### 5. Verify Root

```bash
$ANDROID_HOME/platform-tools/adb root
$ANDROID_HOME/platform-tools/adb shell "su -c 'id && magisk -v && magisk -V'"
```

Expected output:
```
uid=0(root) gid=0(root) context=u:r:magisk:s0
{VERSION}:MAGISK:R
{VERSION_CODE}
```

## Switching Magisk Versions

Restore the stock ramdisk before re-rooting with a different version:

```bash
./rootAVD.sh system-images/android-{API}/{IMAGE_TYPE}/{ARCH}/ramdisk.img restore
```

Then replace Magisk.zip and re-run rootAVD.

## Magisk v30.7 Behavioral Changes

- **`su` no longer drops capabilities by default**, even when switching to a non-root UID. If you need the old behavior, use `su --drop-cap` explicitly.
- **Zygisk** now supports **Android 16 QPR2+** and **Android XR** devices.
- MagiskInit supports Android 16 QPR2 sepolicy format and `klogdump` partition as pre-init storage.

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `su` returns "Permission denied" | Run `adb root` first, then retry `su -c id` |
| Old Magisk version after update | Restore ramdisk first, then re-root with new Magisk.zip |
| rootAVD menu times out or selects wrong version | Use `MAGISK_CHOICE=N` env var |
| `stable.json` doesn't list latest version | Manually download APK from GitHub Releases |
| AVD won't root after patch | Ensure cold boot with `-no-snapshot-load` |
| Android 14+ patching fails | Try adding `FAKEBOOTIMG` parameter to rootAVD command |
| Zygisk modules not loading on Android 16 | Ensure Magisk v30.7+ for Android 16 QPR2 Zygisk support |
