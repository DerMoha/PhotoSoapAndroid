package com.photosoap.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MetricsIngestResponse(
    @SerialName("ingested_bucket_count")
    val ingestedBucketCount: Int,
    @SerialName("registered_install")
    val registeredInstall: Boolean = false,
)
