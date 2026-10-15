# Building Flow

Run commands from the repository root. This guide describes Flow's current Gradle configuration, not the upstream Loop release process.

## Required tools

| Tool | Checked-in requirement |
| --- | --- |
| Java | JDK 17 for the configured JVM toolchains |
| Gradle | Use the included wrapper, pinned to 8.11.1 |
| Android Gradle Plugin | 8.9.2 |
| Kotlin | 2.3.20 |
| Android SDK | Compile SDK 36 and target SDK 36 |
| Supported devices | Android 6.0 / API 23 or later |
| Android Studio | An installation able to open the configured Android Gradle Plugin project |
| Browser | Chrome or Chromium when running the core browser tests |

Plugin and library versions live in [gradle/libs.versions.toml](../gradle/libs.versions.toml). SDK levels, application identity, and release settings live in [uhabits-android/build.gradle.kts](../uhabits-android/build.gradle.kts).

Install SDK platform 36, platform tools, and the build tools requested during Gradle sync. For emulator tests also install the emulator and an appropriate system image. An initial build needs network access to obtain dependencies that are not already cached.

## Android Studio

1. Clone this Flow repository and open its root directory.
2. Configure the Gradle JDK and Java 17 toolchain. Set a valid local SDK location through Android Studio.
3. Sync the project and install any missing SDK components reported by the IDE.
4. Choose the `uhabits-android` run configuration.
5. Run on an emulator or device with API 23 or later.

The Android namespace and existing classes use `org.isoron.uhabits`. Release Flow uses `dev.revilodev.flow`; the debug variant uses `dev.revilodev.flow.debug` and the launcher name **Flow Test**. Debug storage, provider authority, reminder and widget action names, and settings navigation are isolated from the release app. Do not replace the application ID with the source namespace.

## Command line setup

Set `JAVA_HOME` to a valid JDK 17 installation. Make the Android SDK available through your environment or a local `local.properties` containing `sdk.dir`. Do not commit machine-specific paths.

For example, if the SDK is installed in the usual Windows user directory:

```powershell
$env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA "Android\Sdk"
$env:Path = "$env:ANDROID_HOME\platform-tools;$env:Path"
```

On Linux or macOS, use the actual installed SDK path:

```sh
export ANDROID_HOME=/path/to/android-sdk
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
```

Accept the required Android SDK licenses through Android Studio or `sdkmanager --licenses`.

## Build tasks

| Task | Result |
| --- | --- |
| `:uhabits-android:assembleDebug` | Debug APK signed with the local debug key |
| `:uhabits-android:assembleRelease` | Release APK with minification |
| `:uhabits-android:bundleRelease` | Release Android App Bundle |
| `:uhabits-core:jvmTest` | Shared and JVM core tests |
| `:uhabits-android:testDebugUnitTest` | Android module JVM tests |
| `ktlintCheck` | Kotlin formatting checks |
| `:uhabits-android:connectedDebugAndroidTest` | Android instrumented tests on a connected emulator |

Windows:

```powershell
.\gradlew.bat :uhabits-android:assembleDebug
```

Linux or macOS:

```sh
./gradlew :uhabits-android:assembleDebug
```

Use `--stacktrace` to diagnose failures. Use `--offline` only when the required Gradle distribution, plugins, toolchains, and dependencies are already present locally.

The current build sets `versionName` to `1.0.0` and `versionCode` to `20304`. APK names follow `Flow-Habits-Streaks-v<versionName>-<variant>.apk`:

| Output | Current path |
| --- | --- |
| Debug APK | `uhabits-android/build/outputs/apk/debug/Flow-Habits-Streaks-v1.0.0-debug.apk` |
| Release APK | `uhabits-android/build/outputs/apk/release/Flow-Habits-Streaks-v1.0.0-release.apk` |
| Release bundle | `uhabits-android/build/outputs/bundle/release/uhabits-android-release.aab` |

A file under `uhabits-android/release/` is an existing artifact, not proof that a fresh build succeeded.

To install the current debug output after enabling device debugging:

```powershell
adb install -r uhabits-android/build/outputs/apk/debug/Flow-Habits-Streaks-v1.0.0-debug.apk
```

## Keeping existing app data

Debug builds install as Flow Test beside the existing Flow app. Updating Flow Test with the same local debug key retains its own data. It does not replace, import, or erase the release app database. To try existing habits, export a full database backup from Flow and import that copy into Flow Test.

If Android reports `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, the installed package and the APK use different signing certificates. Do not uninstall the installed app or clear its data as a workaround. A direct update of the same package needs its existing app signing key. A Google Play installation normally needs an update delivered through the same Play app; the upload key and the Play app signing key can differ. See [Android app signing](https://developer.android.com/studio/publish/app-signing).

Keep the existing Play app on `dev.revilodev.flow`. Creating Flow Test changes only the debug application ID, not the release identity or signing keys. A real Play update also needs a version code higher than the versions already uploaded to that Play app.

## Release signing

The build creates its release signing configuration when `LOOP_KEY_ALIAS` is present. These inherited environment variable names remain in use:

| Variable | Meaning |
| --- | --- |
| `LOOP_KEY_ALIAS` | Signing key alias |
| `LOOP_KEY_PASSWORD` | Key password |
| `LOOP_KEY_STORE` | Path to the keystore |
| `LOOP_STORE_PASSWORD` | Keystore password |

Configure all four together outside tracked files. Without them, a release build does not have the configured private release signature. Building an artifact does not publish it.

## Shared core and browser target

The core is Kotlin Multiplatform with JVM and JavaScript browser targets. JavaScript dependencies include SQL.js, sprintf-js, and JSZip. Browser tests use Karma and headless Chrome.

```powershell
.\gradlew.bat :uhabits-core:jvmTest
.\gradlew.bat :uhabits-core:jsBrowserTest
```

A full core build can also run browser tasks and provision JavaScript tooling. Choose the narrower JVM test task when browser coverage is not required. See [TEST.md](TEST.md) for test locations and fixtures.

## Inherited build script

`build.sh` is an upstream Bash helper with emulator setup, parallel emulator tests, logging, and screenshot collection. It is not the preferred entry point for the current Flow build.

Its implementation still hardcodes the upstream package `org.isoron.uhabits`, expects APK filenames such as `uhabits-android-debug.apk`, and copies files using Loop names. These do not match Flow's application ID and renamed APK outputs. Its release path also expects a local `.secret/env` file. Do not use its build, install, or test paths without reviewing and adapting those assumptions.

The script also requires Unix tools such as `flock`, `timeout`, `ts`, `rsync`, `pgrep`, and `pkill`. It manages and terminates its own named emulator processes. Gradle's direct tasks avoid those script-specific requirements.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Java cannot be launched | Make sure JAVA_HOME names an existing JDK directory rather than an old installation |
| Java 17 toolchain missing | Install JDK 17 and make it discoverable to Gradle |
| Missing SDK or platform | Configure sdk.dir or ANDROID_HOME and install SDK platform 36 |
| Dependency unavailable offline | Run with network access once to populate the cache |
| Gradle lock access denied | Check access to the configured user Gradle cache and other processes holding the lock |
| Browser executable missing | Install Chrome or Chromium and configure CHROME_BIN when needed |
| Update rejected due to a different signature | Use Flow Test for local testing or the original signing route for an in-place release update; keep the installed app and data |
| Helper script cannot find the APK | Use the renamed Flow output paths and direct Gradle tasks |
| Formatting check fails | Inspect the reported files before applying a formatter; avoid unrelated rewrites |

Do not change dependencies or checked-in build settings merely to work around a local environment issue. Report the exact failing task and distinguish missing tools from a compilation or test failure.
