package org.roberthu.rs.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.roberthu.rs.port.UserSettings
import org.roberthu.rs.port.UserSettingsStore
import org.roberthu.rs.shell.ShellDestination
import org.roberthu.rs.shell.ShellUiAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ShellViewModelTest {

    @Test
    fun navigate_updatesDestination() = runTest {
        val viewModel = ShellViewModel(FakeUserSettingsStore(), this)

        viewModel.dispatch(ShellUiAction.Navigate(ShellDestination.Settings))

        assertEquals(ShellDestination.Settings, viewModel.state.value.destination)
    }

    @Test
    fun toggleRail_flipsCollapsedState() = runTest {
        val viewModel = ShellViewModel(FakeUserSettingsStore(), this)

        viewModel.dispatch(ShellUiAction.ToggleRail)
        assertTrue(viewModel.state.value.railCollapsed)

        viewModel.dispatch(ShellUiAction.ToggleRail)
        assertFalse(viewModel.state.value.railCollapsed)
    }

    @Test
    fun setDarkMode_persistsUpdatedSettings() = runTest {
        val store = FakeUserSettingsStore(UserSettings(darkMode = true, autoConnect = true))
        val viewModel = ShellViewModel(store, this)
        advanceUntilIdle()

        viewModel.dispatch(ShellUiAction.SetDarkMode(enabled = false))
        advanceUntilIdle()

        assertFalse(viewModel.state.value.darkMode)
        assertEquals(
            UserSettings(darkMode = false, autoConnect = true),
            store.savedSettings,
        )
    }

    @Test
    fun setAutoConnect_persistsUpdatedSettings() = runTest {
        val store = FakeUserSettingsStore(UserSettings(darkMode = false))
        val viewModel = ShellViewModel(store, this)
        advanceUntilIdle()

        viewModel.dispatch(ShellUiAction.SetAutoConnect(enabled = true))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.autoConnect)
        assertEquals(
            UserSettings(darkMode = false, autoConnect = true),
            store.savedSettings,
        )
    }

    @Test
    fun dismissError_clearsSettingsFailureBanner() = runTest {
        val viewModel = ShellViewModel(
            settingsStore = FakeUserSettingsStore(loadFailure = IllegalStateException("load failed")),
            scope = this,
        )
        advanceUntilIdle()
        assertEquals("load failed", viewModel.state.value.bannerError)

        viewModel.dispatch(ShellUiAction.DismissError)

        assertEquals(null, viewModel.state.value.bannerError)
    }
}

private class FakeUserSettingsStore(
    private val initial: UserSettings = UserSettings(),
    private val loadFailure: Throwable? = null,
) : UserSettingsStore {
    var savedSettings: UserSettings? = null

    override suspend fun load(): UserSettings {
        loadFailure?.let { throw it }
        return initial
    }

    override suspend fun save(settings: UserSettings) {
        savedSettings = settings
    }
}
