package com.photosoap.android.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PendingDeletionItem(
    val id: String,
    val uri: String,
    val displayName: String,
    val fileSize: Long,
    val queuedAt: Long,
    val mimeType: String = "",
)
