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

## Fase 2 — Kernel `:shared` (sin UI)

Excepción a la regla de "una pantalla por fase": el kernel no tiene UI propia, sólo provee las piezas que usarán los demás. Sigue el patrón estricto del esqueleto Java DDD.

### Dominio compartido (`commonMain/.../shared/domain/`)

- [ ] `AggregateRoot` con `record(event)` y `pullDomainEvents()`.
- [ ] `ValueObject`, `StringValueObject`, `IntValueObject`, `DateValueObject`.
- [ ] `Identifier` (validación UUID con regex).
- [ ] `UuidGenerator` (interfaz).
- [ ] `money/`: `Money(cents: Long, currency: Currency)` inmutable con operaciones; `Currency` (enum ISO 4217 reducido a las relevantes).
- [ ] `bus/command/{Command, CommandBus, CommandHandler}`.
- [ ] `bus/query/{Query, QueryBus, QueryHandler, Response}`.
- [ ] `bus/event/{DomainEvent, EventBus, DomainEventSubscriber, DomainEventStore}`.
- [ ] `criteria/{Criteria, Filter, Filters, Order, FilterField, FilterOperator, FilterValue, OrderType}`.

### Infraestructura compartida (`commonMain/.../shared/infrastructure/`)

- [ ] `RealUuidGenerator` (con `com.benasher44:uuid`).
- [ ] `InMemoryCommandBus`, `InMemoryQueryBus`, `InMemoryEventBus`.
- [ ] `SqlDelightDomainEventStore` (persistencia a `domain_events.sq`).
- [ ] `EventStoreBackedEventBus` (envoltorio: persiste y distribuye).
- [ ] `DomainEventJsonSerializer` (kotlinx.serialization).
- [ ] `SqlCriteriaTranslator` (Criteria → SQL parametrizado).

### Persistencia

- [ ] `src/shared/sqldelight/within/means/shared/db/domain_events.sq`.
- [ ] Configuración del plugin SQLDelight: una clase DB por módulo (`SharedDatabase`, y luego `UsersDatabase`, `CategoriesDatabase`, etc.) compartiendo el archivo físico `within_means.db`. `verifyMigrations = true` en todas.

### Tests (`commonTest`)

- [ ] Tests unitarios de los tres buses con handlers stub.
- [ ] Tests de `Identifier` (UUID válido vs inválido).
- [ ] Tests de `Money` (operaciones, igualdad, currency mismatch).
- [ ] Tests de `Criteria` y `SqlCriteriaTranslator` (filtros, orden, paginación, sanitización).
- [ ] Tests del Event Store con `JdbcSqliteDriver.IN_MEMORY` (append, findByAggregate, findByName, findAllSince).
- [ ] Tests del `EventStoreBackedEventBus` (persiste primero, luego notifica subscribers).
- [ ] Tests del `DomainEventJsonSerializer` (ida y vuelta).

**Entrega:** kernel `:shared` testeado al 100%; los buses dispatch/ask funcionan, los eventos se persisten y se entregan a subscribers. `./gradlew :shared:test` pasa.

---

## Fase 3 — `users` + pantalla "Onboarding"

Primer vertical end-to-end. Introduce **el cableado completo de `apps/android`** (Koin, SQLCipher, navegación, theming) porque es la primera fase con UI.

### Dominio `users`

- [ ] `UserProfile`, `UserId`, `DisplayName`, `Locale`, `BaseCurrency`.
- [ ] Events: `UserDefaultCreated`, `UserPreferencesUpdated`.
- [ ] `UserProfileRepository` (interfaz).
- [ ] Application:
  - `EnsureDefaultUserCommand` + handler (idempotente).
  - `UpdateUserPreferencesCommand` + handler.
  - `FindUserQuery` + handler.
- [ ] Infrastructure:
  - `users.sq` con tabla `user_profile`.
  - `UserProfileRowMapper` (con `rehydrate` y `toRow`).
  - `SqlDelightUserProfileRepository`, `InMemoryUserProfileRepository`.
- [ ] Factorías: `UserProfile.bootstrap(...)` (emite evento) y `UserProfile.rehydrate(...)`.
- [ ] Tests unitarios completos.

### Cableado `apps/android` (común a todas las fases siguientes)

- [ ] Koin con módulos `SharedModule`, `UsersModule`, `BusModule`, `PersistenceModule`. Se irán añadiendo `CategoriesModule`, `TransactionsModule`, `AnalyticsModule` en las próximas fases.
- [ ] `AndroidDatabaseFactory` con `AndroidSqliteDriver` + `SupportFactory` de SQLCipher.
- [ ] `KeystoreManager` (clave maestra en Android Keystore, no exportable).
- [ ] `PassphraseProvider` (PIN inicial; biometría llega en post-MVP).
- [ ] Navegación: `androidx.navigation:navigation-compose` con un `NavHost` declarado en `MainActivity`.
- [ ] Theming Material 3 inicial (paleta clara/oscura básica, tipografía por defecto).
- [ ] `EnsureDefaultUserCommand` ejecutado al primer arranque (dentro de un `LaunchedEffect` en el composable raíz).

### Pantalla "Onboarding" (`apps/android/.../ui/onboarding/`)

- [ ] `OnboardingScreen` (composable): 3 pasos en secuencia.
  - **Paso 1 — Bienvenida:** título + descripción + botón "Empezar".
  - **Paso 2 — PIN:** input PIN 4-6 dígitos + confirmación (despacha derivación de passphrase para SQLCipher).
  - **Paso 3 — Preferencias:** dropdown idioma (ES/EN) + dropdown moneda base (EUR/USD/...). Botón "Finalizar" → despacha `UpdateUserPreferencesCommand` y navega a una pantalla placeholder "Home" temporal.
- [ ] `OnboardingViewModel` (con Koin + `koin-compose-viewmodel`) que recibe `CommandBus` y `QueryBus`.
- [ ] Si `FindUserQuery` ya devuelve un usuario con preferencias completas, el onboarding se salta en arranques posteriores.

**Entrega:** instalas el APK por primera vez → pasas el onboarding → la DB se cifra con tu PIN → el `UserProfile` queda persistido. En arranques posteriores entras directo a Home placeholder.

---

## Fase 4 — `categories` + pantalla "Categorías"

### Dominio `categories`

- [ ] `Category`, `CategoryId`, `CategoryName`, `CategoryColor`, `CategoryIcon`, `ParentCategoryId`.
- [ ] VOs de clasificación: `CategoryKind`, `CategoryNature`, `CategoryEssentiality`, `CategoryProductive`, `EngelGroup`.
- [ ] Events: `CategoryCreated`, `CategoryRenamed`, `CategoryReclassified`, `CategoryDeleted`.
- [ ] Repository + impl SQLDelight + InMemory.
- [ ] Application: comandos CRUD + `SearchCategoriesByCriteriaQuery`.
- [ ] **Seed por defecto** suscrito a `UserDefaultCreated`: `DefaultCategoriesSeeder` siembra las ~13 categorías base con sus clasificadores.
- [ ] Tests unitarios + tests de integración del seeder.

### Pantalla "Categorías" (`apps/android/.../ui/categories/`)

- [ ] `CategoriesListScreen`: lista categorías agrupadas por kind (ingreso/gasto/transferencia), cada una con su color e icono. Filtro por kind.
- [ ] `CategoryEditScreen`: formulario crear/editar con name, color (color picker básico), icono (seleccionable de un set Material Symbols), kind, nature, essentiality, productive, engelGroup. Botón guardar despacha `CreateCategoryCommand` o `ReclassifyCategoryCommand`.
- [ ] `CategoryDeleteDialog` con confirmación.
- [ ] `CategoriesViewModel` con `Flow<List<CategoryResponse>>` reactivo (`Query.asFlow()` de SQLDelight).

### Cableado `apps/android`

- [ ] Añadir `CategoriesModule` a Koin.
- [ ] Añadir entrada de navegación: `home → categorias` (botón en la home placeholder).

**Entrega:** flujo completo CRUD de categorías. Al pasar el onboarding, las categorías por defecto ya están creadas y se ven en la lista. Se pueden crear, editar y borrar nuevas.

---

## Fase 5 — `transactions` + pantalla "Registrar / Listar"

### Dominio `transactions`

- [ ] `Transaction`, `TransactionId`, `TransactionType`, `TransactionDate`, `TransactionDescription`, `Amount`.
- [ ] VOs adicionales: `IncomeSource`, `OriginRef`, `RecurringRef` (reservado, siempre null en MVP).
- [ ] Events: `TransactionRegistered`, `TransactionEdited`, `TransactionDeleted`.
- [ ] Repository + impl SQLDelight + InMemory.
- [ ] Application:
  - `RegisterTransactionCommand` + handler + `TransactionRegistrar`.
  - `EditTransactionCommand` + handler.
  - `DeleteTransactionCommand` + handler.
  - `FindTransactionQuery` + handler.
  - `SearchTransactionsByCriteriaQuery` + handler (usa `SqlCriteriaTranslator`).
- [ ] Invariantes: monto positivo, fecha ≤ hoy, coherencia `type`↔`incomeSource`.
- [ ] Tests unitarios + repo tests con SQLite in-memory.

### Pantalla "Transacciones" (`apps/android/.../ui/transactions/`)

- [ ] `TransactionsListScreen`: lista reverse-cronológica con filtros (rango fechas, categoría, tipo, monto min/max). FAB "+" para añadir.
- [ ] `TransactionEditScreen`: formulario tipo (ingreso/gasto), monto, categoría (selector que hace `SearchCategoriesByCriteriaQuery`), fecha (DatePicker), descripción, opcional fuente (`originRef`). Botón guardar despacha `RegisterTransactionCommand` o `EditTransactionCommand`.
- [ ] `TransactionDeleteDialog`.
- [ ] `TransactionsViewModel` con paginación opcional (SQLDelight `mapToList`).

### Cableado `apps/android`

- [ ] Añadir `TransactionsModule` a Koin.
- [ ] Navegación: `home → transacciones → registrar/editar`.
- [ ] La home placeholder ahora muestra "últimas 5 transacciones" como mejora iterativa.

**Entrega:** flujo completo de registrar, editar y borrar transacciones. La lista refleja cambios en tiempo real (Flow reactivo).

---

## Fase 6 — `analytics` + pantalla "Estadísticas"

### Dominio `analytics`

- [ ] Read models: `MonthlySummary`, `CategoryBreakdown`, `MonthlyEvolution`.
- [ ] Schemas SQLDelight: `monthly_summary.sq`, `category_breakdown.sq`, `monthly_evolution.sq`.
- [ ] Subscribers a eventos de `transactions`:
  - `UpdateMonthlySummaryOnTransactionRegistered` (+ edited, deleted).
  - `UpdateCategoryBreakdownOnTransactionRegistered` (+ edited, deleted).
  - `UpdateMonthlyEvolutionOnTransactionRegistered` (+ edited, deleted).
- [ ] Queries: `FindCurrentMonthSummaryQuery`, `FindMonthlySummaryQuery`, `FindCategoryBreakdownQuery`, `FindMonthlyEvolutionQuery`.
- [ ] Comando administrativo `RebuildProjectionsCommand` que reproduce eventos del Event Store y reconstruye los read models desde cero.
- [ ] Tests: dado un set de transacciones, las proyecciones reflejan los totales correctos.

### Pantalla "Estadísticas" (`apps/android/.../ui/analytics/`)

- [ ] `StatsScreen` con `TabRow` de tres pestañas:
  - **Resumen mensual:** totales ingreso/gasto/saldo + cards de fijo/variable/esencial/discrecional. Selector de mes.
  - **Por categoría:** pie chart o bar chart por categoría (gastos por defecto, toggle a ingresos). Selector de periodo.
  - **Evolución:** gráfica de líneas de ingresos y gastos por mes (últimos 6/12 meses).
- [ ] Gráficas con Compose `Canvas` o librería ligera (decisión en la fase).
- [ ] `AnalyticsViewModel` que llama a las queries vía `QueryBus`.

### Cableado `apps/android`

- [ ] Añadir `AnalyticsModule` a Koin.
- [ ] Navegación: `home → estadísticas`.

**Entrega:** se ven los gráficos respondiendo a las preguntas del MVP. Al editar o borrar una transacción, las estadísticas se actualizan automáticamente vía Event Store + subscribers.

---

## Fase 7 — Pulido e integración

Cierre del MVP. Sin contextos nuevos: integra las pantallas existentes en una experiencia coherente.

- [ ] **Pantalla Home definitiva:**
  - Saludo con el `displayName` del usuario.
  - Resumen del mes en curso (totales).
  - "Últimos movimientos" (5 más recientes con scroll a la lista completa).
  - Botón flotante "+" para registrar transacción rápida.
  - Acceso rápido a las otras pantallas.
- [ ] **Bottom navigation** o **Navigation Rail** con: Home, Transacciones, Estadísticas, Categorías, Ajustes.
- [ ] **Pantalla "Ajustes":** editar perfil, cambiar PIN, cambiar idioma/moneda base (despacha `UpdateUserPreferencesCommand`).
- [ ] **Theming Material 3 refinado:** paleta personalizada (no la system), tipografía, iconografía consistente.
- [ ] **Estados vacíos** ilustrativos en cada pantalla (cuando no hay transacciones, no hay categorías, etc.).
- [ ] **Estados de error** unificados con `Snackbar`.
- [ ] **Loading skeletons** o indicadores en las queries pesadas.
- [ ] **Accesibilidad:** content descriptions, contraste, tamaños tap mínimos.
- [ ] **Validación del cifrado:** test instrumentado que verifica que `within_means.db` no es legible sin passphrase.
- [ ] **Smoke tests manuales completos:** flujo completo desde primer arranque hasta dashboard funcional.

**Entrega:** APK MVP entregable. App usable y atractiva con flujo coherente.

---

## Criterios de "MVP listo"

El MVP se da por entregado cuando se cumplen **todos**:

1. **Compila y testea.** `./gradlew build` y `./gradlew test` pasan.
2. **APK funcional.** `./gradlew :apps:android:installDebug` instala en emulador/dispositivo y la app arranca.
3. **Datos cifrados.** El archivo `within_means.db` no es legible sin passphrase (verificado con `adb pull` + `sqlite3`).
4. **Smoke test completo:** registrar 20 transacciones de ejemplo y ver los tres tipos de estadísticas funcionando.
5. **Reconstrucción:** `RebuildProjectionsCommand` regenera los read models correctamente desde el Event Store.
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
