plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

fun getLocalProperty(key: String, default: String): String {
    val file = rootProject.file("local.properties")
    if (!file.exists()) return default
    for (line in file.readLines()) {
        val parts = line.split("=", limit = 2)
        if (parts.size == 2 && parts[0].trim() == key) {
            return parts[1].trim()
        }
    }
    return default
}

android {
    namespace = "com.example.billtracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.billtracker"
        minSdk = 26
        targetSdk = 34
        versionCode = 8
        versionName = "0.8"
        // API key is now managed at runtime via ApiKeyManager (default bundled + user override in settings)
    }

    signingConfigs {
        create("release") {
            storeFile = file("../release.keystore")
            storePassword = project.findProperty("RELEASE_STORE_PASSWORD") as String? ?: System.getenv("RELEASE_STORE_PASSWORD") ?: ""
            keyAlias = project.findProperty("RELEASE_KEY_ALIAS") as String? ?: "billtracker"
            keyPassword = project.findProperty("RELEASE_KEY_PASSWORD") as String? ?: System.getenv("RELEASE_KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    applicationVariants.configureEach {
        outputs.configureEach {
            if (this is com.android.build.gradle.internal.api.BaseVariantOutputImpl) {
                outputFileName = "记账助手.apk"
            }
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
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    implementation("com.google.mlkit:text-recognition-chinese:16.0.0")

    // DeepSeek API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Palette for color extraction
    implementation("androidx.palette:palette-ktx:1.0.0")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-android-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
}

kapt {
    correctErrorTypes = true
}
