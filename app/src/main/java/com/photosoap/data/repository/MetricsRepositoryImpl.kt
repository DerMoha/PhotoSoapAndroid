package com.photosoap.data.repository

import com.photosoap.data.remote.api.MetricsApi
import com.photosoap.data.remote.dto.MetricsPayload
import com.photosoap.domain.repository.MetricsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetricsRepositoryImpl @Inject constructor(
    private val metricsApi: MetricsApi,
) : MetricsRepository {

    override suspend fun submitMetrics(payload: MetricsPayload): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = metricsApi.submitMetrics(payload)
                response.isSuccessful
            } catch (e: Exception) {
                false
            }
        }
    }
}
