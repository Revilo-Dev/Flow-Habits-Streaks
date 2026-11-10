# Testing the project

Loop Habit Tracker has a fairly large number of automated tests to reduce the chance of bugs being silently introduced in our code base. The tests are divided into three categories:

- **Unit tests:** These tests run very quickly on the developer's computer, inside a JVM, and do not need an Android emulator or device. They typically test the correctness of core functions of the application, such as the computation of scores and streaks.
- **Instrumented tests:** These tests require an Android emulator or device. _Medium_ instrumented tests are still quite fast to run, since only individual classes are tested. The app itself does not need to be launched. Examples include _view tests_, which render our custom views on the device and compare them against prerendered images. _Large_ instrumented tests launch the application on an Android emulator and interact with it by touching the screen, much like a regular user.

## Running unit tests

Unit tests can be launched by running `./gradlew test` or by right-clicking a particular class/method in Android Studio and selecting "Run testMethod()" or "Run ClassTest". An alternative way is to use `build.sh`, the script used by our continuous integration server. By running `./build.sh build`, the script will automatically build and run all small tests.

## Running instrumented tests

To run medium tests, it is recommended to use the `build.sh` script.

1. Run `./build.sh android-setup API` to create the emulator, where `API` is the desired API level.
2. Run `./build.sh android-tests API` to run the tests on a single API.
3. Run `./build.sh android-tests-parallel API API...` to run the tests on multiple APIs in parallel.

Note that instrumented tests are designed to run on a clean install, inside an emulator. They will not work on actual devices. All tests are also designed for a particular screen size, namely the Nexus 4 configuration (4.7" 768x1280 xhdpi), and a particular locale, namely English (US). Furthermore:

- No additional apps should be installed on the device;
- The homescreen must look exactly like it was when the emulator was originally created, with no additional icons or widgets;
- All animations must be manually disabled.

If there are failing view tests (that is, if some custom views do not render exactly like the prerendered images we have), then both the actual and expected images will be automatically downloaded from the device to the folder `uhabits-android/build/outputs`. After verifying the differences, if you feel that the actual images are actually fine and should replace the prerendered ones, then run `./build.sh android-accept-images`.

## Entry dialogs

`EntryDialogTest` checks immediate answer actions, Cancel, optional skip visibility,
centered and bottom placement, strict localized numeric input, and saving a restored
note to its original viewed day. `FlowSurfaceTest` renders the shared entry surface
in light, dark, and legacy pure black themes. These tests belong on an emulator;
they must not be used to reset a personal release installation.

`FlowHeaderBehaviorTest` checks that reaching the top leaves the large title
collapsed until a second touch and that the collapsed scrolling viewport reaches
the window edges. On a Samsung device, check fast flings in Home, habit details,
and Settings, then compare the top fade and control glass with blur enabled and
disabled. Change the system and custom accents and confirm that the calendar icon
continues to match its habit colour.

`StreakProgressTest` covers current and previous perfect streaks, archived habits,
numerical at-most targets, empty histories, the global perfect-streak skip setting, and the distinction between a skipped
day in a habit streak and a perfect day, including the unsaved series supplied to
the shared calendar and completion chart. `StreakProgressCardsTest` checks shared
card icon colours, the score-free overview, and the read-only calendar.
`FlowSwitchTest` checks direct switch taps,
saved preference state after rebinding, and a circular thumb at the measured size.

Toggle **Count skips towards perfect streak** and verify that the home summary and
Perfect streaks calendar agree while completion totals stay unchanged. Check that
the per-habit daily streak switch remains independent and is available even when
entry skip actions are disabled. Both skip-counting settings default to off.

For the streak screen, open the home card, swipe to older calendar dates, rotate the
device, open a habit summary, and check previous-streak pagination. Verify menus
including nested sort choices, checked options, dismissal and Settings dropdowns
with blur enabled and disabled in both themes. Confirm the overflow opens over
its trigger, the top highlight fades down the edge, and the final Settings card
scrolls clear of the bottom fade. Check that both habit dialogs scroll at large
font sizes and that streak card content remains readable over the purple backdrop. Device
rendering and touch checks are still needed when no emulator is available.
