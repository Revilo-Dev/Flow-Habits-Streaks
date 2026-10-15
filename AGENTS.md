# Repository instructions

These instructions apply throughout this repository.

## Commenting expectations

- Write simple explanations in plain English.
- Explain the purpose of a section, a nonobvious rule, or the reason for an implementation choice.
- Use one single line for each explanation.
- Use line comments such as `//` in Kotlin and Java and `#` in shell or Python.
- Do not use emojis in comments.
- Do not use punctuation in the explanation itself. The comment delimiter is required syntax and is allowed.
- Use words and spaces instead of punctuation-heavy identifiers, paths, links, formulas, or code snippets.
- Do not use block comments, multiline comments, KDoc blocks, banners, or decorative separators for new explanations.
- Do not split a long explanation across several comment lines. Shorten it or put the detailed explanation in the relevant Markdown guide.
- Prefer comments above the code being explained. Do not annotate every assignment, obvious accessor, or closing brace.
- Keep comments accurate when behavior changes. Remove stale claims instead of carrying them forward.
- Preserve existing copyright headers, license notices, third party attribution, shebangs, and tool directives. Their required format is separate from explanatory comments.
- Existing legacy comments do not require a repository-wide rewrite during unrelated work. New and revised explanations must follow these rules.
- Markdown documentation can use normal punctuation, headings, links, tables, and code fences.

Examples of the expected style:

```kotlin
// Keep skipped days from increasing the streak length
// Refresh widgets after the habit change is saved
// Use the first visible character when no icon is selected
```

## Project identity

- User-facing app name: Flow - Habits & Streaks.
- Release Android application ID: `dev.revilodev.flow`.
- Debug application ID: `dev.revilodev.flow.debug` with launcher label Flow Test and separate app data.
- Keep test builds separate from installed release builds. Never uninstall the release app or clear its data to work around signing differences.
- Android namespace and existing source packages: `org.isoron.uhabits`.
- Flow is based on Loop Habit Tracker. Preserve attribution and GPL version 3 or later licensing.
- Application ID, source namespace, legacy database names, and intent URI authorities are not interchangeable. Use the variant application ID for Android action names, registered provider authorities, and package targets.
- Verify identifiers at their definitions before changing integrations. Some internal habit URIs retain a legacy authority.
- Do not copy upstream store links or distribution claims into Flow documentation as if they identify this application.

## Read these resources

| Resource | Use |
| --- | --- |
| [README.md](README.md) | Implemented features, usage, settings, and limitations |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Startup, module boundaries, calculations, persistence, and integrations |
| [docs/BUILD.md](docs/BUILD.md) | Toolchain, wrapper commands, signing, and build outputs |
| [docs/TEST.md](docs/TEST.md) | Test locations, verification commands, and emulator requirements |
| [docs/ONE_UI_DESIGN.md](docs/ONE_UI_DESIGN.md) | Applied One UI rulebook decisions and remaining device checks |
| [docs/GUIDELINES.md](docs/GUIDELINES.md) | Contribution scope, formatting, and documentation expectations |
| [docs/widget-blur-handoff.md](docs/widget-blur-handoff.md) | Historical widget surface investigation |
| [CHANGELOG.md](CHANGELOG.md) | Inherited release history rather than a complete Flow release log |
| [NOTICE.md](NOTICE.md) and [LICENSE.txt](LICENSE.txt) | Third party notices and license terms |

Use current source and build configuration to resolve outdated notes.

## Repository map

| Path | Responsibility |
| --- | --- |
| `uhabits-android/src/main/java/org/isoron/uhabits/` | Android application, activities, dialogs, notifications, receivers, widgets, and dependency wiring |
| `uhabits-android/src/main/res/` | Layouts, Flow design resources, localized strings, settings, and widget metadata |
| `uhabits-android/src/main/AndroidManifest.xml` | Entry points, permissions, exported components, backup agent, and file provider |
| `uhabits-core/src/commonMain/kotlin/org/isoron/uhabits/core/` | Habit models, commands, repositories, import and export, preferences, tasks, and presenters |
| `uhabits-core/src/commonMain/kotlin/org/isoron/platform/` | Shared database, file, graphics, and date interfaces |
| `uhabits-core/src/jvmMain/` | JVM platform adapters, date utilities, reminder scheduling, and day boundary timer |
| `uhabits-core/src/jsMain/` | JavaScript platform adapters for the core target |
| `uhabits-core/assets/main/migrations/` | Numbered SQL schema migrations shared with Android |
| `uhabits-core/src/commonTest/` and `uhabits-core/src/jvmTest/` | Model, presenter, database, and platform tests |
| `uhabits-android/src/test/` | Android module JVM tests |
| `uhabits-android/src/androidTest/` | Emulator, view, widget, navigation, and regression tests |
| `gradle/libs.versions.toml` | Dependency and plugin versions |
| `gradle/wrapper/gradle-wrapper.properties` | Pinned Gradle distribution |
| `screenshots/` | Inherited screenshots and Tasker references |
| `tools/parseInstrument.py` | Parser for the inherited emulator script output |

## Implementation notes

- Keep shared business rules in the core module and Android APIs in the Android module or the appropriate platform source set.
- This is a View-based Android app with view binding and custom views. Use its existing components and Flow design resources.
- Dependency injection uses kotlin inject and generated KSP wiring. Do not manually edit generated files.
- Send normal habit changes through commands so the list cache, notifications, and widgets receive completion events.
- Original entries are saved data. Computed entries, scores, and streaks are derived data and must be recomputed after relevant changes.
- Numerical entry values use thousandths of a unit. Keep special skipped and unknown values distinct from ordinary measurements.
- Individual streaks bridge skips by default and count skipped days when the per habit setting is enabled. Scores and perfect days keep their existing skip policy.
- Tapping a noted calendar day previews its note without changing the answer. Editing is an explicit action.
- Preserve viewed days and drafts across navigation and recreation. Measure action region clearance and keep targets at least forty eight density independent pixels.
- Home completion and perfect streak summaries use all active habits, independently of search and row visibility.
- At most completion rules differ between the general completed filter, streak and perfect day presenters, and daily check-ins. Review each consumer before changing shared logic.
- Most tracking dates honor the optional 3 am day boundary. Daily check-ins use their occurrence's local calendar date.
- Preserve stable habit identities during import. Imported dates can replace existing answers; unrelated dates remain.
- Keep schema constants, new migrations, repository column mappings, import handling, and tests in agreement.
- Automatic backup rotation happens before writing a fresh copy. The shared-folder filename filter protects unrelated files.
- Widget surfaces are translucent. Neither the drawable name nor the launcher root ID proves wallpaper blur support.
- Pure black mode is hidden and currently falls back to the Flow dark theme.
- The core JavaScript target is not a released browser app. Inherited sync fields do not establish a cloud sync service.

## Working and verification

- Inspect the working tree before edits and preserve unrelated local changes.
- Make focused changes without unrelated dependency upgrades, identifier renames, formatting sweeps, or generated output.
- Use the checked-in Gradle wrapper and Java 17. Confirm SDK and toolchain requirements in the build guide.
- On Windows use `.\gradlew.bat`; on Linux or macOS use `./gradlew`.
- For core behavior run `:uhabits-core:jvmTest`; for Android JVM behavior run `:uhabits-android:testDebugUnitTest`.
- Run `ktlintCheck` for source formatting. Avoid a repository-wide formatter for a localized change.
- For Android compilation use `:uhabits-android:assembleDebug`; use the appropriate instrumented tests for views, widgets, or device behavior.
- Documentation and comment changes need link, accuracy, style, and diff review. Do not add tests that merely restate the implementation.
- Existing formatting failures must be distinguished from failures introduced by the change. Report unavailable dependencies or device checks honestly.
- Update README when features, settings, limitations, installation, or outputs change. Keep architecture and test notes in step with structural changes.
- Do not edit build caches, generated bindings, generated injection sources, compiled artifacts, local SDK settings, signing keys, or credentials.
- Do not publish releases, alter signing configuration, or replace screenshot baselines as a side effect of a documentation task.
