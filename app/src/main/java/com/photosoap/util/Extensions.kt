package com.photosoap.util

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.SuccessResult

object Extensions {
    fun Long.toFormattedFileSize(): String {
        val kb = this / 1024.0
        return when {
            kb < 1024 -> "%.1f KB".format(kb)
            else -> {
                val mb = kb / 1024.0
                if (mb < 1024) "%.1f MB".format(mb)
                else "%.2f GB".format(mb / 1024.0)
            }
        }
    }

    fun Long.toDurationString(): String {
        val totalSeconds = this / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}

fun Long.millisToDateString(): String {
    val sdf = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(this * 1000))
}
