# Bounded contexts

Mapa de los contextos del proyecto. Sigue el patrón estricto del esqueleto Java: cada contexto es un módulo Gradle independiente; **no se importan entre sí**; la comunicación ocurre vía eventos de dominio y vía consultas al `QueryBus`.

## Contextos del MVP

Detalle completo en [`mvp.md`](mvp.md).

| Contexto | Responsabilidad | Notas |
|---|---|---|
| `users` | Usuario por defecto (single-user) | Mínimo viable; sin login. Modelado desde ya para no migrar después. |
| `categories` | Taxonomía de categorías | **Enriquecida** con `nature`, `essentiality`, `productive`, `engelGroup`. |
| `transactions` | Movimientos diarios (núcleo) | **Enriquecida** con `incomeSource`, `originRef`, `recurringRef`. |
| `analytics` | Read models básicos | Resumen mensual, ingresos por categoría, gastos por categoría, evolución histórica. |
| `shared` | Kernel técnico + Event Store | No es contexto de negocio. Contiene buses, criteria, `domain_events`. |

## Contextos post-MVP

Detalle completo en [`future.md`](future.md). Cada uno habilita una familia de KPIs profesionales (ver [`../scalability/kpi-catalog.md`](../scalability/kpi-catalog.md)).

| Contexto | Cuándo entra | KPIs que habilita |
|---|---|---|
| `recurring` | MVP+1 | Burn rate, fijos vs variables, proyección de flujo. |
| `accounts` | MVP+2 | Saldo total líquido, distribución por cuenta. |
| `budgets` | MVP+2 | Varianza presupuestaria, control por categoría. |
| `assets` | MVP+3 | Patrimonio neto, ratio liquidez, número de Wealth, % productivo. |
| `liabilities` | MVP+3 | DTI, DSCR, ratio cobertura. |
| `forecasts` | MVP+4 | Flujo de caja 12m, sensibilidad, runway dinámico, stress tests. |
| `goals` | MVP+4 | Tiempo a meta, ratio de independencia financiera. |
| `dashboard` | MVP+5 | Cuadro de mando integral con semáforos y umbrales. |

## Mapa de eventos entre contextos

```
                         +------------+
                         |   users    |
                         |  (default) |
                         +------------+
                                |
                                | UserDefaultCreated
                                v
                  +-------------+-------------+
                  |                           |
        +-----------------+         +-----------------+
        |   categories    |         |  transactions   |
        +-----------------+         +-----------------+
                  |                           |
        CategoryCreated              TransactionRegistered
                  |                           |
                  +-------------+-------------+
                                |
                                v
                      +-------------------+
                      |     analytics     |
                      |  (read models)    |
                      +-------------------+
```

En post-MVP el grafo crece, pero el patrón se mantiene: cada contexto publica sus eventos y los demás se suscriben sin acoplarse al emisor.

## Reglas duras

1. **Sin imports cross-context.** Si `transactions` necesita una `Category`, **no** importa el módulo `:categories`. Trabaja con el `CategoryId` (UUID String) y consulta al `QueryBus` si necesita más.
2. **Sin FKs cross-context en la DB.** Los IDs se guardan como `TEXT` sin `REFERENCES`. La integridad la sostiene el dominio.
3. **Comunicación por eventos.** Si un contexto necesita reaccionar a algo de otro, se suscribe a su evento de dominio.
4. **Sin shared mutable state.** Lo único compartido entre contextos es `:shared` (clases base, buses, criteria, Event Store) y el VO `Money` por reutilización.

## Glosario rápido

- **Aggregate Root.** Entidad central de un contexto que protege invariantes (p. ej. `Transaction`, `Category`).
- **Value Object.** Inmutable, sin identidad (p. ej. `Money`, `TransactionDate`).
- **Domain Event.** Algo que pasó en el pasado, inmutable (p. ej. `TransactionRegistered`).
- **Command / Query.** Intención de cambio / pregunta. Despachados por buses.
- **Read Model / Projection.** Vista materializada para queries rápidas (p. ej. `MonthlySummary`).
- **Subscriber.** Suscriptor de un evento que actualiza un read model o dispara efectos colaterales.
