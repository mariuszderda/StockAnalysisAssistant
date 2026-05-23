import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.android.junit5)
}

// Wczytaj local.properties (gitignored). Jeśli nie istnieje — puste wartości i ostrzeżenie.
// Klucze trafiają do BuildConfig.* i są odczytywane WYŁĄCZNIE przez ApiKeys (di/ApiKeys.kt).
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) {
        f.inputStream().use { load(it) }
    } else {
        logger.warn("local.properties not found — API keys will be empty. Copy local.properties.example to local.properties.")
    }
}

fun localProp(key: String): String = (localProps.getProperty(key) ?: "").let { "\"$it\"" }

android {
    namespace = "pl.gwsh.stockanalysis"
    // compileSdk bumped to 36 because Compose 1.11.x i androidx.lifecycle 2.9.x wymagają ≥ 35.
    // targetSdk pozostaje 34 zgodnie z CLAUDE.md § Stack — żeby nie wciągać nowych zachowań
    // runtime Androida 15/16 bez świadomej decyzji.
    compileSdk = 36

    defaultConfig {
        applicationId = "pl.gwsh.stockanalysis"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "TWELVE_DATA_API_KEY", localProp("TWELVE_DATA_API_KEY"))
        buildConfigField("String", "GEMINI_API_KEY", localProp("GEMINI_API_KEY"))
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    sourceSets {
        named("main") {
            kotlin.srcDirs("src/main/kotlin")
        }
        named("test") {
            kotlin.srcDirs("src/test/kotlin")
        }
        named("androidTest") {
            kotlin.srcDirs("src/androidTest/kotlin")
        }
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/LICENSE*",
                "/META-INF/NOTICE*",
            )
        }
    }
}

dependencies {
    // Compose BOM steruje wersjami modułów Compose poniżej.
    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))

    // AndroidX core / activity / lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose UI
    implementation(libs.bundles.compose.ui)
    debugImplementation(libs.compose.ui.tooling)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    // Navigation (potrzebne od Fazy 3, ale rejestrujemy już teraz w katalogu)
    implementation(libs.androidx.navigation.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Retrofit + OkHttp + Moshi (Twelve Data API)
    implementation(libs.bundles.retrofit)
    ksp(libs.moshi.kotlin.codegen)

    // Room (cache OHLC + ulubione)
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    // Test — JUnit 5 + Truth + MockK + Turbine + coroutines-test + MockWebServer
    testImplementation(libs.bundles.junit5)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)

    // AndroidTest (instrumented) — Room in-memory DAO tests
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.turbine)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
