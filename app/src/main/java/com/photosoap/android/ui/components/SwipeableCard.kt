package com.photosoap.android.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import com.photosoap.android.domain.model.SwipeDirection
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class SwipeCardController {
    var busy by mutableStateOf(false)
        internal set
    internal var action: ((SwipeDirection) -> Unit)? = null

    fun swipe(direction: SwipeDirection) {
        if (!busy) action?.invoke(direction)
    }
}

@Composable
fun SwipeableCard(
    modifier: Modifier = Modifier,
    resetKey: Any? = Unit,
    enabled: Boolean = true,
    onSwiped: (SwipeDirection) -> Unit,
    controller: SwipeCardController? = null,
    nextContent: (@Composable () -> Unit)? = null,
    onCommit: (SwipeDirection) -> Unit = {},
    onThreshold: () -> Unit = {},
    onSwipeProgress: (Float) -> Unit = {},
    onTap: () -> Unit = {},
    overlayContent: @Composable (SwipeDirection?, Float) -> Unit = { _, _ -> },
    content: @Composable () -> Unit,
) {
    val swipeOutMotion = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val snapBackMotion = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    var swipeDirection by remember { mutableStateOf<SwipeDirection?>(null) }
    var isAnimating by remember { mutableStateOf(false) }
    var cardWidth by remember { mutableStateOf(1f) }
    val currentEnabled by rememberUpdatedState(enabled)
    val currentOnSwiped by rememberUpdatedState(onSwiped)
    val currentOnCommit by rememberUpdatedState(onCommit)
    val currentOnThreshold by rememberUpdatedState(onThreshold)
    var motionJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var crossedThreshold by remember { mutableStateOf(false) }

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
        val pastThreshold = x.absoluteValue >= threshold
        if (pastThreshold && !crossedThreshold) {
            crossedThreshold = true
            currentOnThreshold()
        }
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
        if (isAnimating || !currentEnabled) return
        isAnimating = true
        controller?.busy = true
        swipeDirection = direction
        currentOnCommit(direction)
        val targetX = if (direction == SwipeDirection.KEEP) cardWidth * 1.45f else -cardWidth * 1.45f
        motionJob?.cancel()
        motionJob = scope.launch {
            coroutineScope {
                launch {
                    var delivered = false
                    offsetX.animateTo(targetX, swipeOutMotion, initialVelocity = initialVelocity) {
                        // Advance when the outgoing card clears the viewport, not after the spring tail.
                        if (!delivered && value.absoluteValue >= cardWidth * 1.15f) {
                            delivered = true
                            currentOnSwiped(direction)
                        }
                    }
                    if (!delivered) currentOnSwiped(direction)
                }
                launch { offsetY.animateTo(0f, swipeOutMotion) }
                launch {
                    rotation.animateTo(
                        if (direction == SwipeDirection.KEEP) 10f else -10f,
                        swipeOutMotion,
                    )
                }
                launch { scale.animateTo(0.97f, swipeOutMotion) }
            }

        }
    }

    fun snapBack(initialVelocity: Float = 0f) {
        motionJob?.cancel()
        motionJob = scope.launch {
            coroutineScope {
                launch { offsetX.animateTo(0f, snapBackMotion, initialVelocity = initialVelocity) }
                launch { offsetY.animateTo(0f, snapBackMotion) }
                launch { rotation.animateTo(0f, snapBackMotion) }
                launch { scale.animateTo(1f, snapBackMotion) }
            }
            onSwipeProgress(0f)
            swipeDirection = null
            crossedThreshold = false
        }
    }

    DisposableEffect(controller, resetKey) {
        controller?.busy = false
        controller?.action = { commitSwipe(it, cardWidth) }
        onDispose { controller?.action = null; controller?.busy = false }
    }

    Box(modifier = modifier.onSizeChanged { cardWidth = it.width.toFloat().coerceAtLeast(1f) }) {
        if (nextContent != null) {
            val reveal = (offsetX.value.absoluteValue / cardWidth).coerceIn(0f, 1f)
            Card(
                modifier = Modifier.fillMaxSize().clearAndSetSemantics { }.graphicsLayer {
                    scaleX = 0.96f + reveal * 0.04f
                    scaleY = scaleX
                    translationY = (1f - reveal) * 12.dp.toPx()
                },
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) { nextContent() }
        }
        Card(
            modifier = Modifier.fillMaxSize()
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
                .pointerInput(resetKey, enabled, isAnimating) {
                    if (!enabled || isAnimating) return@pointerInput
                    awaitEachGesture {
                        val cardWidth = size.width.toFloat().coerceAtLeast(1f)
                        val swipeThreshold = cardWidth * 0.28f
                        val velocityThreshold = cardWidth * 2.2f
                        val velocityTracker = VelocityTracker()
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var pointerId = down.id
                        var dragged = false
                        crossedThreshold = false
                        motionJob?.cancel()
                        var dragX = offsetX.value
                        var dragY = offsetY.value

                        scope.launch {
                            offsetX.stop()
                            offsetY.stop()
                            rotation.stop()
                            scale.stop()
                        }
                        velocityTracker.addPosition(down.uptimeMillis, down.position)

                        val slopChange = awaitTouchSlopOrCancellation(pointerId) { change, overSlop ->
                            // Let the review's compact-height scroll container own
                            // vertical drags instead of stealing them for a card swipe.
                            if (overSlop.y.absoluteValue > overSlop.x.absoluteValue) return@awaitTouchSlopOrCancellation
                            change.consume()
                            dragged = true
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            val newX = dragX + overSlop.x
                            dragX = newX
                            scope.launch {
                                offsetX.snapTo(newX)
                            }
                            updateSwipeState(newX, swipeThreshold)
                        }

                        if (slopChange == null) {
                            val up = currentEvent.changes.firstOrNull { it.id == pointerId }
                            if (up != null && up.changedToUpIgnoreConsumed() && !up.isConsumed &&
                                (up.position - down.position).getDistance() <= viewConfiguration.touchSlop
                            ) onTap()
                            return@awaitEachGesture
                        }

                        pointerId = slopChange.id
                        val completed = horizontalDrag(pointerId) { change ->
                            val dragAmount = change.positionChange()
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            val newX = dragX + dragAmount.x
                            dragX = newX
                            val newY = (dragY + dragAmount.y).coerceIn(-cardWidth * 0.12f, cardWidth * 0.12f)
                            dragY = newY
                            scope.launch {
                                offsetX.snapTo(newX)
                                offsetY.snapTo(newY)
                            }
                            updateSwipeState(newX, swipeThreshold)
                        }

                        if (!completed) {
                            snapBack()
                            return@awaitEachGesture
                        }
                        if (!dragged) return@awaitEachGesture
                        val velocity = velocityTracker.calculateVelocity().x
                        val targetDirection = when {
                            dragX > swipeThreshold || velocity > velocityThreshold -> SwipeDirection.KEEP
                            dragX < -swipeThreshold || velocity < -velocityThreshold -> SwipeDirection.DELETE
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
            Box(Modifier.fillMaxSize()) {
                content()
                overlayContent(swipeDirection, (offsetX.value / (cardWidth * 0.28f)).coerceIn(-1f, 1f))
            }
        }
    }
}
