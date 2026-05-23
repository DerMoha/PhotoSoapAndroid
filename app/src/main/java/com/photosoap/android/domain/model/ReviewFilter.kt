package com.photosoap.android.domain.model

sealed interface ReviewFilter {
    data object All : ReviewFilter
    data class Year(val year: Int) : ReviewFilter
    data class Month(val year: Int, val month: Int) : ReviewFilter
    data class Album(val albumId: Long, val albumName: String) : ReviewFilter
}
