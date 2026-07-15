package org.roberthu.rs.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.DeploymentMode
import org.roberthu.rs.port.ConnectionProfileStore
import org.roberthu.rs.port.ConnectionState
import org.roberthu.rs.port.RedisConnectionPort
import org.roberthu.rs.usecase.ManageConnections

data class ConnectionsUiState(
    val profiles: List<ConnectionProfile> = emptyList(),
    val selectedProfileId: String? = null,
    val draft: ConnectionProfile? = null,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val busy: Boolean = false,
    val testSucceeded: Boolean = false,
    val error: String? = null,
    val pendingDelete: ConnectionProfile? = null,
)

class ConnectionsViewModel(
    profileStore: ConnectionProfileStore,
    connectionPort: RedisConnectionPort,
    private val scope: CoroutineScope,
) {
    private val manage = ManageConnections(profileStore, connectionPort)
    private val mutableState = MutableStateFlow(ConnectionsUiState())
    val state: StateFlow<ConnectionsUiState> = mutableState.asStateFlow()

    init {
        scope.launch {
            manage.connectionState().collect { connectionState ->
                mutableState.update { it.copy(connectionState = connectionState) }
            }
        }
        reload()
    }

    fun reload() {
        scope.launch {
            runCatching { manage.list() }
                .onSuccess { profiles ->
                    mutableState.update {
                        it.copy(
                            profiles = profiles.sortedBy(ConnectionProfile::name),
                            selectedProfileId = it.selectedProfileId
                                ?.takeIf { id -> profiles.any { profile -> profile.id == id } },
                            error = null,
                        )
                    }
                }
                .onFailure(::showError)
        }
    }

    fun select(profile: ConnectionProfile) {
        mutableState.update {
            it.copy(selectedProfileId = profile.id, draft = null, error = null)
        }
    }

    fun beginCreate() {
        val ids = mutableState.value.profiles.mapTo(mutableSetOf()) { it.id }
        var suffix = mutableState.value.profiles.size + 1
        while ("connection-$suffix" in ids) suffix++
        mutableState.update {
            it.copy(
                draft = ConnectionProfile(
                    id = "connection-$suffix",
                    name = "New connection",
                    mode = DeploymentMode.Standalone,
                    host = "localhost",
                ),
                testSucceeded = false,
                error = null,
            )
        }
    }

    fun edit(profile: ConnectionProfile) {
        mutableState.update { it.copy(draft = profile, testSucceeded = false, error = null) }
    }

    fun updateDraft(transform: (ConnectionProfile) -> ConnectionProfile) {
        mutableState.update { state ->
            state.copy(draft = state.draft?.let(transform), testSucceeded = false, error = null)
        }
    }

    fun cancelEdit() {
        mutableState.update { it.copy(draft = null, error = null, testSucceeded = false) }
    }

    fun save() {
        val draft = mutableState.value.draft ?: return
        perform {
            manage.update(draft).getOrThrow()
            val profiles = manage.list().sortedBy(ConnectionProfile::name)
            mutableState.update {
                it.copy(
                    profiles = profiles,
                    selectedProfileId = draft.id,
                    draft = null,
                    testSucceeded = false,
                )
            }
        }
    }

    fun test(profile: ConnectionProfile? = null) {
        val target = profile ?: mutableState.value.draft ?: selected() ?: return
        perform {
            manage.test(target).getOrThrow()
            mutableState.update { it.copy(testSucceeded = true) }
        }
    }

    fun connect(profile: ConnectionProfile? = null) {
        val target = profile ?: selected() ?: return
        perform { manage.connect(target).getOrThrow() }
    }

    fun disconnect() {
        perform { manage.disconnect() }
    }

    fun requestDelete(profile: ConnectionProfile) {
        mutableState.update { it.copy(pendingDelete = profile) }
    }

    fun dismissDelete() {
        mutableState.update { it.copy(pendingDelete = null) }
    }

    fun confirmDelete() {
        val profile = mutableState.value.pendingDelete ?: return
        perform {
            manage.delete(profile.id)
            val profiles = manage.list().sortedBy(ConnectionProfile::name)
            mutableState.update {
                it.copy(
                    profiles = profiles,
                    selectedProfileId = it.selectedProfileId.takeUnless { id -> id == profile.id },
                    pendingDelete = null,
                )
            }
        }
    }

    fun dismissError() {
        mutableState.update { it.copy(error = null) }
    }

    private fun selected(): ConnectionProfile? =
        mutableState.value.profiles.firstOrNull { it.id == mutableState.value.selectedProfileId }

    private fun perform(block: suspend () -> Unit) {
        mutableState.update { it.copy(busy = true, error = null, testSucceeded = false) }
        scope.launch {
            runCatching { block() }
                .onFailure(::showError)
            mutableState.update { it.copy(busy = false) }
        }
    }

    private fun showError(error: Throwable) {
        mutableState.update {
            it.copy(error = error.message ?: "Connection operation failed", busy = false)
        }
    }
}
