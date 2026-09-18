import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.hg.xposed"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hg.xposed"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        // Module metadata surfaced to LSPosed manager
        buildConfigField("String", "MODULE_NAME", "\"HongGuo Xposed\"")
        buildConfigField("String", "TARGET_PACKAGE", "\"com.phoenix.read\"")
        buildConfigField("String", "MODULE_VERSION", "\"1.0.0\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
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

    // Keep Java resources (META-INF/xposed/*) shipped inside the APK root.
    packaging {
        resources {
            excludes -= "/META-INF/xposed/**"
            excludes -= "META-INF/xposed/**"
        }
    }
}

dependencies {
    // --- LSPosed / libxposed modern module API 102 (provided by framework at runtime) ---
    compileOnly("io.github.libxposed:api:102.0.0")
    compileOnly("androidx.annotation:annotation:1.8.0")

    // --- DexKit 2.0: runtime dex querying to locate obfuscated hook points ---
    implementation("org.luckypray:dexkit:2.0.0")

    // --- UI: Jetpack Compose (Material 3) ---
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
