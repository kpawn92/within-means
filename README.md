# within-means

Aplicación móvil para la gestión de **ingresos y gastos personales y familiares**, construida con **Kotlin Multiplatform + Compose Multiplatform** siguiendo estrictamente la arquitectura hexagonal con DDD y CQRS del esqueleto de referencia [java-ddd-example](https://github.com/CodelyTV/java-ddd-example).

**Plataforma inicial:** Android. Desktop JVM (y eventualmente iOS) se incorporan en fases posteriores aprovechando el código compartido en `commonMain`.

## Visión

Empezamos respondiendo dos preguntas básicas — _¿de dónde viene el dinero?_ y _¿en qué se va?_ — sobre datos registrados manualmente cada día. La arquitectura está diseñada para evolucionar hacia una suite de **análisis financiero de nivel profesional** (patrimonio neto, DTI, runway, CAGR, stress tests, dashboard con semáforos) sin reescribir lo construido. Ver [doc/scalability/strategy.md](doc/scalability/strategy.md).

## Estado actual

**Fase 1 — Bootstrap** y **Fase 2 — Kernel `:shared`** completadas:

- Fase 1: el repo compila y empaqueta un APK Android que arranca en emulador Pixel 9 (Android 14+, minSdk 21).
- Fase 2: kernel DDD/CQRS completo en `:shared` — `AggregateRoot`, value objects, `Identifier` con validación UUID, `Money` (USD/EUR/CUP), buses `Command`/`Query`/`Event` con in-memory impls, **Event Store** sobre SQLDelight (`DomainEvents.sq`), `EventStoreBackedEventBus` con persist-then-dispatch, `Criteria` → SQL parametrizado con whitelist. ~42 casos de test, todos verdes en `jvmTest` y `androidUnitTest`.

**Próximo:** Fase 3 — contexto `users` + pantalla de Onboarding (PIN + idioma + moneda) en Android. Detalle en [doc/roadmap/mvp.md](doc/roadmap/mvp.md).

## Stack

| Capa | Tecnología |
|---|---|
| Lenguaje | Kotlin 2.x Multiplatform |
| Build | Gradle Kotlin DSL + version catalog |
| UI | Compose Multiplatform (target inicial: Android) |
| DI | Koin Multiplatform |
| Persistencia | SQLDelight + SQLCipher (AES-256, driver Android; JVM/SQLite cuando se añada Desktop) |
| Event Store | Tabla `domain_events` en `:shared` (SQLDelight) |
| IDs | UUID v4 generados en cliente (`com.benasher44:uuid`) |
| Preferencias UI | Multiplatform Settings (no para datos de dominio) |
| Serialización | kotlinx.serialization |
| Fechas | kotlinx-datetime |
| Logs | kermit |
| Tests | Kotest + MockK + Turbine |

## Plataformas objetivo

- **Fase inicial:** Android (Compose Multiplatform).
  - `minSdk = 21` (Android 5.0 Lollipop) — cobertura ~99% de dispositivos activos.
  - `compileSdk = 35`, `targetSdk = 35`.
  - **Core library desugaring** habilitado para usar `java.time` y APIs Java 8+ en dispositivos pre-API 26.
- **Post-MVP:** Desktop JVM (Compose Desktop), iOS, sincronización familiar vía backend Ktor.

## Arquitectura

Hexagonal con DDD y CQRS estricto. Cada contexto acotado se organiza en tres capas:

- `domain/` — Agregados, value objects, eventos de dominio, interfaces de repositorio.
- `application/` — Comandos, consultas, handlers y servicios de aplicación.
- `infrastructure/` — Implementaciones de repositorios, adaptadores, persistencia.

Los buses (`CommandBus`, `QueryBus`, `EventBus`) viven en el kernel compartido. El `EventBus` persiste todos los eventos en un Event Store, lo que permite reconstruir cualquier proyección desde cero y soportar KPIs históricos sin migraciones.

Detalle completo: [doc/architecture/overview.md](doc/architecture/overview.md).

## Contextos del MVP

| Contexto | Responsabilidad |
|---|---|
| `users` | Perfil único local (single-user implícito; familia llega post-MVP). |
| `categories` | Taxonomía enriquecida (kind, nature, essentiality, productive, engelGroup). |
| `transactions` | Movimientos diarios — núcleo del dominio. |
| `analytics` | Read models: resumen mensual, ingresos/gastos por categoría, evolución. |
| `shared` | Kernel: clases base, buses, criteria, **Event Store**. |

Detalle: [doc/contexts/mvp.md](doc/contexts/mvp.md). Contextos post-MVP (`recurring`, `accounts`, `budgets`, `assets`, `liabilities`, `forecasts`, `goals`, `dashboard`): [doc/contexts/future.md](doc/contexts/future.md).

## Documentación

Estructura organizada por temas en [doc/](doc/). Índice maestro en [doc/README.md](doc/README.md).

```
doc/
├── README.md                       Índice maestro
├── architecture/
│   ├── overview.md                 Hexagonal + DDD + CQRS + Event Store
│   ├── conventions.md              Naming, capas, estilo, tests
│   └── module-structure.md         Gradle, version catalog, KMP targets
├── contexts/
│   ├── README.md                   Mapa MVP vs post-MVP, eventos
│   ├── mvp.md                      Contextos del MVP
│   └── future.md                   Contextos post-MVP
├── persistence/
│   └── overview.md                 SQLDelight, SQLCipher, mapping, Event Store, snapshots
├── scalability/
│   ├── strategy.md                 Cómo escala sin reescribirse
│   └── kpi-catalog.md              ~30 KPIs profesionales mapeados a contextos
└── roadmap/
    ├── mvp.md                      Fases 1-7 (bootstrap → Android funcional)
    └── post-mvp.md                 Fases MVP+1 a MVP+5 y transversales
```

## Cómo empezar (futuro)

> Pendiente. Se completará al cerrar la Fase 1 (bootstrap del repositorio) — ver [doc/roadmap/mvp.md](doc/roadmap/mvp.md).

## Referencia

Este proyecto replica fielmente la arquitectura de [CodelyTV/java-ddd-example](https://github.com/CodelyTV/java-ddd-example), traduciendo:

- Spring Boot → Compose Multiplatform + Koin.
- Hibernate/MySQL → SQLDelight/SQLite + SQLCipher (cifrado at-rest).
- Bus reflection-based → registro explícito de handlers (limitación de KMP commonMain).
- `MySqlDomainEventsConsumer` → Event Store en SQLDelight desde el día 1.
