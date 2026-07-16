package org.roberthu.rs.presentation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.roberthu.rs.domain.CreateRedisKeyRequest
import org.roberthu.rs.domain.DeploymentMode
import org.roberthu.rs.domain.RedisDatabaseSummary
import org.roberthu.rs.domain.RedisError
import org.roberthu.rs.domain.RedisKeyPayload
import org.roberthu.rs.domain.RedisKeySummary
import org.roberthu.rs.domain.RedisKeyType
import org.roberthu.rs.domain.ScanQuery
import org.roberthu.rs.port.KeyBrowserPort
import org.roberthu.rs.port.KeyCommandPort
import org.roberthu.rs.usecase.BrowseKeys
import org.roberthu.rs.usecase.CreateRedisKey

data class AddKeyDialogState(
    val key: String = "",
    val database: Int = 0,
    val type: RedisKeyType = RedisKeyType.String,
    val ttlText: String = "",
    val permanent: Boolean = true,
    val stringValue: String = "",
    val hashFields: List<Pair<String, String>> = listOf("" to ""),
    val listValues: List<String> = listOf(""),
    val setMembers: List<String> = listOf(""),
    val zsetEntries: List<Pair<String, String>> = listOf("" to ""),
    val streamFields: List<Pair<String, String>> = listOf("" to ""),
    val jsonContent: String = "{}",
    val validationErrors: Map<String, String> = emptyMap(),
    val submitting: Boolean = false,
    val submitError: String? = null,
    val redisJsonAvailable: Boolean? = null,
)

data class KeyBrowserUiState(
    val pattern: String = "*",
    val keys: List<RedisKeySummary> = emptyList(),
    val nextCursorToken: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val partialFailures: List<String> = emptyList(),
    val selectedKey: RedisKeySummary? = null,
    val selectedDatabase: Int = 0,
    val databases: List<RedisDatabaseSummary> = emptyList(),
    val databasesLoading: Boolean = false,
    val clusterMode: Boolean = false,
    val addKeyDialog: AddKeyDialogState? = null,
    val scanRequestId: Long = 0,
    val lastCreatedKey: RedisKeySummary? = null,
) {
    val canLoadMore: Boolean get() = nextCursorToken != null && !loading
}

class KeyBrowserViewModel(
    keyBrowserPort: KeyBrowserPort,
    private val keyCommandPort: KeyCommandPort,
    private val scope: CoroutineScope,
    private val deploymentModeProvider: () -> DeploymentMode = { DeploymentMode.Standalone },
) {
    private val browseKeys = BrowseKeys(keyBrowserPort)
    private val createRedisKey = CreateRedisKey(keyCommandPort)
    private val mutableState = MutableStateFlow(KeyBrowserUiState())
    val state: StateFlow<KeyBrowserUiState> = mutableState.asStateFlow()
    private var scanJob: Job? = null

    fun setPattern(pattern: String) {
        mutableState.update { it.copy(pattern = pattern) }
    }

    fun select(key: RedisKeySummary) {
        mutableState.update { it.copy(selectedKey = key) }
    }

    fun onConnected(clusterMode: Boolean, initialDatabase: Int = 0) {
        mutableState.update {
            it.copy(
                clusterMode = clusterMode,
                selectedDatabase = initialDatabase.coerceAtLeast(0),
                lastCreatedKey = null,
            )
        }
        loadDatabases()
        scope.launch {
            browseKeys.selectDatabase(initialDatabase.coerceAtLeast(0))
            refresh()
        }
    }

    fun onDisconnected() {
        cancel()
        mutableState.value = KeyBrowserUiState()
    }

    fun selectDatabase(index: Int) {
        if (mutableState.value.clusterMode && index != 0) return
        mutableState.update {
            it.copy(
                selectedDatabase = index,
                selectedKey = null,
                lastCreatedKey = null,
            )
        }
        scope.launch {
            browseKeys.selectDatabase(index)
                .onFailure { error ->
                    mutableState.update { it.copy(error = error.message) }
                }
            refresh()
        }
    }

    fun openAddKeyDialog() {
        val db = mutableState.value.selectedDatabase
        mutableState.update {
            it.copy(
                addKeyDialog = AddKeyDialogState(database = db),
                lastCreatedKey = null,
            )
        }
        scope.launch {
            val available = keyCommandPort.isRedisJsonAvailable().getOrNull()
            mutableState.update { state ->
                state.copy(
                    addKeyDialog = state.addKeyDialog?.copy(redisJsonAvailable = available),
                )
            }
        }
    }

    fun closeAddKeyDialog() {
        mutableState.update { it.copy(addKeyDialog = null) }
    }

    fun updateAddKeyDialog(transform: (AddKeyDialogState) -> AddKeyDialogState) {
        mutableState.update { state ->
            state.copy(addKeyDialog = state.addKeyDialog?.let(transform))
        }
    }

    fun updateAddKeyKey(key: String) = updateAddKeyDialog { it.copy(key = key, validationErrors = emptyMap()) }

    fun updateAddKeyDatabase(database: Int) =
        updateAddKeyDialog { it.copy(database = database, validationErrors = emptyMap()) }

    fun updateAddKeyTtlText(ttlText: String) =
        updateAddKeyDialog { it.copy(ttlText = ttlText, validationErrors = emptyMap()) }

    fun updateAddKeyPermanent(permanent: Boolean) =
        updateAddKeyDialog {
            it.copy(permanent = permanent, ttlText = if (permanent) "" else it.ttlText, validationErrors = emptyMap())
        }

    fun changeAddKeyType(type: RedisKeyType) {
        updateAddKeyDialog { it.copy(type = type, validationErrors = emptyMap(), submitError = null) }
    }

    fun updateAddKeyStringValue(value: String) =
        updateAddKeyDialog { it.copy(stringValue = value, validationErrors = emptyMap()) }

    fun updateAddKeyHashFields(fields: List<Pair<String, String>>) =
        updateAddKeyDialog { it.copy(hashFields = fields, validationErrors = emptyMap()) }

    fun updateAddKeyListValues(values: List<String>) =
        updateAddKeyDialog { it.copy(listValues = values, validationErrors = emptyMap()) }

    fun updateAddKeySetMembers(members: List<String>) =
        updateAddKeyDialog { it.copy(setMembers = members, validationErrors = emptyMap()) }

    fun updateAddKeyZsetEntries(entries: List<Pair<String, String>>) =
        updateAddKeyDialog { it.copy(zsetEntries = entries, validationErrors = emptyMap()) }

    fun updateAddKeyStreamFields(fields: List<Pair<String, String>>) =
        updateAddKeyDialog { it.copy(streamFields = fields, validationErrors = emptyMap()) }

    fun updateAddKeyJsonContent(content: String) =
        updateAddKeyDialog { it.copy(jsonContent = content, validationErrors = emptyMap()) }

    fun applyImportedText(text: String) {
        val dialog = mutableState.value.addKeyDialog ?: return
        when (dialog.type) {
            RedisKeyType.String -> updateAddKeyStringValue(text)
            RedisKeyType.Json -> updateAddKeyJsonContent(text)
            RedisKeyType.List -> {
                val lines = text.lines().map { it.trimEnd('\r') }.filter { it.isNotEmpty() }
                if (lines.isNotEmpty()) updateAddKeyListValues(lines)
            }
            RedisKeyType.Set -> {
                val lines = text.lines().map { it.trimEnd('\r') }.filter { it.isNotEmpty() }
                if (lines.isNotEmpty()) updateAddKeySetMembers(lines)
            }
            else -> Unit
        }
    }

    fun submitNewKey() {
        val dialog = mutableState.value.addKeyDialog ?: return
        if (dialog.submitting) return

        val request = buildCreateRequest(dialog) ?: return
        val deploymentMode = deploymentModeProvider()

        updateAddKeyDialog { it.copy(submitting = true, submitError = null, validationErrors = emptyMap()) }
        scope.launch {
            createRedisKey.create(request, deploymentMode)
                .onSuccess {
                    val created = RedisKeySummary(request.key.trim(), request.type)
                    mutableState.update { state ->
                        state.copy(
                            addKeyDialog = null,
                            lastCreatedKey = created,
                            selectedKey = created,
                        )
                    }
                    refresh()
                }
                .onFailure { error ->
                    val validationErrors = if (error is RedisError.Validation) {
                        mapOf("global" to (error.message ?: "验证失败"))
                    } else {
                        emptyMap()
                    }
                    updateAddKeyDialog {
                        it.copy(
                            submitting = false,
                            submitError = error.message,
                            validationErrors = validationErrors,
                        )
                    }
                }
        }
    }

    fun refresh() {
        scan(cursor = null, append = false)
    }

    fun loadMore() {
        val cursor = mutableState.value.nextCursorToken ?: return
        scan(cursor = cursor, append = true)
    }

    fun cancel() {
        browseKeys.cancel()
        scanJob?.cancel()
        scanJob = null
        mutableState.update { it.copy(loading = false) }
    }

    private fun loadDatabases() {
        mutableState.update { it.copy(databasesLoading = true) }
        scope.launch {
            browseKeys.listDatabases()
                .onSuccess { databases ->
                    mutableState.update { it.copy(databases = databases, databasesLoading = false) }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(databasesLoading = false, error = error.message)
                    }
                }
        }
    }

    private fun scan(cursor: String?, append: Boolean) {
        browseKeys.cancel()
        scanJob?.cancel()
        val requestId = browseKeys.currentRequestId()
        val pattern = mutableState.value.pattern.ifBlank { "*" }
        val database = mutableState.value.selectedDatabase
        mutableState.update {
            it.copy(
                loading = true,
                error = null,
                scanRequestId = requestId,
                partialFailures = if (append) it.partialFailures else emptyList(),
                keys = if (append) it.keys else emptyList(),
                nextCursorToken = if (append) it.nextCursorToken else null,
            )
        }
        scanJob = scope.launch {
            try {
                browseKeys.scan(
                    ScanQuery(
                        database = database,
                        pattern = pattern,
                        cursorToken = cursor,
                    ),
                )
                    .onSuccess { page ->
                        mutableState.update { current ->
                            if (current.scanRequestId != requestId) return@update current
                            val keys = if (append) {
                                (current.keys + page.keys).distinctBy(RedisKeySummary::key)
                            } else {
                                page.keys.distinctBy(RedisKeySummary::key)
                            }
                            current.copy(
                                keys = keys,
                                nextCursorToken = page.nextCursorToken,
                                partialFailures = page.partialFailures,
                                loading = false,
                                error = null,
                            )
                        }
                    }
                    .onFailure { error ->
                        mutableState.update { current ->
                            if (current.scanRequestId != requestId) return@update current
                            current.copy(
                                loading = false,
                                error = error.message ?: "无法扫描键",
                            )
                        }
                    }
            } catch (_: CancellationException) {
                mutableState.update { it.copy(loading = false) }
            }
        }
    }

    private fun buildCreateRequest(dialog: AddKeyDialogState): CreateRedisKeyRequest? {
        val ttlSeconds = when {
            dialog.permanent -> null
            dialog.ttlText.isBlank() -> null
            else -> dialog.ttlText.toLongOrNull()
        }
        val payload = when (dialog.type) {
            RedisKeyType.String -> RedisKeyPayload.StringPayload(dialog.stringValue)
            RedisKeyType.Hash -> RedisKeyPayload.HashPayload(dialog.hashFields)
            RedisKeyType.List -> RedisKeyPayload.ListPayload(dialog.listValues)
            RedisKeyType.Set -> RedisKeyPayload.SetPayload(dialog.setMembers)
            RedisKeyType.ZSet -> {
                val entries = dialog.zsetEntries.mapNotNull { (score, member) ->
                    score.toDoubleOrNull()?.let { it to member }
                }
                RedisKeyPayload.ZSetPayload(entries)
            }
            RedisKeyType.Stream -> RedisKeyPayload.StreamPayload(fields = dialog.streamFields)
            RedisKeyType.Json -> RedisKeyPayload.JsonPayload(dialog.jsonContent)
            else -> return null
        }
        return CreateRedisKeyRequest(
            database = dialog.database,
            key = dialog.key,
            type = dialog.type,
            ttlSeconds = ttlSeconds,
            payload = payload,
        )
    }
}
