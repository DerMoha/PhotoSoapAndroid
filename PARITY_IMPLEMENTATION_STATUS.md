# Android / iOS feature parity

Implemented and verified on 2026-10-05. Scope: the gaps identified in the Android/iOS source comparison, with Android equivalents for platform-specific library features.

## Requirement audit

| Requirement | Implementation and evidence |
| --- | --- |
| Calendar progress | Year/month percentage, reviewed/total counts, progress bars and completion marks. Incomplete progress is capped at 99% for display. `ReviewProgressTest` and `ReviewViewModelTest` cover the boundary, library counts, persisted review history and completed periods. API 36 screenshots show 9/9 and 100% for the year and German month. |
| Favorites | Persisted hide-favorites setting defaults to true and filters both the deck and calendar/album counts. API 36 provider check: marking the disposable QA image favorite changed the total from 9 to 8; including favorites returned it to 9. The image’s original favorite flag was restored. |
| Filter accuracy | Date and album data use the same media-kind/favorites query as the review deck. Dates follow oldest/newest order. The All row uses the selected media kind. Reopening a year/month filter restores the relevant year. |
| Filter loading and empty states | Loading hides stale browse rows. Errors offer retry; unavailable years, months, albums and smart categories have explicit empty messages. A regression test exercises a null media cursor followed by a successful empty retry. |
| Smart albums | Counted Android categories: screenshots, videos, selfies, favorites, wide photos, GIFs and RAW photos. Only nonempty categories are listed. `SmartAlbumTest` checks metadata matching and wide-photo boundaries. The UI explains folder/name and aspect-ratio heuristics. |
| Separate media statistics | Persisted reviewed/kept/deleted/storage counters for photos and videos, including kept queue removals and confirmed deletion receipts. Room schema 3, migration 2→3 and repository mappings preserve existing totals. Migration, round-trip mapping, photo/video decision and confirmed deletion tests pass. German and English screenshots verify the two cards. |
| Achievement details | Locked achievements show progress; unlocked achievements show stored unlock dates. Repository/date-flow and view-model tests pass. |
| Completion | Filter-specific descriptions, current-pass reviewed/kept/confirmed-deleted counts, pending count/bytes, change-filter and delete-list actions, and limited-access management. The screen scrolls. API 36 QA: nine decisions showed 9 reviewed, 8 kept, 0 confirmed deleted and one queued item/303 KB. |
| Restart confirmation | Confirmation explains retained statistics and queued items, with a review-delete-list action. Restart keeps queued media excluded. The queued-item dialog was inspected and saved. |
| Completion after relaunch | An eligible library with no unreviewed items remains completed rather than showing an empty-library message. The regression test covers refresh and a recreated view model. |
| Preview interactions | Double-tap zoom/reset alongside existing pinch/pan, with accessible zoom state. `PhotoPreviewSheetTest.doubleTapZoomsAndThenResets` passes on the emulator. |
| Preview hint | First-use hint appears below the photo, disappears after opening preview, and persists that preference. The settings/event regression test and emulator UI inspection cover it. |
| Localization | Every new resource has English and German text. Resource names have no duplicates or missing German counterparts. German month/progress and statistics screens were visually inspected. |

## Verification

All final code checks passed:

```sh
./gradlew testDebugUnitTest testReleaseUnitTest lintDebug lintRelease assembleDebug assembleRelease
./gradlew connectedDebugAndroidTest
```

- 100 debug unit tests and 100 release unit tests passed.
- 11 instrumentation tests passed on PhotoSoap_Beta_API36, including both database migrations and double-tap zoom.
- Debug/release lint and APK assembly passed.
- `git diff --check` passed.
- UI checks used disposable emulator QA media; no media was deleted.

Screenshots are under `verification/emulator/`: `api36-parity-completion.png`, `api36-parity-restart.png`, `api36-parity-filter-complete.png`, `api36-parity-month-german.png`, `api36-parity-stats.png`, and `api36-parity-stats-german.png`.

## Platform and historical differences

Favorites are available through MediaStore on Android 11/API 30 and newer. Earlier versions show an explanation instead of an unsupported toggle. Gallery-specific private/cloud favorite states may not be shared with MediaStore.

Android does not expose Apple Photos smart-album classifications. The curated Android categories use available MediaStore metadata and documented heuristics; they do not claim Apple’s classification coverage.

Existing combined historical counters remain intact. Old reviews cannot reliably be divided into photos/videos, so the separate counters start with this update; the statistics screen explains this when historical reviews are present. Unclassified legacy deletion items stay in combined totals instead of being guessed as photos.
