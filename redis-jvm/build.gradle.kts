plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    api(projects.core)
    implementation(libs.lettuce.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.junit)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit4)
}
