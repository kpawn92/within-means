# Contextos del MVP

Detalle de los cuatro contextos del MVP. Para el resto, ver [`future.md`](future.md). Para el mapa global, [`README.md`](README.md).

Los modelos llevan ya los **metadatos enriquecidos** necesarios para escalar a KPIs profesionales sin migraciones futuras (justificado en [`../scalability/strategy.md`](../scalability/strategy.md)).

---

## `users`

Mínimo viable. Single-user local, sin login, sin familia. Modelado desde el día 1 para no migrar cuando llegue la sincronización familiar (post-MVP).

### Agregado

- `UserProfile` — perfil único del dispositivo.

### Value objects

- `UserId` (Identifier).
- `DisplayName` (StringValueObject).
- `Locale` (enum: `ES`, `EN`, ...).
- `BaseCurrency` (Currency del kernel).

### Casos de uso

- `EnsureDefaultUserCommand` (idempotente, ejecutado al primer arranque).
- `UpdateUserPreferencesCommand` (cambiar nombre, idioma, moneda base).
- `FindUserQuery`.

### Eventos publicados

- `UserDefaultCreated`
- `UserPreferencesUpdated`

### Notas

- Se reserva el `FamilyId` ya en el agregado, opcional, `null` hasta que se introduzca el contexto familiar.
- Compatibilidad ascendente: cuando se añada multi-usuario, el `UserProfile` existente se asocia a una nueva `Family`.

---

## `categories`

Taxonomía de categorías **enriquecida con clasificadores semánticos**. Sin estos clasificadores no se pueden calcular KPIs como ratio de gastos fijos, coeficiente de Engel ni ROAS personal en el futuro.

### Agregado

- `Category` — categoría jerárquica (admite padre).

### Value objects

- `CategoryId` (Identifier).
- `CategoryName` (StringValueObject).
- `CategoryColor` (StringValueObject, hex).
- `CategoryIcon` (StringValueObject, nombre de icono).
- `ParentCategoryId` (opcional, Identifier).
- `CategoryKind` (enum): `EXPENSE`, `INCOME`, `TRANSFER`.
- `CategoryNature` (enum): `FIXED`, `VARIABLE`.
- `CategoryEssentiality` (enum): `ESSENTIAL`, `DISCRETIONARY`.
- `CategoryProductive` (Boolean): true si el gasto genera valor futuro (educación, salud, herramientas).
- `EngelGroup` (enum): `FOOD`, `HOUSING`, `TRANSPORT`, `HEALTH`, `EDUCATION`, `LEISURE`, `OTHER`.

### Casos de uso

- `CreateCategoryCommand` (con todos los clasificadores).
- `RenameCategoryCommand`.
- `RecolorCategoryCommand`.
- `ReclassifyCategoryCommand` (cambiar `nature`, `essentiality`, `productive`, `engelGroup`).
- `DeleteCategoryCommand`.
- `SearchCategoriesByCriteriaQuery` (filtros por kind, nature, parent).

### Eventos publicados

- `CategoryCreated`, `CategoryRenamed`, `CategoryReclassified`, `CategoryDeleted`.

### Categorías por defecto

Al ejecutar `UserDefaultCreated` (post-bootstrap), se siembra un set por defecto:

| Nombre | Kind | Nature | Essentiality | Productive | EngelGroup |
|---|---|---|---|---|---|
| Nómina | INCOME | FIXED | — | — | — |
| Freelance | INCOME | VARIABLE | — | — | — |
| Alquiler | EXPENSE | FIXED | ESSENTIAL | false | HOUSING |
| Hipoteca | EXPENSE | FIXED | ESSENTIAL | false | HOUSING |
| Supermercado | EXPENSE | VARIABLE | ESSENTIAL | false | FOOD |
| Restaurantes | EXPENSE | VARIABLE | DISCRETIONARY | false | FOOD |
| Transporte público | EXPENSE | FIXED | ESSENTIAL | false | TRANSPORT |
| Combustible | EXPENSE | VARIABLE | ESSENTIAL | false | TRANSPORT |
| Salud | EXPENSE | VARIABLE | ESSENTIAL | true | HEALTH |
| Educación | EXPENSE | VARIABLE | DISCRETIONARY | true | EDUCATION |
| Suscripciones | EXPENSE | FIXED | DISCRETIONARY | false | LEISURE |
| Ocio | EXPENSE | VARIABLE | DISCRETIONARY | false | LEISURE |
| Transferencia | TRANSFER | — | — | — | — |

El usuario puede modificar el set; el seed es sólo el punto de partida.

---

## `transactions`

Núcleo del dominio. Cada movimiento de dinero, con metadatos enriquecidos para futuras analíticas.

### Agregado

- `Transaction` — un movimiento.

### Value objects

- `TransactionId` (Identifier).
- `TransactionType` (enum): `INCOME`, `EXPENSE`, `TRANSFER`.
- `TransactionDate` (DateValueObject sobre `kotlinx.datetime.LocalDate`).
- `TransactionDescription` (StringValueObject).
- `Amount`: `Money` del kernel.
- `IncomeSource` (enum, nullable): `ACTIVE`, `PASSIVE`. Sólo aplica a `INCOME`.
- `OriginRef` (StringValueObject, opcional): id opaco de la fuente (empleador, cliente). Habilita Herfindahl en futuro.
- `RecurringRef` (RecurringId, nullable): si vino de una plantilla recurrente (contexto futuro `recurring`).

### Casos de uso

- `RegisterTransactionCommand` (ingreso o gasto). Recibe `categoryId`, `amount`, `date`, `description`, opcionalmente `incomeSource` y `originRef`.
- `EditTransactionCommand`.
- `DeleteTransactionCommand`.
- `FindTransactionQuery`.
- `SearchTransactionsByCriteriaQuery` — usa el patrón `Criteria` (filtros por categoría, rango de fechas, tipo, monto).

> Transferencias y multicuenta se posponen a post-MVP cuando entre el contexto `accounts`.

### Eventos publicados

- `TransactionRegistered` (consumido por `analytics`).
- `TransactionEdited`.
- `TransactionDeleted`.

### Invariantes del agregado

- `amount > 0` (siempre positivo; el tipo `INCOME`/`EXPENSE` da el signo lógico).
- `date <= today` (no se aceptan transacciones futuras en MVP).
- Si `type == INCOME`, `incomeSource` debe estar definido.
- Si `type == EXPENSE`, `incomeSource` debe ser null.

---

## `analytics`

Read models. Sólo consume eventos; no expone comandos. Los read models son **proyecciones materializadas** en tablas SQLDelight separadas.

### Read models del MVP

#### `MonthlySummary`

Resumen del mes en curso o de un mes histórico.

```
MonthlySummary {
  period: YearMonth
  totalIncome: Money
  totalExpenses: Money
  netSaving: Money            // totalIncome - totalExpenses
  fixedExpenses: Money        // suma de transacciones cuya categoría es FIXED
  variableExpenses: Money     // ídem VARIABLE
  essentialExpenses: Money    // ídem ESSENTIAL
  discretionaryExpenses: Money // ídem DISCRETIONARY
}
```

#### `CategoryBreakdown`

Desglose por categoría en un periodo, separable por kind (ingreso/gasto).

```
CategoryBreakdown {
  period: DateRange
  kind: EXPENSE | INCOME
  entries: List<CategoryAmount>   // categoría -> monto, ordenado desc
}
```

#### `MonthlyEvolution`

Serie temporal de los últimos N meses.

```
MonthlyEvolution {
  months: List<MonthlySummary>   // p. ej. últimos 12 meses
}
```

### Queries del MVP

- `FindCurrentMonthSummaryQuery`.
- `FindMonthlySummaryQuery(period: YearMonth)`.
- `FindCategoryBreakdownQuery(period: DateRange, kind: CategoryKind)`.
- `FindMonthlyEvolutionQuery(months: Int)`.

### Suscriptores

- `UpdateMonthlySummaryOnTransactionRegistered`.
- `UpdateMonthlySummaryOnTransactionEdited`.
- `UpdateMonthlySummaryOnTransactionDeleted`.
- `UpdateCategoryBreakdownOnTransactionRegistered` (y edit/delete).
- `UpdateMonthlyEvolutionOnTransactionRegistered` (y edit/delete).

### Reconstrucción

Las proyecciones se pueden reconstruir desde cero reproduciendo eventos del Event Store (ver [`../persistence/overview.md`](../persistence/overview.md)). Esto es lo que permite, en el futuro, añadir nuevas proyecciones sin perder histórico.

---

## Cobertura de las preguntas del MVP

| Pregunta del usuario | Read model que la responde |
|---|---|
| ¿De dónde viene mi dinero este mes? | `CategoryBreakdown(period=mes, kind=INCOME)` |
| ¿En qué se está yendo el dinero? | `CategoryBreakdown(period=mes, kind=EXPENSE)` |
| ¿Cuánto ahorro este mes? | `MonthlySummary.netSaving` |
| ¿Cómo voy comparado con meses anteriores? | `MonthlyEvolution` |
