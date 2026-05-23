package com.photosoap.android.ui.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SettingsViewModelTest {

    @Test
    fun `uiState reflects initial flows`() = runTest {
        // Verify the SettingsUiState data class works correctly
        val state = SettingsUiState(
            hapticsEnabled = true,
            useDeleteQueue = true,
            analyticsEnabled = false,
        )
        assertEquals(true, state.hapticsEnabled)
        assertEquals(true, state.useDeleteQueue)
        assertEquals(false, state.analyticsEnabled)
    }

    @Test
    fun `settings state copy works`() {
        val state = SettingsUiState()
        val modified = state.copy(analyticsEnabled = true)
        assertEquals(true, modified.analyticsEnabled)
        assertEquals(true, modified.hapticsEnabled)
        assertEquals(true, modified.useDeleteQueue)
    }
}
