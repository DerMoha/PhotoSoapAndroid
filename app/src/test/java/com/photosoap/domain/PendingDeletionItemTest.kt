package com.photosoap.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class PendingDeletionItemTest {

    @Test
    fun `creates with correct photo reference`() {
        val photo = Photo(
            id = 1,
            uri = "content://test/1",
            displayName = "test.jpg",
            dateTaken = System.currentTimeMillis(),
            fileSize = 1024 * 1024,
            mimeType = "image/jpeg",
            width = 1920,
            height = 1080,
            isVideo = false,
        )

        val item = PendingDeletionItem(photo = photo, fileSize = photo.fileSize)

        assertEquals(photo.id, item.photo.id)
        assertEquals(photo.fileSize, item.fileSize)
        assertTrue(item.queuedAt > 0)
    }

    @Test
    fun `queuedAt is set automatically`() {
        val photo = Photo(
            id = 2,
            uri = "content://test/2",
            displayName = "video.mp4",
            dateTaken = System.currentTimeMillis(),
            fileSize = 1024 * 1024 * 10,
            mimeType = "video/mp4",
            width = 1920,
            height = 1080,
            isVideo = true,
            durationMs = 5000,
        )

        val before = System.currentTimeMillis()
        val item = PendingDeletionItem(photo = photo)
        val after = System.currentTimeMillis()

        assertTrue(item.queuedAt >= before && item.queuedAt <= after)
    }
}
