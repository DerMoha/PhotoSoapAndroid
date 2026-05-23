package com.photosoap.domain.repository

import com.photosoap.data.remote.dto.MetricsPayload

interface MetricsRepository {
    suspend fun submitMetrics(payload: MetricsPayload): Boolean
}
