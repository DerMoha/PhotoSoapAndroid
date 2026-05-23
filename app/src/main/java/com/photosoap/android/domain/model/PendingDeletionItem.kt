package com.photosoap.android.domain.model

data class PendingDeletionItem(
    val id: String,
    val uri: String,
    val displayName: String,
    val fileSize: Long,
    val queuedAt: Long,
)
