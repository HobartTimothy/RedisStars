package org.roberthu.rs

import org.roberthu.rs.presentation.AppContainer
import org.roberthu.rs.redis.LettuceRedisConnection

class DesktopCompositionRoot : AutoCloseable {
    private val redis = LettuceRedisConnection()
    private val configDirectory = desktopConfigDirectory()
    private val settingsStore = DesktopJsonUserSettingsStore(configDirectory.resolve("settings.json"))
    private val profileStore = DesktopJsonConnectionProfileStore(
        configDirectory.resolve("profiles.json"),
        settingsStore,
    )

    val container = AppContainer(
        connectionProfileStore = profileStore,
        userSettingsStore = settingsStore,
        redisConnectionPort = redis,
        keyBrowserPort = redis,
        keyCommandPort = redis,
        redisDataPort = redis,
    )

    override fun close() {
        redis.close()
    }
}
