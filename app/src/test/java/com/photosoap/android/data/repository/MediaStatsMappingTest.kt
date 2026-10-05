package com.photosoap.android.data.repository

import com.photosoap.android.domain.model.UserStats
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MediaStatsMappingTest {
    @Test fun `media counters survive persistence mapping`() {
        val stats = UserStats(photosReviewed = 10, photosKept = 7, photosDeleted = 3,
            photoStorageFreed = 4096, videosReviewed = 8, videosKept = 6, videosDeleted = 2,
            videoStorageFreed = 8192, totalReviewed = 18, totalKept = 13, totalDeleted = 5,
            storageFreed = 12288)
        assertEquals(stats, stats.toEntity().toDomain())
    }
}
