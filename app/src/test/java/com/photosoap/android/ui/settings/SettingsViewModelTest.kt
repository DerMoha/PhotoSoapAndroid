package com.photosoap.android.ui.settings

import com.photosoap.android.domain.model.AccentColor
import com.photosoap.android.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import com.photosoap.android.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val settingsRepository = mockk<SettingsRepository>()
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { settingsRepository.hapticsEnabled } returns flowOf(true)
        every { settingsRepository.useDeleteQueue } returns flowOf(true)
        every { settingsRepository.analyticsEnabled } returns flowOf(false)
        every { settingsRepository.accentColor } returns flowOf(AccentColor.TEAL)
        coEvery { settingsRepository.setAccentColor(any()) } returns Unit
        every { settingsRepository.themeMode } returns flowOf(ThemeMode.SYSTEM)
        every { settingsRepository.dynamicColor } returns flowOf(true)
        coEvery { settingsRepository.setThemeMode(any()) } returns Unit
        coEvery { settingsRepository.setDynamicColor(any()) } returns Unit
        coEvery { settingsRepository.setHapticsEnabled(any()) } returns Unit
        coEvery { settingsRepository.setUseDeleteQueue(any()) } returns Unit
        coEvery { settingsRepository.setAnalyticsEnabled(any()) } returns Unit
        coEvery { settingsRepository.resetOnboarding() } returns Unit
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state reflects default flows`() = runTest(testDispatcher) {
        val vm = SettingsViewModel(settingsRepository)
        val state = vm.uiState.value
        assertTrue(state.hapticsEnabled)
        assertTrue(state.useDeleteQueue)
        assertFalse(state.analyticsEnabled)
    }

    @Test
    fun `toggle haptics calls repository`() = runTest(testDispatcher) {
        val vm = SettingsViewModel(settingsRepository)
        vm.toggleHaptics(false)
        coVerify { settingsRepository.setHapticsEnabled(false) }
    }

    @Test
    fun `toggle delete queue calls repository`() = runTest(testDispatcher) {
        val vm = SettingsViewModel(settingsRepository)
        vm.toggleDeleteQueue(false)
        coVerify { settingsRepository.setUseDeleteQueue(false) }
    }

    @Test
    fun `toggle analytics calls repository`() = runTest(testDispatcher) {
        val vm = SettingsViewModel(settingsRepository)
        vm.toggleAnalytics(true)
        coVerify { settingsRepository.setAnalyticsEnabled(true) }
    }

    @Test
    fun `reset onboarding calls repository`() = runTest(testDispatcher) {
        val vm = SettingsViewModel(settingsRepository)
        vm.resetOnboarding()
        coVerify { settingsRepository.resetOnboarding() }
    }

    @Test
    fun `new flows update state`() = runTest(testDispatcher) {
        every { settingsRepository.analyticsEnabled } returns flowOf(true)
        val vm = SettingsViewModel(settingsRepository)
        assertEquals(true, vm.uiState.value.analyticsEnabled)
    }
    @Test
    fun `appearance choices persist and flow changes update settings`() = runTest(testDispatcher) {
        val mode = MutableStateFlow(ThemeMode.SYSTEM)
        val dynamic = MutableStateFlow(true)
        every { settingsRepository.themeMode } returns mode
        every { settingsRepository.dynamicColor } returns dynamic
        coEvery { settingsRepository.setThemeMode(any()) } coAnswers { mode.value = firstArg() }
        coEvery { settingsRepository.setDynamicColor(any()) } coAnswers { dynamic.value = firstArg() }
        val vm = SettingsViewModel(settingsRepository)
        vm.setThemeMode(ThemeMode.DARK)
        vm.toggleDynamicColor(false)
        assertEquals(ThemeMode.DARK, vm.uiState.value.themeMode)
        assertFalse(vm.uiState.value.dynamicColor)
        coVerify { settingsRepository.setThemeMode(ThemeMode.DARK) }
        coVerify { settingsRepository.setDynamicColor(false) }
    }
    @Test
    fun `custom accent reflects persisted palette and disables wallpaper colors`() = runTest(testDispatcher) {
        val accent = MutableStateFlow(AccentColor.TEAL)
        val dynamic = MutableStateFlow(true)
        every { settingsRepository.accentColor } returns accent
        every { settingsRepository.dynamicColor } returns dynamic
        coEvery { settingsRepository.setAccentColor(any()) } coAnswers {
            accent.value = firstArg()
            dynamic.value = false
        }
        val vm = SettingsViewModel(settingsRepository)
        vm.setAccentColor(AccentColor.PURPLE)
        assertEquals(AccentColor.PURPLE, vm.uiState.value.accentColor)
        assertFalse(vm.uiState.value.dynamicColor)
        coVerify { settingsRepository.setAccentColor(AccentColor.PURPLE) }
    }
}
