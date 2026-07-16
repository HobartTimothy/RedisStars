package org.roberthu.rs.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.roberthu.rs.domain.ConnectionGroup
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.port.ConnectionProfileStore
import org.roberthu.rs.port.ConnectionState
import org.roberthu.rs.port.RedisConnectionPort
import org.roberthu.rs.usecase.ManageConnections

data class GroupDialogState(
    val name: String = "",
    val error: String? = null,
)

data class ConnectionsUiState(
    val profiles: List<ConnectionProfile> = emptyList(),
    val groups: List<ConnectionGroup> = emptyList(),
    val selectedProfileId: String? = null,
    val selectedGroupId: String? = null,
    val groupDialog: GroupDialogState? = null,
    val editor: ConnectionEditorUiState? = null,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val busy: Boolean = false,
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
            runCatching {
                manage.list() to manage.listGroups()
            }
                .onSuccess { (profiles, groups) ->
                    mutableState.update {
                        it.copy(
                            profiles = profiles.sortedBy(ConnectionProfile::name),
                            groups = groups,
                            selectedProfileId = it.selectedProfileId
                                ?.takeIf { id -> profiles.any { profile -> profile.id == id } },
                            selectedGroupId = it.selectedGroupId
                                ?.takeIf { id -> groups.any { group -> group.id == id } },
                            error = null,
                        )
                    }
                }
                .onFailure(::showListError)
        }
    }

    fun select(profile: ConnectionProfile) {
        mutableState.update {
            it.copy(selectedProfileId = profile.id, error = null)
        }
    }

    fun selectGroup(id: String?) {
        mutableState.update { it.copy(selectedGroupId = id) }
    }

    fun beginCreate(groupId: String? = mutableState.value.selectedGroupId) {
        val ids = mutableState.value.profiles.mapTo(mutableSetOf()) { it.id }
        var suffix = mutableState.value.profiles.size + 1
        while ("connection-$suffix" in ids) suffix++
        val form = ConnectionFormState.defaults().copy(groupId = groupId)
        mutableState.update {
            it.copy(
                editor = ConnectionEditorUiState(
                    mode = ConnectionEditorMode.Create,
                    profileId = "connection-$suffix",
                    selectedSection = ConnectionEditorSection.General,
                    initialForm = form,
                    form = form,
                    pendingGroupId = groupId,
                ),
                error = null,
            )
        }
    }

    fun openAddGroupDialog() {
        mutableState.update {
            it.copy(groupDialog = GroupDialogState())
        }
    }

    fun updateGroupName(name: String) {
        mutableState.update { state ->
            state.copy(
                groupDialog = state.groupDialog?.copy(name = name, error = null),
            )
        }
    }

    fun confirmCreateGroup() {
        val dialog = mutableState.value.groupDialog ?: return
        val existingNames = mutableState.value.groups.map { it.name.trim() }
        val trimmed = dialog.name.trim()
        val validationErrors = ConnectionGroup(
            id = "pending",
            name = trimmed,
        ).validate(existingNames)
        if (validationErrors.isNotEmpty()) {
            mutableState.update {
                it.copy(groupDialog = dialog.copy(error = validationErrors.first()))
            }
            return
        }

        val ids = mutableState.value.groups.mapTo(mutableSetOf()) { it.id }
        var suffix = mutableState.value.groups.size + 1
        while ("group-$suffix" in ids) suffix++
        val group = ConnectionGroup(
            id = "group-$suffix",
            name = trimmed,
            order = mutableState.value.groups.size,
        )
        scope.launch {
            runCatching { manage.createGroup(group).getOrThrow() }
                .onSuccess {
                    mutableState.update { state ->
                        state.copy(
                            groups = state.groups + group,
                            selectedGroupId = group.id,
                            groupDialog = null,
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(groupDialog = dialog.copy(error = error.message))
                    }
                }
        }
    }

    fun dismissGroupDialog() {
        mutableState.update { it.copy(groupDialog = null) }
    }

    fun toggleGroupExpanded(groupId: String) {
        val group = mutableState.value.groups.firstOrNull { it.id == groupId } ?: return
        val expanded = !group.expanded
        scope.launch {
            runCatching { manage.setGroupExpanded(groupId, expanded).getOrThrow() }
                .onSuccess {
                    mutableState.update { state ->
                        state.copy(
                            groups = state.groups.map { g ->
                                if (g.id == groupId) g.copy(expanded = expanded) else g
                            },
                        )
                    }
                }
                .onFailure(::showListError)
        }
    }

    fun edit(profile: ConnectionProfile) {
        val form = ConnectionFormState.from(profile)
        mutableState.update {
            it.copy(
                editor = ConnectionEditorUiState(
                    mode = ConnectionEditorMode.Edit,
                    profileId = profile.id,
                    selectedSection = ConnectionEditorSection.General,
                    initialForm = form,
                    form = form,
                ),
                error = null,
            )
        }
    }

    fun selectEditorSection(section: ConnectionEditorSection) {
        updateEditor { it.copy(selectedSection = section) }
    }

    fun updateEditorForm(transform: (ConnectionFormState) -> ConnectionFormState) {
        updateEditor { editor ->
            val previous = editor.form
            val next = transform(previous)
            val clearedKeys = changedFieldKeys(previous, next)
            editor.copy(
                form = next,
                fieldErrors = editor.fieldErrors.filterKeys { key ->
                    clearedKeys.none { changed -> key == changed || key.startsWith("$changed.") }
                },
                globalError = null,
                testSucceeded = false,
            )
        }
    }

    fun requestCloseEditor() {
        val editor = mutableState.value.editor ?: return
        if (!editor.isDirty) {
            closeEditor()
        } else {
            updateEditor { it.copy(confirmDiscardVisible = true) }
        }
    }

    fun confirmDiscardEditor() {
        closeEditor()
    }

    fun dismissDiscardConfirmation() {
        updateEditor { it.copy(confirmDiscardVisible = false) }
    }

    fun saveEditor() {
        val editor = mutableState.value.editor ?: return
        if (editor.testing || editor.saving) return

        when (val conversion = editor.form.toConnectionProfile(editor.profileId)) {
            is ConnectionFormConversionResult.Failure -> {
                updateEditor {
                    it.copy(
                        fieldErrors = conversion.fieldErrors,
                        globalError = conversion.globalError
                            ?: conversion.domainErrors.joinToString("; ").ifBlank { null },
                        saving = false,
                        testSucceeded = false,
                    )
                }
                return
            }

            is ConnectionFormConversionResult.Success -> {
                val existing = mutableState.value.profiles.firstOrNull { it.id == editor.profileId }
                val converted = when (editor.mode) {
                    ConnectionEditorMode.Create ->
                        conversion.profile.copy(groupId = editor.pendingGroupId ?: editor.form.groupId)
                    ConnectionEditorMode.Edit -> conversion.profile
                }
                // Blank password/SSH secrets on edit keep the previously loaded values so a
                // save after "test with typed password" never silently drops auth.
                val profileToSave = preserveBlankSecrets(converted, existing)
                updateEditor {
                    it.copy(
                        saving = true,
                        fieldErrors = emptyMap(),
                        globalError = null,
                        testSucceeded = false,
                    )
                }
                scope.launch {
                    runCatching {
                        manage.update(profileToSave).getOrThrow()
                        manage.list() to manage.listGroups()
                    }.onSuccess { (profiles, groups) ->
                        mutableState.update {
                            it.copy(
                                profiles = profiles
                                    .map { loaded ->
                                        if (loaded.id == profileToSave.id) {
                                            restoreSecrets(loaded, profileToSave)
                                        } else {
                                            loaded
                                        }
                                    }
                                    .sortedBy(ConnectionProfile::name),
                                groups = groups,
                                selectedProfileId = profileToSave.id,
                                editor = null,
                                busy = false,
                                error = null,
                            )
                        }
                    }.onFailure { error ->
                        updateEditor {
                            it.copy(
                                saving = false,
                                globalError = error.message ?: "Failed to save connection",
                            )
                        }
                    }
                }
            }
        }
    }

    fun testEditor() {
        val editor = mutableState.value.editor ?: return
        if (editor.testing || editor.saving) return

        when (val conversion = editor.form.toConnectionProfile(editor.profileId)) {
            is ConnectionFormConversionResult.Failure -> {
                updateEditor {
                    it.copy(
                        fieldErrors = conversion.fieldErrors,
                        globalError = conversion.globalError
                            ?: conversion.domainErrors.joinToString("; ").ifBlank { null },
                        testing = false,
                        testSucceeded = false,
                    )
                }
                return
            }

            is ConnectionFormConversionResult.Success -> {
                updateEditor {
                    it.copy(
                        testing = true,
                        fieldErrors = emptyMap(),
                        globalError = null,
                        testSucceeded = false,
                    )
                }
                scope.launch {
                    runCatching {
                        manage.test(conversion.profile).getOrThrow()
                    }.onSuccess {
                        updateEditor {
                            it.copy(testing = false, testSucceeded = true, globalError = null)
                        }
                    }.onFailure { error ->
                        updateEditor {
                            it.copy(
                                testing = false,
                                testSucceeded = false,
                                globalError = error.message ?: "Connection test failed",
                            )
                        }
                    }
                }
            }
        }
    }

    fun parseClipboardUrl(raw: String) {
        when (val parsed = RedisConnectionUrlParser.parse(raw)) {
            is RedisConnectionUrlParseResult.Failure -> {
                updateEditor {
                    it.copy(globalError = parsed.error, testSucceeded = false)
                }
            }

            is RedisConnectionUrlParseResult.Success -> {
                updateEditorForm { parsed.value.applyTo(it) }
                updateEditor { it.copy(globalError = null) }
            }
        }
    }

    fun test(profile: ConnectionProfile) {
        perform {
            manage.test(profile).getOrThrow()
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

    private fun closeEditor() {
        mutableState.update { it.copy(editor = null) }
    }

    private fun updateEditor(transform: (ConnectionEditorUiState) -> ConnectionEditorUiState) {
        mutableState.update { state ->
            state.copy(editor = state.editor?.let(transform))
        }
    }

    private fun perform(block: suspend () -> Unit) {
        mutableState.update { it.copy(busy = true, error = null) }
        scope.launch {
            runCatching { block() }
                .onFailure(::showListError)
            mutableState.update { it.copy(busy = false) }
        }
    }

    private fun showListError(error: Throwable) {
        mutableState.update {
            it.copy(error = error.message ?: "Connection operation failed", busy = false)
        }
    }

    private fun preserveBlankSecrets(
        profile: ConnectionProfile,
        existing: ConnectionProfile?,
    ): ConnectionProfile {
        if (existing == null) return profile
        return profile.copy(
            password = profile.password ?: existing.password,
            ssh = profile.ssh.copy(
                password = profile.ssh.password ?: existing.ssh.password,
                privateKey = profile.ssh.privateKey ?: existing.ssh.privateKey,
                privateKeyPassphrase = profile.ssh.privateKeyPassphrase
                    ?: existing.ssh.privateKeyPassphrase,
            ),
        )
    }

    private fun restoreSecrets(
        loaded: ConnectionProfile,
        saved: ConnectionProfile,
    ): ConnectionProfile = loaded.copy(
        password = loaded.password ?: saved.password,
        ssh = loaded.ssh.copy(
            password = loaded.ssh.password ?: saved.ssh.password,
            privateKey = loaded.ssh.privateKey ?: saved.ssh.privateKey,
            privateKeyPassphrase = loaded.ssh.privateKeyPassphrase
                ?: saved.ssh.privateKeyPassphrase,
        ),
    )

    private fun changedFieldKeys(
        previous: ConnectionFormState,
        next: ConnectionFormState,
    ): Set<String> = buildSet {
        if (previous.name != next.name) add("name")
        if (previous.deploymentMode != next.deploymentMode) add("deploymentMode")
        if (previous.host != next.host) add("host")
        if (previous.port != next.port) add("port")
        if (previous.database != next.database) add("database")
        if (previous.username != next.username) add("username")
        if (previous.password != next.password) add("password")
        if (previous.clientName != next.clientName) add("clientName")
        if (previous.connectTimeoutMs != next.connectTimeoutMs) add("connectTimeoutMs")
        if (previous.commandTimeoutMs != next.commandTimeoutMs) add("commandTimeoutMs")
        if (previous.reconnectTimeoutMs != next.reconnectTimeoutMs) add("reconnectTimeoutMs")
        if (previous.tlsEnabled != next.tlsEnabled) add("tlsEnabled")
        if (previous.verifyPeer != next.verifyPeer) add("verifyPeer")
        if (previous.masterName != next.masterName) add("masterName")
        if (previous.sentinelNodes != next.sentinelNodes) add("sentinelNodes")
        if (previous.seedNodes != next.seedNodes) add("seedNodes")
        if (previous.sshEnabled != next.sshEnabled) add("sshEnabled")
        if (previous.sshHost != next.sshHost) add("sshHost")
        if (previous.sshPort != next.sshPort) add("sshPort")
        if (previous.sshUsername != next.sshUsername) add("sshUsername")
        if (previous.sshAuthMethod != next.sshAuthMethod) add("sshAuthMethod")
        if (previous.sshPassword != next.sshPassword) add("sshPassword")
        if (previous.sshPrivateKey != next.sshPrivateKey) add("sshPrivateKey")
        if (previous.sshPrivateKeyPath != next.sshPrivateKeyPath) add("sshPrivateKeyPath")
        if (previous.sshPrivateKeyPassphrase != next.sshPrivateKeyPassphrase) {
            add("sshPrivateKeyPassphrase")
        }
        if (previous.sshConnectTimeoutMs != next.sshConnectTimeoutMs) add("sshConnectTimeoutMs")
    }
}
