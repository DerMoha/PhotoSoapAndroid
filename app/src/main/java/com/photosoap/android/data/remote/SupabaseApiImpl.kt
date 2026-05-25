package com.photosoap.android.data.remote

import com.photosoap.android.data.remote.dto.MetricsIngestResponse
import com.photosoap.android.data.remote.dto.MetricsPayload
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        val metricsUrl = getMetricsUrl()
        if (metricsUrl.isBlank()) return Result.failure(IllegalStateException("Metrics URL not configured"))

        return withContext(Dispatchers.IO) {
            val url = URL(metricsUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", getAnonKey())
                setRequestProperty("Authorization", "Bearer ${getAnonKey()}")
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 15_000
            }

            try {
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun getMetricsUrl(): String = DEFAULT_METRICS_URL
    private fun getAnonKey(): String = DEFAULT_ANON_KEY
}

class PermanentMetricsException(message: String) : Exception(message)
