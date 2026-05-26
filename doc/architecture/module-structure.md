# Estructura de módulos

Multi-módulo Gradle con Kotlin DSL y version catalog. Cada contexto acotado es un módulo KMP independiente. Las apps (Android, Desktop) son módulos finales que ensamblan los contextos.

## Árbol completo

```
within-means/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/
│   └── libs.versions.toml
├── apps/
│   ├── android/                          (com.android.application) <-- inicial
│   │   └── src/main/kotlin/...
│   └── desktop/                          (org.jetbrains.compose, JVM) <-- Fase 10
│       └── src/jvmMain/kotlin/...
└── src/
    ├── shared/                           (KMP library)
    │   ├── build.gradle.kts
    │   └── src/
    │       ├── commonMain/kotlin/within/means/shared/
    │       │   ├── domain/
    │       │   │   ├── AggregateRoot.kt
    │       │   │   ├── ValueObject.kt
    │       │   │   ├── Identifier.kt
    │       │   │   ├── StringValueObject.kt
    │       │   │   ├── IntValueObject.kt
    │       │   │   ├── DateValueObject.kt
    │       │   │   ├── bus/
    │       │   │   │   ├── command/
    │       │   │   │   ├── query/
    │       │   │   │   └── event/
    │       │   │   ├── criteria/
    │       │   │   └── money/             (Money + Currency, shared kernel)
    │       │   └── infrastructure/
    │       │       ├── bus/
    │       │       ├── persistence/
    │       │       └── serialization/
    │       ├── androidMain/
    │       ├── jvmMain/
    │       └── commonTest/
    ├── users/
    │   ├── build.gradle.kts
    │   └── src/
    │       ├── commonMain/
    │       │   ├── kotlin/within/means/users/
    │       │   │   ├── domain/
    │       │   │   ├── application/
    │       │   │   └── infrastructure/persistence/  (mappers, repos SQLDelight, InMemory)
    │       │   └── sqldelight/within/means/users/db/
    │       │       ├── users.sq
    │       │       └── migrations/
    │       ├── androidMain/               (vacío salvo necesidades específicas)
    │       ├── desktopMain/               (Fase 10)
    │       └── commonTest/
    ├── accounts/                          (misma estructura)
    ├── transactions/
    ├── categories/
    ├── budgets/
    └── analytics/
```

## `settings.gradle.kts`

```kotlin
rootProject.name = "within-means"

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

// Kernel
include(":shared")
project(":shared").projectDir = file("src/shared")

// Bounded contexts
listOf("users", "accounts", "transactions", "categories", "budgets", "analytics").forEach { ctx ->
    include(":$ctx")
    project(":$ctx").projectDir = file("src/$ctx")
}

// Apps (desktop se añade en la Fase 10)
include(":apps:android")
// include(":apps:desktop")
```

## Convención de paquetes

`within.means.<context>.<layer>.<feature>`

Ejemplos:

- `within.means.shared.domain.bus.command`
- `within.means.users.domain`
- `within.means.users.application.register`
- `within.means.users.infrastructure.persistence`
- `within.means.transactions.application.search_by_criteria`

Espeja el esqueleto Java (`tv.codely.shared.domain.bus.command`, `tv.codely.mooc.courses.application.create`).

## Dependencias entre módulos

| Módulo | Depende de |
|---|---|
| `:shared` | (ninguno) |
| `:users` | `:shared` |
| `:accounts` | `:shared` |
| `:transactions` | `:shared` |
| `:categories` | `:shared` |
| `:budgets` | `:shared` |
| `:analytics` | `:shared` |
| `:apps:android` (Fase 4+) | `:shared` + todos los contextos |
| `:apps:desktop` (Fase 10) | `:shared` + todos los contextos |

**Regla dura:** los contextos no se importan entre sí. La comunicación ocurre vía eventos a través del `EventBus` cableado en la capa `apps/`.

## Targets KMP por módulo

### `:shared` y contextos

```kotlin
// Nota: cada módulo KMP que tenga androidTarget también declara un bloque
// `android { ... }` con compileSdk = 35, minSdk = 21, source/targetCompatibility = 17.
// El minSdk se valida coherentemente en toda la cadena de módulos.
kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }
    // jvm("desktop") {                  // <-- habilitar en la Fase 10
    //     compilations.all {
    //         kotlinOptions.jvmTarget = "17"
    //     }
    // }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":shared"))   // excepto en :shared
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.koin.core)
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutines)
                implementation(libs.benasher44.uuid)        // sólo en :shared en realidad
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotest.assertions)
                implementation(libs.mockk.common)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.sqldelight.driver.android)
            }
        }
        // val desktopMain by getting {              // <-- habilitar en la Fase 10
        //     dependencies {
        //         implementation(libs.sqldelight.driver.sqlite)
        //     }
        // }
    }
}

// Bloque sqldelight: cada módulo declara SU PROPIA clase DB.
// Comparten el mismo archivo físico SQLite (within_means.db) pero cada
// contexto solo accede a sus tablas a través de su clase. Cross-context
// se hace por buses (EventBus, QueryBus), nunca por SQL.
//
// Nombres por módulo:
//   :shared        -> SharedDatabase        (Event Store + tablas comunes)
//   :users         -> UsersDatabase
//   :categories    -> CategoriesDatabase
//   :transactions  -> TransactionsDatabase
//   :analytics     -> AnalyticsDatabase
sqldelight {
    databases {
        create("<Context>Database") {                // p. ej. UsersDatabase
            packageName.set("within.means.<context>.db")
            srcDirs.setFrom("src/commonMain/sqldelight")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            verifyMigrations.set(true)
        }
    }
}
```

**Por qué una DB por módulo y no una global:** SQLDelight no soporta fusionar schemas declarados en módulos Gradle separados. Si todos los `.sq` viven en un único módulo se pierde el encapsulamiento del contexto. La solución idiomática es **una clase DB por contexto** que apunta al **mismo archivo SQLite físico** desde el driver. Ver detalle en [`../persistence/overview.md`](../persistence/overview.md#una-db-por-contexto).

## Configuration cache: deshabilitada por ahora

En `gradle.properties` está fijado:

```properties
org.gradle.configuration-cache=false
```

**Razón:** el Kotlin Multiplatform plugin tiene fallos conocidos de serialización al guardar el estado del classpath de `BuildToolsApiClasspathEntrySnapshotTransform` cuando se combina con AGP + SQLDelight. Síntoma observado durante la Fase 1:

```
Configuration cache state could not be cached: field `provider` of
`ProviderBackedFileCollectionSpec` ... found in `KotlinCompile$ClasspathSnapshotProperties`
```

**Cuándo reactivar:** cuando madure el soporte (probable Kotlin 2.2+ o AGP 8.9+). Sin config-cache seguimos teniendo `org.gradle.caching=true` (build cache) y `org.gradle.parallel=true`, suficientes para tiempos razonables.

### `:apps:android`

Aplicación Android con plugin `com.android.application` y Compose Multiplatform.

**Configuración de compatibilidad legacy:**

```kotlin
android {
    namespace = "within.means.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "within.means.android"
        minSdk = 21                       // Android 5.0 Lollipop — cubre ~99% de dispositivos
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        multiDexEnabled = true            // No imprescindible con minSdk 21+ pero seguro tenerlo
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true   // java.time y APIs Java 8+ en pre-API 26
    }

    buildFeatures { compose = true }
}

dependencies {
    coreLibraryDesugaring(libs.android.desugar.jdk.libs)
    implementation(libs.sqlcipher.android)              // cifrado at-rest
    implementation(libs.androidx.sqlite)                 // SupportSQLiteOpenHelper
    implementation(libs.sqldelight.driver.android)
}
```

**Implicaciones de `minSdk = 21`:**

- Compose Multiplatform compatible (mínimo soportado).
- AndroidX, Lifecycle, Navigation, Material 3: todos OK.
- SQLDelight con `AndroidSqliteDriver`: OK.
- `java.time.LocalDate` y similares funcionan vía **desugaring**.
- Vector drawables, MultiDex automático.
- No se puede asumir Java 8 nativo (API 26+): por eso `kotlinx-datetime` (multiplatform) en lugar de `java.time` directo en `commonMain`.

### `:apps:desktop`

JVM puro con plugin `org.jetbrains.compose` y target `desktop` para macOS/Windows/Linux.

## Version catalog (`gradle/libs.versions.toml`)

```toml
[versions]
kotlin = "2.0.0"
agp = "8.5.0"
compose = "1.6.11"
koin = "3.5.6"
sqldelight = "2.0.2"
ktor = "2.3.12"
kotlinx-coroutines = "1.8.1"
kotlinx-datetime = "0.6.0"
kotlinx-serialization = "1.7.1"
kotest = "5.9.1"
mockk = "1.13.12"
turbine = "1.1.0"
kermit = "2.0.4"

[libraries]
kotlinx-coroutines-core    = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
kotlinx-datetime           = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
koin-core                  = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-android               = { module = "io.insert-koin:koin-android", version.ref = "koin" }
koin-compose               = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
sqldelight-runtime         = { module = "app.cash.sqldelight:runtime", version.ref = "sqldelight" }
sqldelight-coroutines      = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqldelight" }
sqldelight-driver-android  = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }
sqldelight-driver-sqlite   = { module = "app.cash.sqldelight:sqlite-driver", version.ref = "sqldelight" }
sqlcipher-android          = { module = "net.zetetic:sqlcipher-android", version = "4.6.1" }
androidx-sqlite            = { module = "androidx.sqlite:sqlite", version = "2.4.0" }
benasher44-uuid            = { module = "com.benasher44:uuid", version = "0.8.4" }
multiplatform-settings     = { module = "com.russhwolf:multiplatform-settings", version = "1.1.1" }
kermit                     = { module = "co.touchlab:kermit", version.ref = "kermit" }
android-desugar-jdk-libs   = { module = "com.android.tools:desugar_jdk_libs", version = "2.0.4" }
kotest-assertions          = { module = "io.kotest:kotest-assertions-core", version.ref = "kotest" }
mockk-common               = { module = "io.mockk:mockk", version.ref = "mockk" }
turbine                    = { module = "app.cash.turbine:turbine", version.ref = "turbine" }

[plugins]
kotlin-multiplatform       = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-serialization       = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
android-application        = { id = "com.android.application", version.ref = "agp" }
android-library            = { id = "com.android.library", version.ref = "agp" }
jetbrains-compose          = { id = "org.jetbrains.compose", version = "1.6.11" }
sqldelight                 = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
```

## Fuentes y tests

Cada contexto:

```
src/<context>/src/
├── commonMain/
│   └── kotlin/within/means/<context>/
│       ├── domain/
│       ├── application/
│       └── infrastructure/
├── commonTest/
│   └── kotlin/within/means/<context>/
│       ├── domain/
│       └── application/
├── androidMain/
└── desktopMain/                              # se añade en la Fase 10
```

Espeja la separación `main`/`test` del esqueleto Java (`src/mooc/main/...` y `src/mooc/test/...`), adaptada a las convenciones KMP (`commonMain`/`commonTest`).
