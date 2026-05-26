# Persistencia

Este documento fija las decisiones técnicas de persistencia y cómo se cablean **sin contaminar el dominio**. Toda la persistencia vive en la capa `infrastructure/` de cada contexto (esquema, mappers, repositorios) más un pequeño cableado en `apps/` (driver, cifrado, passphrase).

## Decisiones

| Punto | Decisión |
|---|---|
| Motor | **SQLDelight** (multiplataforma, type-safe, SQL real). |
| Driver Android | `AndroidSqliteDriver` con **SQLCipher** (`net.zetetic:sqlcipher-android`). |
| Driver Desktop (Fase 10) | `JdbcSqliteDriver` con `sqlcipher-jdbc` o equivalente JVM. |
| Driver tests | `JdbcSqliteDriver` in-memory **sin cifrado** (la seguridad se testea aparte). |
| Esquema | **Un `.sq` por bounded context.** Sin foreign keys cross-context. |
| IDs | **UUID v4** generados en cliente vía `UuidGenerator`. |
| Cifrado at-rest | **SQLCipher (AES-256)** desde la Fase 4. Passphrase derivada de Android Keystore + PIN/biometría. |
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

## El esquema global de la base de datos

A pesar de tener un `.sq` por contexto, SQLDelight genera **una sola clase `WithinMeansDatabase`** que agrupa todas las queries por contexto:

```kotlin
val db: WithinMeansDatabase = ...
db.transactionsQueries.findById("uuid-...")
db.accountsQueries.searchByFamily("family-uuid-...")
db.usersQueries.findById("user-uuid-...")
```

La generación se configura una vez en cada módulo Gradle. Cada contexto declara su paquete `.sq` dentro de `within.means.<context>.db`, y todos comparten un único archivo SQLite (`within_means.db`).

## Configuración Gradle por contexto

```kotlin
sqldelight {
    databases {
        create("WithinMeansDatabase") {
            packageName.set("within.means.db")
            srcDirs.setFrom("src/commonMain/sqldelight")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            verifyMigrations.set(true)
        }
    }
}
```

Cada módulo de contexto contribuye con sus archivos `.sq` y `.sqm` al mismo nombre lógico de DB. SQLDelight los une en compilación.

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
            schema = WithinMeansDatabase.Schema,
            context = context,
            name = "within_means.db",
            factory = factory,
            callback = object : AndroidSqliteDriver.Callback(WithinMeansDatabase.Schema) {
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
fun testDatabase(): WithinMeansDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    WithinMeansDatabase.Schema.create(driver)
    return WithinMeansDatabase(driver)
}
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
    private val db: WithinMeansDatabase,
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
| Multiplatform Settings (UI prefs) | `apps/<app>` | `Preferences.kt` |
