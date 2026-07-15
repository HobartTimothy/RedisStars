package org.roberthu.rs

/**
 * @constructor 创建[JVMPlatform]
 * @author YueHs
 * @date 2026/07/15
 */
class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()