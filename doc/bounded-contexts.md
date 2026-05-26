# Contextos acotados

Cada contexto es un módulo Gradle independiente bajo `src/`. La única dependencia común es `:shared`. Ningún contexto importa a otro: la comunicación entre contextos ocurre **siempre** a través de eventos de dominio o consultas al `QueryBus`.

## Mapa de contextos

```
                    +-----------+
                    |  shared   |
                    +-----------+
                          ^
        +-------+---------+---------+--------+--------+
        |       |         |         |        |        |
    +-------+ +------+ +---------+ +-------+ +------+ +----------+
    | users | |accts | | transac | | cats  | |budgts| | analytcs |
    +-------+ +------+ +---------+ +-------+ +------+ +----------+
```

Flujos de eventos entre contextos:

- `transactions` publica `TransactionRegistered` → consumen `budgets` y `analytics`.
- `budgets` publica `BudgetExceeded` → consume `analytics` (y `users` en el futuro para notificaciones).
- `accounts` publica `AccountBalanceUpdated` → consume `analytics`.

## `users`

Gestión de usuarios y unidades familiares (grupos compartidos de gasto).

### Agregados

- `User` — un usuario individual.
- `Family` — grupo de usuarios que comparten cuentas, transacciones, presupuestos.

### Value objects

- `UserId`, `FamilyId` (Identifier).
- `Email`, `UserName`, `PasswordHash` (StringValueObject).
- `FamilyRole` (enum: `OWNER`, `MEMBER`).

### Casos de uso

- `RegisterUser` (Command) — crear usuario nuevo.
- `CreateFamily` (Command) — un usuario crea una familia y se convierte en `OWNER`.
- `InviteMember` (Command) — añadir miembro a una familia.
- `FindUser` (Query) — buscar usuario por id.
- `SearchFamilyMembers` (Query) — listar miembros de una familia.

### Eventos publicados

- `UserRegistered`
- `FamilyCreated`
- `MemberInvited`

## `accounts`

Cuentas donde se mueve el dinero: bancos, efectivo, tarjetas de crédito.

### Agregados

- `Account` — una cuenta concreta con saldo.

### Value objects

- `AccountId` (Identifier).
- `AccountName` (StringValueObject).
- `AccountType` (enum: `BANK`, `CASH`, `CREDIT_CARD`, `SAVINGS`).
- `Currency` (enum o VO ISO 4217).
- `Money` — VO con `amount: Long` (céntimos) y `currency: Currency`. **Inmutable**, operaciones devuelven nuevas instancias.
- `Balance` — saldo actual.

### Casos de uso

- `CreateAccount` (Command).
- `RenameAccount` (Command).
- `ArchiveAccount` (Command).
- `FindAccount` (Query).
- `SearchAccountsByFamily` (Query).

### Eventos publicados

- `AccountCreated`
- `AccountBalanceUpdated` (escuchado por `analytics`).
- `AccountArchived`

### Suscripciones

- `TransactionRegistered` (de `transactions`) → recalcular saldo.

## `transactions`

Núcleo del dominio. Cada movimiento de dinero.

### Agregados

- `Transaction` — un movimiento: ingreso, gasto o transferencia.

### Value objects

- `TransactionId` (Identifier).
- `TransactionType` (enum: `INCOME`, `EXPENSE`, `TRANSFER`).
- `TransactionDate` (DateValueObject sobre `LocalDate` de kotlinx-datetime).
- `TransactionDescription` (StringValueObject).
- `Amount` — reutiliza `Money` de `accounts` (sí, este es un caso justificado de tipo compartido en `shared/domain/money` para evitar duplicación).

### Casos de uso

- `RegisterTransaction` (Command) — registrar ingreso o gasto.
- `RegisterTransfer` (Command) — transferencia entre cuentas (afecta a dos cuentas).
- `EditTransaction` (Command).
- `DeleteTransaction` (Command).
- `FindTransaction` (Query).
- `SearchTransactionsByCriteria` (Query) — usa el patrón `Criteria` con filtros (cuenta, categoría, rango de fechas, tipo).

### Eventos publicados

- `TransactionRegistered` (escuchado por `accounts`, `budgets`, `analytics`).
- `TransactionEdited`
- `TransactionDeleted`

## `categories`

Taxonomía de categorías para clasificar transacciones.

### Agregados

- `Category` — categoría jerárquica (admite padre).

### Value objects

- `CategoryId` (Identifier).
- `CategoryName` (StringValueObject).
- `CategoryColor` (StringValueObject, formato hex).
- `CategoryIcon` (StringValueObject, nombre de icono).
- `ParentCategoryId` (opcional, Identifier).

### Casos de uso

- `CreateCategory` (Command).
- `RenameCategory` (Command).
- `RecolorCategory` (Command).
- `DeleteCategory` (Command).
- `SearchCategoriesByFamily` (Query).

### Eventos publicados

- `CategoryCreated`, `CategoryRenamed`, `CategoryDeleted`.

### Notas

Las categorías son por familia (no globales). Se crean unas por defecto al ejecutar `CreateFamily`.

## `budgets`

Presupuestos mensuales por categoría.

### Agregados

- `Budget` — presupuesto para una categoría en un periodo (mes).

### Value objects

- `BudgetId` (Identifier).
- `BudgetPeriod` — VO con `year` y `month`.
- `MonthlyLimit` — `Money`.
- `Consumption` — `Money` consumido en el periodo.

### Casos de uso

- `SetBudget` (Command) — establecer límite mensual para una categoría.
- `UpdateBudget` (Command).
- `RemoveBudget` (Command).
- `FindBudget` (Query).
- `SearchBudgetsByPeriod` (Query).

### Eventos publicados

- `BudgetSet`
- `BudgetExceeded` (cuando el consumo supera el límite).

### Suscripciones

- `TransactionRegistered` (de `transactions`) → si la transacción es un gasto en una categoría con presupuesto, recalcula `Consumption` y, si supera el límite, publica `BudgetExceeded`.

## `analytics`

Read models. Proyecciones materializadas para queries rápidas. **Solo consume eventos**, no expone comandos.

### Read models

- `MonthlySummary` — total ingresos/gastos por mes y familia.
- `CategoryBreakdown` — gasto por categoría en un periodo.
- `AccountFlow` — entradas/salidas por cuenta y mes.

### Casos de uso (solo queries)

- `FindMonthlySummary` (Query).
- `FindCategoryBreakdown` (Query).
- `FindAccountFlow` (Query).

### Suscripciones

- `TransactionRegistered`, `TransactionEdited`, `TransactionDeleted` → actualizar proyecciones.
- `AccountBalanceUpdated` → refrescar `AccountFlow`.

### Notas

Las proyecciones se almacenan en tablas SQLDelight separadas (`monthly_summary`, `category_breakdown`, `account_flow`). No son entidades de dominio: son cachés materializadas. Se pueden reconstruir reproduciendo eventos desde el historial.

## `shared`

No es un contexto de negocio, sino el kernel técnico. Ver [architecture.md](architecture.md#kernel-compartido-shared).

Excepción: el VO `Money` y `Currency` viven en `shared/domain/money/` porque son ubicuos (los usan `accounts`, `transactions`, `budgets`, `analytics`). Es un caso de **Shared Kernel** consciente.
