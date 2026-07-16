package org.roberthu.rs

import org.roberthu.rs.logging.DesktopApplicationLogService
import org.roberthu.rs.logging.desktopLogsDirectory
import org.roberthu.rs.presentation.AppContainer
import org.roberthu.rs.redis.LettuceRedisConnection

class DesktopCompositionRoot : AutoCloseable {
    private val redis = LettuceRedisConnection()
    private val configDirectory = desktopConfigDirectory()
    private val settingsStore = DesktopJsonUserSettingsStore(configDirectory.resolve("settings.json"))
    private val profileStore = DesktopJsonConnectionProfileStore(
        configDirectory.resolve("profiles.json"),
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
