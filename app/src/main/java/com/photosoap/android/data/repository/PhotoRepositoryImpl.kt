package com.photosoap.android.data.repository

import com.photosoap.android.data.local.db.dao.ReviewedPhotoDao
import com.photosoap.android.data.local.db.entity.ReviewedPhotoEntity
import com.photosoap.android.domain.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PhotoRepositoryImpl @Inject constructor(
    private val reviewedPhotoDao: ReviewedPhotoDao,
) : PhotoRepository {

    override fun observeReviewedPhotoUris(): Flow<List<String>> {
        return reviewedPhotoDao.observeAll().map { entities ->
            entities.map { it.photoUri }
        }
    }

    override suspend fun isReviewed(uri: String): Boolean {
        return reviewedPhotoDao.countByUri(uri) > 0
    }

    override suspend fun markReviewed(uri: String) {
        reviewedPhotoDao.insert(
            ReviewedPhotoEntity(
                photoUri = uri,
                reviewDate = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun unmarkReviewed(uri: String) {
        reviewedPhotoDao.deleteByUri(uri)
    }

    override suspend fun clearReviewed() {
        reviewedPhotoDao.deleteAll()
    }
}
