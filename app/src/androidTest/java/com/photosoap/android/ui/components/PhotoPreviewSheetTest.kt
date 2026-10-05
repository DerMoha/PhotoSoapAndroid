package com.photosoap.android.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performTouchInput
import com.photosoap.android.domain.model.Photo
import com.photosoap.android.ui.review.components.PhotoPreviewSheet
import org.junit.Rule
import org.junit.Test
import java.text.NumberFormat

class PhotoPreviewSheetTest {
    @get:Rule val compose = createComposeRule()

    private fun zoom(scale: Double) = SemanticsMatcher.expectValue(
        SemanticsProperties.StateDescription, "Zoom " + NumberFormat.getPercentInstance().format(scale))

    @Test fun doubleTapZoomsAndThenResets() {
        compose.setContent {
            MaterialTheme {
                PhotoPreviewSheet(Photo(1, "content://test/preview", "photo.jpg", "image/jpeg", 0, 0, 1024, 1000, 1000), onDismiss = {})
            }
        }
        compose.onNode(zoom(1.0)).performTouchInput { doubleClick() }
        compose.onNode(zoom(2.5)).assertExists()
        compose.onNode(zoom(2.5)).performTouchInput { doubleClick() }
        compose.onNode(zoom(1.0)).assertExists()
    }
}
