package org.roberthu.rs

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform