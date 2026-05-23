package com.photosoap.domain.model

data class AlbumInfo(
    val id: String,
    val title: String,
    val count: Int,
    val isSmartAlbum: Boolean = false,
)
