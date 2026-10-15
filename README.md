# Flow - Habits & Streaks

Flow is an Android habit tracker for recording daily actions, measuring progress, and building consistent routines. It is a fork of Loop Habit Tracker with a Flow interface, custom habit icons, daily progress summaries, and an optional daily check-in notification.

Habit tracking works offline without an account. The app is open source under GPL version 3 or later and has no advertising or in-app purchases.

## Contents

- [Features](#features)
- [Using Flow](#using-flow)
- [How progress works](#how-progress-works)
- [Reminders and daily check-ins](#reminders-and-daily-check-ins)
- [Home screen widgets](#home-screen-widgets)
- [Data and backups](#data-and-backups)
- [Settings](#settings)
- [Build and development](#build-and-development)
- [Documentation and repository resources](#documentation-and-repository-resources)
- [Attribution and license](#attribution-and-license)

## Features

| Area | Available features |
| --- | --- |
| Habit types | Yes or no habits and measurable habits with units and at least or at most targets |
| Schedules | Daily, weekly, monthly, and custom repetition frequencies for yes or no habits; daily, weekly, or monthly target periods for measurable habits |
| Personalization | Habit names, reminder questions, descriptions, colors, and a custom emoji or character icon |
| Daily tracking | Recent answers on the home screen, numerical entry, per-date notes and calendar note tooltips, optional skips, and optional unanswered markers |
| Home summary | Current date, active habit completion count, perfect day streak, calendar week indicators, and dismissible tips |
| Habit insights | Current streak, best streaks, per-habit skip counting, habit strength, accessible history calendar, bar chart, weekday frequency, notes, and numerical target totals |
| Organization | Search, manual drag ordering, sorting by name, color, score, or status, archive filtering, completion filtering, and multiple selection |
| Bulk actions | Change colors, archive, restore archived habits, and delete with confirmation; edit a single selected habit |
| Appearance | Light and dark themes, system theme selection where supported, collapsing headers, shared card styling, scroll edge fades, and completion confetti |
| Reminders | Per-habit reminder time and weekdays, actions from notifications, optional sticky reminders, and Android notification customization |
| Daily check-in | Optional scheduled progress notification, an appear now action, and an Android 16 promoted progress presentation request |
| Widgets | Checkmark, history, score, streak, frequency, and numerical target widgets |
| Data tools | SQLite backup export, CSV archive export, compatible database imports, automatic backups, and a user-selected backup folder |
| Integrations | Locale and Tasker actions for checking, unchecking, toggling, incrementing, and decrementing habits |
| Accessibility and language | Accessible labels, system locale formatting, translated resources, right to left support, and declared Android app locales |

## Using Flow

1. Use the add button and choose a yes or no habit or a measurable habit.
2. Enter a name and optionally add a question, description, color, and icon. An empty icon falls back to the first visible character of the habit name when saved.
3. Choose a frequency or numerical target period. Measurable habits also have a unit and an at least or at most target.
4. Optionally choose a reminder time and the weekdays on which it should appear.
5. Save the habit and record answers using its day buttons. Numerical entries use a number editor. The answer editors also support notes for the selected date.
6. Tap the habit card to open its statistics and editable history calendar.

The home timeline adapts the number of day columns to the current window width and text size. Swipe or use the previous and more recent day buttons to browse up to 60 days. Returning from detail or recreating the screen preserves the viewed days; the history calendar provides access to older records.

Search matches the habit name, question, or description without case sensitivity. It does not inspect per-date entry notes. Closing search clears the text filter.

Long press a habit or choose Select visible habits from More to enter selection mode. The action bar shows the selection count and an Exit selection control. Select more habits for bulk actions, or use manual ordering to drag habits into place. The reorder handle also offers Move up and Move down. Selecting the same sort field again toggles its direction. The previous sort can remain as the secondary ordering rule.

Archiving preserves a habit and its history while excluding it from active summaries, reminder scheduling, and new widget selection. Enable archived visibility to find and restore it. Deleting removes the habit and its entries after confirmation.

## How progress works

### Answers and schedules

Yes or no habits distinguish a manual completion, a schedule-derived automatic completion, a missed day, a skipped day, and an unanswered day. Automatic completions allow frequencies such as three repetitions in seven days without requiring an action every day.

Measurable entries are stored as integers representing thousandths of a unit. For example, an entered value of `2.5` is stored as `2500`. Special values represent skipped and unanswered entries. The number editor accepts the current locale's decimal separator; saving a blank number restores the unanswered state.

### Scores and streaks

Habit strength blends recent performance with the previous score using the habit frequency. It changes gradually rather than resetting after a single missed day. Skipped days retain the previous score.

Each habit has a **Skips count towards streaks** switch in its create or edit screen. It defaults to off for existing and new habits. With it off, skips bridge completed days without increasing the streak length or starting a streak on their own. With it on, every skipped day adds one day and can start a streak, for both yes or no and measurable habits. Changing the switch recalculates the individual current and best streaks across existing history. It does not rewrite answers, change habit strength, or make a skipped day qualify as a perfect day.

An unanswered today allows a current streak through yesterday; an explicit unsuccessful entry today resets the displayed current streak. Trailing skips keep an existing streak current under either policy.

A perfect day requires every currently active habit to meet its completion rule. Skips and unanswered entries do not qualify. The home summary uses all active habits even when search or visibility filters hide some rows. Changing which habits are archived can therefore change the perfect day history shown in the summary.

At most habits intentionally remain open in the ordinary completed-today filter because later entries can still exceed the limit. Perfect day and individual streak cards compare a known numerical value with its target. The daily check-in uses a separate answered rule for at most habits, described below.

The detail screen also provides historical strength and bar charts, an editable history calendar with note indicators, weekday frequency, and best streaks. Tap a day with a note to read a tooltip containing its date and note, then choose Edit entry to change the answer or note. Opening or dismissing the tooltip never changes the entry, including when short press toggling is enabled. Calendar days expose dates, states, note availability, and editing to accessibility services. Numerical target summaries show totals for applicable daily, weekly, monthly, quarterly, and yearly intervals, with skipped days reducing the corresponding targets.

The optional delayed day boundary keeps the previous tracking day active until 3 am. The first weekday setting controls calendar week layouts and relevant chart grouping.

## Reminders and daily check-ins

Per-habit reminders use a chosen time and weekday selection. Boolean notifications provide yes and no actions; numerical notifications open the number editor. Sticky reminders are optional. Notification sound and channel behavior can be customized through Android settings. The snooze action is included on Android versions below 12.

The optional daily check-in is separate from per-habit reminders:

- Enable it in settings and choose a daily time, which defaults to 9 am.
- Use appear now to start a check-in immediately.
- It counts all non-archived habits, including habits without individual reminders.
- It stays active for up to one hour and shows completed progress briefly for ten seconds.
- It opens Flow so you can record the remaining answers.
- On Android 16 it requests promoted ongoing presentation using the native progress notification style. Actual presentation depends on the operating system, launcher, and notification settings.
- On earlier Android versions it uses a standard progress notification.

Check-in completion uses automatic or manual completion for boolean habits, the target threshold for at least habits, and any known entry for at most habits. Its date is the local calendar date at the start of the check-in, independently of the optional 3 am tracking boundary. These rules can make its count differ from the perfect day summary.

Android notification permission must be granted where required. The daily check-in falls back to an inexact alarm when exact alarm access is unavailable. Opening the habit list requests notification permission when habits with reminders exist.

## Home screen widgets

Add widgets through your launcher's widget picker and select a compatible active habit.

| Widget | Purpose |
| --- | --- |
| Checkmark | Show today's answer and habit strength; toggle a boolean answer or open numerical entry |
| History | Show the habit's calendar history |
| Score | Show habit strength over time |
| Streak | Show the habit's streak chart |
| Frequency | Show progress grouped by weekday |
| Target | Show progress toward a measurable habit's targets |

Widgets refresh after relevant habit changes and at the configured tracking day boundary. Their appearance includes rounded surfaces and configurable opacity. Completed checkmark widgets can use the habit color.

Widget content is rendered into a bitmap inside a launcher-hosted layout. Android 12 and later support the implemented background tint adjustment; older versions retain the layout's drawable surface. The current implementation provides translucency and does not implement wallpaper blur. Stack rendering support remains in the source, while the current picker selects one habit at a time.

## Data and backups

Flow stores habits, original entries, and per-date notes in a local SQLite database. Scores, computed entries, and streaks are derived from those records. Database schema 27 includes Flow's habit icon and per-habit skip counting fields. Compatible older backups migrate with skip counting disabled.

| Tool | Behavior |
| --- | --- |
| Full backup export | Share a SQLite database copy containing saved habits and entries |
| CSV export | Share a ZIP archive containing habit metadata, combined scores and checkmarks, and individual habit files |
| Daily notes in CSV | Included in each individual habit's `Checkmarks.csv` |
| Import | Recognize compatible Flow or Loop SQLite backups, Rewire databases, Tickmate databases, and HabitBull CSV files |
| Matching backup habits | Merge by stable habit identity and replace matching imported dates while keeping other dates |
| Automatic backups | Check when the home screen resumes and create a copy when the latest backup is more than a day old |
| Public backup folder | Use Android's folder picker to retain access to a user-selected document folder |
| Database repair | Rebuild habit ordering through the troubleshooting action |

Automatic backups otherwise use the app's backup folder. Rotation defaults to keeping five existing copies before writing a new one, so a run can leave six copies. In a selected shared folder, rotation only considers names matching `Loop Habits Backup ... .db`; this inherited name is still used for compatibility.

CSV files are for analysis and sharing. Flow's CSV archive is not a full restore format; use the database backup for restoring Flow data. Database exports do not include all application preferences or launcher widget placement. Imports from databases newer than the supported schema are rejected.

The manifest enables Android system backup and supplies a backup agent. Availability depends on device settings and the system backup provider. Exporting or sharing data is initiated by the user. The manifest does not request internet access; developer sync preference fields are inherited resources and do not establish a working account or cloud sync feature.

## Settings

| Group | Options |
| --- | --- |
| Appearance | Disable completion confetti and adjust widget opacity; theme switching is in the home menu |
| Habits | Short press toggling, delayed tracking day boundary, skips, unanswered markers, reverse day order, and first weekday |
| Notifications | Sticky reminders and Android notification channel customization |
| Daily check-in | Enable or disable, daily time, and appear now |
| Data | Full backup export, CSV export, import, and automatic backup folder |
| Troubleshooting | Generate a bug report for review in an external email app and repair database ordering |
| Support | Help, app rating, and about screen |

The pure black switch is currently hidden and disabled. Previously saved pure black selections use the normal dark theme. Developer settings and inherited links or translated strings may still refer to Loop; they should not be treated as published Flow services or distribution channels.

## Build and development

The release Android application ID is `dev.revilodev.flow`. Debug builds install separately as **Flow Test** with ID `dev.revilodev.flow.debug`, so Android Studio can run them alongside the Play Store app without replacing its data. Flow Test has its own database and starts with no habits; export a database backup from Flow and import a copy into Flow Test if you want to test with existing history. Existing source packages and the Android namespace remain `org.isoron.uhabits`.

The checked-in configuration uses Java 17, Gradle 8.11.1, Kotlin 2.3.20, Android Gradle Plugin 8.9.2, Android SDK 36, and a minimum Android version of 6.0 / API 23. The configured app version is `1.0.0` with version code `20304`.

Open the repository root in Android Studio and run the `uhabits-android` module, or use the wrapper from the repository root:

```powershell
.\gradlew.bat :uhabits-android:assembleDebug
```

```sh
./gradlew :uhabits-android:assembleDebug
```

The debug APK is written to `uhabits-android/build/outputs/apk/debug/Flow-Habits-Streaks-v1.0.0-debug.apk` for the current version. See [the build guide](docs/BUILD.md) for SDK setup, signing variables, release outputs, and inherited script limitations. Upstream Loop store badges were removed from this README because they identify a different application.

For verification:

```powershell
.\gradlew.bat :uhabits-core:jvmTest :uhabits-android:testDebugUnitTest
.\gradlew.bat ktlintCheck
```

The core also has a JavaScript target with browser tests; this does not provide a standalone web app. See [the test guide](docs/TEST.md) for browser and Android emulator checks.

## Documentation and repository resources

| Resource | Contents |
| --- | --- |
| [AGENTS.md](AGENTS.md) | Commenting expectations, repository map, invariants, and contributor notes |
| [Architecture](docs/ARCHITECTURE.md) | Startup, data flow, storage, calculations, UI, reminders, widgets, and extension points |
| [Build guide](docs/BUILD.md) | Toolchain, local builds, outputs, signing, and troubleshooting |
| [Test guide](docs/TEST.md) | Test locations, commands, emulator setup, and screenshot verification |
| [One UI design notes](docs/ONE_UI_DESIGN.md) | Applied rulebook decisions, component mapping, and device acceptance checks |
| [Developer guidelines](docs/GUIDELINES.md) | Scope, style, documentation, and review expectations |
| [Widget surface notes](docs/widget-blur-handoff.md) | Historical investigation of translucency and wallpaper blur |
| [Changelog](CHANGELOG.md) | Inherited release history and unreleased upstream notes |
| [Third party notices](NOTICE.md) | Bundled dependency notices |
| [Screenshots](screenshots/) | Inherited reference images and Tasker setup images |

The checked-in screenshots are inherited references and do not document every part of the current Flow design. The changelog is likewise not a complete Flow release history. Some older handoff details describe the state at the time they were written; current source takes precedence.

## Attribution and license

Flow is based on [Loop Habit Tracker](https://github.com/iSoron/uhabits). The original project and its contributors provide the foundation for the tracking engine and Android app.

Copyright (C) 2016-2025 Álinson Santos Xavier and contributors. Existing copyright headers and third party notices remain in the repository.

This program is free software under the GNU General Public License, version 3 or, at your option, any later version. It is distributed without any warranty, including the implied warranties of merchantability or fitness for a particular purpose. See [LICENSE.txt](LICENSE.txt) for the full license and [NOTICE.md](NOTICE.md) for third party notices.
