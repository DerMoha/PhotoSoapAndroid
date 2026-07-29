# Android release checklist

## Required before Play submission

- [ ] Choose the final application ID. It is currently `com.photosoap` and cannot be changed after publishing without creating a new Play listing.
- [ ] Replace version code/name for the release and keep version codes monotonically increasing.
- [ ] Configure and back up the Play upload key; run `./gradlew verifyProductionConfiguration`.
- [ ] Run `./gradlew testDebugUnitTest lintDebug bundleRelease` with JDK 17.
- [ ] Test the signed release on API 28, 29, 33, 34, and 36, including full, limited, denied, and revoked media access.
- [ ] Verify keep, queued delete, undo, partial legacy deletion, system-confirmed deletion, rotation, dark mode, German, 200% font scale, TalkBack, and process restart.
- [ ] Publish a public HTTPS privacy-policy URL for the Play listing. The in-app policy is complete, but the existing GitHub Pages URLs currently return 404.
- [ ] Enter and verify the prepared answers in `PLAY_STORE_DATA_SAFETY.md` in Play Console.
- [ ] Upload the localized title, descriptions, and release notes from `fastlane/metadata/android/`; provide the support email, feature graphic, phone screenshots, app icon, and content rating.
- [ ] Test analytics opt-in and opt-out against the production metrics endpoint without using a service-role key.
- [ ] Review the pre-launch report, Android vitals, crash/ANR reporting choice, and staged rollout settings.

## Release artifact

The Play artifact is `app/build/outputs/bundle/release/app-release.aab`. Confirm its certificate and version metadata before uploading.
