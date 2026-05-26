# Roadmap — MVP

Fases del MVP. Objetivo: app Android que permite **registrar ingresos y gastos diarios** y responde a "de dónde viene mi dinero" y "en qué se va".

Cada fase entrega un incremento verificable. Las fases posteriores al MVP están en [`post-mvp.md`](post-mvp.md).

## Fase 1 — Bootstrap del repositorio

- [ ] `settings.gradle.kts` con módulos (`:shared`, contextos del MVP, `:apps:android`).
- [ ] `build.gradle.kts` raíz con plugins comunes.
- [ ] `gradle/libs.versions.toml` (version catalog).
- [ ] `.gitignore`, `.editorconfig`.
- [ ] Wrapper de Gradle.
- [ ] `./gradlew tasks` compila sin errores.

**Entrega:** repositorio que compila vacío.

---

## Fase 2 — Kernel `:shared`

- [ ] Base classes en `commonMain`:
  - `AggregateRoot` (con `pullDomainEvents()`).
  - `ValueObject`, `StringValueObject`, `IntValueObject`, `DateValueObject`.
  - `Identifier` (validación UUID).
- [ ] Buses (interfaces) en `commonMain`:
  - `Command`, `CommandBus`, `CommandHandler<C>`.
  - `Query`, `QueryBus`, `QueryHandler<Q, R>`, `Response`.
  - `DomainEvent`, `EventBus`, `DomainEventSubscriber`.
- [ ] Implementaciones in-memory:
  - `InMemoryCommandBus`, `InMemoryQueryBus`, `InMemoryEventBus`.
- [ ] **Event Store** (`domain_events.sq` + `SqlDelightDomainEventStore` + `EventStoreBackedEventBus`).
- [ ] `Criteria`, `Filter`, `Filters`, `Order`, `FilterField`, `FilterOperator`, `FilterValue`.
- [ ] `SqlCriteriaTranslator` en `shared/infrastructure/persistence/`.
- [ ] `UuidGenerator` (interfaz) + `RealUuidGenerator` (impl con `benasher44/uuid`).
- [ ] VOs compartidos: `Money`, `Currency`.
- [ ] Configuración del plugin SQLDelight en cada módulo: `SharedDatabase` en `:shared`, y una DB por contexto (`UsersDatabase`, `CategoriesDatabase`, `TransactionsDatabase`, `AnalyticsDatabase`) compartiendo el archivo físico `within_means.db`. `verifyMigrations = true` en todas.
- [ ] Tests de buses, `Criteria`, `SqlCriteriaTranslator`, `UuidGenerator` y Event Store en `commonTest`.

**Entrega:** kernel testeado; los buses dispatch/ask funcionan y los eventos se persisten en el Event Store.

---

## Fase 3 — Contexto `users` (mínimo single-user)

Primer contexto end-to-end. Valida la arquitectura de extremo a extremo en su forma más simple.

- [ ] Domain: `UserProfile`, `UserId`, `DisplayName`, `Locale`, `BaseCurrency`.
- [ ] Events: `UserDefaultCreated`, `UserPreferencesUpdated`.
- [ ] Repository: `UserProfileRepository` (interfaz en `domain/`).
- [ ] Application: `EnsureDefaultUserCommand` + handler; `UpdateUserPreferencesCommand` + handler.
- [ ] Queries: `FindUserQuery` + handler.
- [ ] Infrastructure:
  - Schema SQLDelight `users.sq` en `src/users/sqldelight/within/means/users/db/`.
  - `UserProfileRowMapper` (con `rehydrate` y `toRow`).
  - `SqlDelightUserProfileRepository`, `InMemoryUserProfileRepository`.
- [ ] Factorías: `UserProfile.bootstrap(...)` (emite evento) y `UserProfile.rehydrate(...)`.
- [ ] Tests unitarios completos.

**Entrega:** ejecutar `EnsureDefaultUserCommand` crea el usuario por defecto idempotentemente; se puede actualizar preferencias.

---

## Fase 4 — Contexto `categories` (enriquecido)

Categorías con clasificadores semánticos desde el día 1 (ver [`../contexts/mvp.md`](../contexts/mvp.md#categories) para justificación).

- [ ] Domain: `Category`, `CategoryId`, `CategoryName`, `CategoryColor`, `CategoryIcon`, `ParentCategoryId`.
- [ ] VOs de clasificación: `CategoryKind`, `CategoryNature`, `CategoryEssentiality`, `CategoryProductive`, `EngelGroup`.
- [ ] Events: `CategoryCreated`, `CategoryRenamed`, `CategoryReclassified`, `CategoryDeleted`.
- [ ] Repository + impl SQLDelight + InMemory.
- [ ] Application: comandos CRUD + query `SearchCategoriesByCriteriaQuery`.
- [ ] **Seed por defecto** suscrito a `UserDefaultCreated`: `DefaultCategoriesSeeder` siembra las categorías base.
- [ ] Tests unitarios + tests de integración del seeder.

**Entrega:** al crear el usuario por defecto, se crean automáticamente las ~13 categorías base con sus clasificadores.

---

## Fase 5 — Contexto `transactions` (núcleo enriquecido)

- [ ] Domain: `Transaction`, `TransactionId`, `TransactionType`, `TransactionDate`, `TransactionDescription`, `Amount`.
- [ ] VOs adicionales: `IncomeSource`, `OriginRef`, `RecurringRef` (este último reservado, siempre null en MVP).
- [ ] Events: `TransactionRegistered`, `TransactionEdited`, `TransactionDeleted`.
- [ ] Repository + impl SQLDelight + InMemory.
- [ ] Application:
  - `RegisterTransactionCommand` + handler + `TransactionRegistrar`.
  - `EditTransactionCommand` + handler.
  - `DeleteTransactionCommand` + handler.
  - `FindTransactionQuery` + handler.
  - `SearchTransactionsByCriteriaQuery` + handler (usa `SqlCriteriaTranslator`).
- [ ] Invariantes en el agregado: monto positivo, fecha ≤ hoy, coherencia `type`↔`incomeSource`.
- [ ] Tests unitarios + repo tests con SQLite in-memory.

**Entrega:** registrar/editar/borrar transacciones funciona end-to-end; búsquedas con filtros dinámicos funcionan.

---

## Fase 6 — Contexto `analytics` (read models MVP)

- [ ] Read models: `MonthlySummary`, `CategoryBreakdown`, `MonthlyEvolution`.
- [ ] Schemas SQLDelight: `monthly_summary.sq`, `category_breakdown.sq`, `monthly_evolution.sq`.
- [ ] Subscribers a eventos de `transactions`:
  - `UpdateMonthlySummaryOnTransactionRegistered` (+ edited, deleted).
  - `UpdateCategoryBreakdownOnTransactionRegistered` (+ edited, deleted).
  - `UpdateMonthlyEvolutionOnTransactionRegistered` (+ edited, deleted).
- [ ] Queries: `FindCurrentMonthSummaryQuery`, `FindMonthlySummaryQuery`, `FindCategoryBreakdownQuery`, `FindMonthlyEvolutionQuery`.
- [ ] Comando administrativo `RebuildProjectionsCommand` que reproduce eventos del Event Store y reconstruye los read models desde cero (utilidad de mantenimiento).
- [ ] Tests: dado un set de transacciones, las proyecciones reflejan los totales correctos.

**Entrega:** las preguntas "¿cuánto entró/salió este mes?", "¿en qué categorías?" y "¿cómo evolucionó?" se responden vía `QueryBus`.

---

## Fase 7 — `apps/android` mínimo viable

Cableado completo: UI → buses → handlers → repos → DB cifrada.

- [ ] Configuración `com.android.application` con Compose Multiplatform.
- [ ] `MainActivity` con `setContent { ... }`.
- [ ] Cableado Koin: módulos `SharedModule`, `UsersModule`, `CategoriesModule`, `TransactionsModule`, `AnalyticsModule`, `BusModule`, `PersistenceModule`.
- [ ] `AndroidDatabaseFactory` con `AndroidSqliteDriver` + `SupportFactory` de SQLCipher.
- [ ] `KeystoreManager` (clave maestra en Android Keystore, no exportable).
- [ ] `PassphraseProvider` (PIN inicial; biometría más adelante).
- [ ] `EnsureDefaultUserCommand` ejecutado al primer arranque.
- [ ] Pantallas Compose:
  - **Onboarding** (PIN inicial + idioma + moneda base).
  - **Inicio**: resumen del mes (totales + saldo neto) + lista de transacciones recientes.
  - **Registrar transacción** (formulario con tipo, monto, categoría, fecha, descripción, opcional fuente).
  - **Categorías** (lista + crear/editar/borrar).
  - **Estadísticas** (3 vistas: desglose gastos, desglose ingresos, evolución mensual).
- [ ] Theming Material 3.
- [ ] Smoke tests manuales en emulador.

**Entrega:** APK instalable en Android 5.0+; flujo completo de registrar movimientos durante un mes y ver estadísticas.

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
