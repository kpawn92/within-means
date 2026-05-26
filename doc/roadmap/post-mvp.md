# Roadmap — Post-MVP

Fases posteriores al MVP. Cada bloque introduce uno o varios contextos y desbloquea una familia de KPIs profesionales (ver [`../scalability/kpi-catalog.md`](../scalability/kpi-catalog.md)).

Cada contexto es un módulo Gradle nuevo. Se añade sin modificar contextos estables, con las excepciones documentadas en [`../contexts/future.md`](../contexts/future.md).

---

## MVP+1 — `recurring`

Plantillas de transacciones recurrentes. Necesario para distinguir "fijo" de "variable" con certeza y para el burn rate fiable.

- [ ] Contexto `recurring`: `RecurringTransaction`, `Periodicity`, `NextOccurrence`.
- [ ] Job `MaterializeDueRecurringTransactionsCommand` al arrancar la app + scheduler diario.
- [ ] Events: `RecurringCreated`, `RecurringMaterialized`, `RecurringPaused`, `RecurringDeleted`.
- [ ] Suscriptor en `transactions`: al materializar, se crea una `Transaction` con `recurringRef` poblado.
- [ ] UI Android: lista de recurrencias, alta/baja/edición.
- [ ] `analytics` añade KPI **Burn rate mensual** (promedio últimos 3 meses).

**Entrega:** la nómina mensual y el alquiler se registran automáticamente; burn rate aparece en estadísticas.

---

## MVP+2 — `accounts` + `budgets`

Recuperamos los contextos del plan original. **Cambio destructivo** en `transactions`: añadir `accountId` con migración.

- [ ] Contexto `accounts`: `Account`, `AccountId`, `AccountName`, `AccountType`, `Balance`.
- [ ] Migración SQLDelight: crear "Cuenta principal" y asociar todas las transacciones existentes a ella.
- [ ] `transactions` añade `accountId` no nullable; nueva versión del evento `TransactionRegisteredV2` con `accountId`.
- [ ] `RegisterTransferCommand` + handler (afecta a dos cuentas).
- [ ] Subscriber `UpdateBalanceOnTransactionRegistered`.
- [ ] Contexto `budgets`: `Budget`, `BudgetPeriod`, `MonthlyLimit`, `Consumption`.
- [ ] Subscriber `UpdateBudgetOnTransactionRegistered`.
- [ ] Events: `BudgetSet`, `BudgetExceeded`.
- [ ] UI Android: gestión de cuentas, presupuestos por categoría, alertas visuales de exceso.
- [ ] `analytics` añade KPI **Varianza presupuestaria**.

**Entrega:** múltiples cuentas, transferencias entre ellas, presupuestos con alertas.

---

## MVP+3 — `assets` + `liabilities`

Patrimonio y deudas. Habilita el grupo más grande de KPIs profesionales (patrimonio neto, liquidez, DTI, DSCR, runway, CAGR, número de Wealth).

- [ ] Contexto `assets`: `Asset`, `AssetType`, `Liquidity`, `Productive`, `CurrentValue`, `Valuation` (snapshot histórico).
- [ ] Events: `AssetRegistered`, `AssetRevalued`, `AssetSold`.
- [ ] Contexto `liabilities`: `Liability`, `LiabilityType`, `Principal`, `OutstandingBalance`, `InterestRate`, `MonthlyPayment`, `MaturityDate`.
- [ ] Events: `LiabilityRegistered`, `PaymentApplied`, `LiabilityPaidOff`.
- [ ] Suscriptor en `transactions`: pagos a `liabilities` reducen `OutstandingBalance`.
- [ ] Snapshots mensuales de `assets` y `liabilities` en `analytics`.
- [ ] UI Android: pantallas de patrimonio (activos + pasivos + patrimonio neto), gráfica de evolución.
- [ ] `analytics` añade KPIs:
  - Patrimonio neto.
  - Ratio liquidez.
  - Ratio activos productivos.
  - Tasa crecimiento patrimonio MoM/YoY.
  - CAGR patrimonio.
  - Número de Wealth (requiere edad en `UserProfile`).
  - DTI, DSCR, ratio cobertura.
  - Runway dinámico.

**Entrega:** dashboard de patrimonio funcional con histórico mensual; 8+ KPIs profesionales disponibles.

---

## MVP+4 — `forecasts` + `goals`

Proyecciones, stress tests y metas. Los indicadores predictivos y de planificación.

- [ ] Contexto `forecasts`:
  - `CashflowForecast` (proyección N meses con estacionalidad).
  - `StressTest` con escenarios predefinidos (-20/-40/-100% ingresos × 1/3/6 meses) y custom.
  - Servicio `SensitivityAnalysis` (+10% tarifas, -1 cliente, +15% gastos fijos).
- [ ] Contexto `goals`: `FinancialGoal`, `GoalKind`, `TargetAmount`, `Deadline`, `Progress`, `MonthlyContribution`.
- [ ] Events: `GoalSet`, `GoalProgressUpdated`, `GoalAchieved`.
- [ ] Suscriptor: `ContributeToGoalOnTransactionRegistered` (transacciones etiquetadas como aportación a meta).
- [ ] `analytics` añade KPIs:
  - Coeficiente variación ingresos.
  - Herfindahl concentración.
  - Proyección flujo de caja 12m.
  - Punto de equilibrio personal.
  - Tiempo a meta.
  - Ratio de independencia financiera.
  - Stress test personal.
- [ ] UI Android: pantalla de proyecciones, simulador de escenarios, gestión de metas.

**Entrega:** "qué pasa si me bajan el sueldo un 20%" tiene una respuesta cuantificada; metas con fecha estimada de cumplimiento.

---

## MVP+5 — `dashboard`

Cuadro de mando integral con semáforos y umbrales configurables. **No introduce datos nuevos**, orquesta queries existentes.

- [ ] Contexto `dashboard`: `Dashboard`, `KpiCard`, `Thresholds(green, yellow, red)`.
- [ ] Catálogo de KPIs configurables (lista cerrada de los implementados hasta esa fase).
- [ ] Defaults de umbrales (ver [`../scalability/kpi-catalog.md`](../scalability/kpi-catalog.md)).
- [ ] UI Android: dashboard configurable con 8-12 tarjetas, semáforos, tendencia (vs mes anterior), sugerencia accionable por KPI en rojo.

**Entrega:** vista única con los KPIs clave del usuario, accionable.

---

## Fases transversales (en paralelo o intercaladas)

### Mejoras de UX y plataforma

- [ ] **Biometría** sobre el PIN (Android BiometricPrompt).
- [ ] **Importar / exportar** CSV y JSON.
- [ ] **Backup local** cifrado a archivo.
- [ ] **Multidivisa** real con tipos de cambio (servicio + cache).
- [ ] **OCR de recibos** (post-MVP+5, depende de cámara y modelos ligeros).
- [ ] **Categorización automática** con heurísticas o ML on-device.

### `apps/desktop`

- [ ] Habilitar target JVM en módulos KMP.
- [ ] `apps/desktop` con `org.jetbrains.compose`.
- [ ] Reutilizar pantallas Compose extrayendo lo común a `:apps:ui-common`.
- [ ] SQLDelight driver JVM con SQLCipher JDBC.
- [ ] Theming adaptado a ventana.

**Cuándo:** cuando haya señales de demanda real. No es bloqueante.

### iOS

- [ ] Añadir target `iosX64`, `iosArm64`, `iosSimulatorArm64` a módulos KMP.
- [ ] `apps/ios` con Compose Multiplatform iOS (alpha al inicio de 2026) o SwiftUI consumiendo lógica común.
- [ ] SQLDelight driver iOS native.
- [ ] Adaptar `KeystoreManager` a iOS Keychain.

**Cuándo:** post-MVP+5; tiene coste alto y baja prioridad inicial.

### Sincronización familiar

- [ ] `apps/backend` Ktor.
- [ ] Push/pull de eventos del Event Store entre dispositivos.
- [ ] Resolución de conflictos por vector clock o CRDT.
- [ ] Contexto `users` se expande a `family`, `members`, `roles`.
- [ ] Multi-tenant.

**Cuándo:** la fase más lejana. Es esencialmente otro producto.

---

## Lectura complementaria

- [`mvp.md`](mvp.md) — fases del MVP.
- [`../contexts/future.md`](../contexts/future.md) — detalle de cada contexto post-MVP.
- [`../scalability/strategy.md`](../scalability/strategy.md) — cómo escala todo esto sin reescribir.
- [`../scalability/kpi-catalog.md`](../scalability/kpi-catalog.md) — qué KPI desbloquea cada fase.
