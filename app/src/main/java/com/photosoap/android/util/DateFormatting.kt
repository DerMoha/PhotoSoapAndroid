package com.photosoap.android.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateFormatting {
    private val displayFormat = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    private val shortFormat = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    private val relativeTodayFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val relativeThisYear = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    private val relativeFull = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

    fun formatDisplay(epochMillis: Long): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        return displayFormat.withZone(ZoneId.systemDefault()).format(instant)
    }

    fun formatShort(epochMillis: Long): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        return shortFormat.withZone(ZoneId.systemDefault()).format(instant)
    }
}

fun Long.millisToHumanReadable(): String {
    val hours = TimeUnit.MILLISECONDS.toHours(this)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60
    return when {
        hours > 0 -> "%d:%02d:%02d".format(hours, minutes, seconds)
        else -> "%d:%02d".format(minutes, seconds)
    }
}
