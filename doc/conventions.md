# Convenciones

Convenciones obligatorias del proyecto. Replican fielmente las del esqueleto Java, adaptadas a Kotlin.

## Estructura por capa

| Capa | Contenido | Importaciones permitidas |
|---|---|---|
| `domain` | Agregados, value objects, eventos, interfaces de repositorio, excepciones de dominio. | `kotlin.*`, `kotlinx-datetime`, `:shared` domain. |
| `application` | Comandos, queries, handlers, application services, responses. | `:shared` domain/bus, propio `domain/`. |
| `infrastructure` | Implementaciones de repositorios, adaptadores, serializadores. | Cualquiera; es el ÚNICO sitio que toca librerías externas (SQLDelight, drivers, etc.). |

**Regla dura:** prohibido importar desde `infrastructure` o `application` hacia `domain`. Lint check obligatorio (Konsist o Detekt en futuro).

## Naming

### Clases

| Tipo | Patrón | Ejemplo |
|---|---|---|
| Agregado raíz | `<Sustantivo>` | `Transaction`, `Account` |
| Identifier | `<Aggregate>Id` | `TransactionId` |
| Value object | `<Concepto>` | `Money`, `Email`, `Amount` |
| Evento de dominio | `<Aggregate><VerboParticipio>` | `TransactionRegistered`, `BudgetExceeded` |
| Excepción de dominio | `<Aggregate><Condición>` | `TransactionNotExist`, `AccountAlreadyArchived` |
| Repositorio (interfaz) | `<Aggregate>Repository` | `TransactionRepository` |
| Implementación repo | `<Tech><Aggregate>Repository` | `SqlDelightTransactionRepository`, `InMemoryTransactionRepository` |
| Command | `<Verbo><Aggregate>Command` | `RegisterTransactionCommand` |
| Query | `<Verbo><Aggregate>Query` | `FindTransactionQuery`, `SearchTransactionsByCriteriaQuery` |
| Command handler | `<Verbo><Aggregate>CommandHandler` | `RegisterTransactionCommandHandler` |
| Query handler | `<Verbo><Aggregate>QueryHandler` | `FindTransactionQueryHandler` |
| Application service | `<Aggregate><Verbo-er>` | `TransactionCreator`, `TransactionFinder`, `TransactionsSearcher` |
| Response (DTO) | `<Aggregate>Response`, `<Aggregate>sResponse` | `TransactionResponse`, `TransactionsResponse` |
| Subscriber | `<Aggregate>On<Event>` | `UpdateBudgetOnTransactionRegistered` |

### Paquetes

`within.means.<context>.<layer>[.<feature>]` — siempre kebab-case interno no se usa en Kotlin; preferir snake_case en nombres de features cuando agrupan casos de uso:

- `within.means.transactions.application.register`
- `within.means.transactions.application.search_by_criteria`

Espeja el esqueleto Java (`search_last`, `find`, `create`).

### Tests

- Archivo: `<Class>Should.kt`. Ej: `RegisterTransactionCommandHandlerShould.kt`.
- Método: `` `should <comportamiento esperado>`() ``. Ej: `` `should reject negative amount`() ``.
- Object mothers: `<Aggregate>Mother`, con `random()`, `withAmount(...)`, etc. Replican `CourseMother`, `UuidMother`.

## Estilo Kotlin

### Inmutabilidad

- Todos los value objects son `data class` con propiedades `val`.
- Los agregados son clases normales con estado mutable encapsulado y métodos que devuelven el agregado (no copias).
- Los DTOs (`Response`) son `data class`.
- Las colecciones expuestas desde el dominio son `List` / `Map` (inmutables), nunca `MutableList`.

### Constructores y factorías

Agregados: constructor primario `internal` o `private`, factoría pública `create`:

```kotlin
class Transaction private constructor(
    val id: TransactionId,
    val accountId: AccountId,
    val amount: Money,
    val type: TransactionType,
    val date: TransactionDate,
    val description: TransactionDescription,
) : AggregateRoot() {

    companion object {
        fun register(
            id: TransactionId,
            accountId: AccountId,
            amount: Money,
            type: TransactionType,
            date: TransactionDate,
            description: TransactionDescription,
        ): Transaction {
            require(amount.amount > 0) { "Amount must be positive" }
            return Transaction(id, accountId, amount, type, date, description).apply {
                record(TransactionRegistered(id.value, accountId.value, amount, type, date))
            }
        }
    }
}
```

### Validación en value objects

Validación en el `init`:

```kotlin
data class Email(val value: String) : StringValueObject(value) {
    init {
        require(value.contains("@")) { "Invalid email: $value" }
    }
}
```

### Marker interfaces vs sealed

- `Command`, `Query`, `Response`, `DomainEvent` son interfaces (o clases abiertas). No `sealed`, para permitir extensión desde otros módulos.

### Coroutines

- Los handlers exponen `suspend fun handle(...)` siempre que toquen IO.
- Persistencia con SQLDelight: usar `suspendingTransaction { }`.
- UI con Compose: lanzar coroutines desde `LaunchedEffect` o `rememberCoroutineScope`.

### Nulabilidad

- Evitar `?` en el dominio. Si algo puede no existir, usar `Optional<T>` propio o devolver el agregado en estado terminal.
- `search(id): Transaction?` está permitido en repositorios (espeja `Optional<Course>` del esqueleto).

### Fechas y tipos Java 8+ (compatibilidad con `minSdk 21`)

- En `commonMain`: usar **siempre** `kotlinx.datetime.LocalDate`, `Instant`, `LocalDateTime`. No importar `java.time.*` en código común.
- En `androidMain`: si se necesita `java.time.*` (p. ej. para interoperar con APIs de Android antiguas), está permitido gracias al **core library desugaring** habilitado en `apps/android`.
- Nunca usar `java.util.Date` ni `java.util.Calendar`.
- Para formateo localizado, preferir `kotlinx-datetime` + helpers propios en `:shared`, no `SimpleDateFormat`.

### Errores

- Excepciones de dominio: heredan de `DomainError` (sealed class en `:shared`).
- No lanzar `IllegalArgumentException` ni `IllegalStateException` desde el dominio: lanzar la excepción de dominio apropiada (`AmountMustBePositive`, `AccountNotFound`...).
- Capa de aplicación: traduce excepciones de infraestructura a excepciones de dominio si es necesario.

## Tests

### Estructura

```kotlin
class RegisterTransactionCommandHandlerShould {
    private val repository = mockk<TransactionRepository>(relaxed = true)
    private val eventBus = mockk<EventBus>(relaxed = true)
    private val handler = RegisterTransactionCommandHandler(TransactionCreator(repository, eventBus))

    @Test
    fun `should register a valid transaction`() = runTest {
        val command = RegisterTransactionCommandMother.random()

        handler.handle(command)

        coVerify { repository.save(any()) }
        coVerify { eventBus.publish(any()) }
    }

    @Test
    fun `should reject negative amount`() = runTest {
        val command = RegisterTransactionCommandMother.withAmount(-100)

        assertFailsWith<AmountMustBePositive> { handler.handle(command) }
    }
}
```

### Object mothers

Un mother por agregado, value object o command:

```kotlin
object TransactionMother {
    fun random(): Transaction = Transaction.register(
        id = TransactionIdMother.random(),
        accountId = AccountIdMother.random(),
        amount = MoneyMother.random(),
        type = TransactionType.EXPENSE,
        date = TransactionDateMother.today(),
        description = TransactionDescriptionMother.random(),
    )
}
```

### No mockear el dominio

Solo se mockean repositorios y buses. Los agregados y value objects se usan con datos reales (vía mothers).

## Git y commits

- Idioma de los mensajes: inglés.
- Formato: `<contexto>: <verbo> <qué>`. Ej: `transactions: add RegisterTransaction command`.
- Un commit por unidad lógica (no mezclar refactor + feature).
- PRs por contexto. No PR multi-contexto.

## Documentación en código

Por defecto, no comentar. Solo añadir comentario cuando el **por qué** no es obvio (invariante oculta, workaround específico, decisión contraintuitiva). Nunca explicar **qué hace** el código — eso lo dicen los nombres.

KDoc (`/** */`) opcional en interfaces públicas del `:shared` kernel; no obligatorio en agregados ni handlers.
