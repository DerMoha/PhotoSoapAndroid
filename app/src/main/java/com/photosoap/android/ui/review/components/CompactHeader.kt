package com.photosoap.android.ui.review.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.photosoap.android.R
import com.photosoap.android.ui.components.ProgressRing

@Composable
fun CompactHeader(
    todayReviewCount: Int,
    hasActiveFilter: Boolean,
    dailyChallengeProgress: Int,
    dailyChallengeTarget: Int,
    dailyChallengeType: String,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val challengeLabel = stringResource(
        when (dailyChallengeType) {
            "delete" -> R.string.daily_challenge_delete
            "streak" -> R.string.daily_challenge_streak
            else -> R.string.daily_challenge_review
        },
    )
    Row(
        modifier = modifier.padding(start = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = stringResource(R.string.today_review_count),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$todayReviewCount",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = dailyChallengeProgress.toFloat(),
                target = dailyChallengeTarget.toFloat(),
                size = 24.dp,
                strokeWidth = 3.dp,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$challengeLabel $dailyChallengeProgress/$dailyChallengeTarget",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
