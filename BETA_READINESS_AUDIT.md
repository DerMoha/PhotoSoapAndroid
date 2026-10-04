# PhotoSoap Android beta readiness audit

> This is the original audit snapshot. Code fixes and current verification are recorded in [BETA_IMPLEMENTATION_STATUS.md](BETA_IMPLEMENTATION_STATUS.md). Source line numbers below refer to the audited snapshot.

Audit date: 2026-10-03. Scope: current working tree, including existing uncommitted changes. No application code was changed during this audit.

## Verdict

**Hold external beta distribution for now.** The main features are implemented, but permission handling and settings behavior have concrete defects. Accessibility/layout, deletion lifecycle, and video playback still require device validation. This is a source review with historical test evidence, not a completed end-to-end certification.

## Verification evidence and limits

- Inspected onboarding, permission handling, navigation, review gestures/buttons, deletion queue and recovery, filters, media previews, stats, achievements, settings, manifest, build configuration, and tests.
- Existing debug results: 70 tests, zero failures/errors, dated August 12, 2026. Existing release results: 70 tests, zero failures/errors, dated August 8, 2026. These do not establish that today's working tree passes.
- Existing debug lint report has 38 warnings and no errors; warnings concern dependency/plugin updates. It is also historical.
- English and German string resource files each contain 137 names, with no missing German names. This checks key coverage, not translation quality or layout.
- Fresh `testDebugUnitTest lintDebug assembleDebug assembleRelease verifyProductionConfiguration` could not start because the sandbox forbids writing the shared Gradle wrapper lock. A second attempt using the installed Gradle with a writable temporary user home failed because Gradle's lock service could not open a socket. No fresh build, lint, signing check, or tests completed.
- ADB could not start its server because opening its listener is prohibited. No live emulator/device interaction, instrumentation tests, screenshot capture, or destructive deletion tests were performed.
- Reviewed `device-screenshot-latest.png`: German dark-mode review view has clear actions and navigation, but a blank video card. Its date/build provenance is unknown, and current code now supplies loading/error placeholders and a video decoder; this image cannot prove the current implementation is broken or fixed.
- Release signing fields are absent from local configuration; optional metrics fields are present. Secret values were not exposed. A signed distributable release is not configured.

## Findings, ordered by beta impact

### 1. High: denied storage access is incorrectly accepted on Android 9–12

Source: `PermissionChecker.kt:26–48`.

For API levels below 33, the video permission is unconditionally treated as granted. When READ_EXTERNAL_STORAGE is denied, `hasImages` is false but `hasVideos` is true, so access becomes LIMITED and `hasMediaPermissions()` returns true. The app enters review instead of requesting access. The media query can then fail and be presented as an empty library.

Fix: use the shared storage permission for both images and videos on API 28–32. Add denied/granted regression coverage across the permission branches. On API 28, also verify write permission behavior when deletion is requested.

### 2. High: media access changes are not refreshed while the app is in Main

Source: `MainViewModel.kt:74–78`; `SettingsScreen.kt:95–108`.

`onResume()` rechecks permissions only in PermissionDenied. Opening app settings from Main and changing from limited to full access leaves `isLimitedAccess` stale; a permission revocation is also not reflected by this path. This directly affects the advertised Manage Access flow. OS process termination may sometimes hide the defect, but the app should not depend on that.

Fix: refresh permission state when returning from settings and while resuming Main. Refresh the media deck after access changes, including selected-media changes that retain the LIMITED category. Test limited-to-full, selected-item additions/removals, and revocation.

### 3. High: delete-list preference changes do not update the existing review session

Source: `ReviewViewModel.kt:673–676`; `SettingsViewModel.kt:54–55`.

Review reads `useDeleteQueue.first()` once at initialization. Settings updates the repository, but Review does not collect that preference. Returning to the existing Review destination therefore keeps its prior deletion behavior until its ViewModel is recreated. A setting that controls deletion timing must match actual behavior.

Fix: observe the preference continuously. Define how existing queued items behave when switching to immediate deletion, and verify both switch directions without restarting the app.

### 4. Medium: Haptics setting has no application interaction wired to it

Source: `util/Haptics.kt`; `SettingsScreen.kt:74–80`.

The controller and composable helper exist, but a project-wide source search found no callers outside their definitions. The toggle persists, yet swipes, buttons, and completion feedback do not use it.

Fix: connect haptics to intended interactions and verify enabled/disabled behavior on a physical device, or remove the setting until the feature is functional.

### 5. Medium: filter sheet is likely to clip browsing controls

Source: `FilterSheet.kt:64`, `:226`, `:255`.

A non-scrollable outer Column contains six radio-option rows, headers/dividers, an All row, and separate fixed 200dp year and album lists. The total exceeds common phone sheet heights before accounting for large text or landscape. Later controls may become unreachable. This is a source-derived layout risk; it was not reproduced live here.

Fix: use one bounded scrollable layout for the complete sheet, retaining usable list interactions. Verify compact phones, landscape, German, and 200% font scale.

### 6. Medium: date filters mishandle media without DATE_TAKEN

Source: `ReviewViewModel.kt:795–821`, `:850–872`, `:945–987`; compare `PhotoCardContent.kt:47–52`.

The card displays DATE_ADDED when DATE_TAKEN is missing, but years/months, date filtering, and date sorting use DATE_TAKEN exclusively. Imported/downloaded media with zero DATE_TAKEN appears in 1970 during year discovery and will not appear under the year shown on its card.

Fix: consistently use an effective date with DATE_ADDED fallback and correct milliseconds/seconds conversion. Test imported media and zero/null dates.

### 7. Medium: query failures are shown as an empty library

Source: `ReviewViewModel.kt:904–908` and `ReviewScreen.kt` empty-state branch.

Any non-cancellation query exception becomes `emptyList()`. Permission failures and provider failures therefore look like successful empty results, with no useful retry/error state.

Fix: represent loading, true empty results, permission failures, and other errors distinctly. Provide a retry action and access recovery where appropriate.

### 8. Medium: video playback does not explicitly follow lifecycle

Source: `PhotoPreviewSheet.kt:153–165`.

The player starts automatically and is released only when the composable is disposed. There is no explicit pause on background/stop, nor an application-level player-error state. Playback may continue while the preview remains composed in a background activity. Confirm device behavior rather than assuming disposal on background.

Fix: observe lifecycle to pause/resume intentionally, and provide visible playback-error feedback. Test Home, screen locking, interruptions, rotation, missing media, and unsupported codecs.

### 9. Medium: “Reviewed today” can display yesterday's value

Source: `StatsViewModel.kt:30–45`, `StatsScreen.kt:143`; Review's daily-count normalization occurs during `advanceStats()`.

The stats screen displays persisted todayReviewCount without checking todayDate. Reopening the app on a new day before reviewing another item can show yesterday's count as today's.

Fix: derive displayed daily values from the stored date and current local date. Verify midnight rollover and time-zone changes.

## Features and current confidence

| Feature | Assessment |
| --- | --- |
| Onboarding | Implemented; analytics defaults off; scroll and large-font adaptation exist. Visual redesign remains explicitly requested in POLISH_BACKLOG.md. |
| Full/limited/denied media access | Implemented with high-priority defects above. Not beta-ready across supported Android versions. |
| Swipe and button review | Implemented; real gesture/button interleaving, rapid actions, TalkBack, and rotation need device checks. |
| Delete queue, remove, clear, undo | Implemented and persisted. Undo converts a queued item to kept; it does not return to the preceding card. Validate that this matches expected product behavior. |
| System/legacy deletion | API-specific paths, partial-success handling, journal recovery, and idempotent stats are implemented. Real OS confirmation flows remain unverified. |
| Media type/date/album/sort filters | Implemented; layout and missing-date issues above. Shuffled queries need provider/device validation. |
| Photo preview | Zoom/pan and load/error UI implemented. Large images, orientation, gesture conflicts, and bounds require device QA. |
| Video preview/thumbnails | Decoder and player implemented; current device behavior and lifecycle/error handling require verification. |
| Stats/daily challenges | Implemented with historical tests; daily display normalization needs correction. |
| Achievements | Implemented, persisted, and localized; historical tests exist. Verify thresholds and toast visibility through actual review/deletion. |
| Settings | Analytics/delete/haptics toggles and support/privacy actions exist; delete preference and haptics are incomplete as described above. Support silently does nothing if no email app handles the intent. |
| German/dark mode/accessibility | String-key coverage and theme implementation exist. Device validation at 200% fonts and with TalkBack is outstanding. |
| Analytics | Opt-in control and configuration exist; production ingestion, offline recovery, and opt-out require end-to-end verification. Backend behavior was not audited. |
| Release packaging | Version 1 / 1.0.0 and application ID com.photosoap configured. Signing is not configured; current release artifact cannot be certified. |

## Deletion-specific validation required before external beta

Source inspection shows substantial defensive work, but unit coverage does not establish safe behavior under the OS deletion UI. Existing ReviewViewModel tests mainly cover initialization/state events and two queue-recovery cases.

Test with disposable media:

1. API 28 direct deletion and denied write access; API 29 per-item permission requests and partial success; API 33/34/36 system batch confirmation.
2. Confirm and cancel, including multiple consecutive requests, stale queued items, and very large queues.
3. Rotate during the system dialog. `pendingDeleteIntentSender` remains set until the result while a composition-scoped LaunchedEffect launches it; verify it cannot launch a second request on recreation.
4. Kill/restart during approval and after deletion. Verify queue recovery and exactly-once deleted count/storage accounting.
5. Confirm that results are matched to the originally requested item IDs. `finishDeletion()` currently reads the live queue instead of the persisted request snapshot; protect against queue changes and overlapping requests.
6. Revoke/restrict access with an existing queue and interrupted request. Ensure inaccessible items are not mistaken for confirmed deletions.
7. Rapidly tap keep/delete while a swipe animation is active, then change filters. Verify no skipped, duplicated, or unintended decisions.

These are explicit unverified risks, not claims of device reproductions.

## Exit criteria

- Correct high-priority permission and deletion-preference defects; address haptics, filter usability, missing dates, error states, video lifecycle, and daily stats.
- Run fresh debug and release unit tests, lint, assembly, and instrumentation migration tests in an environment where Gradle/ADB can operate.
- Complete the supported-API disposable-media matrix above and accessibility/localization checks using the actual signed beta build.
- Configure release signing and select the beta version/application ID. Produce and inspect the signed APK/AAB.
- For Play distribution, complete RELEASE_CHECKLIST.md, including public privacy policy and data-safety/listing work. The checklist records previously failing privacy URLs; their current HTTP status was not verified during this audit.
- Validate optional production analytics or disable it for the beta if it cannot be validated.

A tightly controlled internal test can be useful after the high-priority fixes. An external beta should wait for fresh automated results and deletion/permission device evidence.
