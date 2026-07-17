package org.roberthu.rs

import org.roberthu.rs.logging.DesktopApplicationLogService
import org.roberthu.rs.logging.desktopLogsDirectory
import org.roberthu.rs.presentation.AppContainer
import org.roberthu.rs.redis.LettuceRedisConnection

/**
 * @constructor 创建[DesktopCompositionRoot]
 *
 * @author YueHs
 *
 * @date 2026/07/16
 */
class DesktopCompositionRoot : AutoCloseable {
    private val redis = LettuceRedisConnection()

    private val configDirectory = desktopConfigDirectory()

    private val settingsStore = DesktopJsonUserSettingsStore(configDirectory.resolve("settings.json"))

    private val profileStore = DesktopJsonConnectionProfileStore(
        file = configDirectory.resolve("profiles.json"),
        rememberPasswords = settingsStore::rememberPasswordsEnabled,
    )

    private val applicationLogService = DesktopApplicationLogService(desktopLogsDirectory())

    val container = AppContainer(
        connectionProfileStore = profileStore,
        userSettingsStore = settingsStore,
        redisConnectionPort = redis,
        keyBrowserPort = redis,
        keyCommandPort = redis,
        redisDataPort = redis,
        applicationLogPort = applicationLogService,
    )

    override fun close() {
        redis.close()
    }
}