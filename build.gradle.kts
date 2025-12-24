buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.21")
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.9.21")
    }
}

plugins {
    // Kotlin Multiplatform
    kotlin("multiplatform") version "1.9.21" apply false
    kotlin("plugin.serialization") version "1.9.21" apply false
    kotlin("android") version "1.9.21" apply false
    
    // Compose Multiplatform
    id("org.jetbrains.compose") version "1.5.11" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
