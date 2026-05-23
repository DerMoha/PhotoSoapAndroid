package com.photosoap.android.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateFormatting {
    private val displayFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val shortFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    private val relativeTodayFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val relativeThisYear = SimpleDateFormat("MMM d", Locale.getDefault())
    private val relativeFull = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    fun formatDisplay(epochMillis: Long): String = displayFormat.format(Date(epochMillis))
    fun formatShort(epochMillis: Long): String = shortFormat.format(Date(epochMillis))
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
