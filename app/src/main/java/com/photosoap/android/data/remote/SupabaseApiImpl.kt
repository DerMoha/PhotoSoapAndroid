package com.photosoap.android.data.remote

import com.photosoap.android.BuildConfig
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

    override suspend fun ingestMetrics(payload: MetricsPayload): Result<Unit> {
        val metricsUrl = getMetricsUrl()
        if (metricsUrl.isBlank()) return Result.failure(IllegalStateException("Metrics URL not configured"))
        if (!metricsUrl.startsWith("https://")) {
            return Result.failure(PermanentMetricsException("Metrics URL must use HTTPS"))
        }
        if (getAnonKey().isBlank()) {
            return Result.failure(IllegalStateException("Metrics key not configured"))
        }

        return withContext(Dispatchers.IO) {
            val url = URL(metricsUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", getAnonKey())
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 15_000
            }

            try {
                val body = encodeMetricsPayload(payload)
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

    private fun getMetricsUrl(): String = BuildConfig.METRICS_URL
    private fun getAnonKey(): String = BuildConfig.METRICS_ANON_KEY
}

// The endpoint has different defaults (including iOS as its platform). Send
// every contract field explicitly instead of inheriting the server's defaults.
internal fun encodeMetricsPayload(payload: MetricsPayload): String = Json {
    encodeDefaults = true
}.encodeToString(
    payload.copy(
        appVersion = BuildConfig.VERSION_NAME,
        buildNumber = BuildConfig.VERSION_CODE.toString(),
    ),
)

class PermanentMetricsException(message: String) : Exception(message)
