package com.photosoap.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MetricsPayload(
    @SerialName("install_id") val installId: String,
    @SerialName("platform") val platform: String = "android",
    @SerialName("app_version") val appVersion: String,
    @SerialName("build_number") val buildNumber: String,
    @SerialName("submitted_at") val submittedAt: String,
    @SerialName("register_install") val registerInstall: Boolean = true,
    @SerialName("daily_buckets") val dailyBuckets: List<DailyMetricsBucket> = emptyList(),
)

@Serializable
data class DailyMetricsBucket(
    @SerialName("date") val date: String,
    @SerialName("reviewed_photos") val reviewedPhotos: Int = 0,
    @SerialName("deleted_photos") val deletedPhotos: Int = 0,
    @SerialName("kept_photos") val keptPhotos: Int = 0,
    @SerialName("bytes_freed") val bytesFreed: Long = 0L,
)

@Serializable
data class MetricsResponse(
    @SerialName("ingested_bucket_count") val ingestedBucketCount: Int = 0,
    @SerialName("registered_install") val registeredInstall: Boolean = false,
)

@Serializable
data class MetricsErrorResponse(
    val error: String = "",
    val message: String = "",
)
