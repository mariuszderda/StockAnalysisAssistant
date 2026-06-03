import com.android.build.gradle.ProguardFiles.getDefaultProguardFile
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.android)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        load(file.inputStream())
    }
}
android {
    namespace = "pl.gwsh.stockanalysis"
    compileSdk = 35

    defaultConfig {
        applicationId = "pl.gwsh.stockanalysis"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField(
                "String", "TWELVE_DATA_API_KEY",
                "\"${localProperties.getProperty("TWELVE_DATA_API_KEY") ?: ""}\""
            )
            buildConfigField(
                "String", "GEMINI_API_KEY",
                "\"${localProperties.getProperty("GEMINI_API_KEY") ?: ""}\""
            )
        }
        debug {
            buildConfigField(
                "String", "TWELVE_DATA_API_KEY",
                "\"${localProperties.getProperty("TWELVE_DATA_API_KEY") ?: ""}\""
            )
            buildConfigField(
                "String", "GEMINI_API_KEY",
                "\"${localProperties.getProperty("GEMINI_API_KEY") ?: ""}\""
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        disable += "FrequentlyChangingValue"
        disable += "NullSafeMutableLiveData"
        disable += "RememberInComposition"
        abortOnError = false
        checkReleaseBuilds = false
    }

    packaging {
        resources {
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // COMPOSE BOM
    implementation("androidx.compose.foundation:foundation")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.compose.ui:ui-test")

    // NAVIGATION
    implementation(libs.androidx.navigation.compose)
    androidTestImplementation(libs.androidx.navigation.testing)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    // coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // HILT
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler.v2571)
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // VIEWMODEL
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    //MOSHI
    implementation("com.squareup.moshi:moshi-kotlin:1.15.2")

    //OKHTTP3
    implementation(platform("com.squareup.okhttp3:okhttp-bom:5.3.2"))
    // define any required OkHttp artifacts without version
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.squareup.okhttp3:logging-interceptor")

    //retrofit
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-moshi:3.0.0")

    // Room
    val room_version = "2.8.4"
    implementation("androidx.room:room-runtime:$room_version")
    // If this project uses any Kotlin source, use Kotlin Symbol Processing (KSP)
    // See Add the KSP plugin to your project
    ksp("androidx.room:room-compiler:$room_version")
    // If this project only uses Java source, use the Java annotationProcessor
    // No additional plugins are necessary
    annotationProcessor("androidx.room:room-compiler:$room_version")
    // optional - Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:${room_version}")

    // Charts - Vico
    implementation("com.patrykandpatrick.vico:compose-m3:2.1.2")

    // AI - Gemini (TODO: dodać po weryfikacji wersji na Maven Central)
    // implementation("com.google.generativeai:google-generativeai-kotlin:X.X.X")
}