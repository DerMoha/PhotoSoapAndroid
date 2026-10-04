package com.photosoap.android

import androidx.lifecycle.viewModelScope
import com.photosoap.android.data.local.datastore.SettingsDataStore
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val context = mockk<android.content.Context>(relaxed = true)
    private val settings = mockk<SettingsDataStore>(relaxed = true)
    private val permissions = mockk<PermissionChecker>()
    private var viewModel: MainViewModel? = null

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { settings.hasSeenOnboarding } returns flowOf(false)
    }

    @AfterEach
    fun tearDown() {
        viewModel?.viewModelScope?.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `onboarding stores explicit analytics choice before continuing`() = runTest(dispatcher) {
        every { permissions.hasMediaPermissions() } returns true
        every { permissions.getMediaAccess() } returns MediaAccess.FULL
        viewModel = MainViewModel(context, settings, permissions)

        viewModel!!.onGetStarted(shareAnalytics = true)

        coVerifyOrder {
            settings.setAnalyticsEnabled(true)
            settings.setOnboardingSeen()
        }
        assertEquals(MainUiState.Main(isLimitedAccess = false), viewModel!!.uiState.value)
    }

    @Test
    fun `selected media grant wins over a false permission callback`() = runTest(dispatcher) {
        every { permissions.hasMediaPermissions() } returns true
        every { permissions.getMediaAccess() } returns MediaAccess.LIMITED
        viewModel = MainViewModel(context, settings, permissions)

        viewModel!!.onPermissionResult(granted = false)

        assertEquals(MainUiState.Main(isLimitedAccess = true), viewModel!!.uiState.value)
    }

    @Test
    fun `denied permission remains recoverable through settings`() = runTest(dispatcher) {
        every { permissions.hasMediaPermissions() } returns false
        viewModel = MainViewModel(context, settings, permissions)

        viewModel!!.onPermissionResult(granted = false)

        assertEquals(MainUiState.PermissionDenied, viewModel!!.uiState.value)
    }
    @Test
    fun `returning from settings refreshes limited full and revoked access`() = runTest(dispatcher) {
        every { settings.hasSeenOnboarding } returns flowOf(true)
        every { permissions.hasMediaPermissions() } returns true
        every { permissions.getMediaAccess() } returns MediaAccess.LIMITED
        viewModel = MainViewModel(context, settings, permissions)
        assertEquals(MainUiState.Main(true), viewModel!!.uiState.value)

        every { permissions.getMediaAccess() } returns MediaAccess.FULL
        viewModel!!.onResume()
        assertEquals(MainUiState.Main(false), viewModel!!.uiState.value)

        every { permissions.getMediaAccess() } returns MediaAccess.NONE
        viewModel!!.onResume()
        assertEquals(MainUiState.PermissionDenied, viewModel!!.uiState.value)
    }

}
