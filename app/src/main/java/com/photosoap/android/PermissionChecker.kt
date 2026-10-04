package com.photosoap.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun hasMediaPermissions(): Boolean {
        return getMediaAccess() != MediaAccess.NONE
    }

    fun getMediaAccess(): MediaAccess {
        val imagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val videoPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            imagePermission
        }

        val selectedMediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            )
        } else {
            PackageManager.PERMISSION_DENIED
        }

        val hasImages = imagePermission == PackageManager.PERMISSION_GRANTED
        val hasVideos = videoPermission == PackageManager.PERMISSION_GRANTED
        val hasSelected = selectedMediaPermission == PackageManager.PERMISSION_GRANTED
        return mediaAccessForGrants(Build.VERSION.SDK_INT, hasImages, hasVideos, hasSelected)
    }

    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
            )
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}

enum class MediaAccess {
    NONE,
    LIMITED,
    FULL,
}

/** Before Android 13 both media kinds share READ_EXTERNAL_STORAGE. */
internal fun mediaAccessForGrants(sdk: Int, images: Boolean, videos: Boolean, selected: Boolean): MediaAccess = when {
    sdk < 33 -> if (images) MediaAccess.FULL else MediaAccess.NONE
    images && videos -> MediaAccess.FULL
    images || videos || (sdk >= 34 && selected) -> MediaAccess.LIMITED
    else -> MediaAccess.NONE
}
