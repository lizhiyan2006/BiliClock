# Build

## Requirements

- JDK 17
- Android SDK Platform 35
- Android SDK Build-Tools 35.0.0
- Windows PowerShell for the convenience script

The project uses `compileSdk 35`, `targetSdk 34` and `minSdk 23`. It has no
AndroidX, Compose, Lottie or WebView dependency.

## Android Studio

1. Open the repository root in Android Studio.
2. Use JDK 17 and let Gradle sync (AGP 8.7.3 / Gradle 8.9).
3. Select the `app` configuration and run on a device or emulator.

If Gradle asks for the SDK location, create a local `local.properties`:

```properties
sdk.dir=C\:\\Android\\Sdk
```

`local.properties` is ignored by git.

## Command line without Gradle

The direct build script uses `aapt2`, `javac`, `d8`, `zipalign` and
`apksigner`:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build.ps1
```

Pass explicit paths if the SDK/JDK are not on the environment:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build.ps1 `
  -SdkPath "C:\Android\Sdk" `
  -JdkPath "C:\Program Files\Microsoft\jdk-17"
```

Output:

```text
dist/BiliClock-1.3.1-debug.apk
```

The script creates a local debug keystore under `keystore/` if needed. Both
`dist/` and `keystore/` are ignored; do not commit signing material.

## Font asset

The clock reads `app/src/main/assets/Helvetica-Regular.ttf` and uses it for
Latin letters and digits. Chinese glyphs are not in Helvetica, so Android
falls back to the system CJK font. If the asset is missing or unreadable the
code falls back to `sans-serif`.

This font file was supplied by the project owner. See
[THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md) before redistributing it.

## Release checklist

- [ ] Update `versionCode` / `versionName` in `app/build.gradle` and
      `scripts/build.ps1`.
- [ ] Update `CHANGELOG.md`.
- [ ] Build and install on a clean device.
- [ ] Run `apksigner verify`.
- [ ] Upload the APK and source ZIP to GitHub Releases, not to the git tree.
- [ ] Set the repository social preview to `docs/images/social-preview.png`.
- [ ] Review font/icon licensing if the release will be public.
