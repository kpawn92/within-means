# Arquitectura

Este documento describe la arquitectura de `within-means`, que replica estrictamente el esqueleto [CodelyTV/java-ddd-example](https://github.com/CodelyTV/java-ddd-example) adaptado a Kotlin Multiplatform.

## Principios

1. **Hexagonal (puertos y adaptadores).** El dominio no depende de ningún detalle de infraestructura. Las dependencias siempre apuntan hacia el dominio.
2. **DDD táctico estricto.** Agregados, value objects, eventos de dominio, repositorios como interfaces en el dominio.
3. **CQRS explícito con buses.** Toda la entrada al sistema pasa por `CommandBus` (escritura) o `QueryBus` (lectura). Nunca se invocan los handlers directamente desde la capa de UI.
4. **Eventos de dominio in-process.** El `EventBus` propaga eventos entre contextos sin acoplarlos.
5. **Bounded contexts independientes.** Cada contexto es un módulo Gradle. La única dependencia común es `:shared`.

## Capas por contexto

Cada contexto acotado se estructura en tres capas. La estructura espeja el esqueleto Java.

```
src/<context>/
├── domain/
│   ├── <Aggregate>.kt              -> Agregado raíz
│   ├── <Aggregate>Id.kt            -> Identificador (Value Object)
│   ├── <Aggregate>Repository.kt    -> Interfaz de repositorio
│   ├── <ValueObject>.kt            -> Value objects del contexto
│   ├── <Aggregate><Event>.kt       -> Eventos de dominio
│   └── <Aggregate>NotExist.kt      -> Excepciones de dominio
├── application/
│   ├── <use_case>/
│   │   ├── <UseCase>Command.kt     -> o Query
│   │   ├── <UseCase>CommandHandler.kt
│   │   └── <UseCase>er.kt          -> Application service (Creator, Finder, Searcher...)
│   └── <Aggregate>Response.kt      -> DTOs de respuesta
└── infrastructure/
    ├── persistence/
    │   ├── SqlDelight<Aggregate>Repository.kt
    │   └── InMemory<Aggregate>Repository.kt
    └── ...
```

### Capa `domain/`

Las únicas dependencias permitidas son las del lenguaje (`kotlin.*`, `kotlinx-datetime`) y el kernel `:shared`. Prohibido importar nada de `application/` ni `infrastructure/`.

- **Aggregate Root.** Hereda de `AggregateRoot` (kernel). Encapsula invariantes, expone factorías (`fun create(...): Aggregate`) y métodos de mutación. Registra eventos vía `record(event)`.
- **Value Objects.** Inmutables. Validan en el constructor. Igualdad estructural. Heredan de `StringValueObject`, `IntValueObject` o `ValueObject` base.
- **Identifier.** Heredan de `Identifier` (kernel), que valida UUID.
- **Repository.** Interfaz en el dominio. Métodos en lenguaje de dominio (`save`, `search`, `matching(Criteria)`). Nunca expone tipos de infraestructura.
- **Domain Events.** Heredan de `DomainEvent` (kernel). Inmutables, serializables.

### Capa `application/`

Orquesta el caso de uso. Sin lógica de negocio (que vive en el agregado).

- **Comandos y consultas.** Heredan de `Command` o `Query` (marker interfaces del kernel). Inmutables.
- **Handlers.** Implementan `CommandHandler<C>` o `QueryHandler<Q, R>`. Reciben el bus y los repositorios por constructor.
- **Application service.** El "verbo" del caso de uso: `<Aggregate>Creator`, `<Aggregate>Finder`, `<Aggregate>Searcher`. Encapsula el flujo: validar → mutar agregado → persistir → publicar eventos.
- **Responses.** DTOs planos. Implementan `Response`.

Estructura interna por caso de uso, espejo del esqueleto Java:

```
application/create/
├── CreateTransactionCommand.kt
├── CreateTransactionCommandHandler.kt
└── TransactionCreator.kt
```

### Capa `infrastructure/`

Implementaciones concretas. Es donde varía entre plataformas en KMP.

- **Persistencia.** `SqlDelight<Aggregate>Repository` implementa la interfaz del dominio. Para tests, `InMemory<Aggregate>Repository`. El detalle completo (SQLCipher, mapping fila↔agregado, `Criteria` → SQL) está en [persistence.md](persistence.md).
- **Source sets por plataforma.** La interfaz del repo en `commonMain`. La implementación con driver SQLDelight vive en `commonMain` (lógica) y los drivers concretos en `androidMain` (y `desktopMain` en la Fase 10).
- **Adaptadores externos.** Cliente HTTP, serializadores, etc.

### Capa `apps/`

Adaptador de entrada. Equivalente a los controladores Spring del esqueleto Java.

- `apps/android/` — `MainActivity`, `@Composable` screens. Plataforma de arranque.
- `apps/desktop/` — `main()`, ventana Compose Desktop. Se incorpora en la Fase 10 del roadmap.
- Las pantallas reciben los buses por DI (Koin) y disparan `dispatch(Command)` o `ask(Query)`.

## Kernel compartido (`:shared`)

Contiene únicamente lo reutilizable entre contextos. Es a la vez un contexto y la base de los demás.

```
src/shared/
├── domain/
│   ├── AggregateRoot.kt
│   ├── ValueObject.kt
│   ├── Identifier.kt
│   ├── StringValueObject.kt
│   ├── IntValueObject.kt
│   ├── DateValueObject.kt
│   ├── bus/
│   │   ├── command/{Command,CommandBus,CommandHandler}.kt
│   │   ├── query/{Query,QueryBus,QueryHandler,Response}.kt
│   │   └── event/{DomainEvent,EventBus,DomainEventSubscriber}.kt
│   └── criteria/{Criteria,Filter,Filters,Order,FilterField,FilterOperator,FilterValue,OrderType}.kt
└── infrastructure/
    ├── bus/
    │   ├── command/InMemoryCommandBus.kt
    │   ├── query/InMemoryQueryBus.kt
    │   └── event/InMemoryEventBus.kt
    ├── persistence/SqlDelightRepository.kt   (base helpers)
    └── serialization/DomainEventJsonSerializer.kt
```

## CQRS con buses

### Flujo de escritura (Command)

```
UI -> CommandBus.dispatch(cmd) -> CommandHandler.handle(cmd)
                                  -> ApplicationService
                                     -> AggregateRoot (lógica)
                                     -> Repository.save(...)
                                     -> EventBus.publish(domainEvents)
```

### Flujo de lectura (Query)

```
UI -> QueryBus.ask(query) -> QueryHandler.handle(query)
                             -> Finder/Searcher
                                -> Repository.search/matching(...)
                                -> Response (DTO)
```

### Adaptación KMP del bus en memoria

El `InMemoryCommandBus` del esqueleto Java escanea handlers por reflection (`@DomainEventSubscriber`). KMP **`commonMain` no tiene reflection**, así que el registro es explícito:

```kotlin
class InMemoryCommandBus(
    handlers: Map<KClass<out Command>, CommandHandler<*>>
) : CommandBus {
    override fun <C : Command> dispatch(command: C) {
        @Suppress("UNCHECKED_CAST")
        val handler = handlers[command::class] as? CommandHandler<C>
            ?: error("No handler registered for ${command::class}")
        handler.handle(command)
    }
}
```

El cableado vive en un módulo Koin por contexto (`UsersModule`, `TransactionsModule`...), agregado en un `BusModule` en `apps/`. Equivalente conceptual al `MoocBackendServerConfiguration` del esqueleto.

## Eventos de dominio

Los agregados registran eventos en `AggregateRoot.record(event)`. El repositorio, al persistir, los extrae con `pullDomainEvents()` y los entrega al `EventBus`, que los distribuye a los `DomainEventSubscriber` registrados.

Casos de uso típicos:

- `TransactionRegistered` -> `budgets` actualiza el consumo del presupuesto.
- `TransactionRegistered` -> `analytics` actualiza la proyección mensual.
- `BudgetExceeded` -> `users` envía notificación (futuro).

## Criteria pattern

Las consultas con filtros dinámicos usan `Criteria` (filtros + orden + paginación) en lugar de añadir métodos al repositorio por cada combinación. Replicado tal cual del esqueleto Java.

```kotlin
val criteria = Criteria(
    filters = Filters.fromValues(listOf(
        mapOf("field" to "accountId", "operator" to "=", "value" to accountId.value),
        mapOf("field" to "date", "operator" to ">=", "value" to "2026-01-01"),
    )),
    order = Order.desc("date"),
    limit = 50,
    offset = 0,
)
repository.matching(criteria)
```

## Tests

- **Unit tests** por contexto en `src/<context>/.../test/`. Mockean el repositorio y verifican el comportamiento del handler.
- **Object mothers** (`<Aggregate>Mother`) en `:shared` test source set, equivalentes a `UuidMother`, `CourseMother` del esqueleto.
- **Integration tests** en `apps/` que cablean buses y SQLDelight real.

Convención de naming: `<Class>Should.kt` (igual que el esqueleto: `CoursesCounterGetControllerShould.java`).

## Mapeo desde el esqueleto Java

| Java DDD example | within-means |
|---|---|
| Spring Boot | Compose Multiplatform + Koin |
| Hibernate + MySQL | SQLDelight + SQLite + SQLCipher (cifrado at-rest) |
| RabbitMQ event bus | InMemoryEventBus (sin distribución entre procesos) |
| `ApiController` Spring | `@Composable` screens + Koin |
| Reflection-based bus | Registro explícito de handlers |
| `@Service`, `@Repository` | Definiciones Koin |
| JUnit5 + Mockito | Kotest + MockK |
| `*Should.java` | `*Should.kt` |
| `*Mother.java` | `*Mother.kt` |
