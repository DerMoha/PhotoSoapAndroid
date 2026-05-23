package com.photosoap.android.domain.repository

import com.photosoap.android.domain.model.Photo
import kotlinx.coroutines.flow.Flow

interface PhotoRepository {
    fun observeReviewedPhotoUris(): Flow<List<String>>
    suspend fun isReviewed(uri: String): Boolean
    suspend fun markReviewed(uri: String)
    suspend fun unmarkReviewed(uri: String)
    suspend fun clearReviewed()
}
