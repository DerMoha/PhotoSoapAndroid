package com.photosoap.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.photosoap.android.ui.theme.AppColors

@Composable
fun AchievementCard(
    title: String,
    description: String,
    emoji: String,
    isUnlocked: Boolean,
    progress: Float,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUnlocked) 3.dp else 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) AppColors.AchievementContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(12.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isUnlocked)
                        AppColors.OnAchievementContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!isUnlocked) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }

            if (!isUnlocked) {
                ProgressRing(
                    progress = progress,
                    target = 1f,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    size = 24.dp,
                    strokeWidth = 3.dp,
                )
            }
        }
    }
}
