package com.photosoap.android.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.photosoap.android.domain.model.SwipeDirection
import com.photosoap.android.ui.theme.AppMotion
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeableCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSwiped: (SwipeDirection) -> Unit,
    onSwipeProgress: (Float) -> Unit = {},
    onTap: () -> Unit = {},
    overlayContent: @Composable (SwipeDirection?) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp.value
    val swipeThreshold = screenWidth * 0.3f

    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    var swipeDirection by remember { mutableStateOf<SwipeDirection?>(null) }
    var isAnimating by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .graphicsLayer {
                    rotationZ = rotation.value
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .pointerInput(enabled, isAnimating) {
                    if (!enabled || isAnimating) return@pointerInput
                    detectDragGestures(
                        onDragEnd = {
                            if (offsetX.value > swipeThreshold) {
                                commitSwipe(SwipeDirection.KEEP)
                            } else if (offsetX.value < -swipeThreshold) {
                                commitSwipe(SwipeDirection.DELETE)
                            } else {
                                scope.launch {
                                    offsetX.animateTo(0f, AppMotion.cardSnapBack)
                                    offsetY.animateTo(0f, AppMotion.cardSnapBack)
                                    rotation.animateTo(0f, AppMotion.cardSnapBack)
                                    scale.animateTo(1f, AppMotion.cardSnapBack)
                                    onSwipeProgress(0f)
                                    swipeDirection = null
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                offsetX.animateTo(0f, AppMotion.cardSnapBack)
                                offsetY.animateTo(0f, AppMotion.cardSnapBack)
                                rotation.animateTo(0f, AppMotion.cardSnapBack)
                                scale.animateTo(1f, AppMotion.cardSnapBack)
                                onSwipeProgress(0f)
                                swipeDirection = null
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val newX = offsetX.value + dragAmount.x
                                val newY = offsetY.value + dragAmount.y
                                offsetX.snapTo(newX)
                                offsetY.snapTo(newY)
                                rotation.snapTo(newX * 0.05f)
                                val progress = (newX / swipeThreshold).coerceIn(-1f, 1f)
                                onSwipeProgress(progress)
                                swipeDirection = when {
                                    progress > 0.3f -> SwipeDirection.KEEP
                                    progress < -0.3f -> SwipeDirection.DELETE
                                    else -> null
                                }
                                scale.snapTo(1f - kotlin.math.abs(progress) * 0.05f)
                            }
                        },
                    )
                }
                .pointerInput(enabled, isAnimating) {
                    if (!enabled || isAnimating) return@pointerInput
                    awaitPointerEventScope {
                        awaitPointerEvent()
                        if (offsetX.value == 0f && offsetY.value == 0f) {
                            onTap()
                        }
                    }
                },
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F5F5),
            ),
        ) {
            content()
        }

        overlayContent(swipeDirection)
    }
}

private suspend fun Animatable<Float, *>.commitSwipe(
    direction: SwipeDirection,
    onSwiped: (SwipeDirection) -> Unit,
    screenWidth: Float,
) {
    val targetX = if (direction == SwipeDirection.KEEP) screenWidth * 2 else -screenWidth * 2
    launch { animateTo(targetX, AppMotion.cardSwipeOut) }
    launch { animateTo(0f, AppMotion.cardSwipeOut) }
    launch { animateTo(if (direction == SwipeDirection.KEEP) 15f else -15f, AppMotion.cardSwipeOut) }
    launch { animateTo(0.95f, AppMotion.cardSwipeOut) }
    onSwiped(direction)
}
