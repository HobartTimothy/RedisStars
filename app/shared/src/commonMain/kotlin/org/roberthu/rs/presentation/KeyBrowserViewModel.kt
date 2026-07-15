package org.roberthu.rs.presentation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.roberthu.rs.domain.RedisKeySummary
import org.roberthu.rs.domain.ScanQuery
import org.roberthu.rs.port.KeyBrowserPort
import org.roberthu.rs.usecase.BrowseKeys

data class KeyBrowserUiState(
    val pattern: String = "*",
    val keys: List<RedisKeySummary> = emptyList(),
    val nextCursorToken: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val partialFailures: List<String> = emptyList(),
    val selectedKey: RedisKeySummary? = null,
) {
    val canLoadMore: Boolean get() = nextCursorToken != null && !loading
}

class KeyBrowserViewModel(
    keyBrowserPort: KeyBrowserPort,
    private val scope: CoroutineScope,
) {
    private val browseKeys = BrowseKeys(keyBrowserPort)
    private val mutableState = MutableStateFlow(KeyBrowserUiState())
    val state: StateFlow<KeyBrowserUiState> = mutableState.asStateFlow()
    private var scanJob: Job? = null

    fun setPattern(pattern: String) {
        mutableState.update { it.copy(pattern = pattern) }
    }

    fun select(key: RedisKeySummary) {
        mutableState.update { it.copy(selectedKey = key) }
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

    private fun scan(cursor: String?, append: Boolean) {
        cancel()
        val pattern = mutableState.value.pattern.ifBlank { "*" }
        mutableState.update {
            it.copy(
                loading = true,
                error = null,
                partialFailures = if (append) it.partialFailures else emptyList(),
                keys = if (append) it.keys else emptyList(),
                nextCursorToken = if (append) it.nextCursorToken else null,
            )
        }
        scanJob = scope.launch {
            try {
                browseKeys.scan(ScanQuery(pattern = pattern, cursorToken = cursor))
                    .onSuccess { page ->
                        mutableState.update { current ->
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
                        mutableState.update {
                            it.copy(
                                loading = false,
                                error = error.message ?: "Unable to scan keys",
                            )
                        }
                    }
            } catch (_: CancellationException) {
                mutableState.update { it.copy(loading = false) }
            }
        }
    }
}
