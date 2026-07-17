package org.roberthu.rs.usecase

import org.roberthu.rs.domain.CreateRedisKeyRequest
import org.roberthu.rs.domain.DeploymentMode
import org.roberthu.rs.domain.RedisError
import org.roberthu.rs.domain.RedisKeyPayload
import org.roberthu.rs.domain.RedisKeyType
import org.roberthu.rs.port.KeyCommandPort

class CreateRedisKey(
    private val commands: KeyCommandPort,
) {
    /**
     * Validates the request, rejects duplicates, then creates the key.
     * [deploymentMode] is used to enforce Cluster db0-only rules.
     */
    suspend fun create(
        request: CreateRedisKeyRequest,
        deploymentMode: DeploymentMode = DeploymentMode.Standalone,
    ): Result<Unit> {
        val validation = validate(request, deploymentMode)
        if (validation.isFailure) {
            return validation
        }

        val exists = commands.exists(request.key, request.database)
        if (exists.isFailure) {
            return Result.failure(exists.exceptionOrNull()!!)
        }
        if (exists.getOrThrow()) {
            return Result.failure(RedisError.Validation("该键已存在"))
        }

        if (request.type == RedisKeyType.Json) {
            val jsonAvailable = commands.isRedisJsonAvailable()
            if (jsonAvailable.isFailure) {
                return Result.failure(jsonAvailable.exceptionOrNull()!!)
            }
            if (!jsonAvailable.getOrThrow()) {
                return Result.failure(
                    RedisError.NotSupported("当前 Redis 实例未安装 RedisJSON 模块"),
                )
            }
        }

        return commands.createKey(request)
    }

    fun validate(
        request: CreateRedisKeyRequest,
        deploymentMode: DeploymentMode = DeploymentMode.Standalone,
    ): Result<Unit> {
        val errors = mutableListOf<String>()
        val key = request.key.trim()
        if (key.isEmpty()) {
            errors += "键名不能为空"
        }
        if (request.database < 0) {
            errors += "数据库编号不能为负数"
        }
        if (deploymentMode == DeploymentMode.Cluster && request.database != 0) {
            errors += "Cluster 模式只允许使用 db0"
        }

        val ttl = request.ttlSeconds
        if (ttl != null && ttl != -1L && ttl <= 0L) {
            errors += "TTL 必须为 -1 或大于 0 的整数"
        }

        errors += payloadErrors(request.type, request.payload)

        return if (errors.isEmpty()) {
            Result.success(Unit)
        } else {
            Result.failure(RedisError.Validation(errors.joinToString("; ")))
        }
    }

    private fun payloadErrors(type: RedisKeyType, payload: RedisKeyPayload): List<String> =
        when (type) {
            RedisKeyType.String -> when (payload) {
                is RedisKeyPayload.StringPayload -> emptyList()
                else -> listOf("STRING 类型需要字符串值")
            }

            RedisKeyType.Hash -> when (payload) {
                is RedisKeyPayload.HashPayload -> {
                    buildList {
                        if (payload.fields.isEmpty()) add("HASH 至少需要一个字段")
                        val fields = payload.fields.map { it.first.trim() }
                        if (fields.any { it.isEmpty() }) add("HASH 字段名不能为空")
                        if (fields.size != fields.distinct().size) add("HASH 字段名不能重复")
                    }
                }
                else -> listOf("HASH 类型与 Payload 不匹配")
            }

            RedisKeyType.List -> when (payload) {
                is RedisKeyPayload.ListPayload ->
                    if (payload.values.isEmpty()) listOf("LIST 至少需要一个元素") else emptyList()
                else -> listOf("LIST 类型与 Payload 不匹配")
            }

            RedisKeyType.Set -> when (payload) {
                is RedisKeyPayload.SetPayload -> {
                    buildList {
                        if (payload.members.isEmpty()) add("SET 至少需要一个成员")
                        if (payload.members.size != payload.members.distinct().size) {
                            add("SET 成员不能重复")
                        }
                    }
                }
                else -> listOf("SET 类型与 Payload 不匹配")
            }

            RedisKeyType.ZSet -> when (payload) {
                is RedisKeyPayload.ZSetPayload -> {
                    buildList {
                        if (payload.entries.isEmpty()) add("ZSET 至少需要一行")
                        val members = payload.entries.map { it.second.trim() }
                        if (members.any { it.isEmpty() }) add("ZSET Member 不能为空")
                        if (members.size != members.distinct().size) add("ZSET Member 不能重复")
                    }
                }
                else -> listOf("ZSET 类型与 Payload 不匹配")
            }

            RedisKeyType.Stream -> when (payload) {
                is RedisKeyPayload.StreamPayload -> {
                    buildList {
                        if (payload.fields.isEmpty()) add("STREAM 至少需要一组 Field/Value")
                        if (payload.fields.any { it.first.trim().isEmpty() }) {
                            add("STREAM Field 不能为空")
                        }
                    }
                }
                else -> listOf("STREAM 类型与 Payload 不匹配")
            }

            RedisKeyType.Json -> when (payload) {
                is RedisKeyPayload.JsonPayload -> {
                    val trimmed = payload.json.trim()
                    if (trimmed.isEmpty()) {
                        listOf("JSON 内容不能为空")
                    } else {
                        // Lightweight structural check — adapter/server still validates fully.
                        val ok = (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                            (trimmed.startsWith("[") && trimmed.endsWith("]"))
                        if (ok) emptyList() else listOf("JSON 语法无效")
                    }
                }
                else -> listOf("JSON 类型与 Payload 不匹配")
            }

            RedisKeyType.Other, RedisKeyType.Unknown ->
                listOf("不支持创建该键类型")
        }
}
