package com.photosoap.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoTest {

    @Test
    fun `formattedDate returns correct format`() {
        val photo = createTestPhoto(dateTaken = 1700000000L)
        assertTrue(photo.formattedDate.isNotEmpty())
    }

    @Test
    fun `fileSizeFormatted for KB`() {
        val photo = createTestPhoto(fileSize = 1024 * 500)
        assertEquals("500.0 KB", photo.fileSizeFormatted)
    }

    @Test
    fun `fileSizeFormatted for MB`() {
        val photo = createTestPhoto(fileSize = 1024 * 1024 * 5)
        assertEquals("5.0 MB", photo.fileSizeFormatted)
    }

    @Test
    fun `isVideo correctly identifies video`() {
        val photo = createTestPhoto(isVideo = true)
        assertTrue(photo.isVideo)
    }

    @Test
    fun `formattedDuration returns correct format`() {
        val photo = createTestPhoto(isVideo = true, durationMs = 125000L)
        assertEquals("2:05", photo.formattedDuration)
    }

    @Test
    fun `dimensions returns correct string`() {
        val photo = createTestPhoto(width = 1920, height = 1080)
        assertEquals("1920 × 1080", photo.dimensions)
    }

    private fun createTestPhoto(
        id: Long = 1L,
        uri: String = "content://test/1",
        displayName: String = "IMG_0001.jpg",
        dateTaken: Long = System.currentTimeMillis(),
        fileSize: Long = 1024 * 1024,
        mimeType: String = "image/jpeg",
        width: Int = 1920,
        height: Int = 1080,
        isVideo: Boolean = false,
        durationMs: Long = 0L,
    ) = Photo(
        id = id,
        uri = uri,
        displayName = displayName,
        dateTaken = dateTaken,
        fileSize = fileSize,
        mimeType = mimeType,
        width = width,
        height = height,
        isVideo = isVideo,
        durationMs = durationMs,
    )
}
