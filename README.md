# PipePipe MS PGothic

Modern Xposed API (libxposed) 101 module that renders every bullet comment of
[PipePipe](https://github.com/InfinityLoop1308/PipePipe) with MS PGothic.

- Works on the official PipePipe APK (`InfinityLoop1309.NewPipeEnhanced`). The app is not modified.
- MS PGothic is always used while the module is enabled, whatever font is selected in PipePipe.
  Disable the module to go back to the app's own font.
- For personal use only. The bundled font is extracted from Windows 10 `msgothic.ttc` (Version 5.31).

## Requirements

An Xposed framework that loads modern API modules (API 101 or later), e.g. Vector 2.1+.

## Install

1. Install the module APK.
2. Enable it in the manager. The scope is fixed to PipePipe (`staticScope=true`).
3. Force stop PipePipe and open it again.

## How it works

| File | Role |
|---|---|
| `META-INF/xposed/java_init.list` | Entry class `MSPGothicModule` |
| `META-INF/xposed/module.prop` | `minApiVersion=101`, `targetApiVersion=101`, `staticScope=true` |
| `META-INF/xposed/scope.list` | `InfinityLoop1309.NewPipeEnhanced` |
| `assets/ms_pgothic.ttf` | The font (stored uncompressed) |

PipePipe creates a `TextView` for each comment and adds it to the container inside
`org.schabi.newpipe.views.BulletCommentsView`. The module hooks the framework method
`ViewGroup.addView(View, int, LayoutParams)` with the interceptor chain API and replaces the
typeface of those views. It does not hook PipePipe's own methods, so app updates are unlikely
to break it.

The font is loaded once per process from the module APK's assets
(`PackageManager.getResourcesForApplication(getModuleApplicationInfo())`). If that fails, it is
extracted from the module APK into PipePipe's cache directory instead.

Logs are written with the tag `PipePipeMSPGothic` and can be viewed in the manager.

## Build

```sh
./gradlew assembleRelease
```

JDK 17+ and the Android SDK (platform 37) are required. The release APK is signed with the
local debug key and written to `app/build/outputs/apk/release/app-release.apk`.
