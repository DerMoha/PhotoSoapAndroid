# External beta implementation status

Date: 2026-10-04. Candidate version: **1.0.0-beta.1**.

**The audited defects are fixed and the signed beta candidate passes the normal Android build and core emulator acceptance matrix.** The local upload key is configured. Privacy and support pages are live. Play Console setup and off-device key backup remain outside the completed local validation.

## Implemented changes

| Audit issue | Implementation |
| --- | --- |
| Legacy permission denial accepted | Android 9–12 use the shared storage grant for both media kinds; denied access stays denied. Permission-category regression tests cover APIs 28–36. |
| Access changes stay stale | Main refreshes permission categories on resume; Review refreshes the deck and browsing data on resume. |
| Delete-list settings stay stale | Review observes the live setting. Existing queued decisions stay queued when the mode changes. Older Android immediate mode shows an explicit app confirmation before deletion. |
| Haptics toggle has no effect | Review buttons, swipes, undo, and notifications now use a controller observing the live preference. |
| Filters clip | All filter sections scroll together, without fixed-height nested lists. Media/sort radio rows expose a single accessible selection action. |
| Missing capture dates | Cards, year/month browsing, filtering, and sorting consistently fall back from DATE_TAKEN to DATE_ADDED in milliseconds. Shuffle runs in memory rather than relying on SQL expressions accepted by particular providers. |
| Query errors look empty | Permission errors and provider failures have separate recovery states with retry/access actions. A null provider cursor is treated as failure. |
| Video background playback/errors | Playback pauses on lifecycle pause, resumes only if previously playing, releases on disposal, and displays errors. Image pan bounds account for both dimensions, and zoom state is keyed to the media item. |
| Yesterday's count shown today | Daily display values check their stored date and refresh on resume and periodically while composed. |

Additional deletion safety:

- Review actions carry the displayed media URI; stale gesture callbacks cannot apply a decision to the next card. Deck advancement is synchronous, duplicate decisions are rejected, and canceled drags snap back.
- Queue persistence is ordered and joined before a deletion journal is created.
- Deletion requests use frozen snapshots and batches of at most 1,000 items. Queue mutations, review decisions, and overlapping requests are blocked while deletion is active; the UI shows a waiting state.
- Intent senders are consumed before launch to prevent launching the same request again on rotation. Launch failures restore an actionable queue.
- Saved request IDs match restored activity results to their original batch. A result for an unrelated request is ignored.
- Android 10 partial success survives a canceled permission request; successful items are counted and remaining items stay queued.
- Confirmed receipts, including item metadata, are persisted before counters or queue removal. Recovery does not infer deletion from inaccessible media. Confirmed receipts can recover under limited access.
- Recovery accounts for confirmed deletions before dropping historical queue data, preserving the existing idempotent stats/metrics operations.

For Android 11+ system deletion, a successful result applies to the frozen requested snapshot. Android documents that the operation finishes before RESULT_OK is delivered; this avoids treating lost selected-media visibility as a failed deletion. See the [MediaStore API reference](https://developer.android.com/reference/android/provider/MediaStore#createDeleteRequest(android.content.ContentResolver,%20java.util.Collection%3Candroid.net.Uri%3E)).

Additional polish and release work:

- Onboarding uses theme-aware text contrast, a library identity mark, readable benefit rows, and deletion-safety copy. Android emulator visual review is complete; physical-device review and iOS comparison remain outstanding.
- Support shows the email address if no email client can handle the action.
- English and German string resource names remain aligned (144 each).
- Optional metrics configuration can be omitted entirely; configured metrics require an HTTPS endpoint and client key. Production checks reject secret-key prefixes and legacy service-role JWT claims without exposing values.
- CI now requests debug/release unit tests and lint, and retains verification reports.
- `scripts/verify-beta.sh` runs the complete normal release verification sequence; optional connected migration tests are enabled with `PHOTOSOAP_RUN_DEVICE_TESTS=1`.

## Verification completed

- Normal JDK 17 Gradle verification passes: debug/release unit tests, debug/release lint, debug/release assembly, production configuration validation, and signed release App Bundle.
- **86 debug unit tests and 86 release unit tests pass**, with zero failures, errors, or skips. This replaces the earlier standalone-run evidence.
- Debug and release lint each have **zero errors**. The 39 warnings concern dependency and Android Gradle plugin version updates.
- Room migration instrumentation passed on Android 16/API 36.1: existing stats preserved and deletion ledger created.
- Signed release iterations installed and exercised on dedicated API **28, 29, 33, 34, and 36.1** emulators with disposable generated images/video. No user media was deleted. The final compact-height/gesture pass is verified on API 36; the other core matrix checks were completed during this iteration before those final UI-only adjustments.
- Core checks: first run, denied access, recovery to full access, previews and date fallback, keep/delete-list decisions, queue persistence across restart, approved deletion, and statistics accounting. API 28 denied write access fails safely without deleting media.
- API 36 additionally covers undo, canceled system confirmation, rotation during confirmation, preference changes without restarting, video previews, dark mode, German, and 200% text. Android reports video audio started → paused after Home → started on resume.
- API 34 selected-media picker: selecting one photo and one video produces two visible items and a limited-access banner. Manage opens app settings, and revoking access returns to the permission-recovery screen.
- New findings from visual/accessibility QA were fixed: Settings rows now own their switch action/state and labels; navigation labels stay on one line; compact-height review scrolls instead of collapsing the media card. Direction-aware touch-slop handling lets vertical scrolling reach that content without triggering preview taps; compact scroll position survives library refreshes. Final landscape screenshots show media metadata and both decision buttons, and horizontal keep/short canceled drags were verified.
- Real app-to-backend testing found and fixed omitted JSON default fields: Android platform, install-registration flag, and build metadata are now sent explicitly. Added payload-contract regression tests. Real app-to-production integration now records platform=android, version=1.0.0-beta.1, build=1, and registered_install=true. App opt-out clears the stored identifier, pending metrics, and upload timer.
- The existing PhotoSoap Supabase project was restored from paused state. HTTPS registration and daily-total submissions return 200; sending identical totals twice produces one stored record. Synthetic test records were removed afterward.
- [Privacy policy](https://dermoha.github.io/PhotoSoap/privacy.html) and [support](https://dermoha.github.io/PhotoSoap/support.html) return **HTTP 200** after [the Pages fix](https://github.com/DerMoha/PhotoSoap/pull/1) was merged and deployed.

Evidence is under `verification/`: Gradle log, API smoke notes, emulator screenshots, video playback states, public upload certificate, and artifact checksums. The original audit remains a historical snapshot.

## Signed artifacts and key

- Play bundle: `app/build/outputs/bundle/release/app-release.aab`.
- Installable release APK: `app/build/outputs/apk/release/app-release.apk`.
- Package `com.photosoap`, version code **1**, version **1.0.0-beta.1**, minimum API **28**.
- APK signature verified. App Bundle reports `jar verified`; jarsigner also reports the expected self-signed certificate/no timestamp and ZIP-stream manifest ordering warnings from the Gradle-produced bundle.
- Upload certificate SHA-256: `c4198e9657a2d75cb6dd41649d5be1521f5baa2c79ed21c346e161d1b5aa8b11`.
- New RSA 3072 PKCS12 upload key is in ignored `.signing/photosoap-upload.p12`; passwords are only in ignored `secrets.properties`. Private local recovery copies and backup instructions are in `.signing/recovery/` and `.signing/BACKUP_INSTRUCTIONS.txt`.
- **The recovery copy is on the same computer. Copy the key and signing credentials to secure off-device storage before publication.** Do not commit or publicly share them. `verification/photosoap-upload-certificate.pem` is the public certificate.

## Remaining release acceptance and account steps

1. Complete Play Console listing, data-safety form, broad photo/video permission declaration, content rating, store artwork/screenshots, and external testing setup. Upload the signed AAB through the correct developer account; review Play's pre-launch report and any policy feedback.
2. Securely back up the new upload key off-device and confirm `com.photosoap` is the intended permanent Play package.
3. Perform a physical-device pass for tactile haptics and a full spoken TalkBack walkthrough. TalkBack service binding and UI semantics were inspected on the emulator, but this does not establish spoken announcements or hardware feedback quality.
4. Extended safety scenarios have regression/unit coverage but were not exhaustively reproduced on devices: queues over 1,000 items, API 29 cancellation after partial success, process death at every receipt boundary, midnight rollover, and unusual corrupt/very large media or vendor-specific providers. Emulator process-kill commands were exercised during confirmation, but process replacement was not independently proven in that run.

This is a verified signed beta candidate, not proof that every device or media library is defect-free. External testers and Play's pre-launch checks should validate the remaining hardware/provider scenarios.
