package com.photosoap.android.domain.model

sealed interface ReviewFilter {
    data class Smart(val kind: SmartAlbum) : ReviewFilter
    data object All : ReviewFilter
    data class Year(val year: Int) : ReviewFilter
    data class Month(val year: Int, val month: Int) : ReviewFilter
    data class Album(val albumId: Long, val albumName: String) : ReviewFilter
}

enum class SmartAlbum {
    SCREENSHOTS, VIDEOS, SELFIES, FAVORITES, PANORAMAS, ANIMATED, RAW;
    fun matches(photo: Photo): Boolean = when (this) {
        FAVORITES -> photo.isFavorite
        PANORAMAS -> photo.isImage && photo.height > 0 && photo.width.toDouble() / photo.height >= 2.0
        ANIMATED -> photo.mimeType.equals("image/gif", ignoreCase = true)
        RAW -> photo.mimeType.equals("image/x-adobe-dng", ignoreCase = true) || photo.displayName.endsWith(".dng", ignoreCase = true)
        VIDEOS -> photo.isVideo
        SCREENSHOTS -> photo.isImage && (photo.bucketName.contains("screenshot", ignoreCase = true) || photo.displayName.contains("screenshot", ignoreCase = true))
        SELFIES -> photo.isImage && photo.bucketName.equals("selfies", ignoreCase = true)
    }
}
