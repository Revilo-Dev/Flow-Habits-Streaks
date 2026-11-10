# Developer guidelines

These guidelines apply to the Flow fork in this repository. [AGENTS.md](../AGENTS.md) records the repository-wide commenting rules and implementation notes.

## Scope and review

Keep changes focused on the requested behavior. Preserve unrelated local work. Avoid mixing fixes with dependency upgrades, broad formatting, identifier renames, or feature redesigns.

Use the target branch and review process established for this repository. The inherited upstream Loop branching and release process is not automatically Flow's process. Flow's checked-in application ID is `dev.revilodev.flow`.

Describe the concrete problem, resulting behavior, and verification in a change description. Explain meaningful compatibility constraints and identify any checks that could not run. Separate current implementation from plans or unverified device behavior.

## Code and comments

Use Kotlin for new app logic and follow the style of the existing module. The Android app uses Views, custom views, XML resources, and view binding. Shared business rules belong in the core module; Android-specific behavior belongs in Android code or an appropriate platform adapter.

Use simple explanatory comments:

```kotlin
// Keep skipped days from increasing the streak length
// Refresh widgets after the habit change is saved
```

Keep each explanation on one line with no emojis or punctuation in its text. Do not introduce multiline or block explanations. Explain intent or a nonobvious rule instead of restating assignments. Preserve copyright, license, attribution, and necessary tool directives. Detailed explanations belong in the Markdown guides.

Kotlin formatting is checked with `ktlintCheck`. Review reported files before applying any formatter so unrelated existing formatting is not swept into the change.

## Behavior and data

Use the existing command and observer paths for normal habit edits so the list, widgets, and notifications stay synchronized.

Keep original entries distinct from computed entries, scores, and streaks. Numerical values use thousandths of a unit, with dedicated skip and unknown sentinels. Read each consumer's completion rules before changing shared predicates.

Changes to storage may require a migration, schema version update, repository mappings, model mappings, import changes, and tests. Preserve stable habit UUIDs during imports and do not replace compatibility filenames without checking backup discovery.

Review the manifest, intent factories, parsers, and receivers together for integration changes. Application ID, source namespace, and URI authorities can differ.

## Appearance and resources

Reuse Flow colors, dimensions, styles, drawables, and shared header and scroll helpers. Check both light and dark mode and account for system bars, keyboards, accessibility labels, and font scaling.

Put user-visible strings in resources. Review relevant localized resources when adding or changing strings. Translation lists are stored in `translators-classic.csv` and `translators-crowdin.csv`; `gradle/translators.gradle.kts` generates translator credits.

Do not claim wallpaper blur based on a drawable name or translucent background. Do not advertise pure black mode while its control remains disabled. Distinguish the Android 16 promoted notification request from verified presentation on a particular phone.

## Documentation

Update [README.md](../README.md) for new features, changed settings, limitations, or installation steps. Update [ARCHITECTURE.md](ARCHITECTURE.md) when ownership or data flow changes.

Keep [BUILD.md](BUILD.md) aligned with the wrapper, dependency catalog, SDK levels, signing environment variables, and generated output names. Update [TEST.md](TEST.md) when test setup or locations change.

Treat the changelog, screenshots, store listings, translated strings, and widget handoff as inherited or historical resources where applicable. Verify claims against current source. Do not redirect Flow users to upstream distribution or support channels as if they were Flow-specific.

## Verification

Choose checks for the changed behavior using [TEST.md](TEST.md). For core logic run the relevant JVM tests; for Android changes run the Android JVM tests or relevant instrumented tests and compilation as appropriate.

For documentation and comments, check source accuracy, local links, comment style, and that executable content is unchanged. Do not add tests that merely mirror implementation details or tests solely to cover a comment edit.

Record existing formatting failures separately from failures introduced by a change. Do not silently replace screenshot baselines or describe an unavailable device check as passed.

## Licensing and releases

Flow is based on Loop Habit Tracker and remains GPL version 3 or later. Preserve [LICENSE.txt](../LICENSE.txt), [NOTICE.md](../NOTICE.md), and original file notices.

Keep credentials, signing keys, passwords, and local SDK configuration out of tracked files. Release signing uses the inherited LOOP environment variable names documented in [BUILD.md](BUILD.md). Building an artifact and publishing it are separate actions.
