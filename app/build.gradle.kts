import java.util.Properties
import java.net.URI
import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

val releaseProperties = Properties().apply {
    val propertiesFile = rootProject.file("secrets.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use(::load)
    }
}

val signingStoreFile = releaseProperties.getProperty("signing.storeFile", "")
val signingStorePassword = releaseProperties.getProperty("signing.storePassword", "")
val signingKeyAlias = releaseProperties.getProperty("signing.keyAlias", "")
val signingKeyPassword = releaseProperties.getProperty("signing.keyPassword", "")
val hasReleaseSigning = listOf(
    signingStoreFile,
    signingStorePassword,
    signingKeyAlias,
    signingKeyPassword,
).all(String::isNotBlank) && file(signingStoreFile).isFile

fun quotedBuildConfigValue(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "com.photosoap.android"
    compileSdk {
        version = release(37) { minorApiLevel = 0 }
    }

    defaultConfig {
        applicationId = "com.photosoap"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0-beta.1"

        buildConfigField(
            "String",
            "METRICS_URL",
            quotedBuildConfigValue(releaseProperties.getProperty("metrics.url", "")),
        )
        buildConfigField(
            "String",
            "METRICS_ANON_KEY",
            quotedBuildConfigValue(releaseProperties.getProperty("metrics.anonKey", "")),
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(signingStoreFile)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }


    buildFeatures {
        compose = true
        buildConfig = true
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }

    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.animation)
    implementation(libs.compose.foundation)
    implementation(libs.compose.runtime)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.video)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Media
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    // Network
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Testing
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.13.0-alpha01")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.13.0-alpha01")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register("verifyProductionConfiguration") {
    group = "verification"
    description = "Checks release signing and optional metrics configuration before publishing."
    doLast {
        check(hasReleaseSigning) {
            "Release signing is not configured. Add signing.* values to secrets.properties."
        }
        val metricsUrl = releaseProperties.getProperty("metrics.url", "")
        val metricsKey = releaseProperties.getProperty("metrics.anonKey", "")
        if (metricsUrl.isNotBlank() || metricsKey.isNotBlank()) {
            val endpoint = runCatching { URI(metricsUrl) }.getOrNull()
            check(endpoint?.scheme == "https" && !endpoint.host.isNullOrBlank() && metricsKey.isNotBlank()) {
                "Optional aggregate metrics require both an HTTPS URL and a publishable key."
            }
            val keyClaims = runCatching {
                val encodedClaims = metricsKey.split('.').getOrNull(1).orEmpty()
                String(Base64.getUrlDecoder().decode(encodedClaims), Charsets.UTF_8)
            }.getOrDefault("")
            check(!metricsKey.startsWith("sb_secret_") &&
                !Regex("\"role\"\\s*:\\s*\"service_role\"").containsMatchIn(keyClaims)) {
                "A secret Supabase key must never be included in an Android application."
            }
        }
    }
}
