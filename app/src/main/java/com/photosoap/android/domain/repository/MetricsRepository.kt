package com.photosoap.android.domain.repository

import com.photosoap.android.data.remote.dto.MetricsPayload
import kotlinx.coroutines.flow.Flow

interface MetricsRepository {
    val installId: Flow<String>
    val isEnabled: Flow<Boolean>

    suspend fun trackReview()
    suspend fun trackDeletion(fileSize: Long)
    suspend fun trackBatchDeletion(count: Int, totalFileSize: Long)
    suspend fun flush()
}
