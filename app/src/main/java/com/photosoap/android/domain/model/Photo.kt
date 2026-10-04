package com.photosoap.android.domain.model

data class Photo(
    val id: Long,
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val dateTaken: Long,
    val dateAdded: Long,
    val fileSize: Long,
    val width: Int,
    val height: Int,
    val duration: Long = 0,
    val bucketId: Long = 0,
    val bucketName: String = "",
) {
    /** MediaStore DATE_ADDED is in seconds; DATE_TAKEN is in milliseconds. */
    val effectiveDateMillis: Long get() = if (dateTaken > 0) dateTaken else dateAdded * 1000L

    val isVideo: Boolean get() = mimeType.startsWith("video/")
    val isImage: Boolean get() = mimeType.startsWith("image/")

    val formattedFileSize: String
        get() = when {
            fileSize < 1024 -> "$fileSize B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024} KB"
            fileSize < 1024 * 1024 * 1024 -> "${"%.1f".format(fileSize / (1024.0 * 1024.0))} MB"
            else -> "${"%.2f".format(fileSize / (1024.0 * 1024.0 * 1024.0))} GB"
        }

    val formattedDuration: String
        get() {
            if (duration == 0L) return ""
            val totalSeconds = duration / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}
