# Testing Flow

The repository includes shared model tests, JVM platform tests, Android JVM tests, and Android instrumented tests. Choose checks that cover the changed behavior. See [BUILD.md](BUILD.md) for Java 17 and SDK setup.

## Test locations

| Location | Coverage |
| --- | --- |
| `uhabits-core/src/commonTest/kotlin/` | Shared models, commands, repositories, import and export, preferences, presenters, and chart behavior |
| `uhabits-core/src/jvmTest/java/` | JVM platform helpers, graphics, date utilities, reminder scheduling, and day boundary timing |
| `uhabits-core/src/jsTest/kotlin/` | JavaScript test platform and database helpers |
| `uhabits-core/assets/test/` | Import databases, CSV fixtures, and chart image references |
| `uhabits-android/src/test/java/` | Android module JVM tests such as ReminderControllerTest |
| `uhabits-android/src/androidTest/java/` | Device storage, backups, views, widgets, navigation, user flows, and regressions |
| `uhabits-android/src/androidTest/assets/` | Reference assets used by instrumented view tests |

Shared common tests run on the configured core targets. The jvmTest source directory contains Kotlin files despite its `java` directory name.

## JVM checks

These commands do not need a running Android device:

```powershell
.\gradlew.bat :uhabits-core:jvmTest
.\gradlew.bat :uhabits-android:testDebugUnitTest
.\gradlew.bat ktlintCheck
```

On Linux and macOS, replace `.\gradlew.bat` with `./gradlew`. Android module configuration still requires an available Android SDK even when the selected tests run on the host JVM.

To focus on a specific test:

```powershell
.\gradlew.bat :uhabits-core:jvmTest --tests "org.isoron.uhabits.core.models.StreakListTest"
```

Particularly useful regression tests include:

| Behavior | Existing tests |
| --- | --- |
| Flexible schedule and derived answers | EntryListTest |
| Strength calculation | ScoreListTest |
| Streak length and per-habit skip policy | StreakListTest, CurrentStreakCardPresenterTest |
| Note preview and explicit editing | HistoryCardPresenterTest, FlowHabitFeaturesTest |
| Home summaries and filtered lists | HabitCardListCacheTest |
| Individual current streak card | CurrentStreakCardPresenterTest |
| Search matching | HabitMatcherTest |
| Storage and import | Repository, SQLite model, and importer tests |
| Reminder timing | ReminderSchedulerTest and ReminderControllerTest |

JVM reports normally appear at `uhabits-core/build/reports/tests/jvmTest/index.html` and `uhabits-android/build/reports/tests/testDebugUnitTest/index.html`. Machine-readable results are under each module's `build/test-results/` directory.

## Browser checks

The JavaScript target runs the common tests plus its platform helpers through Karma and headless Chrome:

```powershell
.\gradlew.bat :uhabits-core:jsBrowserTest
```

Install Chrome or Chromium and set `CHROME_BIN` if automatic discovery fails. The first run may need to download Node, Yarn, and JavaScript dependencies. Reports are normally under `uhabits-core/build/reports/tests/jsBrowserTest/`.

The browser target tests the shared core. It does not launch a separate Flow website.

## Android instrumented checks

Use a clean emulator rather than a personal device. Existing user interface and screenshot tests assume a Nexus 4 configuration with a 4.7 inch 768 by 1280 display at xhdpi and English US locale.

Prepare the emulator before running the suite:

1. Use a clean installation and avoid adding other apps, home screen icons, or widgets.
2. Disable window, transition, and animator animations.
3. Confirm the emulator is visible to adb.
4. Run the direct Gradle task.

```powershell
.\gradlew.bat :uhabits-android:connectedDebugAndroidTest
```

To run one instrumented test class:

```powershell
.\gradlew.bat :uhabits-android:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.isoron.uhabits.widgets.WidgetSurfaceTest
```

The instrumentation runner uses the existing source namespace. Debug app tests target `dev.revilodev.flow.debug`, while release Flow uses `dev.revilodev.flow`. Gradle resolves the test package; do not substitute the upstream package from the Bash helper.

Instrumented reports are normally under `uhabits-android/build/reports/androidTests/connected/`, with raw results under `uhabits-android/build/outputs/androidTest-results/`.

## Visual tests and manual checks

The Android view tests render components and compare them with saved reference images. A difference can indicate a regression, a deliberate design change, an emulator mismatch, a locale mismatch, or a font rendering difference.

Inspect the actual and expected images before replacing reference assets. Replacing baselines to make a test pass does not verify the intended appearance.

For UI work, check the relevant screens in light and dark mode, large font settings, and portrait and landscape where supported. For header, keyboard, or inset changes, check scrolling, system bars, keyboard visibility, and back behavior. For widget changes, check launcher size changes, taps, completion colors, and the shared surface path.

Existing instrumented tests include FlowHeaderBehaviorTest, FlowSurfaceTest, FlowHabitFeaturesTest, WidgetNavigationTest, WidgetSurfaceTest, and AutoBackupTest. FlowSurfaceTest renders light, dark, and legacy pure black input, creation, and About surfaces at 100 and 200 percent font scale. FlowHabitFeaturesTest creates and removes its own habit and checks skip setting persistence, calendar note preview and editing, and home timeline restoration. It also checks calendar viewport restoration and validates that numeric drafts survive errors and recreation before saving to the original habit and date. It writes review images under the target app external files oneui-review directory. Promoted check-in presentation and launcher wallpaper behavior need device-specific verification; a source or JVM test does not establish how a launcher displays them.

## Choosing verification

| Change | Appropriate verification |
| --- | --- |
| Documentation or comments | Read source for accuracy, check local links and comment style, review the diff, and confirm executable code is unchanged |
| Shared model or calculation | Focused existing core tests plus new regression coverage when behavior changes |
| Import or persistence | Repository and importer tests, migration compatibility, and backup round-trip checks |
| Android notification behavior | Controller tests plus a suitable emulator or device for permissions and presentation |
| View or widget behavior | Android compilation, relevant instrumented tests, and visual inspection |
| Build configuration | The affected Gradle tasks with the documented toolchain |

Use `ktlintCheck` to inspect formatting. Prefer a targeted formatting fix when needed; running `ktlintFormat` across the repository can alter unrelated existing code.

## Inherited script limitations

The historical `build.sh` can create emulators, divide medium and large tests, parse output through `tools/parseInstrument.py`, and collect failed screenshots. It still assumes the upstream package and old APK filenames, so its install and test commands do not match the current Flow configuration without adaptation. See [BUILD.md](BUILD.md).

There are no checked-in GitHub Actions workflow files in this checkout. Local test reports establish only the checks actually run. Distinguish a failed test from a task that never started because a required tool, dependency, browser, or device was unavailable.
