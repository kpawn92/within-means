# Persistencia

Este documento fija las decisiones técnicas de persistencia y cómo se cablean **sin contaminar el dominio**. Toda la persistencia vive en la capa `infrastructure/` de cada contexto (esquema, mappers, repositorios) más un pequeño cableado en `apps/` (driver, cifrado, passphrase).

## Decisiones

| Punto | Decisión |
|---|---|
| Motor | **SQLDelight** (multiplataforma, type-safe, SQL real). |
| Driver Android | `AndroidSqliteDriver` con **SQLCipher** (`net.zetetic:sqlcipher-android`). |
| Driver Desktop (post-MVP) | `JdbcSqliteDriver` con `sqlcipher-jdbc` o equivalente JVM. |
| Driver tests | `JdbcSqliteDriver` in-memory **sin cifrado** (la seguridad se testea aparte). |
| Esquema | **Un `.sq` por bounded context.** Sin foreign keys cross-context. |
| IDs | **UUID v4** generados en cliente vía `UuidGenerator`. |
| Cifrado at-rest | **SQLCipher (AES-256)** desde la Fase 7 (cableado Android). Passphrase derivada de Android Keystore + PIN/biometría. |
| Event Store | Tabla `domain_events` en `:shared` desde la Fase 2. Persiste todos los eventos publicados. |
| Snapshots periódicos | Tablas materializadas en `analytics` para cierres mensuales y bases de KPIs históricos (post-MVP). |
| Migraciones | Archivos `.sqm` versionados por contexto, ejecutados por SQLDelight. |
| Reactividad | `Query.asFlow().mapToList(Dispatchers.IO)` para observar cambios desde la UI. |
| Key-value (preferencias) | **Multiplatform Settings**, solo para UI state (tema, idioma, último filtro). Nunca para datos de dominio. |

## Estructura física por contexto

```
src/<context>/src/
├── commonMain/
│   ├── kotlin/within/means/<context>/
│   │   ├── domain/
│   │   │   ├── <Aggregate>.kt
│   │   │   ├── <Aggregate>Id.kt
│   │   │   └── <Aggregate>Repository.kt      <-- INTERFAZ pura, sin SQL
│   │   ├── application/...
│   │   └── infrastructure/
│   │       └── persistence/
│   │           ├── SqlDelight<Aggregate>Repository.kt
│   │           ├── <Aggregate>RowMapper.kt
│   │           └── InMemory<Aggregate>Repository.kt
│   └── sqldelight/
│       └── within/means/<context>/db/
│           ├── <context>.sq                  <-- DDL + queries nombradas
│           └── migrations/
│               ├── 1.sqm
│               └── 2.sqm
├── androidMain/                              (vacío normalmente)
└── desktopMain/                              (Fase 10)
```

## Un `.sq` por bounded context

Cada contexto define **sus** tablas en **su** `.sq`. Las referencias a otros contextos se guardan como `TEXT` (UUID) sin `FOREIGN KEY` ni `REFERENCES`. La integridad la sostiene el dominio, no la DB.

```sql
-- src/transactions/sqldelight/within/means/transactions/db/transactions.sq
CREATE TABLE transactions (
    id              TEXT NOT NULL PRIMARY KEY,
    family_id       TEXT NOT NULL,         -- ref. lógica a users
    account_id      TEXT NOT NULL,         -- ref. lógica a accounts
    category_id     TEXT,                  -- ref. lógica a categories (nullable)
    type            TEXT NOT NULL,
    amount_cents    INTEGER NOT NULL,
    currency        TEXT NOT NULL,
    occurred_on     TEXT NOT NULL,         -- ISO-8601 (kotlinx.datetime.LocalDate)
    description     TEXT NOT NULL,
    created_at      TEXT NOT NULL          -- ISO-8601 (kotlinx.datetime.Instant)
);

CREATE INDEX idx_tx_family_date ON transactions(family_id, occurred_on DESC);
CREATE INDEX idx_tx_account     ON transactions(account_id);
CREATE INDEX idx_tx_category    ON transactions(category_id);

save:
INSERT OR REPLACE INTO transactions VALUES ?;

findById:
SELECT * FROM transactions WHERE id = ?;

deleteById:
DELETE FROM transactions WHERE id = ?;

findRecentByFamily:
SELECT * FROM transactions
WHERE family_id = ?
ORDER BY occurred_on DESC, created_at DESC
LIMIT ?;
```

**Consecuencia importante:** un cambio de esquema en `accounts` no obliga a una migración acoplada en `transactions`. Cada contexto evoluciona su `.sq` por separado.

## Una DB por contexto

SQLDelight **no fusiona schemas** declarados en módulos Gradle separados. La solución idiomática es: **cada módulo declara su propia clase DB** apuntando al **mismo archivo físico SQLite**.

```
:shared        -> SharedDatabase        (Event Store + tablas comunes)
:users         -> UsersDatabase         (tablas de users)
:categories    -> CategoriesDatabase    (tablas de categories)
:transactions  -> TransactionsDatabase  (tablas de transactions)
:analytics     -> AnalyticsDatabase     (read models de analytics)
```

Todas las clases DB se construyen con un driver que apunta al mismo archivo `within_means.db`. SQLite acepta múltiples handles sobre el mismo archivo; las transacciones individuales se aíslan a nivel de DB engine.

```kotlin
// apps/android/.../PersistenceModule.kt (conceptual)
val factory = AndroidSqliteDriverFactory(context, passphrase)
val sharedDriver       = factory.create("within_means.db", SharedDatabase.Schema)
val usersDriver        = factory.create("within_means.db", UsersDatabase.Schema)
val categoriesDriver   = factory.create("within_means.db", CategoriesDatabase.Schema)
val transactionsDriver = factory.create("within_means.db", TransactionsDatabase.Schema)
val analyticsDriver    = factory.create("within_means.db", AnalyticsDatabase.Schema)

val sharedDb       = SharedDatabase(sharedDriver)
val usersDb        = UsersDatabase(usersDriver)
val categoriesDb   = CategoriesDatabase(categoriesDriver)
val transactionsDb = TransactionsDatabase(transactionsDriver)
val analyticsDb    = AnalyticsDatabase(analyticsDriver)
```

Cada repositorio recibe **únicamente la clase DB de su contexto** por inyección de Koin:

```kotlin
class SqlDelightTransactionRepository(
    private val db: TransactionsDatabase,
    private val ioDispatcher: CoroutineDispatcher,
) : TransactionRepository { ... }
```

### Implicaciones para DDD estricto

Esta decisión refuerza la regla "sin imports cross-context": **es físicamente imposible** que un repositorio de `transactions` consulte tablas de `accounts` directamente, porque su clase DB no las conoce. La comunicación entre contextos pasa por buses (`EventBus` para escritura derivada, `QueryBus` para lectura), como manda el DDD.

### Migraciones por contexto

Cada contexto versiona sus propias migraciones en `src/<ctx>/sqldelight/.../migrations/`. La migración de un contexto no afecta a otro. SQLDelight ejecuta las migraciones pendientes de cada DB de forma independiente al abrir el driver.

## Configuración Gradle por contexto

```kotlin
sqldelight {
    databases {
        create("<Context>Database") {           // p. ej. UsersDatabase
            packageName.set("within.means.<context>.db")
            srcDirs.setFrom("src/commonMain/sqldelight")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            verifyMigrations.set(true)
        }
    }
}
```

## Cifrado: SQLCipher

### Dónde vive el cifrado

- **`apps/android`** — selección del driver con `SupportFactory` de SQLCipher.
- **`apps/android`** — gestión de la passphrase (Keystore + PIN/biometría).
- **Nada más.** El dominio, la aplicación y los repositorios SQLDelight **no saben** que la DB está cifrada.

### Cableado del driver Android

```kotlin
// apps/android/src/main/kotlin/within/means/android/persistence/DatabaseFactory.kt
class AndroidDatabaseFactory(
    private val context: Context,
    private val passphraseProvider: PassphraseProvider,
) {
    fun create(): SqlDriver {
        System.loadLibrary("sqlcipher")
        val passphrase: ByteArray = passphraseProvider.get()
        val factory = SupportFactory(passphrase)
        return AndroidSqliteDriver(
            schema = TransactionsDatabase.Schema,         // o la DB del contexto que corresponda
            context = context,
            name = "within_means.db",
            factory = factory,
            callback = object : AndroidSqliteDriver.Callback(TransactionsDatabase.Schema) {
                override fun onConfigure(db: SupportSQLiteDatabase) {
                    db.setForeignKeyConstraintsEnabled(false) // no usamos FKs cross-context
                }
            },
        )
    }
}
```

### Passphrase: derivación desde Android Keystore

Patrón resumido (detalles en la fase de implementación):

1. La primera vez que se abre la app: generar una clave maestra simétrica AES-256 en **Android Keystore** (no exportable, ligada al dispositivo).
2. El usuario configura un PIN (4-6 dígitos) o habilita biometría.
3. Derivar la passphrase de SQLCipher como `HMAC-SHA256(keystoreKey, pin_or_biometric_token)`.
4. La passphrase se mantiene en memoria solo mientras la app está activa; al pasar a background se vacía.

```kotlin
// apps/android/src/main/kotlin/within/means/android/persistence/PassphraseProvider.kt
class PassphraseProvider(
    private val keystoreManager: KeystoreManager,
    private val userUnlock: UserUnlock,           // PIN + biometric
) {
    fun get(): ByteArray = keystoreManager
        .deriveKey(userUnlock.requestToken())     // suspending en práctica
        .also { /* zero out after use */ }
}
```

Para datos no sensibles del cliente (tema, idioma, último filtro) usamos **Multiplatform Settings** sin cifrar — no merece la pena la complejidad.

### Tests sin cifrado

Los tests usan `JdbcSqliteDriver.IN_MEMORY` puro. El cifrado se valida con tests específicos en `apps/android` (instrumentation).

```kotlin
fun testTransactionsDb(): TransactionsDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TransactionsDatabase.Schema.create(driver)
    return TransactionsDatabase(driver)
}
// Helper análogo por cada contexto: testSharedDb(), testUsersDb(), etc.
```

## UUID v4 en cliente

### Interfaz en el dominio compartido

```kotlin
// src/shared/.../domain/UuidGenerator.kt
interface UuidGenerator {
    fun next(): String
}
```

### Implementación en `:shared` infraestructura

Usa **`com.benasher44:uuid`** (KMP, sin dependencias de plataforma).

```kotlin
// src/shared/.../infrastructure/RealUuidGenerator.kt
class RealUuidGenerator : UuidGenerator {
    override fun next(): String = uuid4().toString()
}
```

### Uso desde aplicación

El handler (no el agregado) pide un id al generador y lo pasa al agregado:

```kotlin
class TransactionRegistrar(
    private val repository: TransactionRepository,
    private val uuids: UuidGenerator,
    private val eventBus: EventBus,
) {
    suspend fun register(command: RegisterTransactionCommand) {
        val id = TransactionId(uuids.next())
        val tx = Transaction.register(
            id = id,
            accountId = AccountId(command.accountId),
            /* ... */
        )
        repository.save(tx)
        eventBus.publish(tx.pullDomainEvents())
    }
}
```

### Tests determinísticos

```kotlin
class FixedUuidGenerator(private val ids: Iterator<String>) : UuidGenerator {
    override fun next(): String = ids.next()
}
```

### Validación

`Identifier` en `:shared` valida el formato UUID en `init`:

```kotlin
abstract class Identifier(open val value: String) {
    init {
        require(UUID_REGEX.matches(value)) { "Invalid UUID: $value" }
    }
    companion object {
        private val UUID_REGEX =
            Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    }
}
```

## Mapping fila ↔ agregado

Único punto donde se cruzan los tipos generados por SQLDelight (`TransactionsRow`) y el agregado (`Transaction`).

```kotlin
// transactions/infrastructure/persistence/TransactionRowMapper.kt
internal fun TransactionsRow.toAggregate(): Transaction = Transaction.rehydrate(
    id = TransactionId(id),
    familyId = FamilyId(family_id),
    accountId = AccountId(account_id),
    categoryId = category_id?.let { CategoryId(it) },
    type = TransactionType.valueOf(type),
    amount = Money(amount_cents, Currency.valueOf(currency)),
    occurredOn = TransactionDate(LocalDate.parse(occurred_on)),
    description = TransactionDescription(description),
)

internal fun Transaction.toRow(): TransactionsRow = TransactionsRow(
    id = id.value,
    family_id = familyId.value,
    account_id = accountId.value,
    category_id = categoryId?.value,
    type = type.name,
    amount_cents = amount.cents,
    currency = amount.currency.code,
    occurred_on = occurredOn.value.toString(),
    description = description.value,
    created_at = createdAt.toString(),
)
```

### Dos factorías en el agregado

- `Transaction.register(...)` — caso "nuevo": valida invariantes y **emite** `TransactionRegistered`.
- `Transaction.rehydrate(...)` — caso "viene de la DB": construye sin emitir eventos.

Es el mismo patrón que el esqueleto Java (`Course.create` vs reconstrucción desde Hibernate). Sin esta separación, cada lectura de DB emitiría falsos eventos.

## El repositorio implementa la interfaz del dominio

```kotlin
class SqlDelightTransactionRepository(
    private val db: TransactionsDatabase,
    private val ioDispatcher: CoroutineDispatcher,
) : TransactionRepository {

    override suspend fun save(transaction: Transaction): Unit = withContext(ioDispatcher) {
        db.transactionsQueries.save(transaction.toRow())
    }

    override suspend fun search(id: TransactionId): Transaction? = withContext(ioDispatcher) {
        db.transactionsQueries.findById(id.value).executeAsOneOrNull()?.toAggregate()
    }

    override suspend fun matching(criteria: Criteria): List<Transaction> = withContext(ioDispatcher) {
        val (sql, args) = SqlCriteriaTranslator.translate(criteria, table = "transactions")
        db.transactionsQueries
            .executeRaw(sql, args, ::TransactionsRow)
            .map(TransactionsRow::toAggregate)
    }

    override fun observeRecentByFamily(familyId: FamilyId, limit: Long): Flow<List<Transaction>> =
        db.transactionsQueries.findRecentByFamily(familyId.value, limit)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toAggregate() } }
}
```

`executeRaw` es un helper en `:shared/infrastructure/persistence` que ejecuta SQL parametrizado sobre el `SqlDriver`. Lo usa **solo** la implementación con `Criteria`; las queries fijas usan los métodos generados por SQLDelight.

## Patrón `Criteria` traducido a SQL

`Criteria` (filtros + orden + paginación) vive en `:shared/domain`. La traducción a SQL parametrizado vive en `:shared/infrastructure`:

```kotlin
// shared/infrastructure/persistence/SqlCriteriaTranslator.kt
object SqlCriteriaTranslator {
    fun translate(criteria: Criteria, table: String): Pair<String, List<Any?>> {
        // Construye "SELECT * FROM <table> WHERE ... ORDER BY ... LIMIT ... OFFSET ..."
        // con args parametrizados (?). Sin string concat de valores -> a salvo de SQLi.
    }
}
```

Esto replica el `CriteriaToSqlConverter` del esqueleto Java. Los nombres de columna permitidos por contexto se configuran con un `FilterFieldMapper` por agregado para no exponer SQL crudo a la UI.

## Event Store

Tabla en `:shared` que registra **todos** los eventos de dominio publicados, sirviendo de fuente de verdad para reconstruir cualquier proyección, auditar y, en el futuro, sincronizar.

### Esquema

```sql
-- src/shared/sqldelight/within/means/shared/db/domain_events.sq
CREATE TABLE domain_events (
    event_id        TEXT NOT NULL PRIMARY KEY,        -- UUID v4
    event_name      TEXT NOT NULL,                    -- "transactions.registered"
    aggregate_id    TEXT NOT NULL,
    aggregate_type  TEXT NOT NULL,                    -- "Transaction", "Category", ...
    occurred_on     TEXT NOT NULL,                    -- ISO-8601 Instant
    payload_json    TEXT NOT NULL,
    schema_version  INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX idx_events_aggregate ON domain_events(aggregate_id, occurred_on);
CREATE INDEX idx_events_name      ON domain_events(event_name, occurred_on);
CREATE INDEX idx_events_time      ON domain_events(occurred_on);

append:
INSERT INTO domain_events VALUES ?;

findByAggregate:
SELECT * FROM domain_events WHERE aggregate_id = ? ORDER BY occurred_on;

findByName:
SELECT * FROM domain_events WHERE event_name = ? ORDER BY occurred_on;

findAllSince:
SELECT * FROM domain_events WHERE occurred_on >= ? ORDER BY occurred_on;
```

### Cómo se publica un evento (flujo completo)

```kotlin
// shared/infrastructure/bus/event/EventStoreBackedEventBus.kt
class EventStoreBackedEventBus(
    private val store: DomainEventStore,
    private val subscribers: List<DomainEventSubscriber<*>>,
    private val serializer: DomainEventJsonSerializer,
) : EventBus {

    override suspend fun publish(events: List<DomainEvent>) {
        store.append(events.map { serializer.toRecord(it) })   // persistencia primero
        events.forEach { event ->
            subscribers
                .filter { it.subscribesTo(event::class) }
                .forEach { it.consume(event) }
        }
    }
}
```

**Orden importa:** primero se persiste, luego se notifica. Si un subscriber falla, los demás siguen ejecutando, y el evento ya está en el store para reintento.

### Serialización

Cada `DomainEvent` implementa `toPrimitives()`/`fromPrimitives()` (patrón del esqueleto Java) o, alternativamente, se usa `kotlinx.serialization` con `@Serializable`. Decisión: **`kotlinx.serialization`** por idiomático en Kotlin.

```kotlin
@Serializable
data class TransactionRegistered(
    override val eventId: String,
    override val aggregateId: String,
    override val occurredOn: Instant,
    val accountId: String? = null,           // opcional en MVP
    val categoryId: String,
    val amountCents: Long,
    val currency: String,
    val type: String,
    val date: String,
    val description: String,
    val incomeSource: String? = null,
    val originRef: String? = null,
    val recurringRef: String? = null,
) : DomainEvent {
    override val eventName: String = "transactions.registered"
}
```

### Reconstrucción de proyecciones

Comando administrativo `RebuildProjectionsCommand` que:

1. Borra las tablas materializadas (read models) del contexto destino.
2. Lee eventos del store desde el inicio.
3. Re-publica cada evento al subscriber correspondiente.
4. Las proyecciones se reconstruyen.

Operación cara pero rara; útil al introducir un read model nuevo retroactivo.

### Versionado de eventos

Si un evento cambia de forma incompatible:

- Se mantiene el evento original con `schema_version = 1`.
- Se crea `<EventName>V2` con `schema_version = 2`.
- El deserializador inspecciona `schema_version` y migra v1 → v2 al cargar.
- Nunca se reescriben eventos del pasado.

## Snapshots periódicos (`analytics`)

Patrón complementario al Event Store: para KPIs históricos caros (CAGR, varianza, runway, dashboard) se materializan **snapshots** mensuales inmutables.

### Esquema (ejemplo simplificado, post-MVP)

```sql
-- src/analytics/sqldelight/within/means/analytics/db/monthly_close.sq
CREATE TABLE monthly_close (
    period              TEXT NOT NULL PRIMARY KEY,    -- "2026-05"
    total_income_cents  INTEGER NOT NULL,
    total_expenses_cents INTEGER NOT NULL,
    net_saving_cents    INTEGER NOT NULL,
    fixed_expenses_cents INTEGER NOT NULL,
    variable_expenses_cents INTEGER NOT NULL,
    essential_expenses_cents INTEGER NOT NULL,
    discretionary_expenses_cents INTEGER NOT NULL,
    burn_rate_3m_cents  INTEGER,
    snapshot_json       TEXT NOT NULL,                -- detalle por categoría
    closed_at           TEXT NOT NULL
);
```

### Generación

- **Trigger:** comando `CloseMonthCommand(period)` ejecutado:
  - Manualmente desde la UI ("cerrar mes").
  - Automáticamente al abrir la app si hay meses pasados sin cerrar.
- **Naturaleza:** un snapshot **no se modifica** una vez cerrado, salvo recálculo explícito vía `RecomputeMonthCommand`.

### Por qué snapshots y no recalcular siempre

- Las queries de KPIs históricos (12-24 meses) se vuelven `O(1)` lectura de N filas.
- CAGR, variabilidad, tendencias mensuales: trivial sobre snapshots.
- Las proyecciones (`forecasts`) leen snapshots; no reagregan el ledger completo.

### Coexistencia con el Event Store

| Origen | Para qué |
|---|---|
| Event Store (eventos crudos) | Reconstruir proyecciones, auditoría, sync futura. |
| Snapshots mensuales | Lecturas rápidas de KPIs históricos, dashboard. |

Si un snapshot se corrompe, se recalcula desde el Event Store. El Event Store es la fuente de verdad; los snapshots son cache.

## Migraciones

- Archivos `.sqm` numerados consecutivamente dentro de `migrations/`.
- SQLDelight ejecuta automáticamente las migraciones pendientes al abrir la DB.
- `verifyMigrations = true` en Gradle: el build falla si una migración deja el esquema inconsistente con el `.sq` final.
- **Política:** nunca editar una `.sqm` ya publicada. Si se descubre un fallo, escribir una nueva migración correctiva.

## Tests

### Repositorio in-memory (rapidísimo, sin DB)

```kotlin
class InMemoryTransactionRepository : TransactionRepository {
    private val data = mutableMapOf<TransactionId, Transaction>()
    override suspend fun save(t: Transaction) { data[t.id] = t }
    override suspend fun search(id: TransactionId) = data[id]
    override suspend fun matching(c: Criteria) = data.values.filter { /* aplicar c */ }.toList()
}
```

Lo usan los handlers en sus unit tests.

### Repositorio SQLDelight con DB en memoria

```kotlin
class SqlDelightTransactionRepositoryShould {
    private val db = testDatabase()
    private val repo = SqlDelightTransactionRepository(db, Dispatchers.Unconfined)

    @Test
    fun `should save and retrieve a transaction`() = runTest {
        val tx = TransactionMother.random()
        repo.save(tx)
        repo.search(tx.id) shouldBe tx
    }
}
```

### Tests instrumentados (Android, con SQLCipher real)

Solo para validar el cifrado: instalan la app en emulador, crean DB cifrada, verifican que el archivo en disco no es legible sin passphrase. Se ejecutan en CI con `connectedAndroidTest`.

## Resumen — qué va dónde

| Decisión | Capa | Archivo |
|---|---|---|
| Esquema `.sq` por contexto | `infrastructure` (recursos) | `src/<ctx>/sqldelight/within/means/<ctx>/db/<ctx>.sq` |
| Migraciones | `infrastructure` | `src/<ctx>/sqldelight/.../migrations/N.sqm` |
| Driver SQLCipher (Android) | `apps/android` | `DatabaseFactory.kt` |
| Passphrase + Keystore | `apps/android` | `PassphraseProvider.kt`, `KeystoreManager.kt` |
| `UuidGenerator` interfaz | `shared/domain` | `UuidGenerator.kt` |
| `UuidGenerator` impl | `shared/infrastructure` | `RealUuidGenerator.kt` |
| `Identifier` (valida UUID) | `shared/domain` | `Identifier.kt` |
| Repo SQLDelight | `infrastructure` del contexto | `SqlDelight<X>Repository.kt` |
| Row ↔ Aggregate mapper | `infrastructure` del contexto | `<X>RowMapper.kt` |
| Repo InMemory (tests) | `infrastructure` del contexto | `InMemory<X>Repository.kt` |
| `Criteria` → SQL | `shared/infrastructure` | `SqlCriteriaTranslator.kt` |
| Event Store (tabla) | `shared` recursos | `src/shared/sqldelight/.../domain_events.sq` |
| Event Store (impl) | `shared/infrastructure` | `SqlDelightDomainEventStore.kt` |
| EventBus persistente | `shared/infrastructure` | `EventStoreBackedEventBus.kt` |
| Serializador de eventos | `shared/infrastructure` | `DomainEventJsonSerializer.kt` (kotlinx.serialization) |
| Snapshots mensuales | `analytics` recursos | `src/analytics/sqldelight/.../monthly_close.sq` (post-MVP) |
| Multiplatform Settings (UI prefs) | `apps/<app>` | `Preferences.kt` |
