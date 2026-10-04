package com.photosoap.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ToastOverlay(
    visible: Boolean,
    message: String,
    modifier: Modifier = Modifier,
    emoji: String = "",
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier,
    ) {
        Snackbar(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (emoji.isNotEmpty()) {
                    Icon(
                        imageVector = when (emoji) {
                            "❌", "⚠️" -> Icons.Outlined.WarningAmber
                            "🏆", "🎉" -> Icons.Outlined.EmojiEvents
                            "✅" -> Icons.Outlined.CheckCircle
                            else -> Icons.Outlined.Info
                        },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(message)
            }
        }
    }
}
