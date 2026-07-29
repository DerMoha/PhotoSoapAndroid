package com.photosoap.android.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import com.photosoap.android.domain.model.SwipeDirection
import com.photosoap.android.ui.theme.AppMotion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun SwipeableCard(
    modifier: Modifier = Modifier,
    resetKey: Any? = Unit,
    enabled: Boolean = true,
    onSwiped: (SwipeDirection) -> Unit,
    onSwipeProgress: (Float) -> Unit = {},
    onTap: () -> Unit = {},
    overlayContent: @Composable (SwipeDirection?) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    var swipeDirection by remember { mutableStateOf<SwipeDirection?>(null) }
    var isAnimating by remember { mutableStateOf(false) }

    LaunchedEffect(resetKey) {
        offsetX.snapTo(0f)
        offsetY.snapTo(0f)
        rotation.snapTo(0f)
        scale.snapTo(1f)
        swipeDirection = null
        isAnimating = false
        onSwipeProgress(0f)
    }

    fun updateSwipeState(x: Float, threshold: Float) {
        val progress = (x / threshold).coerceIn(-1f, 1f)
        onSwipeProgress(progress)
        swipeDirection = when {
            progress > 0.35f -> SwipeDirection.KEEP
            progress < -0.35f -> SwipeDirection.DELETE
            else -> null
        }
        scope.launch {
            rotation.snapTo(progress * 8f)
            scale.snapTo(1f - progress.absoluteValue * 0.035f)
        }
    }

    fun commitSwipe(direction: SwipeDirection, cardWidth: Float, initialVelocity: Float = 0f) {
        isAnimating = true
        val targetX = if (direction == SwipeDirection.KEEP) cardWidth * 1.45f else -cardWidth * 1.45f
        scope.launch {
            coroutineScope {
                launch { offsetX.animateTo(targetX, AppMotion.cardSwipeOut, initialVelocity = initialVelocity) }
                launch { offsetY.animateTo(0f, AppMotion.cardSwipeOut) }
                launch {
                    rotation.animateTo(
                        if (direction == SwipeDirection.KEEP) 10f else -10f,
                        AppMotion.cardSwipeOut,
                    )
                }
                launch { scale.animateTo(0.97f, AppMotion.cardSwipeOut) }
            }
            isAnimating = false
            onSwiped(direction)
        }
    }

    fun snapBack(initialVelocity: Float = 0f) {
        scope.launch {
            coroutineScope {
                launch { offsetX.animateTo(0f, AppMotion.cardSnapBack, initialVelocity = initialVelocity) }
                launch { offsetY.animateTo(0f, AppMotion.cardSnapBack) }
                launch { rotation.animateTo(0f, AppMotion.cardSnapBack) }
                launch { scale.animateTo(1f, AppMotion.cardSnapBack) }
            }
            onSwipeProgress(0f)
            swipeDirection = null
        }
    }

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .graphicsLayer {
                    rotationZ = rotation.value
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .semantics {
                    onClick {
                        if (enabled && !isAnimating) onTap()
                        enabled && !isAnimating
                    }
                }
                .pointerInput(enabled, isAnimating) {
                    if (!enabled || isAnimating) return@pointerInput
                    awaitEachGesture {
                        val cardWidth = size.width.toFloat().coerceAtLeast(1f)
                        val swipeThreshold = cardWidth * 0.28f
                        val velocityThreshold = cardWidth * 2.2f
                        val velocityTracker = VelocityTracker()
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var pointerId = down.id
                        var dragged = false

                        scope.launch {
                            offsetX.stop()
                            offsetY.stop()
                            rotation.stop()
                            scale.stop()
                        }
                        velocityTracker.addPosition(down.uptimeMillis, down.position)

                        val slopChange = awaitTouchSlopOrCancellation(pointerId) { change, overSlop ->
                            change.consume()
                            dragged = true
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            val newX = offsetX.value + overSlop.x
                            val newY = (offsetY.value + overSlop.y).coerceIn(-cardWidth * 0.12f, cardWidth * 0.12f)
                            scope.launch {
                                offsetX.snapTo(newX)
                                offsetY.snapTo(newY)
                            }
                            updateSwipeState(newX, swipeThreshold)
                        }

                        if (slopChange == null) {
                            onTap()
                            return@awaitEachGesture
                        }

                        pointerId = slopChange.id
                        horizontalDrag(pointerId) { change ->
                            val dragAmount = change.positionChange()
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            val newX = offsetX.value + dragAmount.x
                            val newY = (offsetY.value + dragAmount.y).coerceIn(-cardWidth * 0.12f, cardWidth * 0.12f)
                            scope.launch {
                                offsetX.snapTo(newX)
                                offsetY.snapTo(newY)
                            }
                            updateSwipeState(newX, swipeThreshold)
                        }

                        if (!dragged) return@awaitEachGesture
                        val velocity = velocityTracker.calculateVelocity().x
                        val targetDirection = when {
                            offsetX.value > swipeThreshold || velocity > velocityThreshold -> SwipeDirection.KEEP
                            offsetX.value < -swipeThreshold || velocity < -velocityThreshold -> SwipeDirection.DELETE
                            else -> null
                        }

                        if (targetDirection != null) {
                            commitSwipe(targetDirection, cardWidth, velocity)
                        } else {
                            snapBack(velocity)
                        }
                    }
                },
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            content()
        }

        overlayContent(swipeDirection)
    }
}
