package com.photosoap.android.domain.model

data class AlbumInfo(
    val id: Long,
    val name: String,
    val count: Int,
    val isSmartAlbum: Boolean = false,
)
