package com.photosoap.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.photosoap.android.ui.theme.AppColors
import com.photosoap.android.util.FileSize

@Composable
fun DeleteQueueTray(
    itemCount: Int,
    totalFileSize: Long,
    onUndo: () -> Unit,
    onViewList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = AppColors.DeleteContainer,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$itemCount",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.OnDeleteContainer,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "items to delete (${FileSize.format(totalFileSize)})",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.OnDeleteContainer,
            )
        }
        Row {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = "Undo",
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onUndo),
                tint = AppColors.OnDeleteContainer,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onViewList,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.OnDeleteContainer,
                    contentColor = AppColors.DeleteContainer,
                ),
            ) {
                Text("List")
            }
        }
    }
}
