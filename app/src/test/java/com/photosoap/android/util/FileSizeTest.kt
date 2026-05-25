package com.photosoap.android.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FileSizeTest {

    @Test
    fun `format bytes`() {
        assertEquals("0 B", FileSize.format(0))
        assertEquals("512 B", FileSize.format(512))
        assertEquals("1023 B", FileSize.format(1023))
    }

    @Test
    fun `format kilobytes`() {
        assertEquals("1 KB", FileSize.format(1024))
        assertEquals("512 KB", FileSize.format(512 * 1024))
    }

    @Test
    fun `format megabytes`() {
        assertEquals("1.0 MB", FileSize.format(1024 * 1024))
        assertEquals("2.5 MB", FileSize.format((2.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun `format gigabytes`() {
        assertEquals("1.00 GB", FileSize.format(1024L * 1024L * 1024L))
    }

    @Test
    fun `boundary between bytes and kilobytes`() {
        assertEquals("1023 B", FileSize.format(1023))
        assertEquals("1 KB", FileSize.format(1024))
    }
}
