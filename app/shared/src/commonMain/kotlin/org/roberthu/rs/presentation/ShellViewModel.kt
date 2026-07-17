package org.roberthu.rs.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.roberthu.rs.domain.AppLanguage
import org.roberthu.rs.i18n.AppI18n
import org.roberthu.rs.i18n.StringKeys
import org.roberthu.rs.platform.resolveInitialAppLanguage
import org.roberthu.rs.port.UserSettings
import org.roberthu.rs.port.UserSettingsStore
import org.roberthu.rs.shell.ShellUiAction
import org.roberthu.rs.shell.ShellUiState

class ShellViewModel(
    private val settingsStore: UserSettingsStore,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(
        ShellUiState(language = resolveInitialAppLanguage(storedTag = null)),
    )
    val state: StateFlow<ShellUiState> = mutableState.asStateFlow()

    private var settings = UserSettings()

    init {
        AppI18n.update(mutableState.value.language)
        scope.launch {
            runCatching { settingsStore.load() }
                .onSuccess { loaded ->
                    settings = loaded
                    val language = resolveInitialAppLanguage(loaded.language)
                    AppI18n.update(language)
                    mutableState.update {
                        it.copy(
                            darkMode = loaded.darkMode,
                            autoConnect = loaded.autoConnect,
                            language = language,
                            railCollapsed = loaded.railCollapsed ?: false,
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

            ShellUiAction.ToggleRail -> {
                val collapsed = !mutableState.value.railCollapsed
                settings = settings.copy(railCollapsed = collapsed)
                mutableState.update { it.copy(railCollapsed = collapsed) }
                persistSettings()
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

            is ShellUiAction.SetLanguage -> {
                settings = settings.copy(language = action.language.tag)
                AppI18n.update(action.language)
                mutableState.update { it.copy(language = action.language) }
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
            it.copy(
                bannerError = error.message
                    ?: AppI18n.t(StringKeys.Shell.ErrorSaveSettings),
            )
        }
    }
}
