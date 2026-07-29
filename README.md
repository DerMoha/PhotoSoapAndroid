# PhotoSoap for Android

PhotoSoap is an on-device photo and video review app for Android 9 (API 28) and newer. Swipe right to keep media or left to add it to a system-confirmed delete list.

## Development

Requirements:

- JDK 17
- Android SDK 36

Copy `secrets.properties.example` to `secrets.properties` and fill in the optional aggregate-metrics configuration. The file is ignored by Git.

Run the complete local verification suite:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
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
