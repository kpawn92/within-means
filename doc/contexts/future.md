# Contextos post-MVP

Esquema de contextos que se añaden tras el MVP. Cada uno habilita una familia de KPIs profesionales (ver [`../scalability/kpi-catalog.md`](../scalability/kpi-catalog.md)).

Estos diseños son **provisionales**: se confirman cuando llegue su fase. Aquí se anticipan para que las decisiones del MVP sean compatibles.

---

## `recurring` — MVP+1

Plantillas de transacciones recurrentes (nómina, alquiler, suscripciones, hipoteca). Materializan `Transaction` cada periodo.

### Agregado

- `RecurringTransaction` — plantilla con periodicidad.

### Value objects

- `RecurringId`, `Periodicity` (DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM), `NextOccurrence`, `EndDate?`.

### Casos de uso

- `CreateRecurringTransaction`, `PauseRecurring`, `ResumeRecurring`, `EditRecurring`, `DeleteRecurring`.
- Job periódico: `MaterializeDueRecurringTransactions` (al abrir la app o programado).

### Eventos

- `RecurringCreated`, `RecurringMaterialized` (con el `TransactionId` resultante), `RecurringPaused`, `RecurringDeleted`.

### KPIs habilitados

- Burn rate fiable (gastos fijos identificados con certeza).
- Proyección de flujo de caja: suma de recurrencias futuras.
- Separación dura "fijo vs variable".

---

## `accounts` — MVP+2

Cuentas donde se mueve el dinero. Se recupera del plan original.

### Agregado

- `Account` — cuenta con saldo.

### Value objects

- `AccountId`, `AccountName`, `AccountType` (BANK, CASH, CREDIT_CARD, SAVINGS, INVESTMENT), `Balance` (Money).

### Cambios en `transactions`

- Se añade `AccountId` (no nullable) al agregado `Transaction`.
- Migración: las transacciones existentes se asocian a una "Cuenta principal" creada automáticamente.

### Casos de uso

- `CreateAccount`, `RenameAccount`, `ArchiveAccount`.
- `RegisterTransfer` (entre cuentas).
- `FindAccount`, `SearchAccounts`.

### Eventos

- `AccountCreated`, `AccountBalanceUpdated`, `AccountArchived`.

### Suscripciones

- `TransactionRegistered` → recalcula saldo.

### KPIs habilitados

- Saldo total líquido por dispositivo/persona.
- Distribución de liquidez.

---

## `budgets` — MVP+2

Presupuestos mensuales por categoría.

### Agregado

- `Budget` — presupuesto para una categoría en un periodo.

### Value objects

- `BudgetId`, `BudgetPeriod` (YearMonth), `MonthlyLimit` (Money), `Consumption` (Money).

### Casos de uso

- `SetBudget`, `UpdateBudget`, `RemoveBudget`, `FindBudget`, `SearchBudgetsByPeriod`.

### Eventos

- `BudgetSet`, `BudgetExceeded`.

### Suscripciones

- `TransactionRegistered` → recalcula consumo, dispara `BudgetExceeded` si procede.

### KPIs habilitados

- Varianza presupuestaria = (Real − Presupuestado) / Presupuestado por categoría.
- Detección de categorías sistemáticamente fuera de control.

---

## `assets` — MVP+3

Patrimonio: activos líquidos, ilíquidos, productivos.

### Agregados

- `Asset` — un activo individual.

### Value objects

- `AssetId`, `AssetType` (CASH, BANK, INVESTMENT, REAL_ESTATE, VEHICLE, OTHER).
- `Liquidity` (LIQUID, ILLIQUID).
- `Productive` (Boolean): genera ingresos por sí mismo (alquiler, dividendos).
- `CurrentValue` (Money), `Valuation` (snapshot histórico).

### Eventos

- `AssetRegistered`, `AssetRevalued`, `AssetSold`.

### KPIs habilitados

- Patrimonio neto = Σ activos − Σ pasivos.
- Ratio activos productivos / activos totales.
- Ratio de liquidez = activos líquidos / gastos mensuales.
- Número de Wealth (Stanley & Danko).

---

## `liabilities` — MVP+3

Deudas y préstamos.

### Agregado

- `Liability` — un préstamo o deuda.

### Value objects

- `LiabilityId`, `LiabilityType` (MORTGAGE, PERSONAL_LOAN, CREDIT_CARD_DEBT, OTHER), `Principal` (Money), `OutstandingBalance` (Money), `InterestRate`, `MonthlyPayment` (Money), `MaturityDate`.

### Eventos

- `LiabilityRegistered`, `PaymentApplied`, `LiabilityPaidOff`.

### KPIs habilitados

- DTI (Debt-to-Income Ratio) = pagos mensuales / ingreso bruto mensual.
- DSCR (Debt Service Coverage) = ingreso operativo / servicio de deuda.
- Ratio de cobertura de deuda.

---

## `forecasts` — MVP+4

Proyecciones y stress tests. Lee de `recurring`, `assets`, `liabilities`, `analytics`.

### Agregados / Servicios

- `CashflowForecast` — proyección a N meses.
- `StressTest` — simulación de escenarios (caída ingreso, subida gastos).

### Casos de uso

- `ProjectCashflow(months: Int)`.
- `RunStressTest(scenario: StressScenario)` con escenarios predefinidos y custom.
- `EstimateRunway`.

### KPIs habilitados

- Proyección de flujo de caja a 12 meses con estacionalidad.
- Análisis de sensibilidad (+10% tarifas, -1 cliente, +15% gastos fijos).
- Runway dinámico = activos líquidos / burn rate.
- Stress test personal (caída 20/40/100% de ingreso × 1/3/6 meses).

---

## `goals` — MVP+4

Metas financieras y planificación.

### Agregado

- `FinancialGoal` — meta de ahorro/inversión/independencia.

### Value objects

- `GoalId`, `GoalKind` (EMERGENCY_FUND, SAVINGS_TARGET, INVESTMENT_TARGET, DEBT_PAYOFF, INDEPENDENCE), `TargetAmount` (Money), `Deadline?`, `Progress` (Money), `MonthlyContribution` (Money).

### Casos de uso

- `SetGoal`, `UpdateGoal`, `ContributeToGoal`, `RemoveGoal`, `FindGoalsByKind`.

### Eventos

- `GoalSet`, `GoalProgressUpdated`, `GoalAchieved`.

### KPIs habilitados

- Tiempo a meta = (target − progress) / aportación mensual ajustada por rendimiento.
- Ratio de independencia financiera = ingresos pasivos / gastos totales.

---

## `dashboard` — MVP+5

Cuadro de mando integral. Es un contexto de **agregación** que consume read models de los demás y los compone en una vista única con semáforos.

### Agregado

- `Dashboard` — definición personalizable: qué KPIs mostrar, qué umbrales (verde/amarillo/rojo).

### Value objects

- `KpiCard` — id del KPI, valor actual, umbrales, tendencia (vs mes anterior).
- `Thresholds(green, yellow, red)`.

### Casos de uso

- `ConfigureDashboard`, `FindDashboardState`, `ListAvailableKpis`.

### Notas

- Este contexto **no** introduce datos nuevos: orquesta queries existentes y aplica umbrales.
- Es la "vista profesional" que cumple el objetivo de "construir un cuadro de mando con 8-12 indicadores clave actualizados mensualmente".

---

## Tabla de impacto cross-context

Qué cambios en contextos existentes requiere cada nuevo contexto:

| Contexto que entra | Cambia... | Cómo |
|---|---|---|
| `recurring` | `transactions` | Añade campo `recurringRef` (ya reservado en MVP). |
| `accounts` | `transactions` | Añade `accountId` no nullable + migración con cuenta default. |
| `budgets` | — | Independiente; solo se suscribe a eventos. |
| `assets` | — | Independiente. |
| `liabilities` | — | Independiente. |
| `forecasts` | — | Sólo consume queries; no muta nada. |
| `goals` | — | Independiente. |
| `dashboard` | — | Sólo consume queries; no muta nada. |

Los únicos cambios destructivos son los de `recurring` y `accounts` sobre `transactions`. Ambos están **anticipados en el MVP** con campos reservados (`recurringRef`) y un plan de migración (`accountId`).
