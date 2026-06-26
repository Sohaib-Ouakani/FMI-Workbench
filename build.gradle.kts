allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    alias(libs.plugins.gradle.cmake) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
    id("org.danilopianini.gradle-kotlin-qa") version "1.6.0" apply false
    id("com.dorongold.task-tree") version "4.0.1"
    `lifecycle-base`
}

subprojects {
    //apply(plugin = "org.danilopianini.gradle-kotlin-qa")
}
