# Flow interface design notes

Flow applies the user supplied One UI 9 habit app rulebook to its existing Android Views interface. The rulebook combines Samsung guidance, Android guidance, and proposed habit app choices. Its sample toolkit classes belong to a separate library and are not dependencies of this repository.

This implementation keeps Flow's single habit list, detail pages, and Material compatible Flow theme. Today is the stable home title. The proposed three destination navigation, timers, cloud synchronization, and separate Insights page remain outside the implemented feature set.

## Shared contract

| Area | Flow choice | Rulebook references |
| --- | --- | --- |
| Layout | Shared 24 dp content keyline and a centered content column up to 840 dp | BASE05, PAGE01, ADAPT01, ADAPT02 |
| Headers | 32 sp expanded and 20 sp compact titles with persistent collapsed identity; compact headers in short windows | HEAD04, HEAD06, HEAD08, ACCESS03 |
| Actions | Targets of at least 48 dp; measured editor and home action clearance; snackbars above active actions | CTRL08, ACT06, ACT08, DEPTH09 |
| Habit rows | Row opens detail; trailing controls record the selected date; wrapping habit names and explicit accessibility states | LIST03, LIST10, LIST11, ACCESS07 |
| Selection | Labeled actions, selected count, explicit exit, and a horizontally scrollable action region | ACT01, MENU08, LIST12 |
| Reordering | Drag in manual order or use Move up and Move down from the reorder handle | GEST04, ACCESS11 |
| Search | Separate Clear query and Close actions; query and mode restored with the activity; distinct empty and filtered states | SEARCH02, SEARCH04, SEARCH05, PAGE06 |
| Timeline | Visible date navigation buttons and columns adapted to available width and font size | PICK05, GEST12, ADAPT01 |
| Drafts | Save commits; Cancel and Back share dirty draft handling; valid input survives errors and activity recreation | FORM05, FORM07, FORM08, DIALOG05 |
| Entry dialogs | Bottom placement with constrained width, scrolling content, localized numeric validation, and retained drafts | DIALOG07, DIALOG10, FORM09, ADAPT06 |
| Calendar | Day labels include date, state, and note availability; noted days preview before explicit editing; a textual legend accompanies history | ACCESS09, HABIT03, PICK05 |
| Colors | Semantic Flow foreground and surface roles, readable on accent actions, stronger supporting text contrast | THEME04, THEME09, ACCESS04 |
| Effects | Existing scroll edge color fades remain color fades; no wallpaper or pixel blur claim | DEPTH02, DEPTH04 |

The calendar remains a dense history chart rather than a full size date picker. Individual virtual day nodes and the explicit edit path provide access to its records; physical cell targets are smaller than the standard action buttons.

The listed dimensions are this app's chosen starting values from the rulebook. They are not claims about exact Samsung One UI 9 component measurements.

## Main implementation resources

| Resource | Responsibility |
| --- | --- |
| [Flow dimensions](../uhabits-android/src/main/res/values/flow_dimens.xml) and [styles](../uhabits-android/src/main/res/values/flow_styles.xml) | Shared sizing and type hierarchy |
| [View helpers](../uhabits-android/src/main/java/org/isoron/uhabits/utils/ViewExtensions.kt) | Window insets, measured action clearance, and snackbar anchoring |
| [Home root](../uhabits-android/src/main/java/org/isoron/uhabits/activities/habits/list/ListHabitsRootView.kt) | Header, adaptive timeline, search, selection, and floating action layout |
| [Habit editor](../uhabits-android/src/main/java/org/isoron/uhabits/activities/habits/edit/EditHabitActivity.kt) | Draft preservation, discard handling, skip policy, and validation |
| [Entry support](../uhabits-android/src/main/java/org/isoron/uhabits/activities/common/dialogs/EntryDialogSupport.kt) | Dialog sizing and restoration with saved habit and date identity |
| [Note popup](../uhabits-android/src/main/java/org/isoron/uhabits/activities/common/views/EntryNotePopup.kt) | Anchored date and note preview, Close, and Edit entry |
| [Accessible calendar](../uhabits-android/src/main/java/org/isoron/uhabits/activities/common/views/AccessibleHistoryChartView.kt) | Virtual day nodes for keyboard and screen reader interaction |

## Streak policy

The habit editor exposes **Skips count towards streaks** for both tracking types. The default is off, preserving previous lengths. Off bridges skipped dates without adding days. On adds each skipped date and allows skips to start an individual streak. Changing the policy recalculates existing individual streak history. Skips remain visually distinct from completed dates and never make a perfect day. Habit strength retains its existing skip handling.

A calendar note preview is read only. Opening or closing it must never invoke short press completion, even when short press toggling is enabled. Edit entry opens a separate draft dialog for that habit and date.

## Verification and release checks

Automated coverage is described in [TEST.md](TEST.md). View rendering at 100 and 200 percent font scale checks input and creation surfaces in each Flow theme. Feature tests cover skip policy drafts and saving, note previews, and returning to the same timeline days.

Verification completed on 15 October 2026:

| Check | Result |
| --- | --- |
| Core JVM suite | 290 tests passed |
| Android JVM suite | 5 tests passed |
| Feature and Flow surface instrumentation | 6 tests passed |
| Build | Debug app and Android test APKs built successfully |
| Emulator | Pixel 10 Pro AVD, Android 17 API 37, build CP31.260623.009, English US, 1280 by 2856 at 480 dpi |
| Text rendering | Input, creation, and About surfaces at 100 and 200 percent in light, dark, and legacy pure black themes |
| Source style | Edited feature files pass; the repository check retains 84 existing issues in other files |

The device checks cover skip policy drafts and saving, read-only note preview with explicit editing, calendar viewport restoration, home date and collapsed header restoration, and numeric validation with restored save context. Render captures were visually reviewed for note preview, numeric entry, and enlarged actions.

Before claiming One UI 9 device compatibility, record the actual Samsung model, OS build, window dimensions, navigation mode, font scale, and theme with comparison captures. An Android emulator build and a matching visual style do not establish Samsung specific behavior.

The full rulebook remains the acceptance baseline. These checks still need the intended device and input environments:

- TalkBack traversal, announcements, and focus restoration through menus and dialogs
- Predictive Back preview completion and cancellation on the intended OS build
- Physical fold and hinge avoidance, folded cover display, split screen, and DeX resizing
- Keyboard, mouse, stylus, right to left text, and long translations in every critical journey
- Light and dark contrast across custom habit colors, chart labels, and disabled controls
- Notification permission denial, exact and inexact reminder delivery, widget refresh, and promoted notification presentation
- Reduced motion and haptic preferences with real system settings

Flow currently offers light, dark, and system appearance. Wallpaper derived palettes and the separate toolkit's glass components are not implemented features. Platform target upgrades and notification delivery require their own checks apart from this visual migration.
