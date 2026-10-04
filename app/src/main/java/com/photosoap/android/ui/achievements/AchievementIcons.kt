package com.photosoap.android.ui.achievements

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.ui.graphics.vector.ImageVector
import com.photosoap.android.domain.model.Achievement

internal fun Achievement.materialIcon(): ImageVector = when (id) {
    "first_steps" -> Icons.Outlined.DirectionsWalk
    "spring_cleaning" -> Icons.Outlined.CleaningServices
    "memory_keeper" -> Icons.Outlined.Save
    "streak_master" -> Icons.Outlined.LocalFireDepartment
    "daily_devotee" -> Icons.Outlined.CalendarMonth
    "storage_saver" -> Icons.Outlined.Storage
    "century_club" -> Icons.Outlined.Collections
    "photo_pro" -> Icons.Outlined.PhotoLibrary
    "decisive" -> Icons.Outlined.Bolt
    "cleanup_champion" -> Icons.Outlined.EmojiEvents
    else -> Icons.Outlined.AutoAwesome
}
