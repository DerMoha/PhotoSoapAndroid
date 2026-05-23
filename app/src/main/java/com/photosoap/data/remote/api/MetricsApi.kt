package com.photosoap.data.remote.api

import com.photosoap.data.remote.dto.MetricsPayload
import com.photosoap.data.remote.dto.MetricsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface MetricsApi {
    @POST("functions/v1/metrics-ingest")
    suspend fun submitMetrics(@Body payload: MetricsPayload): Response<MetricsResponse>
}
