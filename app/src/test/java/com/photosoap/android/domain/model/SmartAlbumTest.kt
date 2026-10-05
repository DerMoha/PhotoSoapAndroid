package com.photosoap.android.domain.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SmartAlbumTest {
    private fun photo(mime: String = "image/jpeg", name: String = "photo.jpg", bucket: String = "Camera", favorite: Boolean = false, width: Int = 1000) =
        Photo(1, "content://test/1", name, mime, 0, 0, 1024, width, 1000, isFavorite = favorite, bucketName = bucket)

    @Test fun `smart filters use exposed media metadata`() {
        assertTrue(SmartAlbum.SCREENSHOTS.matches(photo(name = "Screenshot_2026.png")))
        assertFalse(SmartAlbum.SCREENSHOTS.matches(photo(mime = "video/mp4", bucket = "Screenshots")))
        assertTrue(SmartAlbum.SELFIES.matches(photo(bucket = "Selfies")))
        assertFalse(SmartAlbum.SELFIES.matches(photo()))
        assertTrue(SmartAlbum.FAVORITES.matches(photo(favorite = true)))
        assertFalse(SmartAlbum.FAVORITES.matches(photo()))
        assertTrue(SmartAlbum.VIDEOS.matches(photo(mime = "video/mp4")))
        assertTrue(SmartAlbum.RAW.matches(photo(name = "photo.DNG")))
        assertTrue(SmartAlbum.ANIMATED.matches(photo(mime = "image/gif")))
    }

    @Test fun `wide photo classification respects aspect ratio boundary`() {
        assertFalse(SmartAlbum.PANORAMAS.matches(photo(width = 1999)))
        assertTrue(SmartAlbum.PANORAMAS.matches(photo(width = 2000)))
        assertFalse(SmartAlbum.PANORAMAS.matches(photo(mime = "video/mp4", width = 3000)))
    }
}
