plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                }
            }
        }
    }
    // JVM target enabled solely for fast unit tests with JdbcSqliteDriver.IN_MEMORY.
    // The production Android app does not depend on this target.
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.sqldelight.primitive.adapters)
            implementation(libs.benasher44.uuid)
            implementation(libs.kermit)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotest.assertions.core)
            implementation(libs.turbine)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.driver.android)
        }
        jvmTest.dependencies {
            implementation(libs.sqldelight.driver.sqlite)
        }
    }
}

android {
    namespace = "within.means.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}

dependencies {
    coreLibraryDesugaring(libs.androidx.desugar.jdk.libs)
}

sqldelight {
    databases {
        create("SharedDatabase") {
            packageName.set("within.means.shared.db")
            srcDirs.setFrom("src/commonMain/sqldelight")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            verifyMigrations.set(true)
        }
    }
}
