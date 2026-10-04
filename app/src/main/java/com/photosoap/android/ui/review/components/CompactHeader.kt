package com.photosoap.android.ui.review.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.photosoap.android.R
import com.photosoap.android.ui.components.ProgressRing

@Composable
fun CompactHeader(
    hasActiveFilter: Boolean,
    dailyChallengeProgress: Int,
    dailyChallengeTarget: Int,
    dailyChallengeType: String,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progressLabel = stringResource(
        if (dailyChallengeType == "delete") R.string.daily_goal_deleted
        else R.string.daily_goal_reviewed,
        dailyChallengeProgress,
        dailyChallengeTarget,
    )
    Row(
        modifier = modifier.padding(start = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = dailyChallengeProgress.toFloat().coerceAtMost(dailyChallengeTarget.toFloat()),
                target = dailyChallengeTarget.toFloat(),
                size = 28.dp,
                strokeWidth = 3.dp,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                Text(
                    text = stringResource(if (dailyChallengeTarget > 0 && dailyChallengeProgress >= dailyChallengeTarget)
                        R.string.daily_goal_complete else R.string.daily_goal_title),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = progressLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        IconButton(onClick = onFilterClick) {
            Icon(
                imageVector = Icons.Filled.FilterList,
                contentDescription = stringResource(R.string.filter_title),
                tint = if (hasActiveFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
