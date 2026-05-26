# within-means

Aplicación móvil para la gestión de **ingresos y gastos personales y familiares**, construida con **Kotlin Multiplatform + Compose Multiplatform** siguiendo estrictamente la arquitectura hexagonal con DDD y CQRS del esqueleto de referencia [java-ddd-example](https://github.com/CodelyTV/java-ddd-example).

**Plataforma inicial:** Android. Desktop JVM (y eventualmente iOS) se incorporan en fases posteriores aprovechando el código compartido en `commonMain`.

## Visión

Una herramienta sencilla y rápida para registrar movimientos, organizarlos por categorías, controlar presupuestos y obtener una visión clara del gasto compartido en una unidad familiar — sin depender de servicios en la nube en su versión inicial.

## Estado actual

Fase de diseño y documentación. Sin código aún. Ver [doc/roadmap.md](doc/roadmap.md).

## Stack

| Capa | Tecnología |
|---|---|
| Lenguaje | Kotlin 2.x Multiplatform |
| Build | Gradle Kotlin DSL + version catalog |
| UI | Compose Multiplatform (target inicial: Android) |
| DI | Koin Multiplatform |
| Persistencia | SQLDelight (driver Android; JVM/SQLite cuando se añada Desktop) |
| Serialización | kotlinx.serialization |
| Fechas | kotlinx-datetime |
| Logs | kermit |
| Tests | Kotest + MockK + Turbine |

## Plataformas objetivo

- **Fase inicial:** Android (Compose Multiplatform).
- **Posterior:** Desktop JVM (Compose Desktop).
- **Fuera del alcance:** iOS, Web y backend Ktor de sincronización (futuras consideraciones).

## Arquitectura

Hexagonal con DDD y CQRS estricto. Cada contexto acotado se organiza en tres capas:

- `domain/` — Agregados, value objects, eventos de dominio, interfaces de repositorio.
- `application/` — Comandos, consultas, handlers y servicios de aplicación.
- `infrastructure/` — Implementaciones de repositorios, adaptadores, persistencia.

Los buses (`CommandBus`, `QueryBus`, `EventBus`) viven en el kernel compartido y son el único punto de acoplamiento entre la capa `apps/` y los contextos.

Detalles: [doc/architecture.md](doc/architecture.md).

## Contextos acotados

| Contexto | Responsabilidad |
|---|---|
| [`users`](doc/bounded-contexts.md#users) | Usuarios y familias. |
| [`accounts`](doc/bounded-contexts.md#accounts) | Cuentas (banco, efectivo, tarjeta) y saldos. |
| [`transactions`](doc/bounded-contexts.md#transactions) | Movimientos: ingresos, gastos, transferencias. |
| [`categories`](doc/bounded-contexts.md#categories) | Categorías y subcategorías. |
| [`budgets`](doc/bounded-contexts.md#budgets) | Presupuestos mensuales y alertas. |
| [`analytics`](doc/bounded-contexts.md#analytics) | Informes y agregados (read models). |
| `shared` | Kernel: clases base, buses, criteria, eventos. |

## Documentación

- [doc/architecture.md](doc/architecture.md) — Arquitectura hexagonal, DDD, CQRS y mapeo desde el esqueleto Java.
- [doc/bounded-contexts.md](doc/bounded-contexts.md) — Detalle de cada contexto acotado.
- [doc/module-structure.md](doc/module-structure.md) — Estructura física de módulos Gradle.
- [doc/conventions.md](doc/conventions.md) — Convenciones de código, naming y testing.
- [doc/roadmap.md](doc/roadmap.md) — Fases de trabajo.

## Cómo empezar (futuro)

> Pendiente. Se completará al cerrar la Fase 1 (bootstrap del repositorio).

## Referencia

Este proyecto replica fielmente la arquitectura de [CodelyTV/java-ddd-example](https://github.com/CodelyTV/java-ddd-example), traduciendo:

- Spring Boot a Ktor/Compose + Koin.
- Hibernate/MySQL a SQLDelight/SQLite.
- Reflection-based bus a registro explícito de handlers (limitación de KMP commonMain).
