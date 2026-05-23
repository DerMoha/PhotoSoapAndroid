package com.photosoap.domain.model

sealed class PhotoFilter {
    data object All : PhotoFilter()
    data class Album(val id: String, val title: String) : PhotoFilter()
    data class Year(val year: Int) : PhotoFilter()
    data class Month(val year: Int, val month: Int) : PhotoFilter()
}
