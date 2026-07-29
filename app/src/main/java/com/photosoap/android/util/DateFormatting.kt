package com.photosoap.android.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.concurrent.TimeUnit

object DateFormatting {
    fun formatDisplay(epochMillis: Long): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withZone(ZoneId.systemDefault())
            .format(instant)
    }

    fun formatShort(epochMillis: Long): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        return DateTimeFormatter.ofPattern("MMM d")
            .withZone(ZoneId.systemDefault())
            .format(instant)
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
