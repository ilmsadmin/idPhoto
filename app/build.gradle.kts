plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.idphoto.app"
    compileSdk = 35

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("idPhoto.jks")
            storePassword = project.property("RELEASE_STORE_PASSWORD") as String
            keyAlias = project.property("RELEASE_KEY_ALIAS") as String
            keyPassword = project.property("RELEASE_KEY_PASSWORD") as String
        }
    }

    defaultConfig {
        applicationId = "com.idphoto_pro.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ── Chỉ giữ arm64-v8a ──
        // Loại bỏ armeabi-v7a, x86, x86_64 (emulator/thiết bị cũ)
        // 99%+ thiết bị Android hiện tại dùng arm64-v8a
        // Áp dụng cho cả APK lẫn AAB
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    // Không nén file ONNX trong APK — cho phép memory mapping hiệu quả
    androidResources {
        noCompress += "onnx"
    }

    packaging {
        // Loại bỏ các file không cần thiết
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module",
                "META-INF/versions/**",
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json",
            )
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // CameraX
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")

    // ML Kit Face Detection (quality check)
    implementation("com.google.mlkit:face-detection:16.1.7")

    // ML Kit Selfie Segmentation (fast preview)
    implementation("com.google.mlkit:segmentation-selfie:16.0.0-beta6")

    // ONNX Runtime for MODNet (deep processing)
    // v1.22.0+ ships 16KB-aligned native libs required for Android 15+ / Google Play
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.22.0")

    // Lifecycle ViewModel Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // DataStore for settings/preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // ExifInterface for DPI metadata
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Splash Screen API
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
