package com.photosoap.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MetricsPayload(
    @SerialName("install_id")
    val installId: String,
    @SerialName("platform")
    val platform: String = "android",
    @SerialName("app_version")
    val appVersion: String = "1.0.0",
    @SerialName("build_number")
    val buildNumber: String = "1",
    @SerialName("submitted_at")
    val submittedAt: String,
    @SerialName("register_install")
    val registerInstall: Boolean = true,
    @SerialName("daily_buckets")
    val dailyBuckets: List<DailyBucket>,
)
