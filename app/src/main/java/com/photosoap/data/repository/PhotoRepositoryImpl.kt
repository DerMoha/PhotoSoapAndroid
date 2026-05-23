package com.photosoap.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.photosoap.domain.model.AlbumInfo
import com.photosoap.domain.model.Photo
import com.photosoap.domain.model.PhotoFilter
import com.photosoap.domain.model.ReviewMediaKind
import com.photosoap.domain.repository.PhotoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : PhotoRepository {

    private val contentResolver: ContentResolver
        get() = context.contentResolver

    override fun observePhotos(
        filter: PhotoFilter,
        mediaKind: ReviewMediaKind,
        sortNewestFirst: Boolean,
    ): Flow<List<Photo>> = flow {
        emit(loadPhotos(filter, mediaKind, sortNewestFirst))
    }

    override suspend fun loadPhotos(
        filter: PhotoFilter,
        mediaKind: ReviewMediaKind,
        sortNewestFirst: Boolean,
    ): List<Photo> {
        val collection = when (mediaKind) {
            ReviewMediaKind.Photos -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            ReviewMediaKind.Videos -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            ReviewMediaKind.All -> MediaStore.Files.getContentUri("external")
        }

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.MediaColumns.IS_PENDING else "_data",
        )

        val selectionBuilder = StringBuilder()
        val selectionArgs = mutableListOf<String>()

        when (mediaKind) {
            ReviewMediaKind.All -> {
                selectionBuilder.append(
                    "(${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR " +
                        "${MediaStore.Files.FileColumns.MEDIA_TYPE}=?)"
                )
                selectionArgs.add(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
                selectionArgs.add(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    selectionBuilder.append(" AND ${MediaStore.MediaColumns.IS_PENDING}=0")
                }
            }
            ReviewMediaKind.Photos -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    selectionBuilder.append("${MediaStore.MediaColumns.IS_PENDING}=0")
                }
            }
            ReviewMediaKind.Videos -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    selectionBuilder.append("${MediaStore.MediaColumns.IS_PENDING}=0")
                }
            }
        }

        when (filter) {
            is PhotoFilter.Year -> {
                val year = filter.year
                val startTime = java.time.LocalDate.of(year, 1, 1)
                    .atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
                val endTime = java.time.LocalDate.of(year + 1, 1, 1)
                    .atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
                if (selectionBuilder.isNotEmpty()) selectionBuilder.append(" AND ")
                selectionBuilder.append("${MediaStore.MediaColumns.DATE_TAKEN}>=? AND ${MediaStore.MediaColumns.DATE_TAKEN}<?")
                selectionArgs.add(startTime.toString())
                selectionArgs.add(endTime.toString())
            }
            is PhotoFilter.Month -> {
                val year = filter.year
                val month = filter.month
                val startTime = java.time.LocalDate.of(year, month, 1)
                    .atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
                val endDate = if (month == 12) java.time.LocalDate.of(year + 1, 1, 1)
                else java.time.LocalDate.of(year, month + 1, 1)
                val endTime = endDate.atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
                if (selectionBuilder.isNotEmpty()) selectionBuilder.append(" AND ")
                selectionBuilder.append("${MediaStore.MediaColumns.DATE_TAKEN}>=? AND ${MediaStore.MediaColumns.DATE_TAKEN}<?")
                selectionArgs.add(startTime.toString())
                selectionArgs.add(endTime.toString())
            }
            is PhotoFilter.Album -> {
                // Albums are virtual — we don't filter at the query level for albums
                // The album filter UI shows album information, but the actual listing
                // uses bucket_id or similar. For simplicity, we treat albums as "all" here.
            }
            is PhotoFilter.All -> { /* no additional filter */ }
        }

        val sortOrder = if (sortNewestFirst)
            "${MediaStore.MediaColumns.DATE_TAKEN} DESC"
        else
            "${MediaStore.MediaColumns.DATE_TAKEN} ASC"

        val photos = mutableListOf<Photo>()

        contentResolver.query(
            collection,
            projection,
            selectionBuilder.toString().ifEmpty { null },
            selectionArgs.toTypedArray().ifEmpty { null },
            sortOrder,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val mimeType = cursor.getString(mimeCol) ?: ""
                val isVideo = mimeType.startsWith("video/")

                val contentUri = when {
                    isVideo -> ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                    )
                    else -> ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                    )
                }

                photos.add(
                    Photo(
                        id = id,
                        uri = contentUri.toString(),
                        displayName = cursor.getString(nameCol) ?: "",
                        dateTaken = cursor.getLong(dateCol) / 1000,
                        fileSize = cursor.getLong(sizeCol),
                        mimeType = mimeType,
                        width = 0,
                        height = 0,
                        isVideo = isVideo,
                    )
                )
            }
        }

        return photos
    }

    override suspend fun getAlbums(): List<AlbumInfo> {
        val buckets = mutableMapOf<String, MutableList<String>>()

        val projection = arrayOf(
            MediaStore.Images.ImageColumns.BUCKET_ID,
            MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
        )

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null, null, null,
        )?.use { cursor ->
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val id = cursor.getString(bucketIdCol)
                val name = cursor.getString(bucketNameCol) ?: "Unknown"
                buckets.getOrPut(id) { mutableListOf() }.add(name)
            }
        }

        contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Video.VideoColumns.BUCKET_ID,
                MediaStore.Video.VideoColumns.BUCKET_DISPLAY_NAME,
            ),
            null, null, null,
        )?.use { cursor ->
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.BUCKET_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val id = cursor.getString(bucketIdCol)
                val name = cursor.getString(bucketNameCol) ?: "Unknown"
                buckets.getOrPut(id) { mutableListOf() }.add(name)
            }
        }

        return buckets.map { (id, names) ->
            AlbumInfo(id = id, title = names.firstOrNull() ?: "Unknown", count = names.size)
        }
    }

    override suspend fun getAvailableYears(): List<Int> {
        val years = mutableSetOf<Int>()
        listOf(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .forEach { uri ->
                contentResolver.query(
                    uri,
                    arrayOf(MediaStore.MediaColumns.DATE_TAKEN),
                    null, null, null,
                )?.use { cursor ->
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
                    while (cursor.moveToNext()) {
                        val millis = cursor.getLong(dateCol)
                        if (millis > 0) {
                            val year = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .year
                            years.add(year)
                        }
                    }
                }
            }
        return years.sortedDescending()
    }

    override suspend fun getAvailableMonths(year: Int): List<Int> {
        val months = mutableSetOf<Int>()
        val startTime = java.time.LocalDate.of(year, 1, 1)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val endTime = java.time.LocalDate.of(year + 1, 1, 1)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()

        listOf(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .forEach { uri ->
                contentResolver.query(
                    uri,
                    arrayOf(MediaStore.MediaColumns.DATE_TAKEN),
                    "${MediaStore.MediaColumns.DATE_TAKEN}>=? AND ${MediaStore.MediaColumns.DATE_TAKEN}<?",
                    arrayOf(startTime.toString(), endTime.toString()),
                    null,
                )?.use { cursor ->
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
                    while (cursor.moveToNext()) {
                        val millis = cursor.getLong(dateCol)
                        if (millis > 0) {
                            val month = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .monthValue
                            months.add(month)
                        }
                    }
                }
            }
        return months.sortedDescending()
    }

    override suspend fun deletePhotos(photoIds: List<Long>): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val uris = photoIds.map { id ->
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                }
                val pendingIntent = MediaStore.createDeleteRequest(contentResolver, uris)
                // The deletion is handled via the launcher in the UI layer
                pendingIntent.intentSender
                true
            } else {
                val values = ContentValues()
                var deleted = 0
                photoIds.forEach { id ->
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                    )
                    val rows = contentResolver.delete(uri, null, null)
                    if (rows > 0) deleted++
                }
                deleted == photoIds.size
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getFileSize(photoId: Long): Long {
        val projection = arrayOf(MediaStore.MediaColumns.SIZE)
        val uri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), photoId)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE))
            }
        }
        return 0L
    }
}
