package com.photosoap.android.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "processed_deletions")
data class ProcessedDeletionEntity(
    @PrimaryKey
    val requestId: String,
    val processedAt: Long = System.currentTimeMillis(),
)
