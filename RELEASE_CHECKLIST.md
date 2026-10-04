# Android release checklist

## Required before Play submission

- [ ] Choose the final application ID. It is currently `com.photosoap` and cannot be changed after publishing without creating a new Play listing.
- [x] Beta candidate uses version code 1 / `1.0.0-beta.1`; increase the version code for subsequent Play uploads.
- [x] Configure the new local upload key and pass `verifyProductionConfiguration`.
- [ ] Copy the key and credentials to secure off-device backup storage.
- [x] Run `./scripts/verify-beta.sh` with JDK 17.
- [ ] Test the signed release on API 28, 29, 33, 34, and 36, including full, limited, denied, and revoked media access.
- [ ] Verify keep, queued delete, undo, partial legacy deletion, system-confirmed deletion, rotation, dark mode, German, 200% font scale, TalkBack, and process restart.
- [x] Publish and verify the privacy/support pages. `https://dermoha.github.io/PhotoSoap/privacy.html` returns HTTP 200.
- [ ] Enter and verify the prepared answers in `PLAY_STORE_DATA_SAFETY.md` in Play Console.
- [ ] Upload the localized title, descriptions, and release notes from `fastlane/metadata/android/`; provide the support email, feature graphic, phone screenshots, app icon, and content rating.
- [x] Test real-app analytics opt-in/upload/opt-out against the production endpoint using the configured client key; remove synthetic test records.
- [ ] Review the pre-launch report, Android vitals, crash/ANR reporting choice, and staged rollout settings.

## Release artifact

The Play artifact is `app/build/outputs/bundle/release/app-release.aab`. Confirm its certificate and version metadata before uploading.

Core signed-release smoke tests pass on API 28/29/33/34/36. See `BETA_IMPLEMENTATION_STATUS.md` for the exact coverage and outstanding extended scenarios.

## Beta regression acceptance

- [ ] API 28–32: deny library access, confirm permission recovery, and test denied write access.
- [ ] API 33–36: images only, videos only, selected media, full access, and revocation after opening system settings.
- [ ] Change delete-list preference in both directions without restarting; preserve existing queued items.
- [ ] Rapid keep/delete actions and button presses during a swipe never apply an old gesture to the next photo.
- [ ] Rotate during the Android deletion dialog: one request and one result only.
- [ ] Cancel API 29 per-item approval after partial success: count successful deletions and preserve remaining items.
- [ ] Kill/restart after confirmation: recover receipt and counters once; restricted access never implies an unconfirmed deletion.
- [ ] Confirm queues larger than 1,000 items in explicit batches; verify remaining items stay queued.
- [ ] Download/import media without capture dates; verify card dates, year/month browsing, and sorting agree.
- [ ] Browse every filter section in landscape, on a compact phone, in German, and with 200% fonts.
- [ ] Check haptics on/off on a physical device; preview large photos and unsupported/missing media.
- [ ] Background/lock during video playback: audio pauses and resumes only when previously playing.
- [ ] Yesterday’s review count displays zero today before the first review; verify midnight rollover.
- [ ] Test support action with and without an email client, plus onboarding in light/dark mode and TalkBack.
