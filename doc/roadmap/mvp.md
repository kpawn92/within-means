# Roadmap — MVP

Fases del MVP. Objetivo: app Android que permite **registrar ingresos y gastos diarios** y responde a "de dónde viene mi dinero" y "en qué se va".

**Estrategia de entrega:** _una pantalla Android mínima por contexto_. Desde la Fase 3, cada fase añade simultáneamente el dominio y una pantalla Compose que lo ejercita. Cada fase entrega un APK instalable y observable en el emulador, no solo tests verdes. Esto:

- Mantiene el dominio DDD intacto (las pantallas viven en `apps/android/.../ui/`, no en los módulos KMP).
- Acelera el feedback visual: cada fase es presentable.
- Reduce el riesgo de la Fase 7 al distribuir el trabajo de UI entre Fases 3-6.

Las fases posteriores al MVP están en [`post-mvp.md`](post-mvp.md).

---

## Fase 1 — Bootstrap del repositorio ✅

- [x] `settings.gradle.kts` con módulos (`:shared`, contextos del MVP, `:apps:android`).
- [x] `build.gradle.kts` raíz con plugins comunes.
- [x] `gradle/libs.versions.toml` (version catalog).
- [x] `.gitignore`, `.editorconfig`.
- [x] `gradle.properties` con `configuration-cache=false` (incompatibilidad KMP+AGP+SQLDelight, ver [`../architecture/module-structure.md`](../architecture/module-structure.md#configuration-cache-deshabilitada-por-ahora)).
- [x] Wrapper de Gradle (8.11.1 con `networkTimeout=120000` + `retries=3`).
- [x] Esqueletos `build.gradle.kts` por módulo (`:shared`, `:users`, `:categories`, `:transactions`, `:analytics`) + `AndroidManifest.xml` vacío.
- [x] `apps/android` con `MainActivity` Compose placeholder ("within means / Fase 1 — Bootstrap").
- [x] `local.properties` con `sdk.dir` (gitignorado, per-máquina).
- [x] **Verificado end-to-end:**
  - `./gradlew help` → BUILD SUCCESSFUL.
  - `./gradlew projects` → lista los 6 módulos en 7s.
  - `./gradlew :shared:tasks` → resuelve plugins KMP+Android+Compose+SQLDelight en 6s.
  - `./gradlew :apps:android:assembleDebug` → APK debug 22.9 MB en 1m 44s.
  - `adb install` + lanzamiento en emulador Pixel 9 → app arranca correctamente.

**Entrega:** APK instalable que muestra el placeholder; toda la cadena de build funciona end-to-end.

---

## Fase 2 — Kernel `:shared` (sin UI) ✅

Excepción a la regla de "una pantalla por fase": el kernel no tiene UI propia, sólo provee las piezas que usarán los demás. Sigue el patrón estricto del esqueleto Java DDD.

### Dominio compartido (`commonMain/.../shared/domain/`)

- [x] `AggregateRoot` con `record(event)` y `pullDomainEvents()`.
- [x] `ValueObject`, `StringValueObject`, `IntValueObject`, `DateValueObject`.
- [x] `Identifier` (validación UUID con regex).
- [x] `UuidGenerator` (interfaz).
- [x] `money/`: `Money(cents: Long, currency: Currency)` inmutable con operaciones; `Currency` (enum **USD / EUR / CUP**).
- [x] `bus/command/{Command, CommandBus, CommandHandler}`.
- [x] `bus/query/{Query, QueryBus, QueryHandler, Response}`.
- [x] `bus/event/{DomainEvent, EventBus, DomainEventSubscriber, DomainEventStore, DomainEventRecord}`.
- [x] `criteria/{Criteria, Filter, Filters, Order, FilterField, FilterOperator, FilterValue, OrderType, OrderField}`.

### Infraestructura compartida (`commonMain/.../shared/infrastructure/`)

- [x] `RealUuidGenerator` (con `com.benasher44:uuid`).
- [x] `InMemoryCommandBus`, `InMemoryQueryBus`, `InMemoryEventBus`.
- [x] `SqlDelightDomainEventStore` (persistencia a `DomainEvents.sq`).
- [x] `EventStoreBackedEventBus` (persist-then-dispatch).
- [x] `DomainEventJsonSerializer` (kotlinx.serialization con registry por nombre de evento).
- [x] `SqlCriteriaTranslator` (Criteria → SQL parametrizado con whitelist de columnas y validación de nombre de tabla).

### Persistencia

- [x] `src/shared/sqldelight/within/means/shared/db/DomainEvents.sq` (tabla + índices + 6 queries: `append`, `findById`, `findByAggregate`, `findByName`, `findAllSince`, `findAll`).
- [x] `verifyMigrations = true` en el bloque sqldelight.
- [x] **Decisión técnica registrada:** Los archivos `.sq` se nombran en PascalCase (no snake_case) para que SQLDelight genere la propiedad de queries en camelCase (`domainEventsQueries`).

### Build

- [x] Target `jvm()` añadido a `:shared` exclusivamente para tests con `JdbcSqliteDriver.IN_MEMORY` (no se usa en producción Android).

### Tests

- [x] **commonTest** (corre en Android Debug/Release + JVM):
  - `IdentifierTest` — 5 casos (UUID válido/inválido, igualdad por tipo concreto).
  - `MoneyTest` — 9 casos (suma, resta, multiplicación, negación, abs, comparación, currency mismatch, igualdad incluye currency, lookup por código).
  - `AggregateRootTest` — 2 casos (record + pull, pull clears buffer).
  - `InMemoryCommandBusTest` — 3 casos (dispatch correcto, falla sin handler, rechaza duplicados).
  - `InMemoryQueryBusTest` — 2 casos (routing, falla sin handler).
  - `InMemoryEventBusTest` — 1 caso multievento (delivery selectivo por tipo).
  - `SqlCriteriaTranslatorTest` — 8 casos (vacío, filtros simples y combinados, CONTAINS con `LIKE`, orden+limit+offset, columna no permitida, tabla con SQL injection).
  - `RealUuidGeneratorTest` — 2 casos.
- [x] **jvmTest** (Event Store, requiere driver SQLite):
  - `SqlDelightDomainEventStoreTest` — 7 casos (append, findById, appendAll transaccional, findByAggregate, findByName, findAllSince, empty append).
  - `EventStoreBackedEventBusTest` — 3 casos (persist-then-dispatch, empty publish no-op, serializer round-trip).

### Verificado

- `./gradlew :shared:test` → BUILD SUCCESSFUL en 23s (Android Debug + Release).
- `./gradlew :shared:jvmTest` → BUILD SUCCESSFUL en 17s (incluye Event Store).
- ~42 casos en total, 0 fallos.

**Entrega:** kernel `:shared` testeado; los buses dispatch/ask funcionan, los eventos se persisten en el Event Store y se entregan a subscribers. Listo para construir contextos sobre él.

---

## Fase 3 — `users` + pantalla "Onboarding" ✅

Primer vertical end-to-end. Introduce **el cableado completo de `apps/android`** (Koin, SQLCipher, navegación, theming) porque es la primera fase con UI.

### Dominio `users`

- [x] `UserProfile`, `UserId`, `DisplayName`, `Locale (ES/EN)`, `Currency (USD/EUR/CUP)`.
- [x] Events `@Serializable`: `UserDefaultCreated`, `UserPreferencesUpdated`.
- [x] `UserProfileRepository` (interfaz en `domain/`).
- [x] Application:
  - `EnsureDefaultUserCommand` + handler + `DefaultUserBootstrap` (idempotente).
  - `UpdateUserPreferencesCommand` + handler + `UserPreferencesUpdater`.
  - `FindDefaultUserQuery` + handler + `OptionalUserResponse` DTO.
- [x] Infrastructure:
  - `UserProfile.sq` con tabla `user_profile` y `is_default` (índice único parcial).
  - `UserProfileRowMapper` (con `rehydrate` y `toRow`).
  - `SqlDelightUserProfileRepository`, `InMemoryUserProfileRepository`.
- [x] Factorías: `UserProfile.bootstrap(...)` (emite evento) y `UserProfile.rehydrate(...)` (silenciosa).
- [x] Tests: `UserProfileTest`, `DisplayNameTest`, `DefaultUserBootstrapTest` (idempotencia), `UserPreferencesUpdaterTest`, `SqlDelightUserProfileRepositoryTest` (jvmTest con `JdbcSqliteDriver.IN_MEMORY`).
- [x] `./gradlew :users:test :users:jvmTest` → BUILD SUCCESSFUL.

### Cableado `apps/android`

- [x] Koin con módulos `appModule`, `persistenceModule`, `usersModule`, `busModule`, `uiModule`.
- [x] `WithinMeansApplication.onCreate` arranca Koin con todos los módulos.
- [x] `AndroidDatabaseFactory` con `AndroidSqliteDriver` + `SupportOpenHelperFactory` de SQLCipher.
  - **Bootstrap manual de schemas:** SQLCipher no dispara fiable el `onCreate` del callback. Solución: detectar presencia de la tabla centinela (`domain_events` para `SharedDatabase`, `user_profile` para `UsersDatabase`) en `sqlite_master`; si no existe, ejecutar `Schema.create(driver)`. Esto es idempotente y resistente a archivos residuales.
- [x] `KeystoreManager` con HMAC-SHA256 no exportable en Android Keystore.
- [x] `PassphraseProvider`: `HMAC-SHA256(masterKey, utf8(pin))` produce 32 bytes para SQLCipher.
- [x] `DatabaseUnlocker`: tenedor lazy de `SharedDatabase` + `UsersDatabase`; lanza error explícito si se accede antes de `unlock(pin)`.
- [x] `OnboardingState` (`EncryptedSharedPreferences` AES-GCM) guarda el flag `isCompleted`.
- [x] `androidx.navigation:navigation-compose` con `NavHost` en `MainActivity` y rutas `Onboarding`, `Unlock`, `Home`.
- [x] Theming Material 3 (paleta verde/light + dark).

### Pantallas

- [x] **OnboardingScreen** con 3 pasos: Welcome → PIN (6 dígitos + confirmación) → Preferencias (nombre + idioma ES/EN + moneda USD/EUR/CUP).
- [x] **UnlockScreen** para arranques posteriores: pide PIN para descifrar la DB y entrar a Home.
- [x] **HomePlaceholderScreen** muestra "Hola, <nombre>" + moneda base (vía `FindDefaultUserQuery`).
- [x] Navegación: `MainActivity` resuelve start destination en `LaunchedEffect`:
  - `!onboardingCompleted` → `Onboarding`.
  - `onboardingCompleted && !unlocker.isUnlocked` → `Unlock`.
  - resto → `Home`.

### Decisiones técnicas registradas

- [x] **Resolución de buses lazy con `KoinComponent.get<X>()`** dentro del coroutine del ViewModel. No se pueden inyectar `() -> CommandBus`/`() -> QueryBus` porque ambos colapsan a `Function0<Any>` en runtime y Koin no los distingue.
- [x] **`UserErrorMessages`** mapea excepciones técnicas (SQLite, cipher) a mensajes en español user-friendly. La traza completa va a Kermit para devs.
- [x] **`UnlockViewModel.submit`** fuerza una query real tras `unlock(pin)` (`userProfileQueries.findDefault()`) — SQLCipher solo falla al primer query si el PIN es incorrecto, no al abrir el driver.

### Verificado end-to-end

- Cold install → Welcome → PIN 123456 → Preferences → Finalizar → Home `Hola, Yo / Moneda base: EUR`.
- Force-stop + relaunch → Unlock screen.
- Unlock con PIN incorrecto → "No se pudo descifrar la base de datos. Comprueba tu PIN." con el campo limpiado.
- Unlock con PIN correcto → Home con los datos persistidos.

**Entrega:** instalas el APK por primera vez → pasas el onboarding → la DB se cifra con tu PIN → el `UserProfile` queda persistido. En arranques posteriores entras directo a Unlock y de ahí a Home.

---

## Fase 4 — `categories` + pantalla "Categorías" ✅

### Dominio `categories`

- [x] `Category`, `CategoryId`, `CategoryName`, `CategoryColor`, `CategoryIcon`, `parentId: CategoryId?` (reusamos `CategoryId` para el padre en lugar de un VO `ParentCategoryId` separado — el constraint de jerarquía es el mismo).
- [x] VOs de clasificación: `CategoryKind`, `CategoryNature`, `CategoryEssentiality`, `productive: Boolean` (campo de `CategoryClassifiers`, no VO independiente), `EngelGroup`.
- [x] Events: `CategoryCreated`, `CategoryRenamed`, `CategoryReclassified`, `CategoryDeleted` + extra `CategoryRestyled` (recolor/reicono separado de la reclasificación semántica).
- [x] Repository + impl SQLDelight + InMemory.
- [x] Application: comandos `Create`/`Rename`/`Reclassify`/`Restyle`/`Delete` + queries `FindCategoryQuery`/`SearchCategoriesQuery`/`ListAllCategoriesQuery`.
- [x] **Seed por defecto** suscrito a `UserDefaultCreated`: `DefaultCategoriesSeeder` siembra las 13 categorías base con sus clasificadores; subscriber `SeedDefaultCategoriesOnUserDefaultCreated` en `apps/android/.../subscribers/`.
- [x] Tests unitarios (`CategoryTest`, `CategoryColorTest`, `CategoryCreatorTest`, `DefaultCategoriesSeederTest`) + tests de integración (`SqlDelightCategoryRepositoryTest`, `SeedDefaultCategoriesIntegrationTest`).

### Pantalla "Categorías" (`apps/android/.../ui/categories/`)

- [x] `CategoriesListScreen`: tabs por kind (`Gastos` / `Ingresos` / `Transferencias`), cada tarjeta con color e icono.
- [x] `CategoryEditScreen`: formulario crear/editar con name, color (paleta `CategoryColorPalette`), icono (`CategoryIcons` set de Material Symbols), kind, nature, essentiality, productive, engelGroup. Botón guardar despacha `CreateCategoryCommand` o `Rename`+`Restyle`+`Reclassify` en edición.
- [x] Diálogo de borrado inline en `CategoriesListScreen` (no en archivo separado).
- [x] `CategoriesListViewModel` con `CategoryRepository.observeAll()` → `Query.asFlow().mapToList(ioDispatcher)` (reactivo end-to-end).

### Cableado `apps/android`

- [x] `CategoriesModule` añadido a Koin; eventos `CategoryCreated/Renamed/Restyled/Reclassified/Deleted` registrados en el `DomainEventJsonSerializer`.
- [x] Navegación: `Home` → `Categories` (botón "Gestionar categorías"); rutas `categories`, `categories/new`, `categories/edit/{id}`.

### Tests de ViewModel sin emulador

- [x] `:apps:android` ahora tiene sourceSet JVM `src/test/kotlin/` (junit + kotlinx-coroutines-test + kotest + turbine).
- [x] `MainDispatcherRule` para enchufar `StandardTestDispatcher` en `Dispatchers.Main`.
- [x] `CategoriesTestFixture` réplica in-memory de `categoriesModule` + `busModule` (sin SQLDelight) y arranca Koin para que los `KoinComponent.get<>()` de los VM resuelvan.
- [x] `CategoriesListViewModelTest` (6 casos: observe, tab, filter, delete reactivo, error UUID inválido, clearError).
- [x] `CategoryEditViewModelTest` (6 casos: create, validación de nombre vacío, loadExisting via QueryBus, edición rename+restyle, kind switch limpia clasificadores, hex inválido propaga error).
- [x] `./gradlew :apps:android:testDebugUnitTest` → 12/12 OK en ~2s incremental.

**Entrega:** flujo completo CRUD de categorías. Al pasar el onboarding, las 13 categorías por defecto se siembran via subscriber. Lista reactiva via `Query.asFlow()`. Cambios verificables sin emulador gracias a los unit tests JVM.

---

## Fase 5 — `transactions` + pantalla "Registrar / Listar" ✅

### Dominio `transactions`

- [x] `Transaction`, `TransactionId`, `TransactionType` (`INCOME`/`EXPENSE` — sin `TRANSFER`, requiere `accounts` post-MVP), `TransactionDate`, `TransactionDescription` (max 140), `Amount` (cents > 0).
- [x] VOs adicionales: `IncomeSource`, `OriginRef`, `RecurringRef` (nullable, reservados — siempre null en MVP).
- [x] `CategoryRef` local en `:transactions` (Identifier UUID-validado) en lugar de depender de `:categories` por Gradle.
- [x] Events `@Serializable`: `TransactionRegistered`, `TransactionEdited`, `TransactionDeleted`.
- [x] Repository + `SqlDelightTransactionRepository` + `InMemoryTransactionRepository`.
- [x] Application:
  - `RegisterTransactionCommand` + handler + `TransactionRegistrar`.
  - `EditTransactionCommand` + handler.
  - `DeleteTransactionCommand` + handler.
  - `FindTransactionQuery` + handler.
  - `SearchTransactionsQuery` + handler (filtros type/categoryId/dateFrom/dateTo/amountMin/amountMax + limit/offset; orden default por fecha desc).
- [x] Invariantes: `Amount` valida cents > 0 (estructural); `Transaction.register/edit` valida fecha ≤ hoy (con `Clock`+`TimeZone` inyectables) y "EXPENSE no admite `incomeSource`".
- [x] Tests unitarios (`AmountTest`, `TransactionDescriptionTest`, `TransactionTest`, `TransactionRegistrarTest`, `SearchTransactionsQueryHandlerTest`, `InMemoryTransactionRepositoryTest`) + repo test con SQLite in-memory (`SqlDelightTransactionRepositoryTest`). **29/29 verdes**.

### Pantalla "Transacciones" (`apps/android/.../ui/transactions/`)

- [x] `TransactionsListScreen`: lista reverse-cronológica con filtro por tipo (Todas / Ingresos / Gastos) vía `TabRow`. FAB "+" para añadir. Resuelve nombres de categoría via `ListAllCategoriesQuery` para mostrar nombres legibles en las filas.
- [x] `TransactionEditScreen`: formulario con FilterChips de tipo (deshabilitado en edición), monto en formato decimal, fecha (texto YYYY-MM-DD — DatePicker queda para Fase 7), descripción, selector de categoría (FilterChips poblados por `SearchCategoriesQuery(kind=...)` y recargados al cambiar de tipo), fuente del ingreso (solo si type=INCOME).
- [x] Diálogo de borrado inline.
- [x] `TransactionsListViewModel` con `repository.observeAll()` → `Flow<List<Transaction>>` reactivo (mismo patrón que categorías). `TransactionEditViewModel` con validaciones de monto/categoría.

### Cableado `apps/android`

- [x] `TransactionsModule` añadido a Koin; eventos `TransactionRegistered/Edited/Deleted` registrados en el `DomainEventJsonSerializer`; comandos y queries añadidos a `busModule`.
- [x] `AndroidDatabaseFactory.buildTransactions(passphrase)` + `DatabaseUnlocker.transactions` con tabla centinela `transaction_entry`; `PersistenceModule` expone `TransactionsDatabase` y `SqlDelightTransactionRepository`.
- [x] Navegación: `Home` → `Transactions` → `new` / `edit/{id}`; `Home` añade botón "Transacciones".
- [ ] **Pendiente Fase 7**: la home muestra "últimas 5 transacciones" como mejora iterativa (sigue siendo placeholder con dos botones de acceso rápido).

### Tests JVM de ViewModel (sin emulador)

- [x] `TransactionsTestFixture` réplica in-memory del cableado (transacciones + categorías + buses) compartiendo `MainDispatcherRule` con la Fase 4.
- [x] `TransactionsListViewModelTest` (4 casos: observe + nombres de categoría, filtro por tipo, delete reactivo, error con UUID inválido).
- [x] `TransactionEditViewModelTest` (7 casos: pre-carga EXPENSE, switch de tipo recarga categorías, save crea, monto ≤ 0 valida, categoría requerida, `loadExisting` poblando, fecha futura propaga error del agregado).
- [x] `./gradlew :apps:android:testDebugUnitTest` → 25/25 verdes en ~3s incremental.

**Entrega:** flujo completo de registrar, editar y borrar transacciones. La lista refleja cambios en tiempo real (Flow reactivo + `Query.asFlow().mapToList()`). Las dos pantallas y sus VMs están cubiertas por tests JVM que no necesitan emulador.

---

## Fase 6 — `analytics` + pantalla "Estadísticas" ✅

### Decisión arquitectónica

**On-the-fly en lugar de proyecciones materializadas.** El roadmap original planteaba read models persistidos + subscribers + `RebuildProjectionsCommand`. En su lugar:

- Las queries calculan al vuelo leyendo `TransactionRepository.all()` + `CategoryRepository.all()` y agregando en memoria.
- La reactividad la da `transactions.observeAll()` del repo: el VM se suscribe y re-ejecuta las queries cuando hay cambios.
- Sin tablas `monthly_summary.sq` etc., sin subscribers a eventos de transacciones, sin `RebuildProjectionsCommand`.

**Por qué:** a escala MVP (cientos de transacciones) las queries on-the-fly son sub-milisegundo, evitan el riesgo de proyecciones desincronizadas tras un `CategoryReclassified`, y simplifican el modelo: una sola fuente de verdad (las tablas de transactions + categories). Cuando la escala lo justifique, se reintroducen proyecciones siguiendo el patrón ya construido en Fase 2 (Event Store + subscribers).

### Dominio / Application `analytics`

- [x] Response DTOs: `MonthlySummaryResponse` (totales income/expense/balance + splits fixed/variable/essential/discretionary), `CategoryBreakdownResponse` + `CategoryBreakdownItem` (con `share: Double`), `MonthlyEvolutionResponse` + `MonthlyEvolutionPoint`.
- [x] Helper interno `YearMonth` (formato `YYYY-MM`, aritmética mes-a-mes con manejo de límite de año).
- [x] Queries + handlers:
  - `FindMonthlySummaryQuery(yearMonth)` / `FindCurrentMonthSummaryQuery` — resuelve fijo/variable/esencial/discrecional via lookup en `CategoryRepository`.
  - `FindCategoryBreakdownQuery(yearMonth, type)` — agrupa por categoría con share.
  - `FindMonthlyEvolutionQuery(monthsBack)` — serie contigua hasta el mes actual.
- [x] **Sin proyecciones**: se eliminó el plugin SQLDelight de `:analytics/build.gradle.kts` y se removió `AnalyticsDatabase`; `:analytics` ahora depende de `:transactions` y `:categories` por Gradle.
- [x] `jvm()` target añadido para tests JVM rápidos.
- [x] Tests: 9 casos (`FindMonthlySummaryQueryHandlerTest` × 3, `FindCategoryBreakdownQueryHandlerTest` × 3, `FindMonthlyEvolutionQueryHandlerTest` × 3 incluyendo cruce de año y validación `monthsBack ∈ 1..36`). **9/9 verdes**.

### Pantalla "Estadísticas" (`apps/android/.../ui/analytics/`)

- [x] `StatsScreen` con `TabRow` de tres pestañas:
  - **Resumen**: cards de ingreso/gasto/saldo (saldo en rojo si negativo) + cards mini fijo/variable y esencial/discrecional.
  - **Por categoría**: lista con color de la categoría, monto, barra proporcional dibujada con `Canvas`.
  - **Evolución**: gráfica de líneas (Canvas) de ingresos y gastos en los últimos 6 meses + leyenda + ejes mínimos.
- [x] Gráficas con Compose `Canvas` (sin librería externa).
- [x] `StatsViewModel` con reactividad via `transactions.observeAll()` → re-ejecuta las 3 queries en cada emisión; permite cambiar `yearMonth` y `tab`.

### Cableado `apps/android`

- [x] `analyticsModule` añadido a Koin (4 handlers); `busModule` extendido con las queries.
- [x] Navegación: `Home` → `Stats`; nueva ruta `stats` en `MainActivity`.
- [x] `Home` añade botón "Estadísticas".

### Tests JVM de ViewModel (sin emulador)

- [x] `StatsTestFixture` réplica in-memory que arranca Koin con `TransactionRepository` + `CategoryRepository` + los 4 handlers de analytics; reutiliza `MainDispatcherRule`.
- [x] `StatsViewModelTest` (4 casos: carga inicial de summary+breakdown+evolution, cambio de yearMonth re-fetchea, transacción nueva propaga via flow, error en yearMonth inválido).
- [x] `./gradlew :apps:android:testDebugUnitTest` → 29/29 verdes (incluye los 25 anteriores).

**Entrega:** tres vistas estadísticas funcionales respondiendo a las preguntas del MVP ("de dónde viene mi dinero", "en qué se va"). Al registrar/editar/borrar una transacción, las estadísticas se recalculan automáticamente via el `observeAll()` reactivo del repositorio de transacciones — sin proyecciones intermedias.

---

## Fase 7 — Pulido e integración 🟡 (parcial)

Cierre del MVP. Sin contextos nuevos: integra las pantallas existentes en una experiencia coherente.

### Hecho

- [x] **Pantalla Home definitiva** ([apps/android/.../ui/home/](apps/android/src/main/kotlin/within/means/android/ui/home/)):
  - Saludo con el `displayName` (resuelto via `FindDefaultUserQuery`).
  - Resumen del mes en curso: ingresos / gastos / saldo (saldo en rojo si negativo) + nota de moneda base.
  - "Últimos movimientos" (top 5 via `SearchTransactionsQuery(limit=5)`, con nombre de categoría resuelto) + link "Ver todo" hacia la pestaña de transacciones.
  - FAB "+" que abre el formulario de nueva transacción.
  - Reactividad: `HomeViewModel` se suscribe a `transactions.observeAll()` y re-ejecuta summary + recent en cada cambio.
- [x] **Bottom navigation** ([MainActivity.kt](apps/android/src/main/kotlin/within/means/android/MainActivity.kt)) con 5 tabs (`Inicio`, `Movimientos`, `Estadísticas`, `Categorías`, `Ajustes`). Cada tab mantiene su back-stack via `saveState`/`restoreState`; el bar se oculta en `Onboarding`/`Unlock`. Los list-screens (categorías, transacciones, stats) ya no llevan back-arrow.
- [x] **Pantalla "Ajustes"** ([apps/android/.../ui/settings/](apps/android/src/main/kotlin/within/means/android/ui/settings/)): edición de perfil (nombre, idioma `es/en`, moneda base `EUR/USD/CUP`) → despacha `UpdateUserPreferencesCommand`, confirma con snackbar "Preferencias guardadas". Validación de nombre no vacío.
- [x] **Estados vacíos**: HomeScreen ("Aún no hay movimientos. Pulsa + para registrar el primero."), CategoriesListScreen, TransactionsListScreen, StatsScreen (sin datos para el mes seleccionado).
- [x] **Estados de error unificados con Snackbar** — patrón consistente: `LaunchedEffect(state.errorMessage) → showSnackbar + clearError`. Mensajes user-friendly via `UserErrorMessages` (mapea SQLite/cipher a español).
- [x] **Loading indicators**: `LinearProgressIndicator` en Home (mientras `summary == null && loading`) y Settings (durante la carga del perfil).
- [x] **Tests JVM de ViewModel** para las nuevas pantallas:
  - `HomeViewModelTest` (4 casos: carga inicial, cap a 5 recientes, propagación reactiva, mapa de category names).
  - `SettingsViewModelTest` (4 casos: carga del perfil, save persiste, validación nombre vacío, edición limpia `savedAck`).
  - **37/37 verdes en `:apps:android`** (incluye los 25 anteriores).
- [x] **`./gradlew build` end-to-end verde** (criterio 1 de "MVP listo"):
  - Schema baselines `1.db` generados y commiteados para `:shared`, `:users`, `:categories`, `:transactions` (necesarios con `verifyMigrations = true`). El `.gitignore` se ajustó para versionarlos.
  - `minSdk` subido de 21 a 23 — `Android Keystore` con `KeyGenParameterSpec` + `KEY_ALGORITHM_HMAC_SHA256` requieren API 23. Antes el lint marcaba 4 errores que la app habría reproducido en runtime en cualquier dispositivo <Android 6.0.

### Pendiente (post-MVP o iteración posterior)

- [ ] **Cambiar PIN** en Ajustes — requiere `PRAGMA rekey` sobre cada DB cifrada (Shared/Users/Categories/Transactions) y validación del PIN actual antes de derivar el nuevo. Bloqueado por SQLCipher; sub-feature suficientemente grande para ir aparte.
- [x] **DatePicker** en [TransactionEditScreen](apps/android/src/main/kotlin/within/means/android/ui/transactions/TransactionEditScreen.kt) — `DateField` interno con `OutlinedTextField` read-only que abre `DatePickerDialog` (Material 3). El estado del VM sigue siendo `String` ISO `YYYY-MM-DD`; las conversiones a/desde `epochMillis` UTC viven en el composable.
- [ ] **Theming refinado**: paleta personalizada completa (tipografía, iconografía, tonos custom). Ya tenemos green + dark scheme básico.
- [ ] **Accesibilidad**: audit formal de content descriptions, contraste, tap targets.
- [ ] **Validación del cifrado**: test instrumentado (requiere emulador) que `adb pull within_means.db` no abre con `sqlite3` sin passphrase.
- [ ] **Smoke test manual completo**: instalar APK + 20 tx + tres tabs de estadísticas funcionando.

**Entrega actual:** APK MVP usable con bottom nav, home definitiva, ajustes funcionales (excepto cambio de PIN). Compila + ensambla + 37 tests JVM verdes. Flujo coherente end-to-end desde onboarding hasta editar perfil.

---

## Criterios de "MVP listo"

El MVP se da por entregado cuando se cumplen **todos**:

1. **Compila y testea.** `./gradlew build` y `./gradlew test` pasan.
2. **APK funcional.** `./gradlew :apps:android:installDebug` instala en emulador/dispositivo y la app arranca.
3. **Datos cifrados.** El archivo `within_means.db` no es legible sin passphrase (verificado con `adb pull` + `sqlite3`).
4. **Smoke test completo:** registrar 20 transacciones de ejemplo y ver los tres tipos de estadísticas funcionando.
5. **Event Store íntegro:** los eventos de dominio (`UserDefaultCreated`, `CategoryCreated/Renamed/Restyled/Reclassified/Deleted`, `TransactionRegistered/Edited/Deleted`) se persisten en `domain_events` con su payload serializable. Nota: el plan original hablaba de `RebuildProjectionsCommand`, pero en Fase 6 se decidió no materializar proyecciones (las queries de `analytics` corren on-the-fly), así que este criterio se redujo a verificar el log de eventos.
6. **Sin deuda crítica:** no hay TODOs sin fecha; no hay `expect/actual` sin implementar.
7. **Doc actualizada:** cualquier desviación del plan está reflejada en los `.md` correspondientes.

---

## Política sobre las pantallas en `apps/android`

- **Cada pantalla vive en `apps/android/src/main/kotlin/within/means/android/ui/<context>/`**, no en el módulo KMP. Razón: el módulo de dominio (`:transactions`, `:categories`...) permanece KMP-portable; la UI es Android-specific (Compose Multiplatform aún no es objetivo en MVP, pero esta separación deja la puerta abierta a iOS / Desktop sin reescritura del dominio).
- **Un `ViewModel` por pantalla**, recibe los buses por DI. Sin lógica de negocio.
- **Sin estado mutable global.** El estado de UI vive en el `ViewModel`; el estado de dominio vive en SQLite + Event Store.
- **Pantallas reactivas con `Flow`** donde aplique (`Query.asFlow().mapToList(Dispatchers.IO)`), no polling.

---

## Lo que NO está en el MVP

Para que quede explícito, **fuera del MVP**:

- Contextos `accounts`, `budgets`, `recurring`, `assets`, `liabilities`, `forecasts`, `goals`, `dashboard`.
- Multi-usuario, familias, login.
- Sincronización entre dispositivos.
- Backend Ktor.
- iOS y Desktop (este último se añade en una fase post-MVP).
- Importación / exportación de datos.
- Biometría (sólo PIN en MVP).
- Multidivisa real con tipos de cambio (sólo moneda base del usuario).
- KPIs profesionales (sólo las preguntas básicas — el catálogo profesional llega en fases posteriores).

Para todo eso, ver [`post-mvp.md`](post-mvp.md) y [`../scalability/kpi-catalog.md`](../scalability/kpi-catalog.md).
