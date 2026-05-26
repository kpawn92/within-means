# Estrategia de escalabilidad

Cómo está orquestada la arquitectura para crecer desde el MVP "registrar ingresos y gastos" hasta una **suite de análisis financiero profesional** (ver [`kpi-catalog.md`](kpi-catalog.md)) sin reescribir lo construido.

## Principio rector

Cada nuevo KPI o capacidad debe poder añadirse como un **incremento aditivo**: módulos nuevos, suscriptores nuevos, queries nuevas. **Nunca** modificar los contextos ya estables salvo casos previstos y migrables.

Esto se sostiene en cuatro patrones tomados del esqueleto Java DDD y reforzados:

1. **Bounded contexts aislados.** Cada contexto = módulo Gradle. Sin imports cruzados.
2. **Event-driven.** Los contextos publican eventos de dominio; los demás se suscriben.
3. **CQRS estricto.** Lecturas vía `QueryBus` (proyecciones materializadas). Escrituras vía `CommandBus`.
4. **Event Store persistente.** Todos los eventos quedan en `domain_events`. Cualquier proyección se puede reconstruir desde cero.

## Cómo se añade un KPI nuevo (ejemplo: Coeficiente de Engel)

Una sola unidad de cambio, cinco archivos, **cero modificaciones** en contextos existentes:

```
analytics/
├── domain/projection/
│   └── EngelCoefficient.kt                       # value object con el ratio + meta
├── sqldelight/.../engel_snapshots.sq             # tabla materializada
├── application/find_engel/
│   ├── FindEngelCoefficientQuery.kt
│   ├── FindEngelCoefficientQueryHandler.kt
│   └── EngelCoefficientResponse.kt
└── infrastructure/projection/
    └── UpdateEngelOnTransactionRegistered.kt     # subscriber a TransactionRegistered
```

La UI lo consume vía `queryBus.ask(FindEngelCoefficientQuery(period))`. No se tocó nada de `transactions`, `categories` ni `users`.

## Cómo se añade un contexto nuevo (ejemplo: `assets`)

1. `include(":assets")` en `settings.gradle.kts`.
2. Carpeta `src/assets/` con `domain/`, `application/`, `infrastructure/`, `sqldelight/`.
3. Agregado `Asset` con sus VOs, repos, eventos.
4. Suscriptores opcionales a eventos de otros contextos (p. ej. `TransactionRegistered` para detectar movimientos a cuentas de inversión).
5. `apps/android` cablea el nuevo módulo Koin.

Tiempo estimado para un contexto medio: 1-3 días de trabajo. Sin tocar nada existente.

## Decisiones tempranas que evitan migraciones dolorosas

Estas decisiones se toman en el MVP precisamente para que la lista anterior funcione. Si no se toman ahora, cuando lleguen los KPIs profesionales habrá que añadirlas con migración de datos.

### 1. Categorías con clasificadores semánticos

`Category` lleva desde el día 1:

| Campo | Tipo | KPIs que habilita |
|---|---|---|
| `kind` | EXPENSE / INCOME / TRANSFER | Cualquier descomposición por sentido del flujo. |
| `nature` | FIXED / VARIABLE | Ratio gastos fijos/ingresos, burn rate por componente. |
| `essentiality` | ESSENTIAL / DISCRETIONARY | Capacidad de maniobra ante caídas de ingreso. |
| `productive` | Boolean | ROAS personal (educación, salud, herramientas de trabajo). |
| `engelGroup` | FOOD/HOUSING/TRANSPORT/HEALTH/EDUCATION/LEISURE/OTHER | Coeficiente de Engel y similares. |

Reclasificar una categoría a posteriori es trivial (un command). Migrar miles de transacciones para añadir un campo que falta en el dominio es caro.

### 2. Transacciones con metadatos de origen y recurrencia

`Transaction` lleva desde el día 1:

| Campo | Cuándo se usa |
|---|---|
| `incomeSource` (ACTIVE / PASSIVE) | Ratio de independencia financiera. |
| `originRef` (id opaco) | Concentración de ingresos (Herfindahl), análisis por empleador/cliente. |
| `recurringRef` (RecurringId, nullable) | Reservado para el contexto `recurring` (MVP+1). |

Estos campos se admiten como **opcionales** en el MVP y se llenan progresivamente. No bloquean la UI inicial.

### 3. Event Store desde el inicio (`:shared`)

Tabla `domain_events` con todos los eventos publicados. Beneficios:

- **Reconstruir cualquier proyección desde cero** cuando se añada un KPI nuevo retroactivo.
- **Auditoría** completa.
- **Stress tests "what-if"** reproduciendo eventos con parámetros alterados.
- **Base de la sincronización familiar** futura (push/pull de eventos entre dispositivos).

Detalle de implementación en [`../persistence/overview.md`](../persistence/overview.md).

### 4. Snapshots periódicos en `analytics`

Cada cierre de mes (o on-demand) se genera un snapshot inmutable de los read models clave (`MonthlySummary`, totales por categoría, patrimonio cuando exista). Sobre snapshots se calculan:

- CAGR de ingresos y patrimonio.
- Coeficiente de variación de ingresos (varianza temporal).
- Burn rate de los últimos N meses.
- Tendencias y semáforos del dashboard.

Sin snapshots, cada cálculo histórico reagregaría todo el ledger.

### 5. `Money` correcto (cents + currency)

`Money(cents: Long, currency: Currency)` con operaciones inmutables. Nunca `Double`. Cualquier KPI que multiplique o promedie pierde precisión catastróficamente con `Double`.

### 6. UUID v4 desde cliente

Permite generar eventos offline-first y, eventualmente, sincronizarlos con un backend sin conflictos de IDs.

## Mapa de crecimiento

```
                    MVP
                     |
                     v
+---------------+    +---------------+    +---------------+
|  users        |    |  categories   |    |  transactions |
|  (default)    |    |  (enriched)   |    |  (enriched)   |
+---------------+    +---------------+    +---------------+
                              |
                              v
                     +-----------------+
                     |    analytics    |
                     |  (basic stats)  |
                     +-----------------+
                              |
                ==============v==============
                              |
                              v
                       MVP+1: recurring
                              |
                              v
                MVP+2: accounts, budgets
                              |
                              v
                MVP+3: assets, liabilities
                              |
                              v
                MVP+4: forecasts, goals
                              |
                              v
                MVP+5: dashboard integral
```

Cada nivel añade contextos sin tocar los anteriores (con las excepciones documentadas en [`../contexts/future.md`](../contexts/future.md)).

## Política de cambio retroactivo

Cuando **sí** se acepta tocar contextos ya estables:

- Añadir un campo **opcional** a un agregado, con migración SQLDelight que rellena el campo a `null` o un default sensato.
- Añadir un evento de dominio nuevo (no rompe a nadie).
- Añadir una query nueva (no rompe a nadie).

Cuando **no** se acepta sin justificar:

- Cambiar el contrato de un evento (rompe a los suscriptores).
- Eliminar un campo de un agregado.
- Renombrar value objects (rompe lecturas del Event Store).

Si hay que romper compatibilidad, se versiona el evento: `TransactionRegisteredV2` con migración de eventos antiguos.

## Test de la arquitectura: "regla de los 5 archivos"

Indicador sano: añadir un KPI medio (p. ej. Coeficiente de Engel) debe requerir **≤ 5 archivos nuevos** y **0 modificaciones** a contextos existentes. Si no es así, hay un acoplamiento que arreglar.

Indicador insano: añadir un KPI obliga a tocar `Transaction.kt`. Ahí, parar y rediseñar.

## Lectura complementaria

- Catálogo completo de KPIs profesionales objetivo: [`kpi-catalog.md`](kpi-catalog.md).
- Mapa de contextos: [`../contexts/README.md`](../contexts/README.md).
- Detalle del Event Store y snapshots: [`../persistence/overview.md`](../persistence/overview.md).
