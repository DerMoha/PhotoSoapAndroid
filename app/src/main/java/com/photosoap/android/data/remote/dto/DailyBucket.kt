package com.photosoap.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DailyBucket(
    @SerialName("metric_date")
    val metricDate: String,
    @SerialName("reviewed_photos")
    val reviewedPhotos: Int,
    @SerialName("deleted_photos")
    val deletedPhotos: Int,
    @SerialName("kept_photos")
    val keptPhotos: Int,
    @SerialName("bytes_freed")
    val bytesFreed: Long,
)
