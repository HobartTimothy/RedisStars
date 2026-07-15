package org.roberthu.rs.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.roberthu.rs.domain.BinarySafeString
import org.roberthu.rs.domain.HashEntry
import org.roberthu.rs.domain.KeyMetadata
import org.roberthu.rs.domain.RedisKeySummary
import org.roberthu.rs.domain.RedisKeyType
import org.roberthu.rs.domain.ZSetEntry
import org.roberthu.rs.port.KeyCommandPort
import org.roberthu.rs.port.RedisDataPort
import org.roberthu.rs.usecase.EditKeyValue
import org.roberthu.rs.usecase.ManageTtl

sealed interface KeyContent {
    data class StringValue(val value: BinarySafeString) : KeyContent
    data class HashValue(val entries: List<HashEntry>) : KeyContent
    data class ListValue(val entries: List<BinarySafeString>) : KeyContent
    data class SetValue(val members: List<BinarySafeString>) : KeyContent
    data class ZSetValue(val entries: List<ZSetEntry>) : KeyContent
    data class Unsupported(val reason: String) : KeyContent
}

data class KeyDetailUiState(
    val key: RedisKeySummary? = null,
    val metadata: KeyMetadata? = null,
    val content: KeyContent? = null,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val confirmDelete: Boolean = false,
    val deletedKey: String? = null,
)

class KeyDetailViewModel(
    private val commands: KeyCommandPort,
    private val data: RedisDataPort,
    private val scope: CoroutineScope,
) {
    private val edit = EditKeyValue(data)
    private val ttl = ManageTtl(commands)
    private val mutableState = MutableStateFlow(KeyDetailUiState())
    val state: StateFlow<KeyDetailUiState> = mutableState.asStateFlow()

    fun load(key: RedisKeySummary) {
        mutableState.value = KeyDetailUiState(key = key, loading = true)
        scope.launch {
            runCatching {
                val metadata = commands.metadata(key.key).getOrThrow()
                metadata to loadContent(metadata)
            }.onSuccess { (metadata, content) ->
                mutableState.update {
                    it.copy(metadata = metadata, content = content, loading = false)
                }
            }.onFailure(::showError)
        }
    }

    fun refresh() {
        mutableState.value.key?.let(::load)
    }

    fun saveString(value: String) {
        val key = mutableState.value.key ?: return
        mutate {
            edit.setString(key.key, value).getOrThrow()
            loadContent(commands.metadata(key.key).getOrThrow())
        }
    }

    fun rename(newName: String) {
        val current = mutableState.value.key ?: return
        if (newName.isBlank() || newName == current.key) return
        mutate {
            commands.rename(current.key, newName).getOrThrow()
            val renamed = current.copy(key = newName)
            mutableState.update { it.copy(key = renamed) }
            loadContent(commands.metadata(newName).getOrThrow())
        }
    }

    fun setTtl(seconds: Long?) {
        val key = mutableState.value.key ?: return
        mutate {
            if (seconds == null) ttl.persist(key.key).getOrThrow()
            else ttl.expire(key.key, seconds).getOrThrow()
            mutableState.value.content
        }
    }

    fun requestDelete() {
        mutableState.update { it.copy(confirmDelete = true) }
    }

    fun dismissDelete() {
        mutableState.update { it.copy(confirmDelete = false) }
    }

    fun confirmDelete() {
        val key = mutableState.value.key ?: return
        mutableState.update { it.copy(saving = true, error = null) }
        scope.launch {
            commands.delete(listOf(key.key))
                .onSuccess {
                    mutableState.value = KeyDetailUiState(deletedKey = key.key)
                }
                .onFailure(::showError)
        }
    }

    fun dismissError() {
        mutableState.update { it.copy(error = null) }
    }

    private suspend fun loadContent(metadata: KeyMetadata): KeyContent =
        when (metadata.type) {
            RedisKeyType.String -> KeyContent.StringValue(
                data.getString(metadata.key, MAX_STRING_BYTES).getOrThrow(),
            )
            RedisKeyType.Hash -> KeyContent.HashValue(
                data.hscan(metadata.key, null, PAGE_SIZE).getOrThrow().entries,
            )
            RedisKeyType.List -> KeyContent.ListValue(
                data.lrange(metadata.key, 0, PAGE_SIZE.toLong() - 1).getOrThrow(),
            )
            RedisKeyType.Set -> KeyContent.SetValue(
                data.sscan(metadata.key, null, PAGE_SIZE).getOrThrow().members,
            )
            RedisKeyType.ZSet -> KeyContent.ZSetValue(
                data.zscan(metadata.key, null, PAGE_SIZE).getOrThrow().entries,
            )
            RedisKeyType.Stream -> KeyContent.Unsupported("Stream viewing is not available yet.")
            RedisKeyType.Other, RedisKeyType.Unknown ->
                KeyContent.Unsupported("This Redis value type cannot be displayed.")
        }

    private fun mutate(block: suspend () -> KeyContent?) {
        mutableState.update { it.copy(saving = true, error = null) }
        scope.launch {
            runCatching { block() }
                .onSuccess { content ->
                    val key = mutableState.value.key
                    val metadata = key?.let { commands.metadata(it.key).getOrNull() }
                    mutableState.update {
                        it.copy(
                            metadata = metadata ?: it.metadata,
                            content = content ?: it.content,
                            saving = false,
                        )
                    }
                }
                .onFailure(::showError)
        }
    }

    private fun showError(error: Throwable) {
        mutableState.update {
            it.copy(
                loading = false,
                saving = false,
                error = error.message ?: "Key operation failed",
            )
        }
    }

    private companion object {
        const val PAGE_SIZE = 100
        const val MAX_STRING_BYTES = 1024 * 1024
    }
}
