plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "within.means.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "within.means.android"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    sourceSets["main"].kotlin.srcDirs("src/main/kotlin")
    sourceSets["main"].manifest.srcFile("src/main/AndroidManifest.xml")

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    // Bounded contexts del MVP
    implementation(project(":shared"))
    implementation(project(":users"))
    implementation(project(":categories"))
    implementation(project(":transactions"))
    implementation(project(":analytics"))

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Android
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.security.crypto)

    // DI
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)

    // Persistencia
    implementation(libs.sqldelight.driver.android)
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite)

    // Serialization (event registry)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    // Settings (UI prefs only)
    implementation(libs.multiplatform.settings)

    // Logging
    implementation(libs.kermit)

    // Desugaring
    coreLibraryDesugaring(libs.androidx.desugar.jdk.libs)
}
