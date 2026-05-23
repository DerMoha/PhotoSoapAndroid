package com.photosoap.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.photosoap.domain.model.Photo
import kotlin.math.roundToInt

enum class SwipeResult { Keep, Delete, None }

@Composable
fun SwipeableCard(
    photo: Photo,
    onSwipe: (SwipeResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val threshold = 300f

    val rotation by animateFloatAsState(
        targetValue = if (isDragging) (offsetX / 20f) else 0f,
        animationSpec = tween(100),
    )

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 0.98f else 1f,
        animationSpec = tween(100),
    )

    val keepAlpha by animateFloatAsState(
        targetValue = if (offsetX > 0) (offsetX / threshold).coerceIn(0f, 1f) * 0.8f else 0f,
        animationSpec = tween(100),
    )

    val deleteAlpha by animateFloatAsState(
        targetValue = if (offsetX < 0) (-offsetX / threshold).coerceIn(0f, 1f) * 0.8f else 0f,
        animationSpec = tween(100),
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // Keep overlay
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp)
                .graphicsLayer { alpha = keepAlpha },
        ) {
            Text(
                text = "KEEP",
                color = Color(0xFF4CAF50),
                style = MaterialTheme.typography.headlineLarge,
            )
        }

        // Delete overlay
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp)
                .graphicsLayer { alpha = deleteAlpha },
        ) {
            Text(
                text = "DELETE",
                color = Color(0xFFE53935),
                style = MaterialTheme.typography.headlineLarge,
            )
        }

        Card(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .graphicsLayer {
                    rotationZ = rotation
                    scaleX = scale
                    scaleY = scale
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onDragEnd = {
                            isDragging = false
                            if (offsetX > threshold) {
                                onSwipe(SwipeResult.Keep)
                            } else if (offsetX < -threshold) {
                                onSwipe(SwipeResult.Delete)
                            } else {
                                // Snap back
                                offsetX = 0f
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            offsetX = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX += dragAmount
                        },
                    )
                },
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                AsyncImage(
                    model = photo.uri,
                    contentDescription = photo.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )

                // Gradient overlay at bottom for metadata
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f),
                                ),
                            )
                        ),
                )

                // Photo info
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                ) {
                    Text(
                        text = photo.formattedDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                    )
                }

                // File size
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                ) {
                    Text(
                        text = photo.fileSizeFormatted,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }

                // Video indicator
                if (photo.isVideo) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = photo.formattedDuration,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}
