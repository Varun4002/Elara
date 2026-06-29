---
description: Build the Elara GMS debug APK
agent: build
---

Build the Elara app for debugging and install it on a connected device or emulator:

```bash
./gradlew :app:assembleGmsDebug
```

If the build fails, read the error output carefully, fix the issue, and rebuild until successful.

The output APK will be at: `app/build/outputs/apk/universalGms/debug/app-universal-gms-debug.apk`

Install with: `adb install app/build/outputs/apk/universalGms/debug/app-universal-gms-debug.apk`
