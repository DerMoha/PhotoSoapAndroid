# APK downloads through GitHub Releases

The `Release APK` workflow builds a signed, installable APK when a GitHub release
is published, including a pre-release. It checks release unit tests, lint, production
configuration, and the APK signature before attaching the APK and its SHA-256 checksum.

Configure these repository secrets under **Settings → Secrets and variables → Actions**:

- `ANDROID_KEYSTORE_BASE64`: the release keystore encoded as a single-line Base64 value.
- `ANDROID_KEYSTORE_PASSWORD`: its store password.
- `ANDROID_KEY_ALIAS`: the signing key alias.
- `ANDROID_KEY_PASSWORD`: the signing key password.
- Optional: `METRICS_URL` and `METRICS_ANON_KEY`, provided together to enable aggregate metrics.

Use the same signing key for subsequent APK releases so installed copies can be
updated. Google Play may use a separate app signing key, so a GitHub APK signed
with the upload key may not update an installation from Play.

To distribute a build:

1. Commit and push the intended app changes, including `versionName` and an increased
   `versionCode` in `app/build.gradle.kts` for updates.
2. Open **Releases → Draft a new release** and select the intended commit.
3. Create the tag `v<versionName>` (currently `v1.0.0-beta.3`), mark beta versions
   as a pre-release, and publish the release.
4. Wait for **Actions → Release APK** to finish. The release will then contain
   `PhotoSoap-<versionName>.apk` and its `.sha256` checksum.

Draft releases do not trigger a build. If a build fails, fix the cause and re-run
the workflow from Actions. Re-running replaces the APK and checksum on that release.
The workflow file must be present in the tagged commit.

For a build without publishing a release, open **Actions → Release APK → Run workflow**.
Download `photosoap-release-apk` from the completed run's artifacts. This builds the
selected branch and does not include local uncommitted changes.
