package com.photosoap.android.ui.components

import android.content.Context
import androidx.compose.ui.unit.IntSize
import coil3.request.ImageRequest
import coil3.size.Scale
import com.photosoap.android.domain.model.Photo

/** Identical decode dimensions and crop for both prefetch and the displayed card. */
fun reviewImageRequest(context: Context, photo: Photo, size: IntSize): ImageRequest =
    ImageRequest.Builder(context)
        .data(photo.uri)
        .size(size.width, size.height)
        .scale(Scale.FILL)
        .build()
