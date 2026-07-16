package org.roberthu.rs.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import org.roberthu.rs.desktopConfigDirectory
import org.slf4j.LoggerFactory
import java.nio.file.Files

object ApplicationLogBootstrap {
    fun initialize() {
        val logDirectory = desktopLogsDirectory()
        runCatching { Files.createDirectories(logDirectory) }
        System.setProperty("redisstars.log.dir", logDirectory.toAbsolutePath().toString())
        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        context.reset()
        val configurator = ch.qos.logback.classic.joran.JoranConfigurator()
        configurator.context = context
        val configStream = ApplicationLogBootstrap::class.java.getResourceAsStream("/logback.xml")
            ?: error("logback.xml not found on classpath")
        configStream.use { configurator.doConfigure(it) }
        val root = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
        root.level = Level.INFO
        root.info("RedisStars application logging initialized at {}", logDirectory.toAbsolutePath())
    }
}

fun desktopLogsDirectory() = desktopConfigDirectory().resolve("logs")
