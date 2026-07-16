package org.roberthu.rs.domain

enum class ApplicationLogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
    ;

    companion object {
        fun fromLogbackName(name: String): ApplicationLogLevel? = when (name.uppercase()) {
            "DEBUG" -> DEBUG
            "INFO" -> INFO
            "WARN", "WARNING" -> WARN
            "ERROR" -> ERROR
            else -> null
        }
    }
}

data class ApplicationLogEntry(
    val id: String,
    val timestamp: String,
    val level: ApplicationLogLevel,
    val target: String? = null,
    val message: String,
    val details: String? = null,
)
