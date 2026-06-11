# Página principal (Home) — Especificación de diseño (design-driven)

> **Alcance:** la **pantalla principal** de la app (`Inicio` / `HomeScreen`). Este
> documento es la fuente de verdad del *diseño* de esa pantalla. Donde choque con
> [`SPEC.md`](SPEC.md), **manda este documento** para todo lo relativo al color
> semántico de ingresos/gastos (ver §1).
>
> Convención de etiquetas (igual que `SPEC.md`): `[existe]`, `[restyle]`,
> `[nuevo-ui]`, `[nuevo-dominio]`, `[mock]`.

---

## 1. Regla de color semántico (NO NEGOCIABLE)

> **Ingresos = AZUL. Gastos = ROJO.**

Es una **regla de marca**, no una preferencia de pantalla. Se aplica en toda la app,
pero la Home es donde más se ve y por eso vive aquí su definición canónica.

- **Ingreso / positivo / dinero que entra → azul.**
- **Gasto / negativo / dinero que sale → rojo.**
- **Ahorro / transferencia → se mantiene su acento propio** (`savings`, oliva de
  marca): no es ni ingreso ni gasto, no debe leerse como ninguno de los dos.

Esta regla **sustituye** al esquema anterior (ingreso = verde de marca / gasto =
terracota) descrito en `SPEC.md §2.1` y `§7-D`. El verde de marca (`brand`/`primary`)
sigue existiendo para el *chrome* (botones, acentos, hero), pero **deja de significar
"ingreso"**. La terracota deja de significar "gasto".

### 1.1 Por qué es una regla y no un detalle

- **Convención universal de finanzas:** azul/rojo es el código mental que el usuario
  ya trae (rojo = números en rojo = gasto/deuda). No hacerle pensar.
- **Consistencia:** un solo significado por color en toda la app evita ambigüedad
  (hoy el verde es a la vez "marca" y "ingreso", lo que diluye ambos).
- **Independiente del wallpaper:** como los tokens de finanzas son fijos (no Material
  You), el azul/rojo se mantiene aunque cambie el color dinámico del sistema.

### 1.2 El color NUNCA va solo (accesibilidad)

El color es **redundante**, nunca el único portador de significado (daltonismo,
contraste, modo de alto contraste):

- Los importes con signo conservan su prefijo: **`+`** ingreso, **`−`** gasto,
  **`→`** ahorro.
- Las etiquetas de texto ("Ingresos" / "Gastos") acompañan siempre al punto de color.
- Contraste mínimo objetivo: **AA (4.5:1)** del color sobre su fondo cuando se usa en
  texto/importe; los puntos/badges decorativos no necesitan cumplirlo pero sí ser
  distinguibles.

---

## 2. Tokens de color (definición)

Se introducen tokens semánticos nuevos que reemplazan el rol de `pos`/`neg` como
"ingreso/gasto". Conviven con el resto de tokens fijos de
[`Tokens.kt`](../../apps/android/src/main/kotlin/within/means/android/ui/theme/Tokens.kt).

| Token | Rol | Claro (aprox.) | Oscuro (aprox.) |
|---|---|---|---|
| `income` | Ingreso / positivo | `#2F6FB3` | `#5FA8E0` |
| `incomeSoft` | Fondo suave de ingreso (badge/chip) | `#E1ECF7` | `#1B2C3D` |
| `expense` | Gasto / negativo | `#C5392B` | `#E5705C` |
| `expenseSoft` | Fondo suave de gasto | `#F8E1DD` | `#3A2420` |
| `savings` | Ahorro / transferencia (sin cambios) | `#6E8B3D` | `#9CB35E` |

> Los hex son un **punto de partida** ajustable en implementación (se afinan para
> cumplir AA sobre `surface` claro y oscuro). Lo **fijo es la regla**: ingreso azul,
> gasto rojo. Los valores exactos no.

**Decisión de implementación (aplicada):** se **añaden** tokens nuevos `income`/`expense`
(azul/rojo) en vez de reciclar `pos`/`neg`. `pos`/`neg` (verde/terracota) **se conservan
pero pasan a significar ESTADO** (`ok`/`warning`), no ingreso/gasto:
- `pos` (verde) = estado bueno → "Dentro del plan", delta de ahorro al alza.
- `neg` (terracota) = estado malo / acción destructiva → "Atención", error de PIN, borrar.

Así el azul/rojo queda **reservado en exclusiva** al flujo de dinero (ingreso/gasto) y no
choca con el "semáforo" del presupuesto (§4-A, opción recomendada).

---

## 3. Dónde se aplica la regla en la Home

Anatomía de [`HomeScreen.kt`](../../apps/android/src/main/kotlin/within/means/android/ui/home/HomeScreen.kt),
de arriba abajo:

### 3.1 Hero "Balance del mes" (`BalanceHero`) `[restyle]`
La fila inferior con los dos `HeroStat` **Ingresos** y **Gastos**:
- Punto + importe de **Ingresos → `income` (azul)**.
- Punto + importe de **Gastos → `expense` (rojo)**.
- Hoy usa `WmTheme.colors.pos` (verde) y `neg` (terracota) →
  [`HomeScreen.kt:337-338`](../../apps/android/src/main/kotlin/within/means/android/ui/home/HomeScreen.kt#L337-L338).
- El importe grande de balance neto sigue en `onPrimaryContainer` (no se colorea
  azul/rojo: es un neto que puede ser de cualquier signo; su signo lo da el `−`/`+`).

### 3.2 Hero "Disponible" (`BudgetHero`) `[restyle]`
Es presupuesto, **no** ingreso/gasto. Matices:
- La barra gastado/plan y el badge "Dentro del plan / Atención" representan **estado
  bueno/malo**, no ingreso/gasto. **Decisión §4-A.**
- "Gastado X" es dinero que sale: si se quiere reforzar la regla, su cifra puede ir en
  `expense` (rojo); por defecto se mantiene neutra sobre el hero para no saturar de rojo
  la tarjeta principal. **Decisión §4-A.**

### 3.3 Donut "En qué va el mes" (`SpendingDonutCard`) `[existe]`
**No se toca por esta regla.** Los segmentos usan el **color de cada categoría**
(`categoryColor`), no la semántica ingreso/gasto. El donut es solo de gastos por
categoría; su naturaleza de "gasto" ya está implícita en el título y en el "gastado".

> ⚠️ Cuidado: la paleta de categorías contiene azules y rojos. Eso es aceptable porque
> ahí el color identifica *categoría*, no *signo*. La regla azul/rojo aplica solo a los
> roles **ingreso/gasto**, nunca a categorías.

### 3.4 "Reciente" — filas de transacción (`TransactionRow`) `[restyle]`
[`HomeScreen.kt:436-449`](../../apps/android/src/main/kotlin/within/means/android/ui/home/HomeScreen.kt#L436-L449):
- **Ingreso** → importe `+X` en **`income` (azul)** (hoy verde `pos`).
- **Gasto** → importe `−X` en **`expense` (rojo)** (hoy `onSurface`, neutro).
- **Ahorro** → `→X` en `savings` (sin cambios).

> Cambio respecto a hoy: el gasto pasa de **neutro** a **rojo explícito**. Es el cambio
> más visible de esta regla en la lista.

---

## 4. Decisiones (RESUELTAS)

**A. Hero "Disponible" (presupuesto) → semáforo verde/terracota (NO azul/rojo).**
El badge "Dentro del plan / Atención" y la barra son **estado** (vas bien / ojo), no
flujo de dinero. Se mantiene verde = dentro del plan / terracota = atención, vía los
tokens de estado `pos`/`neg`. El azul/rojo no compite con el presupuesto.

**B. Cifra "Gastado X" del hero de presupuesto → neutra.** Se queda en el color del
hero (`onPrimaryContainer`), para no inundar de rojo la tarjeta principal; el rojo se
reserva a la lista y al `HeroStat` de gastos.

---

## 5. Implementación (HECHA)

1. **Tokens** — [`Tokens.kt`](../../apps/android/src/main/kotlin/within/means/android/ui/theme/Tokens.kt):
   añadidos `income`/`incomeSoft` (azul) y `expense`/`expenseSoft` (rojo) en
   `WmColorTokens`, `LightTokens`, `DarkTokens`. `pos`/`neg`/`*Soft` se conservan como
   tokens de **estado** (ok/warn), documentado en el KDoc del data class.
2. **Aplicado en toda la app** (la regla es global, no solo Home):
   - Home: `HeroStat` Ingresos/Gastos (§3.1) y fila "Reciente" (§3.4) —
     [`HomeScreen.kt`](../../apps/android/src/main/kotlin/within/means/android/ui/home/HomeScreen.kt).
   - Movimientos: filas + total de grupo diario —
     [`TransactionsListScreen.kt`](../../apps/android/src/main/kotlin/within/means/android/ui/transactions/TransactionsListScreen.kt).
   - Análisis: trío Ingresos/Gastos —
     [`StatsScreen.kt`](../../apps/android/src/main/kotlin/within/means/android/ui/analytics/StatsScreen.kt)
     (el delta de tasa de ahorro sigue en verde/terracota = estado bueno/malo).
   - QuickAdd, editor de movimiento y recurrentes: acento/importe por tipo
     (`QuickAddSheet.kt`, `TransactionEditScreen.kt`, `RecurringRulesScreen.kt`).
3. **No tocado a propósito:** donut por categoría (color = categoría), lente
   Esencial/Discrecional (color distingue, no es signo), borrar/error (terracota = peligro).
4. **Pendiente de tus pruebas:** contraste AA en claro/oscuro en pantalla real y revisar
   que los signos `+`/`−`/`→` se leen bien junto al nuevo color.

> Compila limpio (`:apps:android:compileDebugKotlin`).

---

## 6. Notas de fidelidad
- La regla azul/rojo es **semántica fija**, independiente del Material You del sistema.
- No usar el azul/rojo de finanzas para nada que no sea ingreso/gasto (ni categorías,
  ni estado de presupuesto, ni enlaces).
- El color **nunca** sustituye al signo ni a la etiqueta (§1.2).
