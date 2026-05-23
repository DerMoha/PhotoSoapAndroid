package com.photosoap.domain.repository

import com.photosoap.domain.model.AlbumInfo
import com.photosoap.domain.model.Photo
import com.photosoap.domain.model.PhotoFilter
import com.photosoap.domain.model.ReviewMediaKind
import kotlinx.coroutines.flow.Flow

interface PhotoRepository {
    fun observePhotos(
        filter: PhotoFilter = PhotoFilter.All,
        mediaKind: ReviewMediaKind = ReviewMediaKind.All,
        sortNewestFirst: Boolean = true,
    ): Flow<List<Photo>>

    suspend fun loadPhotos(
        filter: PhotoFilter = PhotoFilter.All,
        mediaKind: ReviewMediaKind = ReviewMediaKind.All,
        sortNewestFirst: Boolean = true,
    ): List<Photo>

    suspend fun getAlbums(): List<AlbumInfo>

    suspend fun getAvailableYears(): List<Int>

    suspend fun getAvailableMonths(year: Int): List<Int>

    suspend fun deletePhotos(photoIds: List<Long>): Boolean

    suspend fun getFileSize(photoId: Long): Long
}
