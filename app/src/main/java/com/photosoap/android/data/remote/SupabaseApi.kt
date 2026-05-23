package com.photosoap.android.data.remote

import com.photosoap.android.data.remote.dto.MetricsPayload

interface SupabaseApi {
    suspend fun ingestMetrics(payload: MetricsPayload): Result<Unit>
}
