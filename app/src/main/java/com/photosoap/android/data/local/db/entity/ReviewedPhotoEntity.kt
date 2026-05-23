package com.photosoap.android.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reviewed_photos",
    indices = [Index(value = ["photo_uri"], unique = true)],
)
data class ReviewedPhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "photo_uri")
    val photoUri: String,
    val reviewDate: Long,
)
