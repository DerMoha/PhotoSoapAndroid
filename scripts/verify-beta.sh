#!/bin/sh
# Run from any directory with JDK 17, Android SDK 36, and release signing configured.
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
./gradlew testDebugUnitTest testReleaseUnitTest lintDebug lintRelease assembleDebug assembleRelease verifyProductionConfiguration bundleRelease
# Run migration instrumentation on an attached emulator/device when explicitly enabled.
if [ "${PHOTOSOAP_RUN_DEVICE_TESTS:-0}" = "1" ]; then
    ./gradlew connectedDebugAndroidTest
fi
