# Roadmap

Fases ordenadas de implementación. Cada fase entrega un incremento verificable (smoke test o test automatizado).

## Fase 1 — Bootstrap del repositorio

- [ ] `settings.gradle.kts` con módulos vacíos (`:shared`, contextos, apps).
- [ ] `build.gradle.kts` raíz con plugins comunes.
- [ ] `gradle/libs.versions.toml` (version catalog).
- [ ] `.gitignore`, `.editorconfig`.
- [ ] Wrapper de Gradle.
- [ ] `./gradlew tasks` compila sin errores.

**Entrega:** repositorio que compila vacío.

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
- [ ] `Criteria`, `Filter`, `Filters`, `Order`, `FilterField`, `FilterOperator`, `FilterValue`.
- [ ] VOs compartidos: `Money`, `Currency`.
- [ ] Tests de buses y `Criteria` en `commonTest`.

**Entrega:** kernel testeado; los buses dispatch/ask funcionan en aislamiento.

## Fase 3 — Contexto `users` (vertical end-to-end)

Primer contexto completo. Valida la arquitectura de extremo a extremo.

- [ ] Domain: `User`, `UserId`, `Email`, `UserName`, `PasswordHash`, `Family`, `FamilyId`, `FamilyRole`.
- [ ] Events: `UserRegistered`, `FamilyCreated`.
- [ ] Repository: `UserRepository`, `FamilyRepository`.
- [ ] Application: `RegisterUserCommand` + handler + `UserRegistrar`; `CreateFamilyCommand` + handler.
- [ ] Application queries: `FindUserQuery` + handler.
- [ ] Infrastructure: schemas SQLDelight `users.sq`, `family.sq`; `SqlDelightUserRepository`, `InMemoryUserRepository`.
- [ ] Tests unitarios completos.

**Entrega:** registrar usuario y crear familia con un test de integración usando `InMemoryCommandBus` + `InMemoryUserRepository`.

## Fase 4 — `apps/android` mínimo

Validación de la cadena UI → bus → handler → repo en la plataforma de arranque.

- [ ] Configuración `com.android.application` con Compose Multiplatform.
- [ ] `MainActivity` con `setContent { ... }` apuntando a pantalla "Register user".
- [ ] Cableado Koin: módulo `SharedModule`, `UsersModule`, `BusModule`.
- [ ] SQLDelight driver Android cableado (`AndroidSqliteDriver`).
- [ ] La pantalla dispara `RegisterUserCommand` por el `CommandBus`.
- [ ] Configuración mínima de manifiesto, theming Material 3.

**Entrega:** `./gradlew :apps:android:installDebug` instala la app en emulador/dispositivo; se puede registrar un usuario que persiste en SQLite local.

## Fase 5 — Contexto `accounts`

- [ ] `Account`, `AccountId`, `AccountName`, `AccountType`, `Balance`.
- [ ] Commands: `CreateAccount`, `RenameAccount`, `ArchiveAccount`.
- [ ] Queries: `FindAccount`, `SearchAccountsByFamily`.
- [ ] Events: `AccountCreated`, `AccountArchived`.
- [ ] Persistencia SQLDelight.
- [ ] Suscriptor `UpdateBalanceOnTransactionRegistered` (placeholder hasta Fase 6).
- [ ] Pantallas Android: listado y creación de cuentas.

**Entrega:** crear cuentas asociadas a una familia.

## Fase 6 — Contexto `transactions` (núcleo)

- [ ] `Transaction`, `TransactionId`, `TransactionType`, `TransactionDate`, `TransactionDescription`.
- [ ] Commands: `RegisterTransaction`, `RegisterTransfer`, `EditTransaction`, `DeleteTransaction`.
- [ ] Queries: `FindTransaction`, `SearchTransactionsByCriteria` (estrenamos `Criteria`).
- [ ] Events: `TransactionRegistered`, `TransactionEdited`, `TransactionDeleted`.
- [ ] Persistencia.
- [ ] Cablear suscriptor en `accounts` para recalcular saldo.
- [ ] Pantallas Android: registrar transacciones, buscar por filtros.

**Entrega:** registrar gastos/ingresos; el saldo de la cuenta se actualiza vía evento.

## Fase 7 — Contexto `categories`

- [ ] `Category`, `CategoryId`, `CategoryName`, `CategoryColor`, `CategoryIcon`, `ParentCategoryId`.
- [ ] Commands y queries de CRUD.
- [ ] Seed de categorías por defecto al crear familia (suscriptor de `FamilyCreated`).
- [ ] Asociar `CategoryId` a `Transaction` (modificación del agregado).
- [ ] Pantallas Android: gestión y asignación de categorías.

**Entrega:** transacciones categorizadas; categorías por familia.

## Fase 8 — Contexto `budgets`

- [ ] `Budget`, `BudgetPeriod`, `MonthlyLimit`, `Consumption`.
- [ ] Commands: `SetBudget`, `UpdateBudget`, `RemoveBudget`.
- [ ] Queries: `FindBudget`, `SearchBudgetsByPeriod`.
- [ ] Events: `BudgetSet`, `BudgetExceeded`.
- [ ] Suscriptor `UpdateBudgetOnTransactionRegistered`.
- [ ] Pantallas Android: configuración y consulta de presupuestos.

**Entrega:** establecer límites mensuales por categoría; aviso visual cuando se superan.

## Fase 9 — Contexto `analytics`

- [ ] Read models: `MonthlySummary`, `CategoryBreakdown`, `AccountFlow`.
- [ ] Suscriptores que proyectan eventos en tablas materializadas.
- [ ] Queries de lectura.
- [ ] Pantallas Android: dashboard con gráficos básicos.

**Entrega:** dashboard funcional con resumen mensual y desglose por categoría.

## Fase 10 — `apps/desktop`

- [ ] Configurar `apps/desktop` con `org.jetbrains.compose` y target JVM.
- [ ] Reutilizar pantallas de `apps/android` extrayendo lo común a `:apps:ui-common` o `commonMain`.
- [ ] SQLDelight driver JVM (`JdbcSqliteDriver`).
- [ ] Adaptar navegación a Compose Desktop.
- [ ] Smoke tests manuales.

**Entrega:** `./gradlew :apps:desktop:run` lanza la app con paridad funcional respecto a Android.

## Fase 11 — Pulido y exportación

- [ ] Exportar/importar datos (CSV, JSON).
- [ ] Multidivisa real (tipos de cambio).
- [ ] Backups locales.
- [ ] Iconografía y theming Material.

## Fuera del alcance inicial (post-1.0)

- Backend Ktor para sincronización entre dispositivos.
- iOS (Compose Multiplatform iOS).
- Notificaciones push.
- OCR de recibos.
- Categorización automática (ML).
- Soporte multidivisa con conversión en tiempo real.

## Criterios de "done" por fase

Cada fase se considera completada cuando:

1. **Compila.** `./gradlew build` pasa.
2. **Tests pasan.** Unit + integración del contexto.
3. **Smoke test manual.** Para fases con UI (Fases 4-9), la pantalla funciona en Android. Tras la Fase 10, también en Desktop.
4. **Documentación.** Si la fase introduce nuevos patrones, se actualiza el `.md` correspondiente.
5. **Sin deuda crítica.** No hay TODOs sin fecha ni `expect/actual` sin implementar.
