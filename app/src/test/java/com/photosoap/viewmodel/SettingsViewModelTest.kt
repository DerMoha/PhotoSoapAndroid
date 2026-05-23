package com.photosoap.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SettingsViewModelTest {

    @Test
    fun `initial state has correct defaults`() {
        // This is a basic smoke test for the view model initialization pattern
        val initialState = com.photosoap.domain.model.StatsDisplay()
        assertNotNull(initialState)
        assertEquals(0, initialState.totalReviewed)
    }
}
