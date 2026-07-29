package com.photosoap.android.domain.repository

import kotlinx.coroutines.flow.Flow

interface MetricsRepository {
    val installId: Flow<String>
    val isEnabled: Flow<Boolean>

    suspend fun trackKept(count: Int = 1)
    suspend fun trackDeletion(fileSize: Long)
    suspend fun trackBatchDeletion(count: Int, totalFileSize: Long)
    suspend fun trackBatchDeletionOnce(requestId: String, count: Int, totalFileSize: Long)
    suspend fun flush()
}
