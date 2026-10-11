# APK builds

| File | Source |
|---|---|
| `PipePipe_5.4.0-arm64-v8a-debug.apk` | PipePipeClient `pipepipeclient/ms-pgothic-bullet-font` @ 3d7b81a23 (MS PGothic added to bullet comment fonts) |

Debug build (debug-signed). It installs alongside the official app because the package name differs (`.debug` suffix).

## LSPosed module

| File | Source |
|---|---|
| `PipePipe-MSPGothic-LSPosed-1.0.0.apk` | `lsposed/ms-pgothic-module` @ 0ab9cc7 (libxposed API 101 module, applies MS PGothic to every bullet comment of the official PipePipe) |

Requires an Xposed framework supporting modern API 101+ (e.g. Vector 2.1+). Signed with a local debug key.
