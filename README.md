# PhotoSoap for Android

PhotoSoap is an on-device photo and video review app for Android 9 (API 28) and newer. Swipe right to keep media or left to add it to a system-confirmed delete list.

## Development

Requirements:

- JDK 17
- Android SDK 36

Copy `secrets.properties.example` to `secrets.properties` and fill in the optional aggregate-metrics configuration. The file is ignored by Git.

Run the complete local verification suite:

```sh
./gradlew testDebugUnitTest testReleaseUnitTest lintDebug lintRelease assembleDebug assembleRelease
```

Room schema history is committed under `app/schemas` and destructive migrations are disabled.

## Release signing

Create and securely back up a Play release/upload key, then set these values only in the ignored `secrets.properties` file:

```properties
signing.storeFile=/absolute/path/to/release.keystore
signing.storePassword=...
signing.keyAlias=...
signing.keyPassword=...
```

Validate secrets and produce the Play bundle:

```sh
./gradlew verifyProductionConfiguration bundleRelease
```

Never commit the keystore, passwords, or the Supabase publishable key file. The Supabase key used here must be a publishable/anonymous client key protected by backend authorization and validation—not a service-role key.

## External beta verification

The current beta version is `1.0.0-beta.1`. Use JDK 17 and SDK 37.0 (build tools 37.0.0).

After configuring the upload key, run `./scripts/verify-beta.sh` for unit tests,
lint, debug/release assembly, production configuration checks, and the Play bundle.
Set `PHOTOSOAP_RUN_DEVICE_TESTS=1` to also run the migration instrumentation suite
on an attached emulator or device. Real media-deletion acceptance checks are listed
in `RELEASE_CHECKLIST.md`; use disposable media.

Optional analytics may be omitted by leaving both metrics properties blank. If
configured, the endpoint must use HTTPS and the key must be a client publishable
key. Validate opt-in/opt-out before distributing an analytics-enabled build.

See `BETA_IMPLEMENTATION_STATUS.md` for implemented fixes, verification evidence,
and remaining distribution gates. The signed build and core API 28/29/33/34/36
emulator checks now pass; Play Console setup and physical-device acceptance remain.

## Material 3 Expressive

The UI uses Google's `MaterialExpressiveTheme`, its spring motion scheme, native
Expressive navigation, shape-changing buttons, wavy progress indicators, and
Material shape assets. Settings support system, light, and dark appearance,
wallpaper colors, and four accent palettes.

Expressive components currently use Material 3 `1.5.0-alpha29`; this is an
intentional pre-release dependency for the beta design pass. The app still
supports Android 9 and later and targets Android 16. See
`verification/expressive-design.txt` for the verification scope.
