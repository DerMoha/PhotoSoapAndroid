package com.photosoap.android.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

fun Long.startOfDay(): Long {
    val instant = Instant.ofEpochMilli(this)
    val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
    return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

fun Long.daysBetween(other: Long): Long {
    val date1 = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
    val date2 = Instant.ofEpochMilli(other).atZone(ZoneId.systemDefault()).toLocalDate()
    return ChronoUnit.DAYS.between(date1, date2)
}

fun nowMillis(): Long = System.currentTimeMillis()

fun todayStartMillis(): Long = LocalDate.now()
    .atStartOfDay(ZoneId.systemDefault())
    .toInstant()
    .toEpochMilli()
