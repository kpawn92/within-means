# Within Means — Especificación de rediseño (spec-driven)

> **Fuente de verdad:** el bundle de Claude Design en `doc/within-means-app/project/`
> (HTML/CSS/JSX). Este documento traduce ese prototipo a una especificación
> implementable en la app Android real (Compose + Material 3), con análisis de
> brechas contra el código actual y un plan por fases. **No se implementa nada
> hasta cerrar las "Decisiones abiertas" (§7).**

Convención de etiquetas por requisito:
- `[existe]` ya está en la app, solo necesita re-estilado visual.
- `[restyle]` existe pero cambia de forma/layout notablemente.
- `[nuevo-ui]` nuevo en UI, no requiere cambios de dominio.
- `[nuevo-dominio]` requiere modelo/persistencia nueva (fuera del MVP actual).
- `[mock]` en el prototipo es dato simulado; decidir si se cablea o se aparca.

---

## 1. Visión y principios

Fintech propia, **cálida + sobria + minimalista**. Acompaña a "gastar con calma,
sin culpa y sin hojas de cálculo". Tres pilares que la UI debe reforzar:

1. **Registrar en segundos** — el gesto de añadir un movimiento es el héroe (QuickAdd con teclado numérico).
2. **Saber cuánto puedo gastar** — presupuesto mensual, disponible, ritmo/día.
3. **Entender a dónde va** — donut por categoría, evolución, tasa de ahorro.

---

## 2. Sistema de diseño (tokens)

El prototipo usa **oklch** por uniformidad perceptual. Abajo el intento + un hex
aproximado para Compose. La conversión exacta oklch→sRGB se hace en implementación;
los hex de marca/paleta son los que da el propio diseño (exactos).

### 2.1 Color — tema claro
| Token | oklch (origen) | hex aprox. | Uso |
|---|---|---|---|
| `bg` | 0.972 0.006 95 | `#F7F6F1` | fondo cálido off-white |
| `surface` | 0.995 0.003 95 | `#FEFDFB` | tarjetas |
| `surface-2` | 0.95 0.008 95 | `#EFEDE6` | segmented, tracks |
| `surface-3` | 0.925 0.01 95 | `#E6E3DB` | barras inactivas |
| `ink` | 0.26 0.012 110 | `#34322F` | texto principal |
| `ink-2` | 0.44 0.012 110 | `#5E5A54` | texto secundario |
| `muted` | 0.62 0.012 110 | `#8C877E` | terciario |
| `faint` | 0.74 0.01 110 | `#B1ABA2` | iconos tenues |
| `border` | 0.89 0.008 100 | `#DEDAD1` | bordes |
| `border-strong` | 0.82 0.01 100 | `#C8C3B9` | bordes/grips |
| `brand` | 0.52 0.085 156 | **`#3F8F6B`** | verde marca (= `pos`) |
| `brand-strong` | 0.45 0.09 156 | `#2F7A57` | énfasis/acento texto |
| `brand-soft` | 0.94 0.03 156 | `#E2F0E8` | fondos suaves de marca |
| `on-brand` | 0.98 0.01 120 | `#FAFEF7` | texto sobre marca |
| `neg` | 0.56 0.13 33 | `#C25B47` | gasto/negativo (terracota, NO rojo puro) |
| `neg-soft` | 0.95 0.03 33 | `#F4E2DC` | fondo negativo suave |

### 2.2 Color — tema oscuro
| Token | hex aprox. |
|---|---|
| `bg` `#161814` · `surface` `#1F221D` · `surface-2` `#272A24` · `surface-3` `#30332C` |
| `ink` `#F0EFEB` · `ink-2` `#CBC9C2` · `muted` `#9C988F` · `faint` `#7B776F` |
| `border` `#30332C` · `border-strong` `#3E4239` |
| `brand` `#5FBE8E` · `brand-strong` `#7FD0A4` · `brand-soft` `#22402F` · `on-brand` `#13241A` |
| `neg` `#D98162` · `pos` `#5FBE8E` |

> El acento es **configurable** en el prototipo (Tweaks): `#3F8F6B`, `#2F6F5B`,
> `#3E7A8C`, `#5B6BB4`, `#A65D3C`, `#7A6BB1`. Ver §7-D si lo exponemos en Ajustes.

### 2.3 Tipografía
- Familia: **Hanken Grotesk** (sans). Mono: **JetBrains Mono** (eyebrows + importes a veces).
- Escala: `h1` 27/700 (-0.02em), `h2` 20/700, `h3` 16/600, `body` 15 (`ink-2`, lh 1.45), `small` 13 (`muted`), `eyebrow` 11 mono mayúsculas +0.12em (`muted`).
- Importes: `tabular-nums`, -0.02em, peso 700 (clase `.amount`/`.tnum`).
- Decisión de fuente para Android en §7-D.

### 2.4 Forma, sombra, espaciado
- Radii: `sm` 10 · `md` 16 · `lg` 24 (configurable) · `xl` 32. Tarjetas usan `lg`; el hero card 28; FAB 20.
- Sombras: `shadow` suave en tarjetas, `shadow-lg` en sheets/diálogos.
- Densidad escalable (`--d`, 0.82–1.18) — en Android lo trataremos como un único valor base (no slider) salvo §7-D.
- Padding de contenido: 20px lateral; respeto de safe-area / insets del sistema.

### 2.5 Componentes base (Compose equivalentes)
- **Card**: `surface` + borde `border` + `shadow`, radio `lg`, padding 18.
- **Segmented** (pill): pista `surface-2`, botón activo `surface` con sombra + texto `ink`; inactivo `muted`.
- **Chip**: borde `border`; activo `brand`/`on-brand`.
- **Button**: primario `brand`/`on-brand` con sombra de color; ghost `surface`+borde. Radio `md`, peso 700, `scale(0.97)` al pulsar.
- **CatIcon**: cuadrado redondeado (42/34px, radio 13/11) con color de categoría e icono blanco.
- **Bar** (track/fill) para barras de progreso/desglose.
- **Donut** (SVG→Canvas) con segmentos redondeados y hueco; usado en Home y Stats.
- **Toggle**, **Toast** (auto-dismiss ~2.2s, icono de marca), **Dialog** de confirmación, **sheet** (bottom sheet con grip) y **fullover** (pantalla completa que sube).

---

## 3. Navegación e información

Pestañas (prototipo, `BottomNav`): **Inicio · Movimientos · [+] · Análisis · Categorías**, con
**Ajustes** accesible desde avatar/topbar (no es tab). El **[+] central** abre `QuickAdd`.

> ⚠️ **Conflicto con el trabajo recién hecho:** la app ahora usa un *rail lateral
> izquierdo expandible*. El diseño usa *bottom nav con FAB central*. Ver §7-A.

Overlays (no son rutas de tab):
- `QuickAdd` — bottom sheet.
- `TxnEditor` / `CatEditor` — fullover (pantalla completa que sube).
- `Onboarding` / `Unlock` — fullover.
- `Toast`, diálogos de confirmación.

Nombre de pestaña "Análisis" (el diseño) vs "Estadísticas" (app actual) — adoptar **"Análisis"**.

---

## 4. Especificación por pantalla

### 4.1 Home / Inicio  (`screens-home.jsx` → `Home`)
Saludo con eyebrow "Junio · {nombre}" + "Buenas tardes 👋" + avatar (iniciales) que abre Ajustes.
Bloques (con entrada escalonada `.enter`):
1. **Hero "Disponible"** `[nuevo-dominio]` — tarjeta degradada de marca: importe disponible (= plan − gastado), badge "Dentro del plan / Atención", barra de progreso gastado/plan, labels "Gastado X" / "Plan Y". Requiere **presupuesto mensual** (no existe en dominio).
2. **Ritmo sugerido** `[nuevo-dominio]` — "X/día · N días restantes" (disponible ÷ días que quedan del mes).
3. **Donut "En qué va el mes"** `[restyle]` — donut de gastos por categoría + top-4 con %; enlace "Ver todo" → Movimientos. (Hoy Stats hace esto sin donut.)
4. **Reciente** `[existe]` — últimas 4, fila = CatIcon + desc + "categoría · fecha relativa" + importe con signo/color. (≈ HomeViewModel actual.)

Estado vacío: ya existe ("Aún no hay movimientos…").

### 4.2 Movimientos  (`screens-txn.jsx` → `Txns`)  `[restyle]`
- Topbar con título + "X en junio" (gasto del mes).
- **Buscador** `[nuevo-ui]` (texto libre sobre desc/categoría).
- **Chips de filtro**: Todos / Gastos / Ingresos / **Ahorro** (`transferencia`, §7-C).
- **Agrupación por día** con etiqueta relativa (Hoy/Ayer/"mié 4"/"2 jun") y **total del grupo** con signo.
- Fila de transacción idéntica a Home; subtítulo añade `· fuente` si la hay (ingresos).

### 4.3 QuickAdd  (`screens-home.jsx` → `QuickAdd`)  `[nuevo-ui]` (héroe)
Bottom sheet de entrada rápida:
- Segmented tipo: Gasto / Ingreso / **Ahorro**.
- **Display de importe gigante** (52px) con símbolo de moneda; color según tipo (gasto=`neg`, ingreso=`pos`, ahorro=`brand`).
- Chips de categoría (scroll horizontal) filtrados por tipo; tap asigna al instante.
- Extras plegables: **nota** + **cuándo** (Hoy/Ayer/Otra fecha).
- **Teclado numérico** propio (1-9, ".", 0, ⌫) con límite 2 decimales y 9 enteros.
- CTA: "Guardar $X" (deshabilitado hasta importe>0 + categoría). Toast al guardar.

> Diferencia clave con el `TransactionEditScreen` actual (formulario con teclado del sistema). QuickAdd es el flujo rápido; el editor completo (4.5) sigue existiendo para editar.

### 4.4 Análisis  (`screens-stats.jsx` → `Stats`)  `[restyle]`
- Segmented periodo: **Semana / Mes / Año** (`[nuevo-ui]`: hoy solo "mes actual"; ver §7-C).
- Trío resumen: Ingresos / Gastos / **Ahorro** (= ingresos−gastos).
- **Tasa de ahorro** `[nuevo-ui]` — donut pequeño con % + frase ("Guardaste X… vas mejor que el mes pasado" — el "mes pasado" es `[mock]`).
- **Evolución del gasto** `[restyle]` — barras (6 semanas en el mock); hoy es gráfica de líneas de 6 meses. Definir granularidad real.
- **Desglose** con lente **Categoría / Tipo** (esencial vs discrecional) — barras horizontales. (≈ breakdown actual + el split esencial/discrecional que ya calcula el dominio.)

### 4.5 Editor de movimiento  (`screens-txn.jsx` → `TxnEditor`)  `[restyle]`
Fullover: topbar (cerrar / título / borrar). Segmented tipo. **Importe grande editable** (color = categoría). Picker de categoría en chips horizontales con CatIcon. Descripción. **Fecha** (botón → DatePicker, ya tenemos `DateField`). **Toggle "Recurrente"** `[nuevo-dominio/reservado]`. Barra inferior "Añadir/Guardar". Diálogo de borrado.

### 4.6 Categorías  (`screens-cat.jsx` → `Cats` + `CatEditor`)  `[restyle]`
- Lista en **grid 2 columnas**: tarjeta con CatIcon, badge de naturaleza (FIJO/VARIABLE), nombre y "X este mes / Sin movimientos". Tarjeta "Nueva" punteada al final.
- Tabs: Gastos / Ingresos / **Ahorro**.
- Editor (fullover): **preview en vivo** (icono 72px + nombre), nombre, tipo, **paleta de color** (grid 7), **grid de iconos**, y para gasto: Naturaleza (Fijo/Variable) + **Prioridad (Esencial/Discrecional)**. Guardar / diálogo borrar. (≈ `CategoryEditScreen` actual, re-estilado y reordenado.)

### 4.7 Ajustes  (`screens-misc.jsx` → `Settings`)  `[restyle]` (+ varios `[mock]`)
Tarjeta de perfil (avatar iniciales + nombre + email). Secciones:
- **Preferencias**: Apariencia (toggle claro/oscuro `[nuevo-ui]`), Moneda `[existe]`, Idioma `[existe]`, Inicio del mes `[mock]`.
- **Presupuesto** `[nuevo-dominio/mock]`: Plan mensual, Alertas de gasto, Recurrentes.
- **Seguridad** `[mock/parcial]`: Bloqueo con PIN, Ocultar importes al abrir.
- Botones: **Bloquear ahora** `[nuevo-ui]`, Ver introducción (relanzar onboarding). Footer versión.

### 4.8 Onboarding  (`screens-misc.jsx` → `Onboarding`)  `[restyle]`
3 slides con **arte ilustrativo** (tarjeta balance / keypad / barras), título a 2 líneas, body, **dots de progreso** animados, CTA Siguiente/Empezar + "Saltar". (Hoy es Welcome→PIN→Preferencias; el diseño separa la *introducción* del *setup* — ver §7-E sobre cómo encajan PIN y preferencias.)

### 4.9 Unlock (PIN)  (`screens-misc.jsx` → `Unlock`)  `[restyle]`
Icono candado en chip de marca, saludo "Hola de nuevo, {nombre}", **4 puntos** de PIN + **keypad propio** (no campo de texto del sistema), animación *shake* + estado de error al fallar.

> ⚠️ El diseño usa **PIN de 4 dígitos**; la app actual usa **6 dígitos**. Ver §7-B.

---

## 5. Microinteracciones y motion
- Entrada escalonada de bloques (`enterUp`, delays .04–.16s).
- Count-up de importes (Home hero/donut).
- Sheets suben (`rise`), fullovers (`slideup`), diálogos (`pop`), scrim con blur.
- Botones `scale(0.97)`; FAB `scale+rotate` al pulsar.
- Transiciones de barras/donut (.6–.8s ease) al cambiar datos.
- `prefers-reduced-motion` → animaciones casi nulas (respetar el ajuste de accesibilidad de Android).

---

## 6. Análisis de brechas (diseño ↔ app actual)

| Área | App hoy | Diseño | Etiqueta |
|---|---|---|---|
| Navegación | Rail lateral izq. expandible (recién hecho) | Bottom nav + FAB central | **conflicto §7-A** |
| Tema | Dynamic color (Material You) + dark por sistema | Paleta de marca propia (cálida) + toggle claro/oscuro manual | decisión §7-D |
| Tipos de movimiento | INCOME / EXPENSE | + **Ahorro/transferencia** | **nuevo-dominio §7-C** |
| Presupuesto mensual | No existe | Plan, disponible, ritmo/día, alertas | **nuevo-dominio §7-C** |
| Añadir | Formulario completo | QuickAdd con keypad numérico | nuevo-ui |
| PIN | 6 dígitos, campo de texto | 4 dígitos, keypad propio + 4 dots | **conflicto §7-B** |
| Análisis | Resumen/categoría/evolución (líneas, 6 meses) | + tasa de ahorro, evolución en barras, lente Tipo, periodos S/M/A | restyle + nuevo-ui |
| Categorías | Lista por tabs | Grid 2-col con badges + "X este mes" | restyle |
| Recurrente | Reservado (siempre null) | Toggle en editor + "3 activos" | nuevo-dominio (reservado) |
| Moneda | EUR/USD/CUP, una base | Símbolo $; formato es-ES | alinear |

Lo que **encaja directo** (re-estilado, sin tocar dominio): Home reciente, lista de
movimientos, editor de categoría, breakdown por categoría y el split
esencial/discrecional (el dominio ya lo calcula), editor de transacción, onboarding,
unlock (salvo nº de dígitos).

---

## 7. Decisiones (RESUELTAS — 2026-06-09)

**A. Navegación → Barra inferior + FAB central (fiel al diseño).** Se retira el rail
lateral expandible. Tabs: Inicio · Movimientos · [+] · Análisis · Categorías; Ajustes
desde el avatar. El [+] central abre `QuickAdd`. (El rail puede recuperarse para
tablet en el futuro, no ahora.)

**B. PIN → 4 dígitos (fiel al diseño).** 4 puntos + keypad propio + shake al fallar.
Implica actualizar derivación de passphrase/onboarding/unlock (hoy 6 dígitos).

**C. Alcance → Diseño completo, incluido dominio nuevo.** Entran: (a) **presupuesto
mensual** (hero "Disponible", ritmo/día, alertas), (b) **tipo Ahorro/transferencia**,
(c) **recurrentes**. Esto supera el MVP de `doc/roadmap/mvp.md` — actualizar el roadmap
en consecuencia.

**D. Tema → Mantener dynamic color (Material You).** Los colores de "chrome" salen de
los esquemas dinámicos del sistema (claro/oscuro). Se **mapean los roles semánticos**
del diseño a M3: `surface/surface-2/3`→`surface`/`surfaceContainer*`, `ink/ink-2`→
`onSurface`/`onSurfaceVariant`, `brand`→`primary`, `brand-soft`→`primaryContainer`,
`on-brand`→`onPrimary`. Se conservan como tokens FIJOS los que M3 no aporta:
**paleta de categorías** (13 hex) y el **terracota de gasto** (`neg`/`neg-soft`) +
verde `pos`. Tipografía: usar la del sistema (no se añade Hanken Grotesk para no
chocar con dynamic). Sin selector de acento (lo da el wallpaper).

**E. Flujo de arranque.** Intro (3 slides) → setup PIN (4 díg.) → Preferencias →
cifrado → Home. La introducción es relanzable desde Ajustes ("Ver introducción").

**F. Datos mock.** Se cablean: moneda, idioma, plan mensual, alertas (con el nuevo
dominio de presupuesto), recurrentes. Quedan como UI sin backend por ahora: email del
perfil, "inicio del mes", comparativa "mejor que el mes pasado" (placeholder hasta
tener histórico), "ocultar importes al abrir".

---

## 8. Plan por fases (alcance completo)

- **F0 — Sistema de diseño** `[base]`: extender el tema (mapear roles del diseño sobre
  el dynamic color ya existente; tokens fijos de categoría/`neg`/`pos`), tipografía y
  componentes Compose reutilizables (Card, Segmented, Chip, Button, CatIcon, Bar, Donut,
  Toggle, Toast, Sheet, Dialog).
- **F1 — Navegación**: retirar rail → **bottom nav + FAB central**; "Análisis" como
  título; Ajustes desde avatar/topbar.
- **F2 — Dominio nuevo**: (a) `TransactionType.TRANSFER` (Ahorro) en `:transactions`;
  (b) **presupuesto mensual** (nuevo contexto o extensión de `users`: plan + alertas) y
  queries de "disponible/ritmo"; (c) **recurrentes** (flag + repetición). Tests por capa.
- **F3 — Pantallas re-estiladas** contra F0/F2: Home (hero presupuesto + ritmo + donut +
  reciente), Movimientos (grupos+buscador+chips+Ahorro), Análisis (tasa ahorro, evolución
  en barras, lente Tipo, periodos), Categorías (grid+editor), Ajustes, Onboarding, Unlock.
- **F4 — QuickAdd**: bottom sheet con keypad numérico como alta rápida (FAB central).
- **F5 — Motion & pulido**: entradas escalonadas, count-up, transiciones, reduced-motion, accesibilidad.

Cada fase entrega APK instalable y se verifica en el emulador Pixel_9. Se actualiza
`doc/roadmap/` para reflejar el salto de alcance (presupuesto/Ahorro/recurrentes dejan
de ser post-MVP).

---

## 9. Notas de fidelidad
- El prototipo es HTML/CSS/JS: **recrear el resultado visual**, no la estructura interna.
- Mantener el dominio DDD intacto: las pantallas viven en `apps/android/.../ui/`.
- `oklch` → convertir a sRGB con cuidado (los neutros son *cálidos*, no grises puros).
- Respetar insets/safe-area reales de Android (el prototipo simula un notch 390×844).

---

## 10. Estado de implementación (Ruta B — re-estilado visual sin dominio nuevo)

> Decisión de alcance (2026-06-09): se ejecuta **F0 + F1 + F3** (sistema de diseño +
> navegación + re-estilado visual de todas las pantallas) **sin tocar el dominio**.
> Lo que requiere modelo/persistencia nueva (F2/F4/F5) queda **diferido**.
> Todo verificado en el emulador Pixel_9 (API 36) con capturas por pantalla.

### 10.1 Tareas realizadas

**F1 — Navegación** ✅
- Retirado el rail lateral; barra inferior + FAB central (Inicio · Movimientos · [+] · Análisis · Categorías).
- Ajustes accesible desde el engranaje flotante (top-end). Título "Análisis".
- Archivo: `MainActivity.kt`.

**F0 — Sistema de diseño** ✅
- `ui/theme/Theme.kt`: tema movido fuera de `MainActivity`; mantiene **dynamic color**
  (Material You) mapeando roles del diseño sobre M3 (surface/onSurface/primary/
  primaryContainer…), con fallback al esquema de marca cálido en <API 31.
- `ui/theme/Tokens.kt`: tokens FIJOS que M3 no aporta — `neg`/`negSoft` (terracota),
  `pos`/`posSoft` (verde), `savings`, `track`, **paleta de 13 categorías**, parser
  `categoryColor(hex)`, radios (`WmRadii`) y espaciado (`WmSpacing`). Expuesto vía
  `LocalWmColors` + accesor `WmTheme.colors`.
- `ui/components/Components.kt`: `WmCard`, `WmEyebrow`, `WmSegmented`, `WmChip`,
  `WmPrimaryButton`/`WmGhostButton` (con `scale(0.97)` al pulsar), `CatIcon`,
  `WmBar`, `WmToggle`.
- `ui/components/Donut.kt`: `WmDonut` (Canvas) con segmentos de extremos redondeados,
  hueco y track opcional.
- Modelo UI compartido `ui/CategoryView.kt` y helpers `ui/format/` (`formatAmount`
  con símbolo/decimales opcionales, `currencySymbol`, `relativeDayLabel`).

**F3 — Pantallas re-estiladas** ✅ (solo lo que encaja sin dominio nuevo)
- **Home**: saludo + eyebrow "Mes · nombre", hero "Balance del mes" (ingresos/gastos
  con puntos de color), **donut "En qué va el mes"** (usa el `FindCategoryBreakdownQuery`
  ya existente — sin dominio nuevo) con top-4 y %, lista "Reciente" con `CatIcon`.
  Eliminado el FAB propio (lo aporta la barra).
- **Movimientos**: buscador (filtra desc/categoría/fuente), chips Todos/Gastos/Ingresos,
  **agrupación por día** con etiqueta relativa y total del grupo, filas con `CatIcon`.
- **Análisis**: trío Ingresos/Gastos/Ahorro, **tasa de ahorro** (donut + frase),
  **evolución del gasto en barras** (datos reales de `FindMonthlyEvolutionQuery`),
  **desglose con lente Categoría/Tipo** (esencial/discrecional ya lo calcula el dominio).
- **Categorías**: **grid 2-columnas** con tarjetas (`CatIcon` + badge naturaleza +
  esencialidad) y tarjeta punteada "Nueva". Editor con **preview en vivo** (icono 72px
  + nombre) y `WmPrimaryButton`; paleta del editor alineada a la cálida del diseño.
- **Ajustes**: tarjeta de perfil (avatar con inicial), tarjeta Preferencias
  (nombre + idioma/moneda en `WmChip` + guardar), tarjeta Seguridad (placeholder).
- **Unlock**: chip de candado, "Hola de nuevo", **puntos de PIN + keypad propio**
  con auto-submit al completar (mantiene 6 dígitos — ver diferido).
- **Onboarding**: botón "Empezar" con `WmPrimaryButton` (re-estilado ligero).
- **Editor de transacción**: chips de tipo/categoría → `WmChip`, guardar → `WmPrimaryButton`.

**Coherencia / arreglos**
- Símbolo de moneda unificado: Movimientos y Análisis ahora cargan la **moneda base**
  del usuario (antes mostraban `$` fijo; Home ya usaba la real).

### 10.2 Diferido — progreso (Ruta C: F2/F4/F5 en orden §8)

**Hecho ✅**
- **Tipo Ahorro/transferencia** (`TransactionType.TRANSFER`) — PR1. Enum + invariante
  (`incomeSource` solo en `INCOME`); analítica suma transferencias en `totalTransferCents`
  aparte (no afectan income/expense/balance); chip "Ahorro" en editor y filtros de
  Movimientos; filas/totales con color `savings` y prefijo `→` (neutro en el neto del día).
  Las categorías ya tenían `CategoryKind.TRANSFER`. Tests por capa.
- **Presupuesto mensual + hero "Disponible" + ritmo/día** — PR2. Extensión de `:users`
  (`monthly_budget_cents` + `spending_alerts_enabled`, migración de baseline regenerada y
  verificada). Hero "Disponible = plan − gastado" con badge Dentro del plan/Atención, barra
  gastado/plan y "X/día · N días restantes" (calculado en `HomeViewModel` con Clock+TZ
  inyectables — sin acoplar `:analytics` con `:users`). Card "Presupuesto" en Ajustes (plan
  + toggle de alertas). Tests por capa.

- **Recurrentes** — PR3. Agregado real `RecurringRule` en `:transactions` (cadencia
  WEEKLY/MONTHLY, `nextOccurrence` como cursor idempotente) + tabla `recurring_rule`
  (baseline regenerado y verificado). `RecurringTransactionsMaterializer` materializa las
  ocurrencias vencidas al entrar en Home (catch-up acotado, idempotente). Editor: toggle
  "Recurrente" + chips de frecuencia (solo en creación) + "N activos". Tests de
  materialización (cadencia/catch-up/idempotencia/futuro). Iteración: create-only (sin
  desactivar/editar desde UI todavía; el agregado ya soporta `deactivate`).

- **QuickAdd** (F4) — PR4. Bottom sheet sobre el FAB central: segmented
  Gasto/Ingreso/Ahorro, importe gigante con color por tipo, chips de categoría filtrados,
  nota + Hoy/Ayer, **teclado numérico propio** (≤2 decimales, ≤9 enteros) y CTA
  "Guardar €X" (deshabilitado hasta importe>0 + categoría) con Toast. Reusa
  `RegisterTransactionCommand`; el editor completo sigue para editar. Test de VM (keypad/
  cents/canSave). Verificado en emulador (€25 Ocio → hero "€975 · €44/día" + donut).

- **Periodos Semana/Año** en Análisis — PR5. Analítica generalizada a rango de fechas
  (`FindSummaryInRangeQuery` / `FindBreakdownInRangeQuery`, reutilizando la lógica de mes);
  segmented **Semana / Mes / Año** que recalcula trío, tasa de ahorro y desglose. Semana =
  lunes–domingo de la semana actual; Año = 1 ene–31 dic. Tests de rango + label. Verificado
  en emulador (eyebrow "Junio"↔"2026"). La evolución (barras 6 meses) se mantiene como tendencia.

- **PIN de 4 dígitos** — PR6. La derivación HMAC es agnóstica a la longitud; el cambio es
  de UI/validación. Se introdujo `PinPolicy.LENGTH = 4` como **única fuente de verdad** que
  comparten onboarding y unlock (una divergencia derivaría otra passphrase → bloqueo). Los
  keypad/puntos ya existían. Verificado en emulador (setup "PIN (4 dígitos)" acepta 4 dots).
  ⚠️ Cambiar la longitud invalida la passphrase de DBs creadas con 6 dígitos → reinstalar.

- **F5 Motion & pulido** — PR7. Helper `ui/motion/Motion.kt`: `rememberReducedMotion()`
  (lee el animator duration scale del sistema = `prefers-reduced-motion`), `countUpCents()`
  (cuenta-arriba del importe del hero) y `Modifier.enterUp(index)` (entrada escalonada de
  los bloques de Home). Todo se salta cuando el usuario tiene animaciones desactivadas. Los
  botones ya hacían `scale(0.97)`, las barras/donut/toggle ya animaban, y los sheets/diálogos
  usan las transiciones por defecto de Material 3.

> ✅ Diferido completo (PR1–PR7). Verificado en emulador Pixel_9 (API 36): hero "Disponible €1.000 · €45/día · 22 días"
> + badge "Dentro del plan"; chip Ahorro filtra categoría "Transferencia"; toggle
> Recurrente + Mensual/Semanal; al guardar un Ahorro recurrente el materializador crea la
> transacción real ("Transferencia → €50,00") sin contar como gasto.
> ⚠️ Sin `.sqm`, los cambios de esquema (PR2/PR3) requieren **borrar datos / reinstalar**
> en instalaciones existentes (bootstrap por *sentinel table*; no hay migración in-place).
