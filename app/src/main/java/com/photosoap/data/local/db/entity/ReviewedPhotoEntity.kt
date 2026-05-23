package com.photosoap.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "reviewed_photos",
    primaryKeys = ["photoId"],
    indices = [Index(value = ["photoId"], unique = true)],
)
data class ReviewedPhotoEntity(
    val photoId: Long,
    val reviewDate: Long = System.currentTimeMillis(),
)
