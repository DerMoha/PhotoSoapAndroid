package com.photosoap.domain.model

data class PendingDeletionItem(
    val photo: Photo,
    val queuedAt: Long = System.currentTimeMillis(),
    val fileSize: Long = photo.fileSize,
)
