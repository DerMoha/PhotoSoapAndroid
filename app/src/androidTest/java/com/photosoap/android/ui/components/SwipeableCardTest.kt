package com.photosoap.android.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.photosoap.android.domain.model.SwipeDirection
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SwipeableCardTest {
    @get:Rule val compose = createComposeRule()
    private val controller = SwipeCardController()
    private val index = mutableIntStateOf(0)
    private val enabled = mutableStateOf(true)
    private val decisions = mutableListOf<SwipeDirection>()
    private var confirmations = 0
    private var ticks = 0

    private fun show() {
        compose.setContent {
            MaterialTheme {
                key(index.intValue) {
                    SwipeableCard(
                        modifier = Modifier.size(300.dp, 400.dp).testTag("card"),
                        resetKey = index.intValue,
                        controller = controller,
                        enabled = enabled.value,
                        onThreshold = { ticks++ },
                        onCommit = { confirmations++ },
                        onSwiped = { decisions.add(it); index.intValue++ },
                        nextContent = { Text("Upcoming") },
                    ) { Text("Photo ${index.intValue}") }
                }
            }
        }
    }

    @Test fun buttonsAnimateAndIgnoreDuplicateRequestsUntilHandoff() {
        show()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle {
            controller.swipe(SwipeDirection.KEEP)
            controller.swipe(SwipeDirection.DELETE)
            assertTrue(controller.busy)
            assertEquals(1, confirmations)
            assertTrue(decisions.isEmpty())
        }
        compose.mainClock.advanceTimeBy(2000)
        compose.runOnIdle {
            assertEquals(listOf(SwipeDirection.KEEP), decisions)
            assertFalse(controller.busy)
            controller.swipe(SwipeDirection.DELETE)
        }
        compose.mainClock.advanceTimeBy(2000)
        compose.runOnIdle { assertEquals(listOf(SwipeDirection.KEEP, SwipeDirection.DELETE), decisions) }
    }

    @Test fun shortSlowSwipeReturnsWithoutReviewing() {
        show()
        compose.onNodeWithTag("card").performTouchInput {
            swipe(center, center + Offset(width * .15f, 0f), durationMillis = 600)
        }
        compose.runOnIdle {
            assertTrue(decisions.isEmpty())
            assertEquals(0, confirmations)
            assertEquals(0, ticks)
        }
        compose.onNodeWithText("Photo 0").assertIsDisplayed()
    }

    @Test fun tapDuringReturnAnimationStillRestoresTheCard() {
        show()
        val originalX = compose.onNodeWithText("Photo 0").fetchSemanticsNode().boundsInRoot.center.x
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("card").performTouchInput {
            swipe(center, center + Offset(width * .15f, 0f), durationMillis = 600)
        }
        compose.mainClock.advanceTimeBy(32)
        compose.onNodeWithTag("card").performTouchInput { click(center) }
        compose.mainClock.advanceTimeBy(2000)
        val restoredX = compose.onNodeWithText("Photo 0").fetchSemanticsNode().boundsInRoot.center.x
        assertEquals(originalX, restoredX, 1f)
        compose.runOnIdle { assertTrue(decisions.isEmpty()) }
    }

    @Test fun thresholdCrossAndReturnDoesNotCommitOrRepeatTicks() {
        show()
        compose.onNodeWithTag("card").performTouchInput {
            down(center)
            moveTo(center + Offset(width * .4f, 0f), delayMillis = 300)
            moveTo(center + Offset(width * .05f, 0f), delayMillis = 300)
            moveTo(center + Offset(width * .4f, 0f), delayMillis = 300)
            moveTo(center, delayMillis = 300)
            advanceEventTime(300)
            up()
        }
        compose.runOnIdle {
            assertTrue(decisions.isEmpty())
            assertEquals(1, ticks)
            assertEquals(0, confirmations)
        }
    }

    @Test fun committedGestureConfirmsOnceAndPromotesNextPhoto() {
        show()
        compose.onNodeWithTag("card").performTouchInput {
            swipe(center, center - Offset(width * .45f, 0f), durationMillis = 400)
        }
        compose.runOnIdle {
            assertEquals(listOf(SwipeDirection.DELETE), decisions)
            assertEquals(1, confirmations)
            assertEquals(1, ticks)
        }
        compose.onNodeWithText("Photo 1").assertIsDisplayed()
        compose.onNodeWithText("Upcoming").assertDoesNotExist()
    }

    @Test fun disabledCardIgnoresButtonsAndGestures() {
        enabled.value = false
        show()
        compose.runOnIdle { controller.swipe(SwipeDirection.DELETE) }
        compose.onNodeWithTag("card").performTouchInput { swipeRight() }
        compose.runOnIdle {
            assertTrue(decisions.isEmpty())
            assertEquals(0, confirmations)
        }
    }

    @Test fun cancelledPointerDoesNotReviewEvenBeyondThreshold() {
        show()
        compose.onNodeWithTag("card").performTouchInput {
            down(center)
            moveTo(center + Offset(width * .45f, 0f), delayMillis = 300)
            cancel()
        }
        compose.runOnIdle {
            assertTrue(decisions.isEmpty())
            assertEquals(0, confirmations)
        }
    }
}
