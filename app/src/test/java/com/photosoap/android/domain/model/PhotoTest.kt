package com.photosoap.android.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PhotoTest {

    @Test
    fun `photo with jpeg mime is not video`() {
        val photo = Photo(
            id = 1,
            uri = "content://test/1",
            displayName = "test.jpg",
            mimeType = "image/jpeg",
            dateTaken = 0,
            dateAdded = 0,
            fileSize = 1024,
            width = 1920,
            height = 1080,
        )
        assertTrue(photo.isImage)
        assertFalse(photo.isVideo)
    }

    @Test
    fun `photo with mp4 mime is video`() {
        val photo = Photo(
            id = 2,
            uri = "content://test/2",
            displayName = "test.mp4",
            mimeType = "video/mp4",
            dateTaken = 0,
            dateAdded = 0,
            fileSize = 1024000,
            width = 1920,
            height = 1080,
            duration = 30000,
        )
        assertTrue(photo.isVideo)
        assertFalse(photo.isImage)
    }

    @Test
    fun `file size formatting`() {
        val bytes = Photo(
            id = 1, uri = "", displayName = "", mimeType = "image/jpeg",
            dateTaken = 0, dateAdded = 0, fileSize = 512, width = 0, height = 0,
        )
        val kb = Photo(
            id = 1, uri = "", displayName = "", mimeType = "image/jpeg",
            dateTaken = 0, dateAdded = 0, fileSize = 1024, width = 0, height = 0,
        )
        val mb = Photo(
            id = 1, uri = "", displayName = "", mimeType = "image/jpeg",
            dateTaken = 0, dateAdded = 0, fileSize = 2_500_000, width = 0, height = 0,
        )

        assertEquals("512 B", bytes.formattedFileSize)
        assertEquals("1 KB", kb.formattedFileSize)
    }

    @Test
    fun `duration formatting`() {
        val photo = Photo(
            id = 1, uri = "", displayName = "", mimeType = "video/mp4",
            dateTaken = 0, dateAdded = 0, fileSize = 0, width = 0, height = 0,
            duration = 30000,
        )
        assertEquals("0:30", photo.formattedDuration)
    }
}
