package com.photosoap.android.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.photosoap.android.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class ReviewActionDockTest {
    @get:Rule val compose = createComposeRule()
    private val base = InstrumentationRegistry.getInstrumentation().targetContext
    private val config = Configuration(base.resources.configuration).apply { setLocale(Locale.GERMAN) }
    private val german = base.createConfigurationContext(config)
    private val actions = mutableListOf<String>()

    private fun show(fontScale: Float, enabled: Boolean = true) {
        compose.setContent {
            CompositionLocalProvider(
                LocalContext provides german,
                LocalConfiguration provides config,
                LocalDensity provides Density(LocalDensity.current.density, fontScale),
            ) {
                MaterialTheme {
                    Box(Modifier.width(360.dp)) {
                        ReviewActionDock(9, enabled,
                            onDelete = { actions.add("delete") }, onKeep = { actions.add("keep") },
                            onUndo = { actions.add("undo") }, onOpenList = { actions.add("list") },
                        )
                    }
                }
            }
        }
    }

    @Test fun germanControlsFitOneRowAndRemainActionable() {
        show(1f)
        val labels = listOf(R.string.review_delete, R.string.review_keep, R.string.undo)
            .map { german.getString(it) } + german.getString(R.string.review_queue_chip, 9)
        val nodes = labels.map { label ->
            if (label == german.getString(R.string.undo)) compose.onNodeWithContentDescription(label)
            else compose.onNodeWithText(label)
        }
        val bounds = nodes.map { it.fetchSemanticsNode().boundsInRoot }
        // Text baselines and the icon differ, but each control occupies the same row.
        assertTrue(bounds.maxOf { it.top } < bounds.minOf { it.bottom })
        nodes.forEach { it.assertIsDisplayed().performClick() }
        compose.runOnIdle { assertEquals(listOf("delete", "keep", "undo", "list"), actions) }
    }

    @Test fun largeGermanTextWrapsWithoutLosingQueueActions() {
        show(2f)
        compose.onNodeWithText(german.getString(R.string.review_delete)).assertIsDisplayed().performClick()
        compose.onNodeWithText(german.getString(R.string.review_keep)).assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription(german.getString(R.string.undo)).assertIsDisplayed().performClick()
        compose.onNodeWithText(german.getString(R.string.review_queue_chip, 9)).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(listOf("delete", "keep", "undo", "list"), actions) }
    }

    @Test fun busyDockDisablesEveryAction() {
        show(1f, enabled = false)
        compose.onNodeWithText(german.getString(R.string.review_delete)).assertIsNotEnabled()
        compose.onNodeWithText(german.getString(R.string.review_keep)).assertIsNotEnabled()
        compose.onNodeWithContentDescription(german.getString(R.string.undo)).assertIsNotEnabled()
        compose.onNodeWithText(german.getString(R.string.review_queue_chip, 9)).assertIsNotEnabled()
    }
}
