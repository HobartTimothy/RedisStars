package org.roberthu.rs.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.roberthu.rs.port.UserSettings
import org.roberthu.rs.port.UserSettingsStore
import org.roberthu.rs.shell.ShellUiAction
import org.roberthu.rs.shell.ShellUiState

class ShellViewModel(
    private val settingsStore: UserSettingsStore,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(ShellUiState())
    val state: StateFlow<ShellUiState> = mutableState.asStateFlow()

    private var settings = UserSettings()

    init {
        scope.launch {
            runCatching { settingsStore.load() }
                .onSuccess { loaded ->
                    settings = loaded
                    mutableState.update {
                        it.copy(
                            darkMode = loaded.darkMode,
                            autoConnect = loaded.autoConnect,
                            bannerError = null,
                        )
                    }
                }
                .onFailure(::showError)
        }
    }

    fun dispatch(action: ShellUiAction) {
        when (action) {
            is ShellUiAction.Navigate -> mutableState.update {
                it.copy(destination = action.destination)
            }

            ShellUiAction.ToggleRail -> mutableState.update {
                it.copy(railCollapsed = !it.railCollapsed)
            }

            is ShellUiAction.SetDarkMode -> {
                settings = settings.copy(darkMode = action.enabled)
                mutableState.update { it.copy(darkMode = action.enabled) }
                persistSettings()
            }

            is ShellUiAction.SetAutoConnect -> {
                settings = settings.copy(autoConnect = action.enabled)
                mutableState.update { it.copy(autoConnect = action.enabled) }
                persistSettings()
            }

            ShellUiAction.DismissError -> mutableState.update {
                it.copy(bannerError = null)
            }
        }
    }

    private fun persistSettings() {
        val updatedSettings = settings
        scope.launch {
            runCatching { settingsStore.save(updatedSettings) }
                .onFailure(::showError)
        }
    }

    private fun showError(error: Throwable) {
        mutableState.update {
            it.copy(bannerError = error.message ?: "Unable to save settings")
        }
    }
}
