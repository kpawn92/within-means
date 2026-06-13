# Conceptos — Especificación (spec-driven)

> **Alcance:** introducir el **Concepto** como dimensión ligera y consultable de cada
> movimiento, para responder preguntas que hoy el modelo **no puede** contestar:
> *"¿cuánto gasté en cerveza?"*, *"¿cuánto en papá?"*, *"¿cuánto en el bus de casa al
> trabajo?"*. El concepto **no sustituye** a la categoría: la categoría sigue siendo la
> estructura (presupuesto, esencial/discrecional, grupo de Engel, donut); el concepto es
> el **detalle vivo** que el usuario quiere rastrear. Tras las tres decisiones de diseño
> cerradas con el cliente (§0), el concepto es: **(1)** un primitivo ligero auto-sugerido,
> **(2)** con **cardinalidad múltiple** por movimiento, **(3)** que **infiere la categoría**
> para que dejar de pedirla en cada captura.
>
> La captura se ofrece **dentro** de la app (`QuickAdd` evolucionado) y **fuera** de ella
> (App Shortcuts del icono y un widget de inicio "Añadir rápido"), siempre contra el mismo
> flujo de Guardar — ver §4.
>
> Toca un **bounded context nuevo** (`concepts`, espejo ligero de `categories`), la app
> Android (`QuickAdd`, `TxnEditor`, `Stats`, widgets/shortcuts) y `analytics`. Respeta los límites de
> contexto: `transactions` referencia conceptos **por id** (igual que referencia categorías
> con `CategoryRef`); la orquestación concepto→categoría vive en la **capa de aplicación**
> de la app, no dentro de un contexto KMP. Ver [[respect_architecture]],
> [[cross-context-subscribers]] y [[simplicity_first_for_user]].
>
> Donde choque con [`SPEC.md`](SPEC.md), manda `SPEC.md` para el sistema de diseño; este
> documento manda para **el modelo de Concepto, la captura y la consulta**.
>
> Convención de etiquetas (igual que `SPEC.md`): `[existe]`, `[restyle]`, `[nuevo-ui]`,
> `[nuevo-dominio]`, `[mock]`.

---

## 0. Decisiones cerradas con el cliente

| # | Decisión | Elegido | Implicación |
|---|---|---|---|
| D0.1 | Mecanismo de consulta granular | **Concepto ligero** (no más búsqueda de texto, no subcategorías) | Nuevo value object consultable; `birra`/`cerveza`/`Cerveza 🍺` suman juntos vía clave normalizada. |
| D0.2 | Conceptos por movimiento | **Varios** | Permite cruzar dimensiones ("transporte de papá"). El breakdown por concepto **no es una partición**. |
| D0.3 | Relación con la categoría | **El concepto infiere la categoría** | La categoría deja de ser un campo manual obligatorio en cada captura → entrada de ~2 toques. |
| D0.4 | Agrupación de captura por lotes (§4.4) | **Sí: `batchRef` agrupa la cesta** | Los N movimientos de un lote comparten un `batchRef` opaco (reusa los refs de provenance reservados en `Transaction`); permite verlos juntos en la lista y **deshacer el lote** de una vez. Sin semántica de dominio pesada. |

---

## 1. Visión y principios

El usuario **ya escribe** lo que compra; hoy ese conocimiento se pierde (texto libre no
agregable) o se deforma (explosión de categorías). El Concepto **captura lo que el usuario
ya diría**, de forma estructurada y reutilizable, sin pedirle que administre ninguna
taxonomía.

Principios, **en orden de prioridad**:

0. **Simplicidad ante todo (regla #0).** El concepto es **invisible como concepto**: el
   usuario nunca ve la palabra "etiqueta" ni gestiona una lista. Toca un chip de lo que más
   usa, o teclea, y ya. El novato en su primer arranque ve **exactamente** el `QuickAdd` de
   hoy (chips, teclado, guardar). La potencia es **opt-in y emergente**. Ver
   [[simplicity_first_for_user]].
1. **Registrar en segundos sigue siendo el héroe.** El concepto **no añade pasos**: sustituye
   el paso de "elegir categoría" por "elegir/escribir concepto", que además infiere la
   categoría. Caso repetido (cerveza): teclear monto → tocar chip "Cerveza" → Guardar. **Dos
   toques.**
2. **La categoría no desaparece.** Sigue siendo la estructura: presupuesto, donut, Engel,
   esencial/discrecional. El concepto **vive dentro** de una categoría (la que infiere) y la
   enriquece, no la reemplaza.
3. **Cero taxonomía que mantener.** El usuario no crea ni ordena conceptos a mano. Se crean
   solos al teclearlos; los frecuentes ascienden a chips; los normaliza una clave para que
   variantes sumen juntas.
4. **Honesta con el alcance.** Hoy **no existe** ningún concepto/etiqueta en el repo: la única
   clasificación es `CategoryRef` (obligatoria) en
   [`Transaction.kt`](../../src/transactions/src/commonMain/kotlin/within/means/transactions/domain/Transaction.kt).
   Todo lo aquí descrito es `[nuevo-dominio]`/`[nuevo-ui]` salvo lo marcado `[existe]`.

> Principio rector: **el concepto es lo que el usuario teclearía de todos modos, vuelto
> consultable.** No es un campo más; es el campo que hoy ya rellenan (la descripción) pero
> que por fin se puede sumar.

---

## 2. El problema (por qué ni un campo actual lo resuelve)

El modelo clasifica en **una sola dimensión: `CategoryRef`** (obligatoria). Los tres casos del
cliente **no son categorías**:

| Caso del cliente | Qué es realmente | Por qué la categoría falla |
|---|---|---|
| "cuánto en **cerveza**" | un **ítem/producto** | vive dentro de "Mercado/Comida"; hacerlo categoría rompe el donut y explota la taxonomía |
| "cuánto en **papá**" | una **persona/beneficiario** | cruza varias categorías (transporte de papá, comida de papá) |
| "cuánto en **el bus de casa al trabajo**" | un **trayecto/contexto** recurrente | vive dentro de "Transporte"; es una instancia, no una clase |

Salidas actuales, ambas malas:
- **Categoría por ítem** → explosión de categorías; contradice la regla #0.
- **Texto libre en `description`** (≤140) → hoy solo es buscable por substring en
  [`TransactionsListViewModel.kt`](../../apps/android/src/main/kotlin/within/means/android/ui/transactions/TransactionsListViewModel.kt);
  `birra` y `cerveza` **no suman**, no hay agregación fiable.

El `QuickAdd` actual ([`quickadd.png`](project/screenshots/quickadd.png),
`screens-txn.jsx`) refuerza el problema: sus chips son **categorías** ("Mercado", "Alquiler",
"Transporte"…) y el label dice *"en Mercado"*. Ya hay un gesto de 2 toques, pero **encadenado
a la categoría**, que es demasiado gruesa para las preguntas del cliente.

---

## 3. Qué es un Concepto (modelo conceptual)

Un **Concepto** es una etiqueta corta, libre y reutilizable que describe **en qué** fue el
movimiento, más fina que la categoría.

- **Es:** "Cerveza", "Papá", "Bus casa→trabajo", "Café", "Gasolina", "Mercado".
- **No es:** una categoría (eso es la estructura), ni una cuenta, ni un presupuesto, ni un
  modelo de origen/destino estructurado (el trayecto se captura como **texto del label**:
  "Bus casa→trabajo"; ver No-objetivos §11).

Propiedades clave:

| Propiedad | Detalle |
|---|---|
| **Label** | lo que ve el usuario, tal cual lo tecleó la 1ª vez ("Cerveza 🍺" se permite). 1–40 chars. |
| **Clave normalizada** | `key = trim · lower · sin tildes · sin emoji · espacios colapsados` → identidad para sumar. `"Cerveza"`, `"cerveza"`, `"  CERVEZA "` ⇒ misma `key`. Variantes léxicas (`birra`) **no** se fusionan automáticamente; se ofrece *merge* manual (post-MVP, §10-D). |
| **Categoría por defecto** | la que infiere (D0.3). Aprendida del 1er uso; editable. |
| **Uso** | `usageCount`, `lastUsedAt` → ordenan los chips de "frecuentes/recientes". |
| **Tipo** | un concepto se asocia a EXPENSE/INCOME (no se mezclan chips de gasto e ingreso). |

**Cardinalidad (D0.2):** un movimiento tiene **0..N** conceptos. 0 es válido (no obliga). El
**primer** concepto añadido es el que **propone la categoría**; los demás son etiquetas puras
que **no** cambian la categoría.

---

## 4. Captura — `QuickAdd` evolucionado `[restyle]`

> Evolución del `QuickAdd` existente, **no** una pantalla nueva. El esqueleto (segmented
> Gasto/Ingreso/Ahorro, importe gigante, teclado, botón Guardar) **no cambia**. Lo que
> cambia: la **fila de chips pasa de categorías a conceptos**, y el label "en ___" refleja
> el/los concepto(s).

```
┌─────────────────────────────────────────────┐
│   Gasto      Ingreso      Ahorro             │  ← segmented [existe]
│                                              │
│                 GASTAS                       │  ← eyebrow [existe]
│               $ 42,80                        │  ← importe hero [existe]
│            en Cerveza · Papá                 │  ← [restyle] muestra conceptos elegidos
│                                              │
│  [+ ¿En qué?]  〔Cerveza〕〔Mercado〕〔Bus..  │  ← [restyle] chips = CONCEPTOS frecuentes
│                                                  + campo de texto para teclear/crear
│   1     2     3                              │
│   4     5     6                              │  ← teclado [existe]
│   7     8     9                              │
│   .     0     ⌫                              │
│                                              │
│   ⌄ Detalles            ✓ Guardar $42,80     │  ← Detalles = expander [restyle]
└─────────────────────────────────────────────┘
```

### 4.1 Comportamiento

- **Chips = conceptos frecuentes** del tipo activo (Gasto/Ingreso), ordenados por
  `usageCount`/`lastUsedAt`. Tocar un chip lo **selecciona** (se rellena, pasa a estado
  `brand`/`on-brand`); vuelve a tocarlo para quitarlo. Se pueden seleccionar **varios**
  (D0.2). El label "en ___" lista los seleccionados ("en Cerveza · Papá").
- **Crear al vuelo:** el campo **"¿En qué?"** (placeholder) acepta texto. Al escribir,
  **autocompleta** sobre conceptos existentes (por `key`/prefijo). Si no existe, ofrece
  *"Crear «Birra»"* → se crea el concepto y queda seleccionado.
- **Inferencia de categoría (D0.3):** al seleccionar el **primer** concepto, la categoría del
  movimiento se fija a su **categoría por defecto**, en silencio. El usuario **no ve un
  selector de categoría** en el camino feliz.
  - Si el concepto es **nuevo** y no tiene categoría: aparece **inline** un mini-selector
    compacto de categoría (chips de las categorías del usuario, con una **sugerencia
    resaltada**; ver §8). Un toque y sigue. Se puede **omitir** → cae a categoría *Otros*
    (`fallbackCategory`) y se podrá afinar luego. Esta es la **única** fricción extra, y solo
    **la 1ª vez** que se usa un concepto nuevo. (Ver decisión §10-A.)
- **Sin concepto:** Guardar funciona igual con 0 conceptos. Si no hay concepto y la categoría
  no está resuelta, se usa *Otros* (no se bloquea el guardado por exigir categoría — cambia el
  invariante actual; ver §6.3 y §10-B).
- **Detalles (expander)** `[restyle]`: oculto por defecto. Contiene lo "avanzado": **categoría
  (override manual)**, fecha, hora, descripción libre, recurrente/frecuencia. Hoy todo esto
  está plano en `TxnEditor`; aquí se relega para que `QuickAdd` quede en monto+concepto.

### 4.2 Widgets y accesos rápidos del sistema `[nuevo-ui]`

Llevar la captura **fuera de la app**, a la pantalla de inicio del teléfono, para que añadir un
gasto/ingreso sea un gesto sin abrir la app entera. Vive **solo en `apps/android`** (Glance +
shortcuts); **no toca dominio** (deep-link al mismo `QuickAdd`/use-case de Guardar). Respeta
[[respect_architecture]]: es infraestructura/UI de la app.

**Stack: widgets modernos y nativos (no RemoteViews/XML legacy).**

- **Jetpack Glance** (`androidx.glance:glance-appwidget` + `androidx.glance:glance-material3`)
  — widgets escritos en **Compose**, no en `RemoteViews`/XML. Es el framework nativo actual de
  Google para widgets. Encaja con que la app ya es Compose + Material 3 (BOM 2024.12.01). Hay
  que **añadir las dependencias** (hoy no están) al version catalog y a `apps/android`.
  `GlanceAppWidget` + `GlanceAppWidgetReceiver` declarado en el manifest.
- **Material 3 + Material You (color dinámico).** Vía `glance-material3`: `GlanceTheme` con
  `ColorProviders` que toman el **color dinámico del wallpaper en Android 12+ (API 31+)**. En
  **API 23–30** (la app es `minSdk 23`) no hay Monet → **fallback a la paleta de marca** de
  [`SPEC.md` §2](SPEC.md) (verde `#3F8F6B`, terracota `#C25B47`, off-white). El **botón
  primario de acción** conserva el **acento de marca** aunque haya color dinámico, para que la
  identidad sobreviva (ver decisión §10-G).
- **Tamaños adaptativos nativos** — `SizeMode.Responsive` con un set de tamaños declarados
  (2×1 solo botones · 4×1 botones+chips · 4×2 +disponible+último). Glance elige el layout por
  el tamaño real de la celda; nada de un único layout estirado. `targetCellWidth/Height`
  (API 31+) para el tamaño por defecto en el picker.
- **Tema claro/oscuro** automático (Glance respeta el modo del sistema), bordes redondeados
  del sistema (`@android:dimen/system_app_widget_*`, API 31+), y **icono monocromo/temático**
  del shortcut para integrarse con iconos temáticos de Android 13+.
- **Refresco** ligero por `WorkManager`/`GlanceAppWidget.update` solo del snapshot no sensible
  (disponible, último movimiento). El widget **nunca** abre la DB cifrada (ver §4.2-2).
- **Versiones** a fijar en `libs.versions.toml`: Glance estable compatible con
  `compileSdk 35`/Compose BOM 2024.12.01 (p.ej. `glance = 1.1.x`). Confirmar en implementación.

Tres niveles, de menos a más ambicioso:

1. **App Shortcuts (long-press del icono)** — lo más barato, nativo. Long-press del icono de
   la app ofrece **"Nuevo gasto"** y **"Nuevo ingreso"**, que abren `QuickAdd` con el tipo
   preseleccionado. **Estáticos** en `res/xml/shortcuts.xml` + **dinámicos** vía
   `ShortcutManagerCompat` con los **conceptos más usados** ("Nuevo · Cerveza", deep-link con
   el concepto ya seleccionado). Iconos **adaptativos** (foreground/background) y monocromos
   para integrarse con los iconos temáticos de Android 13+. Cero superficie visual extra.

2. **Widget de inicio — "Añadir rápido"** (Glance/AppWidget) `[nuevo-ui]`:
   ```
   ┌───────────────────────────────┐
   │  Within Means                 │
   │  Disponible  $407,75          │  ← opcional, reusa hero (si hay presupuesto)
   │ ┌──────────┐  ┌────────────┐  │
   │ │ − Gasto  │  │ + Ingreso  │  │  ← dos botones grandes → deep-link a QuickAdd
   │ └──────────┘  └────────────┘  │
   │ 〔Cerveza〕〔Mercado〕〔Bus..〕  │  ← chips de conceptos frecuentes (opcional)
   │   $42,80 · Mercado · hoy      │  ← último movimiento (eco de confirmación)
   └───────────────────────────────┘
   ```
   - **Tamaños:** 2×1 mínimo (solo los dos botones); 4×2 con disponible + chips + último
     movimiento.
   - **Botón Gasto/Ingreso** → abre `QuickAdd` (overlay) con `type` ya fijado.
   - **Chip de concepto** → abre `QuickAdd` con ese concepto **ya seleccionado** (su categoría
     inferida) → solo falta teclear el monto. Es el "dos toques" llevado al home del sistema.
   - El widget **no escribe en la DB directamente** (la DB está cifrada con SQLCipher y la
     clave deriva del PIN; ver [[BIOMETRIC-UNLOCK-SPEC]] y `persistence/overview.md`): siempre
     **deep-linkea** al `QuickAdd` dentro de la app desbloqueada. Datos mostrados en el widget
     (disponible, último) son de un **snapshot/cache no sensible**; si la app está bloqueada,
     el widget muestra los botones pero **oculta importes** ("•••").

3. **Quick Settings Tile** (post-MVP, §10-G) — un `TileService` nativo (API 24+) en el panel
   de ajustes rápidos que lanza `QuickAdd` de gasto. Barato si los shortcuts ya existen.

> Regla #0 aplicada: el widget **no es obligatorio ni intrusivo**. Por defecto la app no
> fuerza nada; el usuario que lo quiera lo añade desde el cajón de widgets. El camino sigue
> siendo monto→concepto→guardar, ahora también desde el home del sistema.

**Decisión abierta §10-G:** ¿widget muestra "Disponible" (requiere presupuesto, aún
`[nuevo-dominio]` no construido — ver `SPEC.md` §4.1) o arranca **solo con los dos botones +
chips** para no bloquear el widget por el presupuesto? *Propuesta: arrancar con botones+chips
(no depende de presupuesto); el bloque "Disponible" se enciende cuando exista presupuesto.*

### 4.3 Migración del chip-row actual (categorías → conceptos)

Para que el día 1 sea idéntico al de hoy y nadie pierda nada:
- **Seed de conceptos = categorías por defecto.** Al sembrar las categorías
  (`DefaultCategoriesSeeder`), se siembra **un concepto por categoría** con el mismo nombre y
  esa categoría como `defaultCategory`. Así "Mercado", "Transporte", etc. siguen siendo chips
  — pero ahora son **conceptos** que infieren su categoría. Estrictamente más potencia, cero
  fricción nueva.
- A medida que el usuario teclea conceptos finos ("Cerveza"), estos se añaden y ascienden a
  chips por uso, empujando a los genéricos hacia abajo.

### 4.4 Captura rápida por lotes — "vaciar la cesta" `[nuevo-ui]`

> **Por qué existe (caso real del cliente):** el usuario vuelve del super y quiere apuntar
> *"patata 90, pan 15, detergente 70, carro ruta1 a ruta2 78"* de una sentada. Eso **no es un
> movimiento con 4 conceptos**: son **4 movimientos** en **3 categorías distintas** (patata→
> Mercado, pan→Mercado, detergente→Hogar/Limpieza, carro→Transporte). Como una transacción
> tiene **una sola** categoría (§6), forzarlo a uno solo es imposible sin romper el modelo. El
> modo lote es la forma de teclearlos **todos rápido** y que cada uno quede como su propio
> movimiento, consultable por concepto.

**Gramática de línea:** `<label libre> <monto>` — el **último token numérico** es el monto; el
resto es el **label del concepto**. Decimales con `,`/`.`. Una línea = un movimiento.

```
patata 90              → concepto "patata"            · monto 90  · cat. inferida Mercado
pan 15                 → concepto "pan"               · monto 15  · cat. inferida Mercado
detergente 70          → concepto "detergente"        · monto 70  · cat. inferida Hogar
carro ruta1 a ruta2 78 → concepto "carro ruta1 a ruta2" · monto 78 · cat. inferida Transporte
```

**Layout** (modo dentro de `QuickAdd`, alterna con el teclado-hero; se llega desde el botón
"Añadir varios" o desde el widget):

```
┌─────────────────────────────────────────────┐
│   Gasto      Ingreso      Ahorro             │  ← type compartido por todas las líneas
│                                              │
│  ┌─────────────────────────────────────┐    │
│  │ escribe: concepto monto…            │ ↵  │  ← campo; Enter añade fila y se limpia
│  └─────────────────────────────────────┘    │
│  🥔 Patata          Mercado          $90  ⌫  │  ← filas ya añadidas (editables)
│  🍞 Pan             Mercado          $15  ⌫  │     cat. inferida visible; tap = corregir
│  🧴 Detergente      Hogar            $70  ⌫  │
│  🚌 Carro ruta1→2   Transporte       $78  ⌫  │
│  ───────────────────────────────────────    │
│  4 movimientos                     Total $253│  ← total corrido
│              ✓ Guardar 4 · $253              │  ← un commit, N movimientos
└─────────────────────────────────────────────┘
```

**Comportamiento:**
- Cada **Enter** parsea la línea, infiere la categoría del concepto (igual que §4.1/§8) y
  añade una **fila editable**. El campo se limpia para la siguiente. Sin salir del teclado.
- Una fila cuyo **concepto es nuevo y sin categoría** muestra su categoría como *Otros* con un
  punto de aviso; un tap abre el mini-selector inline (§4.1). **No bloquea**: se puede guardar
  el lote entero y afinar después.
- **Guardar** emite **N comandos `RegisterTransactionCommand`** (uno por fila), todos con la
  **misma fecha/hora** (la del guardado) y el `type` compartido. No es una transacción con
  líneas: son N transacciones independientes (ver §6.2 — la categoría por movimiento lo exige).
- **Agrupación de la cesta (cerrado, D0.4):** las N comparten un `batchRef` opaco (provenance,
  reusa el patrón de `originRef`/`recurringRef` ya reservado en
  [`Transaction`](../../src/transactions/src/commonMain/kotlin/within/means/transactions/domain/Transaction.kt)).
  La lista las muestra agrupadas ("Compra · 4 movimientos · $253") y permite **deshacer el
  lote** de una vez (borra los N). El `batchRef` es solo agrupación de presentación/undo; **no**
  convierte el lote en un agregado: siguen siendo N transacciones independientes, cada una
  editable/borrable por separado.
- **Multi-concepto sigue disponible** y es ortogonal: dentro de una fila se pueden añadir más
  conceptos a ese movimiento (p.ej. "carro ruta1 a ruta2" + concepto "papá" si lo pagó papá).
  Lote = varios movimientos; multi-concepto = varias etiquetas en un movimiento.

**Pago del ejemplo (lo que desbloquea):** tras esa captura, *"¿cuánto en patata este mes?"*,
*"¿cuánto en detergente?"* y *"¿cuánto en el carro ruta1→ruta2?"* se responden por concepto
(§5), y el donut por categoría reparte los $253 correctamente entre Mercado ($105), Hogar
($70) y Transporte ($78) — sin que el usuario eligiera **ni una** categoría a mano.

**Desde el widget:** el botón "− Gasto" del widget (§4.2) puede abrir directamente este modo
lote (config del widget o un tercer botón "Añadir varios"), de modo que el usuario teclea la
cesta entera sin navegar por la app. Ver §10-G.

---

## 5. Consulta — responder "¿cuánto gasté en X?"

Dos superficies, ninguna nueva en navegación:

### 5.1 Buscador en Movimientos `[restyle]`
- El buscador de [`TransactionsListScreen`](../../apps/android/src/main/kotlin/within/means/android/ui/transactions/TransactionsListScreen.kt)
  **autocompleta conceptos** además del texto libre actual. Escribir "cerv" sugiere el
  **chip-pill "Cerveza"**; al elegirlo, filtra por **concepto** (no por substring) y muestra
  en cabecera **el total del periodo**: *"Cerveza · $63.200 · 14 movimientos"*.
- Esto reusa `SearchTransactionsQuery`
  ([archivo](../../src/transactions/src/commonMain/kotlin/within/means/transactions/application/search/SearchTransactionsQuery.kt))
  añadiéndole `conceptId: String?` (ver §6.4).

### 5.2 Desglose por concepto en Análisis `[nuevo-ui]`
- En `Stats`, junto al breakdown por **categoría** (la partición real, suma 100%), una vista
  **"Conceptos"**: top conceptos del periodo por gasto, con total y tendencia vs. periodo
  anterior. Reusa el patrón de
  [`FindCategoryBreakdownQuery`](../../src/analytics/src/commonMain/kotlin/within/means/analytics/application/find_breakdown/FindCategoryBreakdownQuery.kt)
  con un nuevo `FindConceptBreakdownQuery`.
- **⚠️ No es una partición.** Como un movimiento puede tener varios conceptos (D0.2), un
  mismo importe cuenta **bajo cada uno** de sus conceptos → la suma de conceptos **puede
  exceder** el total del periodo. La UI lo dice explícito: *"Un movimiento puede aparecer en
  varios conceptos"*. El donut de **categoría** sigue siendo la verdad que suma 100%.

---

## 6. Modelo de dominio `[nuevo-dominio]`

### 6.1 Bounded context nuevo: `concepts`
Espejo ligero de `categories`. Vive en `src/concepts/`. Respeta los buses y no acopla otros
contextos.

```
Concept (AggregateRoot)
  id: ConceptId                 // UUID v4
  label: ConceptLabel           // 1..40, lo que ve el usuario
  key: ConceptKey               // normalizada (identidad de agregación)
  kind: ConceptKind             // EXPENSE | INCOME  (no TRANSFER en MVP)
  defaultCategoryId: CategoryId? // inferida; null = sin resolver → Otros
  usageCount: Int
  lastUsedAt: Instant?
  createdAt: Instant
```

Comandos (espejo de `categories`):
- `CreateConceptCommand(label, kind, defaultCategoryId?)`
- `RenameConceptCommand` (cambia label; **no** la `key` si colisiona → ver merge §10-D)
- `SetConceptDefaultCategoryCommand`
- `RecordConceptUsageCommand(conceptId, at)` → incrementa `usageCount`, set `lastUsedAt`
- `DeleteConceptCommand` (solo si `usageCount == 0`, o soft-archive)

Queries:
- `SuggestConceptsQuery(kind, prefix?, limit)` → para chips y autocompletar
- `FindConceptQuery(id)`

Eventos: `ConceptCreated`, `ConceptRenamed`, `ConceptDefaultCategoryChanged`,
`ConceptUsageRecorded`, `ConceptDeleted`.

> **Invariante de unicidad:** no puede haber dos conceptos activos con la misma `(kind, key)`.
> Crear "cerveza" cuando ya existe "Cerveza" **devuelve el existente** (idempotente), no
> duplica.

### 6.2 Referencia desde `transactions`: `ConceptRefs`
`transactions` referencia conceptos **por id**, igual que hace con `CategoryRef`. Nuevo value
object en el dominio de `transactions`:

```kotlin
@JvmInline value class ConceptRefs(val ids: List<String>) {  // 0..N, sin duplicados, orden = relevancia
    init { require(ids.size <= MAX) ; require(ids.distinct().size == ids.size) }
    companion object { const val MAX = 8; val EMPTY = ConceptRefs(emptyList()) }
}
```

Cambios en el agregado
[`Transaction`](../../src/transactions/src/commonMain/kotlin/within/means/transactions/domain/Transaction.kt):
- Nuevo campo `conceptRefs: ConceptRefs` (default `EMPTY`) en `register`, `edit`, `rehydrate`.
- Se incluye en `TransactionRegistered` / `TransactionEdited` (lista de ids).
- **Sin** validaciones cruzadas pesadas: `transactions` **no** conoce el modelo de `concepts`;
  solo guarda ids opacos (límite de contexto). La coherencia (que el id exista, que el tipo
  case) la garantiza la **capa de aplicación** de la app al construir el comando.
- **`batchRef`** (§4.4, **cerrado D0.4**): agrupa los N movimientos de una captura por lotes.
  Encaja en los **refs de provenance ya reservados** (`originRef`/`recurringRef`,
  `ReservedRefs.kt`) — id opaco, sin semántica de dominio nueva; solo agrupa en la lista y
  habilita "deshacer lote". Se incluye en `TransactionRegistered`. Indexar por `batch_ref`
  para listar/borrar el lote.

### 6.3 Cambio de invariante: categoría ya no obligatoria en captura
Hoy `categoryId` es obligatorio en el `QuickAdd`/`TxnEditor`. Con D0.3 la categoría se
**infiere**; cuando no hay concepto resuelto, se usa `fallbackCategory` (*Otros*) sembrada por
defecto. El invariante de dominio (`Transaction` exige `CategoryRef`) **se mantiene** —
siempre hay una categoría—, pero **la UI nunca obliga a elegirla**. Ver §10-B.

### 6.4 Consulta por concepto
- `SearchTransactionsQuery` += `conceptId: String?` → filtra movimientos cuyo
  `conceptRefs` contenga ese id (JOIN, ver §7).
- Nuevo `FindConceptBreakdownQuery(yearMonth | range, kind)` en `analytics` → agrega
  `SUM(amount)` agrupando por `conceptId` vía la tabla puente, con el aviso de no-partición.

---

## 7. Persistencia `[nuevo-dominio]`

SQLDelight + SQLCipher (igual que el resto; ver
[`persistence/overview.md`](../persistence/overview.md)).

- **`Concept.sq`** (context `concepts`): `id PK`, `label`, `key`, `kind`, `default_category_id`,
  `usage_count`, `last_used_at`, `created_at`. Índice único `(kind, key)`; índice por
  `(kind, usage_count DESC, last_used_at DESC)` para los chips.
- **Tabla puente `TransactionConcept.sq`** (context `transactions`):
  `transaction_id`, `concept_id`, `position`. PK `(transaction_id, concept_id)`; índice por
  `concept_id` para "¿cuánto en X?". Es la forma normalizada de un set N-a-N sin acoplar
  esquemas (ids opacos).
- Migración: añadir tablas; las filas existentes quedan con **0 conceptos** (compatible).

---

## 8. Inferencia y aprendizaje (concepto → categoría)

Cómo se rellena `defaultCategoryId` sin pedirle nada al usuario en el camino feliz:

1. **Seed (día 1):** conceptos sembrados desde categorías por defecto ya traen su categoría
   (§4.2). Cobertura inmediata de lo común.
2. **Aprendizaje por uso:** la 1ª vez que se usa un concepto nuevo sin categoría, se fija la
   que el usuario elija (mini-selector inline, §4.1) **o** la sugerida. A partir de ahí,
   ese concepto infiere sola.
3. **Sugerencia para conceptos nuevos** (qué resaltar en el mini-selector), en orden:
   - coincidencia por palabra del label con nombres/sinónimos de categorías existentes
     ("gasolina" → Transporte; "café" → Mercado/Comida), tabla de sinónimos sembrada;
   - si nada casa → `fallbackCategory` *Otros*.
4. **Re-aprendizaje suave:** si el usuario, en `Detalles`, **cambia la categoría** de un
   movimiento que tiene un concepto, se ofrece (toast no intrusivo) *"¿Usar Transporte para
   «Bus» a partir de ahora?"* → actualiza `defaultCategoryId`. Opt-in, nunca automático.

> La inferencia es **heurística local**, sin red ni jerga. Si falla, el peor caso es *Otros* y
> un toque para corregir. Nunca bloquea el guardado.

---

## 9. Orquestación (respeta los límites de contexto)

El "pegamento" concepto↔transacción↔categoría vive en la **capa de aplicación de la app
Android** (orquestador / use-case de UI), **no** dentro de un contexto KMP. Flujo de Guardar
en `QuickAdd`:

1. Resolver cada label de chip/texto a un `conceptId` (crea si no existe →
   `CreateConceptCommand`, idempotente por `(kind,key)`).
2. Determinar `categoryId`: del **primer** concepto resuelto con `defaultCategoryId`; si
   ninguno, `fallbackCategory`.
3. `RegisterTransactionCommand(..., categoryId, conceptRefs = [ids])`.
4. `RecordConceptUsageCommand(conceptId, now)` por cada concepto (vía bus; puede ser
   suscriptor del evento `TransactionRegistered` en la **app**, no en KMP — ver
   [[cross-context-subscribers]] y, para evitar el ciclo de Koin, resolver servicios con
   `get<>()` perezoso, [[koin_cycle_for_event_subscribers]]).

`transactions`, `concepts`, `categories` y `analytics` **no se importan entre sí**; se
comunican por buses. El orquestador conoce a todos porque es app, no dominio.

---

## 10. Decisiones abiertas

- **§10-A — Concepto nuevo sin categoría:** ¿mini-selector inline obligatorio (1 toque) **o**
  guardar directo a *Otros* y afinar luego? *Propuesta: inline con sugerencia resaltada +
  botón "omitir". Equilibra precisión y velocidad.*
- **§10-B — Invariante de categoría:** confirmar que aceptamos *Otros* como categoría real
  sembrada (la UI nunca pide categoría) en vez de relajar el invariante de dominio. *Propuesta:
  sí, *Otros* sembrada; el dominio sigue exigiendo `CategoryRef`.*
- **§10-C — `kind` del concepto y TRANSFER:** ¿conceptos solo para EXPENSE/INCOME en MVP?
  *Propuesta: sí; TRANSFER sin conceptos por ahora.*
- **§10-D — Merge/sinónimos:** fusionar `birra`→`cerveza` y renombrar con re-mapeo de la tabla
  puente. *Propuesta: post-MVP; en MVP solo unicidad por `key` normalizada.*
- **§10-E — Tope de conceptos por movimiento (`ConceptRefs.MAX`):** 8 propuesto. ¿Suficiente?
- **§10-F — Doble conteo en Análisis:** ¿basta el aviso textual, o además ofrecemos un modo
  "repartir importe entre conceptos"? *Propuesta: solo aviso en MVP; el reparto confunde.*
- **§10-G — Contenido del widget de inicio:** ¿el widget muestra "Disponible" (depende de
  presupuesto aún no construido) o arranca solo con los dos botones + chips de conceptos?
  *Propuesta: arrancar con botones+chips; el bloque "Disponible" se enciende cuando exista
  presupuesto.* ¿Incluimos también Quick Settings Tile (§4.2-3) en MVP o lo dejamos post-MVP?
  **Color (Material You vs marca):** ¿el widget usa **color dinámico** del wallpaper en API 31+
  (sensación más nativa/moderna) o **paleta de marca** siempre (identidad fintech consistente)?
  *Propuesta: color dinámico para superficies/fondos en API 31+ con fallback de marca en 23–30,
  pero el **botón de acción primario siempre con el verde de marca** — lo mejor de ambos.*
- ~~**§10-H — Agrupación de lote (cesta):**~~ **Cerrado (D0.4):** sí, `batchRef` opaco que
  agrupa la cesta en la lista y permite "deshacer lote"; reusa los refs de provenance ya
  reservados. Sin semántica de dominio pesada.

---

## 11. No-objetivos (MVP)

- **No** se modela origen/destino estructurado de un trayecto. "Bus casa→trabajo" es **texto
  del label** del concepto, consultable como cualquier otro. (Modelado de rutas = fuera.)
- **No** hay jerarquía de conceptos (padre/hijo). Son planos.
- **No** hay fusión automática por similitud léxica (`birra`≈`cerveza`); solo identidad por
  `key` normalizada. Merge manual es §10-D.
- **No** se tocan `accounts`, `budgets` ni presupuesto: la categoría sigue siendo el ancla de
  esos contextos (presentes o futuros).
- **No** hay conceptos para TRANSFER en MVP (§10-C).

---

## 12. Fases de implementación

> **Estado:** F1–F7 **entregadas** y verificadas por build + tests unitarios + **prueba en
> emulador** (instalación del APK debug y recorrido end-to-end). Marcadas ✅ abajo, con las
> desviaciones respecto al plan original.
>
> **Dos bugs de integración encontrados en la prueba en dispositivo (no los veían los tests
> unitarios), ya corregidos:**
> 1. `koinInject<QueryBus>()` eager en la raíz de `WithinMeansApp` construía todos los
>    handlers→repos→DBs **antes del desbloqueo** → crash "…requested before unlock()". Fix:
>    resolver el `QueryBus` perezosamente dentro del efecto (solo en Home).
> 2. `singleOf(::RecordConceptUsageCommandHandler)` fallaba porque su ctor tiene
>    `clock: Clock = Clock.System` y el DSL `*Of` de Koin no respeta defaults de Kotlin →
>    `NoDefinitionFoundException(Clock)` al construir el `CommandBus` → todo guardado fallaba.
>    Fix: factory explícito `single { RecordConceptUsageCommandHandler(get(), get(), get()) }`
>    (ver [[clock_injection_in_viewmodels]]). Se añadió `ConceptsModuleWiringTest` (arranca
>    Koin con `conceptsModule`+fakes y resuelve cada definición) como **guard de regresión**
>    que caza esta clase de bug sin necesidad de dispositivo.

| Fase | Entregable | Estado |
|---|---|---|
| **F1 — Dominio `concepts`** | Agregado `Concept`, value objects, comandos/queries/eventos, `Concept.sq`, repos | ✅ `src/concepts/` + tests (`ConceptKeyTest`, `ConceptTest`, `ConceptCreatorTest`, repo SQL). El *seed desde categorías* se movió a F6 (es cross-context, vive en app). |
| **F2 — Enlace en `transactions`** | `ConceptRefs` + `batchRef` en `Transaction` (register/edit/rehydrate/eventos), tabla puente `TransactionConcept.sq`, migración in-place, `SearchTransactionsQuery += conceptId` | ✅ `src/transactions/` + tests (`ConceptRefsTest`, `TransactionConceptsTest`, repo SQL round-trip/search). |
| **F3 — Orquestación app** | `MovementCaptureService` (resolver labels→ids, inferir categoría, registrar; +`registerBatch` con `batchRef`), `FallbackCategoryResolver` (find-or-create "Otros"), suscriptor `TransactionRegistered`→`RecordConceptUsage` (lazy, sin ciclo Koin) | ✅ `apps/android/capture` + tests. |
| **F4 — `QuickAdd` conceptos** | Chips=conceptos, campo "¿En qué?", multi-selección, `Detalles` expander con categoría override | ✅ en **ambas** superficies: `TransactionEditScreen` (editor) **y** `QuickAddSheet` (héroe del FAB). **Desviación (§10-A):** no se hizo mini-selector inline para concepto nuevo; se usa inferencia → "Otros" (la UI nunca pide categoría). |
| **F4b — Captura por lotes** | `batchRef` compartido + `registerBatch` (parseo `concepto monto`, categoría inferida por línea) | ⚠️ **Parcial:** el **backend** está (`registerBatch` + `batchRef` + tests). Falta el **modo lista UI** ("vaciar la cesta" con filas + "deshacer lote" en Movimientos). |
| **F5 — Consulta** | Filtro por concepto en Movimientos (búsqueda por label + total visible); `FindConceptBreakdownQuery`/`InRange` + lente "Conceptos" en Stats con aviso de no-partición | ✅ `analytics` + `TransactionsList*` + `StatsScreen` + tests. |
| **F6 — Aprendizaje** | `ConceptCategorySuggester` (sinónimos→Engel + match por nombre) enchufado a la captura; seed day-1 de conceptos desde categorías (suscriptor); re-aprendizaje opt-in | ✅ + tests. **Desviación:** el re-aprendizaje es una **tarjeta inline** en *Detalles* (no un toast). Seed solo en onboarding (instalaciones existentes pueblan por uso). |
| **F7 — Widgets y accesos rápidos** | Deps **Glance** + widget "Añadir rápido" (Material You, deep-link); App Shortcuts estáticos+dinámicos (`ShortcutManagerCompat`, conceptos top); deep-link `transactions/new?type=&concept=` con espera tras desbloqueo | ✅ **verificado en emulador**: deep-link abre el editor con tipo preseleccionado; shortcuts estáticos y widget provider **registrados** (`dumpsys`). Warm-start del deep-link no cubierto (solo arranque en frío). `TileService` post-MVP. |

> Cada fase fue entregable y testeable de forma independiente. El "héroe" del FAB
> (`QuickAddSheet`) y el editor completo (`TransactionEditScreen`) son **dos superficies**
> distintas; ambas comparten ahora el flujo de conceptos vía `MovementCaptureService`.
>
> **Pendiente conocido:** (1) F4b modo-lista UI ("vaciar la cesta" + deshacer lote);
> (2) warm-start del deep-link (solo arranque en frío cubierto); (3) seed de conceptos
> para instalaciones existentes (hoy solo dispara en el onboarding); (4) `TileService`
> (§4.2-3) post-MVP; (5) merge/sinónimos de conceptos (§10-D) post-MVP.
>
> F7 quedó **verificado en emulador** (deep-link, shortcuts estáticos y widget provider
> registrados vía `dumpsys`); los 2 bugs de integración hallados están corregidos y
> blindados por `ConceptsModuleWiringTest`.
