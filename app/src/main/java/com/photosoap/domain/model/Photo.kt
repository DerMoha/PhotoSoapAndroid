package com.photosoap.domain.model

data class Photo(
    val id: Long,
    val uri: String,
    val displayName: String,
    val dateTaken: Long,
    val fileSize: Long,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val isVideo: Boolean,
    val durationMs: Long = 0L,
    val orientation: Int = 0,
) {
    val formattedDate: String get() {
        val sdf = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(dateTaken * 1000))
    }

    val shortFormattedDate: String get() {
        val sdf = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(dateTaken * 1000))
    }

    val dimensions: String get() = "$width × $height"

    val compactDimensions: String get() = "${width}×${height}"

    val formattedDuration: String get() {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    val fileSizeFormatted: String get() {
        val kb = fileSize / 1024.0
        return when {
            kb < 1024 -> "%.1f KB".format(kb)
            else -> {
                val mb = kb / 1024.0
                if (mb < 1024) "%.1f MB".format(mb)
                else "%.2f GB".format(mb / 1024.0)
            }
        }
    }
}
