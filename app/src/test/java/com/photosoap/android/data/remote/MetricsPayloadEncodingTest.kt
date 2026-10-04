package com.photosoap.android.data.remote

import com.photosoap.android.BuildConfig
import com.photosoap.android.data.remote.dto.MetricsPayload
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MetricsPayloadEncodingTest {
    @Test
    fun `default payload explicitly identifies Android and registers the installation`() {
        val body = Json.parseToJsonElement(encodeMetricsPayload(payload())).jsonObject

        // Missing fields would inherit the endpoint's iOS/false defaults.
        assertEquals("android", body.getValue("platform").jsonPrimitive.content)
        assertTrue(body.getValue("register_install").jsonPrimitive.boolean)
        assertTrue(body.getValue("daily_buckets").jsonArray.isEmpty())
        assertEquals("2026-10-04T08:00:00Z", body.getValue("submitted_at").jsonPrimitive.content)
    }

    @Test
    fun `outgoing version and build identify the running app`() {
        val body = Json.parseToJsonElement(
            encodeMetricsPayload(payload().copy(appVersion = "old-version", buildNumber = "old-build")),
        ).jsonObject

        assertEquals(BuildConfig.VERSION_NAME, body.getValue("app_version").jsonPrimitive.content)
        assertEquals(BuildConfig.VERSION_CODE.toString(), body.getValue("build_number").jsonPrimitive.content)
    }

    private fun payload() = MetricsPayload(
        installId = "3d677e89-0ad0-4f9e-bc12-18c6b45974d1",
        submittedAt = "2026-10-04T08:00:00Z",
        dailyBuckets = emptyList(),
    )
}
