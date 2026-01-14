plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.traductorlsa"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.traductorlsa"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // Para CameraX con Analyzer
        vectorDrawables.useSupportLibrary = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "2.2.21"
    }

    // ← evita que AAPT comprima los .tflite (permite memory-mapping)
    androidResources {
        noCompress += setOf("tflite")
    }

    packaging {
        // opcional: filtros típicos
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            )
        }
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    val camerax = "1.3.4"
    implementation("androidx.camera:camera-core:$camerax")
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-view:$camerax")
    implementation("androidx.camera:camera-view:$camerax")

    // MediaPipe Tasks (hands)
    implementation("com.google.mediapipe:tasks-vision:0.10.29")

    // TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.14.0")

    // Clerk Android (API + UI)
    implementation("com.clerk:clerk-android-api:0.1.28")
    implementation("com.clerk:clerk-android-ui:0.1.3")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")

    // Compose + Flows con lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    //implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    // Lifecycle / Activity
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")

    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.material:material-icons-extended")

    val lottieVersion = "6.7.1"
    implementation("com.airbnb.android:lottie-compose:$lottieVersion")

}