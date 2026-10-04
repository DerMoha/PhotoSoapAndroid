package com.photosoap.android

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PermissionCheckerTest {
    @Test
    fun `legacy storage denial never becomes selected media access`() {
        for (sdk in 28..32) {
            assertEquals(MediaAccess.NONE, mediaAccessForGrants(sdk, images = false, videos = true, selected = true))
            assertEquals(MediaAccess.FULL, mediaAccessForGrants(sdk, images = true, videos = false, selected = false))
        }
    }

    @Test
    fun `modern permission categories reflect image video and selected grants`() {
        for (sdk in 33..36) {
            assertEquals(MediaAccess.FULL, mediaAccessForGrants(sdk, true, true, false))
            assertEquals(MediaAccess.LIMITED, mediaAccessForGrants(sdk, true, false, false))
            assertEquals(MediaAccess.LIMITED, mediaAccessForGrants(sdk, false, true, false))
            assertEquals(MediaAccess.NONE, mediaAccessForGrants(sdk, false, false, false))
            assertEquals(if (sdk >= 34) MediaAccess.LIMITED else MediaAccess.NONE,
                mediaAccessForGrants(sdk, false, false, true))
        }
    }
}
