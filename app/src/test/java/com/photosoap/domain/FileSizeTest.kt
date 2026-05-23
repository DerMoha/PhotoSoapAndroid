package com.photosoap.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FileSizeTest {

    @Test
    fun `0 bytes formats correctly`() {
        assertEquals("0 B", StatsDisplay(storageFreed = 0L).storageFreedFormatted)
    }

    @Test
    fun `KB values format correctly`() {
        val stats = StatsDisplay(storageFreed = 1024 * 500)
        assertEquals("500.0 KB", stats.storageFreedFormatted)
    }

    @Test
    fun `MB values format correctly`() {
        val stats = StatsDisplay(storageFreed = 1024L * 1024 * 5)
        assertEquals("5.0 MB", stats.storageFreedFormatted)
    }

    @Test
    fun `GB values format correctly`() {
        val gb = 1024L * 1024 * 1024 * 3
        val stats = StatsDisplay(storageFreed = gb)
        assertEquals("3.00 GB", stats.storageFreedFormatted)
    }

    @Test
    fun `keepDeleteRatio returns 0_5 for no data`() {
        val stats = StatsDisplay()
        assertEquals(0.5f, stats.keepDeleteRatio)
    }

    @Test
    fun `keepDeleteRatio with equal counts`() {
        val stats = StatsDisplay(totalKept = 10, totalDeleted = 10)
        assertEquals(0.5f, stats.keepDeleteRatio)
    }

    @Test
    fun `keepDeleteRatio with more kept`() {
        val stats = StatsDisplay(totalKept = 80, totalDeleted = 20)
        assertEquals(0.8f, stats.keepDeleteRatio)
    }
}
