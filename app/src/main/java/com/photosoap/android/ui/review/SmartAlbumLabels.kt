package com.photosoap.android.ui.review

import androidx.annotation.StringRes
import com.photosoap.android.R
import com.photosoap.android.domain.model.SmartAlbum

@get:StringRes
val SmartAlbum.titleResource: Int
    get() = when (this) {
        SmartAlbum.SCREENSHOTS -> R.string.filter_screenshots
        SmartAlbum.VIDEOS -> R.string.filter_videos
        SmartAlbum.SELFIES -> R.string.filter_selfies
        SmartAlbum.FAVORITES -> R.string.filter_favorites
        SmartAlbum.PANORAMAS -> R.string.filter_panoramas
        SmartAlbum.ANIMATED -> R.string.filter_animated
        SmartAlbum.RAW -> R.string.filter_raw
    }
