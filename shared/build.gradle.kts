plugins {
    id("com.android.library")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("app.cash.sqldelight")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Kotlinx Serialization for JSON/M3U parsing
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                
                // Ktor client for network requests (XStream API)
                implementation("io.ktor:ktor-client-core:2.3.7")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
                
                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                
                // SQLDelight
                implementation("app.cash.sqldelight:runtime:2.0.1")
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.1")
            }
        }
        
        val androidMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-okhttp:2.3.7")
                implementation("app.cash.sqldelight:android-driver:2.0.1")
            }
        }
    }
}

android {
    namespace = "com.suptv.shared"
    compileSdk = 34
    
    defaultConfig {
        minSdk = 21
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("SupTvDatabase") {
            packageName.set("com.suptv.shared.db")
        }
    }
}
