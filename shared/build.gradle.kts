plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.websockets)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.client.cio)
                implementation(libs.jna)
                implementation(libs.mp3spi)
                implementation(libs.mp3spi.spi)

                // Sherpa-ONNX native libraries (bundled .dylib/.so/.dll inside JAR)
                implementation(files("libs/sherpa-onnx-native-lib-osx-aarch64-v1.13.0.jar"))
            }
        }
    }
}
