import org.jetbrains.kotlin.gradle.dsl.JvmTarget // ⬅️ REQUIRED IMPORT for type-safe JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // but kept if you use Compose in parts of the app.
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

android {
    namespace = "si.uni.fri.sprouty"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "si.uni.fri.sprouty"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // ❌ REMOVED: deprecated kotlinOptions block
    // kotlinOptions {
    //     jvmTarget = "11"
    // }
}

// 🎯 NEW: Configure Kotlin compiler options globally
// This is the recommended replacement for android.kotlinOptions for setting jvmTarget
// It applies to all Kotlin compilation tasks in this module.
kotlin {
    compilerOptions {
        // Sets the JVM target using the type-safe JvmTarget enum
        jvmTarget.set(JvmTarget.JVM_11)
    }
}


dependencies {

    implementation(libs.coil)

    implementation(libs.androidx.core.splashscreen)
    // --- Room Database (Required for Plant Persistence) ---
    // Define the version here if not using TOML; otherwise, define it in TOML

    // 1. Core Room Libraries
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler.v250)
    implementation(libs.androidx.room.ktx) // Kotlin Coroutines support

    // --- Firebase Authentication & Services ---
    // 2. Firebase Bill of Materials (BOM) - Keeps all versions aligned
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // play-services-auth is often needed even with the BOM for Google sign-in methods
    // NOTE: If play.services.auth is an alias in libs, use that instead.
    implementation(libs.play.services.auth)

    // --- Android Core & UI ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.preference.ktx)

    // --- Lifecycle and Architecture ---
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose) // Needed if using Compose activities
    // Removed redundant kotlinx-coroutines-android if its version is already managed by other libs or TOML

    // --- Networking (Retrofit, OkHttp) ---
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Explicitly listing Coroutines for clarity, using your provided version or TOML alias
    // Note: If you have a TOML alias for coroutines, use that instead of the literal string.
    implementation(libs.kotlinx.coroutines.android)

    // --- Image Loading ---
    implementation(libs.glide)

    // --- Compose (If you are still using it in some parts) ---
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // --- Testing ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}