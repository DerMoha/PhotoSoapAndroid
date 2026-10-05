package com.photosoap.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.photosoap.android.R

/** One compact row normally; wraps rather than clipping at larger accessibility font sizes. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReviewActionDock(
    pendingCount: Int,
    enabled: Boolean,
    onDelete: () -> Unit,
    onKeep: () -> Unit,
    onUndo: () -> Unit,
    onOpenList: () -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        maxItemsInEachRow = if (LocalDensity.current.fontScale > 1.3f) 2 else 4,
    ) {
        FilledTonalButton(
            shapes = ButtonDefaults.shapesFor(48.dp),
            onClick = onDelete,
            enabled = enabled,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
        ) { Text(stringResource(R.string.review_delete)) }
        Button(
            shapes = ButtonDefaults.shapesFor(48.dp),
            onClick = onKeep,
            enabled = enabled,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
        ) { Text(stringResource(R.string.review_keep)) }
        if (pendingCount > 0) {
            IconButton(onClick = onUndo, enabled = enabled) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.undo))
            }
            FilledTonalButton(
                shapes = ButtonDefaults.shapesFor(48.dp),
                onClick = onOpenList,
                enabled = enabled,
                modifier = Modifier.heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
            ) { Text(stringResource(R.string.review_queue_chip, pendingCount)) }
        }
    }
}
