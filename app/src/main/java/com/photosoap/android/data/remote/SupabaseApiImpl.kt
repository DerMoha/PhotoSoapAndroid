package com.photosoap.android.data.remote

import com.photosoap.android.data.remote.dto.MetricsIngestResponse
import com.photosoap.android.data.remote.dto.MetricsPayload
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class SupabaseApiImpl @Inject constructor() : SupabaseApi {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val DEFAULT_METRICS_URL = ""
        private const val DEFAULT_ANON_KEY = ""
    }

    override suspend fun ingestMetrics(payload: MetricsPayload): Result<Unit> {
        return try {
            val metricsUrl = getMetricsUrl()
            if (metricsUrl.isBlank()) return Result.success(Unit)

            val url = URL(metricsUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("apikey", getAnonKey())
            connection.setRequestProperty("Authorization", "Bearer ${getAnonKey()}")
            connection.doOutput = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000

            val body = json.encodeToString(payload)
            connection.outputStream.use { it.write(body.toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                Result.success(Unit)
            } else if (responseCode in setOf(400, 401, 403, 404, 422)) {
                Result.failure(PermanentMetricsException("HTTP $responseCode"))
            } else {
                Result.failure(Exception("HTTP $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getMetricsUrl(): String = DEFAULT_METRICS_URL
    private fun getAnonKey(): String = DEFAULT_ANON_KEY
}

class PermanentMetricsException(message: String) : Exception(message)
