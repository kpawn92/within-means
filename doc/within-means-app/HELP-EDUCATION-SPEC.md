# Ayuda y Educación financiera — Especificación (spec-driven)

> **Alcance:** una vista nueva de **ayuda al usuario** (`Aprende` / `HelpScreen`) que
> cumple dos funciones inseparables: (1) enseñar **cómo se mueve el dinero dentro de
> Within Means** (flujo operativo de ingresos, gastos y ahorro) y (2) **enriquecer al
> usuario** con técnicas de finanzas personales, ancladas a los clasificadores que la
> app **ya** modela (`CategoryNature`, `CategoryEssentiality`, `CategoryProductive`,
> `IncomeSource`, `EngelGroup`) y a los KPIs que `analytics` **ya** calcula. No es un
> blog genérico: cada técnica apunta a un dato real de la app.
>
> Donde choque con [`SPEC.md`](SPEC.md), manda `SPEC.md` para el sistema de diseño;
> este documento manda para **estructura, navegación y contenido** de la vista de ayuda.
> Para el color semántico ingreso/gasto, manda [`HOME-DESIGN-SPEC.md`](HOME-DESIGN-SPEC.md).
>
> Convención de etiquetas (igual que `SPEC.md`): `[existe]`, `[restyle]`, `[nuevo-ui]`,
> `[nuevo-dominio]`, `[mock]`.

---

## 1. Visión y principios

La ayuda **no es un manual**, es un acompañante. Refuerza el lema del producto
("gastar con calma, sin culpa y sin hojas de cálculo") explicando **por qué** la app
pide tanto detalle al clasificar (fijo/variable, esencial/discrecional, productivo,
activo/pasivo): porque ese detalle es lo que convierte un registro de gastos en
**inteligencia financiera**.

Cinco principios que la vista debe respetar, **en orden de prioridad**:

0. **Simplicidad ante todo (regla #0, manda sobre las demás).** El usuario debe poder
   resolver su duda en **un vistazo, sin leer un muro de texto**. Por defecto se muestra
   lo mínimo (una ruta guiada de pocos pasos, §4.2); todo lo demás vive **plegado o
   detrás de "ver más"**. Si una lección no se entiende en su `oneLiner`, está mal escrita.
   Nada de jerga en la superficie: los términos técnicos (Engel, Herfindahl, "coeficiente")
   **se ocultan**; en su lugar, lenguaje cotidiano (ver §6.3).
1. **Ancla, no teoría suelta.** Toda lección termina en "dónde lo ves / cómo lo haces
   en la app". Si una técnica no se puede observar con un dato real de Within Means, no
   entra (o entra marcada `[nuevo-dominio]` como deseo futuro, nunca como promesa).
2. **Sin culpa.** El tono nunca regaña. No dice "gastas demasiado en ocio"; dice "tu
   gasto discrecional fue X%; aquí está la referencia y tú decides".
3. **Progresivo y opcional.** Tres niveles: *operar* la app → *leer* tus números →
   *aplicar* técnicas. El novato ve solo el nivel 1 por defecto; bajar a KPIs o técnicas
   es una **elección activa**, nunca algo que se le impone al abrir la vista.
4. **Honesta con el alcance.** Lo que la app aún **no** calcula (p. ej. fondo de
   emergencia, diversificación de ingresos) se marca como **referencia conceptual**,
   no como una pantalla que existe.

> Principio rector: **la mejor educación financiera de esta app es enseñar al usuario
> a leer los clasificadores que ya está rellenando.** El valor estaba latente en el
> modelo de dominio ([`../contexts/mvp.md`](../contexts/mvp.md)); esta vista lo activa.

---

## 2. Ubicación y navegación

- La vista **no es un tab** (la barra inferior se mantiene: Inicio · Movimientos ·
  [+] · Análisis · Categorías, ver `SPEC.md §7-A`).
- Entrada principal: desde **Ajustes**, una fila nueva **"Aprende a mover tu dinero"**
  `[nuevo-ui]`, junto a "Ver introducción"
  ([`SettingsScreen.kt:299`](../../apps/android/src/main/kotlin/within/means/android/ui/settings/SettingsScreen.kt#L299)).
- Entradas contextuales (deep-link a una lección concreta) `[nuevo-ui]`:
  - Icono **"?"** discreto junto a KPIs en **Análisis** (tasa de ahorro, lente
    esencial/discrecional) → abre la lección que explica ese número.
  - Enlace **"¿Qué es esto?"** bajo el hero "Disponible" / "Ritmo sugerido" de la Home.
- La vista es **fullscreen con topbar** (título "Aprende", flecha atrás). Scroll
  vertical largo de tarjetas-lección agrupadas por sección (§4).

> Decisión de descubribilidad: la ayuda **se ofrece en el contexto donde duele la
> duda** (junto al número que no se entiende), no solo escondida en Ajustes. Ver §7-A.

---

## 3. Estructura de contenido (modelo de "lección")

La unidad de contenido es la **Lección** (`LessonCard`). Es un patrón fijo, no prosa
libre. Esto mantiene el tono consistente y hace el contenido traducible/testeable.

Anatomía de una `LessonCard` `[nuevo-ui]`:

| Campo | Rol | Ejemplo |
|---|---|---|
| `icon` + `title` | Gancho corto | 🪙 "Págate a ti primero" |
| `oneLiner` | Resumen en 1 frase (visible plegado) | "Aparta el ahorro **antes** de gastar, no con lo que sobre." |
| `whatItIs` | Qué es (2-3 frases) | … |
| `whyItMatters` | Por qué importa, sin culpa | … |
| `inTheApp` | **Ancla**: dónde se ve / cómo se hace | "Regístralo como **Ahorro** apenas entra la nómina → `QuickAdd` tipo Ahorro." |
| `rule` | Regla práctica accionable | "Objetivo: ≥20% del ingreso (regla 50/30/20)." |
| `seeAlso` | Enlaces a lecciones/pantallas relacionadas | → "Tasa de ahorro", → Análisis |

Estados de la tarjeta:
- **Plegada:** `icon` + `title` + `oneLiner` + chevron.
- **Desplegada:** acordeón que revela `whatItIs / whyItMatters / inTheApp / rule / seeAlso`.
- `inTheApp` lleva, cuando aplica, un **botón de acción** que navega a la pantalla real
  (p. ej. "Abrir QuickAdd", "Ver mi tasa de ahorro").

> El campo `inTheApp` es **obligatorio** en toda lección de la §6. Una lección sin ancla
> a un dato/pantalla real no se publica (principio §1.1).

---

## 4. Mapa de la vista (simplicidad primero)

> Aplicación directa de la regla #0 (§1). Lo que el usuario ve **al abrir** es corto y
> guiado. El resto existe, pero **no aparece hasta que lo pide**.

**Lo que se ve por defecto (above the fold):**

### 4.1 Cabecera `[nuevo-ui]`
Eyebrow "Aprende" + título corto + subtítulo de 1 línea. Sin hero pesado.

### 4.2 Ruta guiada — "Empieza por aquí" `[nuevo-ui]` (lo único protagonista)
Una **lista de 4 pasos**, no un temario. Es el destilado de la Sección A (§5) en su
mínima expresión, el único bloque que el usuario *necesita* leer:

1. **Anota lo que entra** → ingreso. *(abre QuickAdd)*
2. **Aparta tu ahorro primero** → Ahorro. *(abre QuickAdd)*
3. **Anota lo que gastas** (1 toque). *(abre QuickAdd)*
4. **Mira cómo vas** → Análisis. *(abre Análisis)*

Cada paso = una fila con icono, frase de una línea y botón de acción. Sin acordeones,
sin párrafos. Si el usuario solo lee esto, ya sabe usar la app.

**Lo que está plegado / detrás de "ver más" (no estorba):**

### 4.3 "¿Quieres profundizar?" — colapsable `[nuevo-ui]`
Un único disclosure cerrado por defecto. Al abrirlo aparecen las tres bibliotecas
opcionales, cada una también plegada:
- **Lee tus números** (Sección B, §6.1) — tasa de ahorro, esencial/discrecional, etc.
- **Técnicas para mover mejor el dinero** (Sección C, §6.2).
- **El flujo completo, paso a paso** (Sección A extendida, §5).

Estas bibliotecas son `LessonCard` plegadas (§3). El usuario que no quiere teoría
**nunca las abre y la vista sigue sintiéndose vacía y calmada**.

### 4.4 Pie — "Esto es una guía, no un consejo financiero"
Descargo breve `[nuevo-ui]`: la app educa, no sustituye asesoría profesional. Tono
calmado, sin letra pequeña agresiva.

> Decisión de superficie (RESUELTA, §7-F): por defecto el usuario ve **cabecera + 4
> pasos + un colapsable cerrado + pie**. Punto. Las 13 lecciones existen pero **ninguna
> se impone**. Profundizar es siempre opt-in.

### 4.5 Lenguaje visual — minimalista profesional `[nuevo-ui]`

> Reutiliza el sistema de diseño existente ([`SPEC.md §2`](SPEC.md)); **no introduce
> tokens ni componentes nuevos**. "Minimalista profesional" aquí significa: editorial,
> sobrio, mucho aire, jerarquía por tipografía y espacio — **no** por color ni adorno.

**Principios visuales:**

1. **El blanco manda.** Fondo `bg` (off-white cálido `#F7F6F1`). La pantalla respira:
   márgenes laterales 20–24 dp, separación generosa entre bloques (≥ 24 dp). El vacío
   es intencional, no "contenido que falta".
2. **Sin tarjetas pesadas.** La ruta guiada (§4.2) son **filas limpias separadas por
   hairline** (`surface-3`), no cards con sombra. Las `LessonCard` (§3) usan `surface`
   plano con radio `lg` (24) y **borde sutil, sin elevación** (sombra a lo sumo `xs`).
   Cero degradados; el hero degradado de marca se reserva a la Home, aquí no aplica.
3. **Color contenido al mínimo.** La paleta es **neutra por defecto** (`surface`,
   `onSurface`, `onSurfaceVariant`). El acento de marca (`brand`) solo en el botón de
   acción de cada paso. El azul/rojo de finanzas **no decora**: aparece únicamente si la
   lección muestra una cifra real de ingreso/gasto (`HOME-DESIGN-SPEC §1`).
4. **Jerarquía por tipografía, no por peso visual.** Títulos de sección en `titleMedium`;
   `oneLiner` en `bodyMedium`; metadatos en `labelSmall`/`onSurfaceVariant`. Un solo
   nivel de énfasis por bloque. Nada de mayúsculas gritando ni negritas múltiples.
5. **Iconografía mínima y monocroma.** Iconos de línea, tamaño contenido (20–24 dp),
   en `onSurfaceVariant` (no de color). Los emojis del catálogo (§3) son opcionales y
   decorativos; si recargan, se sustituyen por iconos de línea. Nunca dos pesos de icono
   juntos.
6. **Acordeón silencioso.** Expandir una `LessonCard` es un `expand/collapse` suave
   (motion `SPEC.md §5`, ~200–300 ms); el chevron es el único indicador. Sin badges, sin
   contadores, sin "nuevo".
7. **Botones discretos.** Las acciones `inTheApp` son **botones de texto / tonales
   ligeros**, no `WmPrimaryButton` lleno. La ayuda invita, no empuja.
8. **Minimalista pero agradable (no frío).** El minimalismo aquí es **cálido**, fiel al
   carácter del producto (`SPEC.md §1`: "cálido + sobrio"). Lo que lo hace agradable sin
   romper la sobriedad:
   - El off-white cálido (`bg`) y las curvas amables (radio `lg` 24) ya dan calidez; se
     conservan, no se enfrían a blanco puro ni esquinas rectas.
   - **Copy humano y en segunda persona** ("Aparta tu ahorro primero"), nunca tono de
     manual técnico. Una pizca de calidez puntual (un emoji suave en la cabecera o el
     paso, máximo uno por bloque) está permitida si suma cercanía.
   - **Microinteracciones suaves** (`SPEC.md §5`): entrada escalonada `.enter` de los
     pasos, acordeón con easing amable, `scale(0.97)` al pulsar. El movimiento es lo que
     transmite cuidado.
   - Generosidad de espacio = sensación de calma, no de pantalla vacía. El equilibrio:
     **sobrio en lo visual, cercano en el tono.**

**Anti-patrones (qué NO hacer):** banners de colores, ilustraciones grandes por lección,
tarjetas con sombra fuerte, multiacento, iconos rellenos multicolor, progreso gamificado,
copys en mayúsculas. Si un elemento no ayuda a *entender más rápido*, sobra (regla #0).

---

## 5. Sección A — Flujo operativo del dinero (contenido)

Enseña el **circuito completo** que el usuario debe interiorizar. Orden deliberado:
primero entra el dinero, luego se aparta el ahorro, luego se gasta y al final se lee.

### A1. "El dinero entra: registra tus ingresos" `[existe]`
- **inTheApp:** `QuickAdd` → tipo **Ingreso** (azul, `HOME-DESIGN-SPEC §1`). Al ser
  ingreso, la app pide **fuente**: `IncomeSource` = **Activo** (trabajas por él:
  nómina, freelance) o **Pasivo** (rinde sin tu tiempo: alquiler, dividendos, intereses).
- **whyItMatters:** distinguir activo/pasivo no es burocracia: es el primer paso para
  ver cuánto de tu vida depende de tu tiempo (ver lección C4).
- **rule:** registra el ingreso **el día que entra**, no a fin de mes de memoria.

### A2. "Aparta el ahorro antes de gastar" `[existe]`
- **inTheApp:** `QuickAdd` → tipo **Ahorro/Transferencia** (acento `savings`, oliva;
  no es ingreso ni gasto, `HOME-DESIGN-SPEC §1`). Hazlo apenas registrado el ingreso.
- **whyItMatters:** lo que se aparta primero, se conserva. Lo que se deja "para lo que
  sobre", se gasta. Esta es la técnica **Págate primero** (C1) vista como gesto en la app.
- **rule:** primer movimiento del mes tras la nómina = un Ahorro.

### A3. "Clasifica bien tus gastos (vale 10 segundos)" `[existe]`
El corazón de la inteligencia de la app. Al registrar un gasto, su **categoría** ya
carga los clasificadores ([`../contexts/mvp.md`](../contexts/mvp.md) → `categories`):
- **Fijo / Variable** (`CategoryNature`) — ¿se repite igual cada mes (alquiler) o
  cambia (super, ocio)?
- **Esencial / Discrecional** (`CategoryEssentiality`) — ¿lo necesitas para vivir o es
  elección?
- **Productivo** (`CategoryProductive`) — ¿genera valor futuro (salud, educación,
  herramientas) o se consume y ya?
- **whyItMatters:** estos tres ejes son lo que luego te deja leer tu salud financiera
  (Sección B). Sin clasificar, solo tienes una lista de gastos; clasificando, tienes un
  diagnóstico.
- **rule:** no crees una categoría sin pensar su nature/essentiality. Edítalas en
  **Categorías** cuando cambie tu criterio (`ReclassifyCategoryCommand`).

### A4. "Lo que se repite, automatízalo" `[existe]`
- **inTheApp:** **Recurrentes** (`RecurringRulesScreen`) para nómina, alquiler,
  suscripciones. Reduce fricción y evita olvidos que ensucian los KPIs.
- **rule:** todo gasto **Fijo** es candidato a recurrente.

### A5. "Cierra el círculo: léelo" `[existe]`
- **inTheApp:** **Análisis** y el hero de la **Home** (Disponible, Ritmo sugerido).
- Puente natural hacia la Sección B.

---

## 6. Secciones B y C — Educación anclada al dominio (contenido)

> **Regla de oro del contenido (§1.1):** cada técnica se mapea a un campo de
> `MonthlySummary`, a un clasificador de `Category`/`Transaction`, o a un KPI de
> `analytics`. Lo que no se ancla, se marca **referencia conceptual**.

### 6.1 Sección B — "Lee tus números"

#### B1. Tasa de ahorro `[existe]`
- **Definición:** `netSaving / totalIncome` (ambos en `MonthlySummary`,
  [`../contexts/mvp.md`](../contexts/mvp.md) → `analytics`). Ya se muestra en
  [`StatsScreen.kt`](../../apps/android/src/main/kotlin/within/means/android/ui/analytics/StatsScreen.kt).
- **rule:** referencia sana ≥ 20%. El delta mes a mes en verde/terracota es **estado**,
  no ingreso/gasto (`HOME-DESIGN-SPEC §4-A`).

#### B2. Esencial vs Discrecional `[existe]`
- **Definición:** `essentialExpenses` vs `discretionaryExpenses` (`MonthlySummary`),
  visible en la **lente Tipo** de Análisis (`SPEC.md §4.5`).
- **whyItMatters:** tu margen de maniobra real en un mes flojo es el discrecional. Es lo
  primero que puedes recortar sin tocar lo esencial.

#### B3. Fijo vs Variable `[existe]`
- **Definición:** `fixedExpenses` vs `variableExpenses` (`MonthlySummary`).
- **whyItMatters:** un ratio de gasto **fijo** alto = poca flexibilidad ante imprevistos
  (el fijo no se recorta de un mes a otro). Bajar el fijo = ganar resiliencia.

#### B4. Coeficiente de Engel `[nuevo-ui]` (cálculo) / `[existe]` (dato)
- **Definición:** proporción del gasto destinada a **alimentación**
  (`EngelGroup.FOOD` ÷ gasto total). El dato ya está en las categorías; el agregado del
  ratio es una proyección sencilla sobre `CategoryBreakdown`.
- **whyItMatters:** históricamente, cuanto **menor** el coeficiente de Engel, mayor el
  margen para todo lo demás. Es un termómetro clásico de holgura, sin juicios.
- **Estado:** el desglose por `EngelGroup` ya está modelado; mostrar el ratio como KPI
  es trabajo de `analytics` futuro. Marcar honesto si aún no aparece.

#### B5. Gasto productivo `[existe]` (dato) / `[nuevo-ui]` (vista)
- **Definición:** suma de gastos con `CategoryProductive = true` (educación, salud,
  herramientas) frente al consuntivo.
- **whyItMatters:** no todo gasto es "fuga". El productivo es **inversión en ti**;
  separarlo evita la culpa de recortar lo que en realidad te hace crecer.

### 6.2 Sección C — "Técnicas para mover mejor el dinero"

#### C1. Págate a ti primero `[existe]`
- Aparta el ahorro **antes** de gastar. **inTheApp:** A2 (QuickAdd tipo Ahorro al recibir
  la nómina). **rule:** automatízalo como recurrente si tu flujo lo permite.

#### C2. Regla 50/30/20 `[existe]`
- 50% **esencial** · 30% **discrecional** · 20% **ahorro**. La app es de las pocas que
  puede **medir esto de verdad** porque ya separa `essential`/`discretionary`
  (`MonthlySummary`) y el ahorro (`netSaving` / movimientos de Ahorro).
- **inTheApp:** compara la lente Esencial/Discrecional (B2) + tasa de ahorro (B1) contra
  el 50/30/20. **rule:** son referencias, no dogma; ajústalas a tu realidad.

#### C3. Presupuesto y ritmo sugerido `[existe]` / `[nuevo-dominio]` (presupuesto)
- "Cuánto puedo gastar hoy" = disponible ÷ días que quedan del mes (hero **Ritmo
  sugerido**, `SPEC.md §4.1`). Requiere **presupuesto mensual** (`[nuevo-dominio]`,
  ver `SPEC.md`). **rule:** gasta al ritmo, no por impulso a principio de mes.

#### C4. Ingreso activo vs pasivo `[existe]`
- **inTheApp:** `IncomeSource` ACTIVE/PASSIVE (A1). **whyItMatters:** la meta de largo
  plazo de cualquier técnica de financiamiento es que **una parte de tu ingreso no
  dependa de tu tiempo**. Ver tu split activo/pasivo es el primer paso medible.
- **rule:** observa si tu ingreso pasivo crece mes a mes, aunque sea poco.

#### C5. Diversifica de dónde viene el dinero `[nuevo-dominio]` (KPI) / `[existe]` (dato)
- Depender de **una sola** fuente de ingreso es frágil. El modelo ya reserva `OriginRef`
  en `Transaction` para, en el futuro, calcular concentración (índice de Herfindahl,
  ver [`../contexts/mvp.md`](../contexts/mvp.md) → `transactions`).
- **Estado:** **referencia conceptual** hoy; el dato (`originRef`) ya se captura, el KPI
  es futuro. No prometer pantalla.

#### C6. Fondo de emergencia `[nuevo-ui]` (cálculo) / `[existe]` (insumo)
- **Definición:** colchón de **3–6 meses de gastos esenciales**. La app **ya tiene el
  insumo**: `essentialExpenses` mensual; el objetivo = `essentialExpenses × 3..6`.
- **whyItMatters:** es el ahorro que convierte un imprevisto en un inconveniente, no en
  una crisis. **rule:** prioridad #1 antes de cualquier inversión.
- **Estado:** el objetivo es un cálculo directo sobre un dato existente; mostrarlo como
  meta es `[nuevo-ui]` ligero (sin dominio nuevo).

#### C7. Distingue gasto productivo de consuntivo `[existe]`
- Espejo aplicado de B5: ante un recorte, protege lo `Productive = true`. **rule:** antes
  de cortar, pregunta "¿esto me devuelve algo?".

#### C8. Método Kakebo (registro consciente) `[existe]`
- Referencia cultural: el ahorro japonés del cuaderno de gastos. **inTheApp:** Within
  Means **es** tu Kakebo digital; el simple acto de registrar en `QuickAdd` (héroe del
  producto, `SPEC.md §1`) ya cumple su esencia: hacer consciente cada gasto.

### 6.3 Jerga oculta → lenguaje cotidiano (regla #0)

El nombre técnico vive en este documento, **no en la pantalla**. En la UI, cada lección
usa su versión llana; el término académico aparece a lo sumo en letra pequeña ("también
llamado…"), nunca como título. Mapa obligatorio:

| Concepto técnico (interno) | Cómo se titula en la app (cara al usuario) |
|---|---|
| Tasa de ahorro | "Cuánto te queda al mes" |
| Esencial vs Discrecional | "Lo que necesitas vs lo que eliges" |
| Fijo vs Variable | "Gastos que se repiten vs los que cambian" |
| Coeficiente de Engel | "Cuánto se va en comida" |
| Gasto productivo | "Gastos que te devuelven algo" |
| Ingreso activo vs pasivo | "Dinero por tu tiempo vs dinero que trabaja por ti" |
| Índice de Herfindahl / concentración | "Si dependes de una sola fuente" |
| Págate a ti primero | "Aparta tu ahorro primero" |
| Método Kakebo | "Anota cada gasto, sin esfuerzo" |

> Si una lección **necesita** explicar el término técnico para entenderse, está mal
> planteada (regla #0). El usuario aprende el **concepto**, no el vocabulario.

---

## 7. Decisiones (RESUELTAS)

**A. Descubribilidad dual.** La ayuda vive en Ajustes **y** se ofrece contextualmente
con "?" junto a los KPIs (Análisis) y "¿Qué es esto?" en la Home. Razón: la duda surge
frente al número, no en Ajustes. El deep-link abre la lección concreta (§2).

**B. Contenido anclado, no genérico (RESUELTA por preferencia del usuario).** Toda
lección de §6 mapea a un campo de `MonthlySummary`, un clasificador de `Category`, o un
KPI de `analytics`. Nada de consejos sueltos. Lo no medible hoy se marca **referencia
conceptual** (C5) o `[nuevo-dominio]`, nunca como pantalla existente.

**C. Sin dominio nuevo en v1 de la ayuda.** La vista es **solo contenido + navegación**
(`[nuevo-ui]`). Los KPIs que faltan (Engel B4, fondo de emergencia C6) se documentan como
lecciones que **describen** el concepto y, cuando el dato exista, se cablean. La ayuda no
bloquea su entrega esperando esas proyecciones.

**D. Tono sin culpa, fijo.** Ninguna lección usa lenguaje de juicio. El color de estado
(verde/terracota) se usa para "vas bien / atención", nunca el azul/rojo de ingreso/gasto
para valorar comportamiento (`HOME-DESIGN-SPEC §1`).

**E. Contenido como dato, no hardcode en Compose.** Las lecciones se modelan como una
lista de `Lesson` (data class) renderizada por un único componente `LessonCard`. Facilita
i18n (ES/EN, `Locale` del contexto `users`), test y futura edición sin tocar UI.

**F. Simplicidad por defecto (regla #0, RESUELTA por petición del usuario).** Al abrir,
la vista muestra **solo**: cabecera + ruta guiada de 4 pasos + un colapsable cerrado
("¿Quieres profundizar?") + pie. Las bibliotecas B y C **no se ven** hasta que el usuario
las pide (§4.3). Ninguna lección obliga a leerse; toda profundidad es opt-in. La jerga
técnica se traduce a lenguaje cotidiano en la UI (§6.3). Esta decisión **manda sobre
cualquier impulso de exhaustividad**: ante la duda, se muestra menos.

---

## 8. Plan por fases

- **F0 — Modelo de contenido** `[nuevo-ui]`: `Lesson` (data class con los campos de §3)
  + catálogo estático en ES; `LessonCard` (acordeón) y `HelpSection`. Sin red, sin
  dominio nuevo.
- **F1 — Vista mínima y navegación**: `HelpScreen` con **solo** la superficie por
  defecto (cabecera + ruta guiada de 4 pasos + colapsable cerrado + pie, §4) + entrada
  desde **Ajustes** ("Aprende a mover tu dinero"). Las bibliotecas B/C se renderizan
  plegadas dentro del colapsable; ninguna se expande al cargar (regla #0).
- **F2 — Anclas accionables**: botones `inTheApp` que navegan a `QuickAdd` / Análisis /
  Categorías; deep-link a lección concreta (`HelpScreen(initialLessonId)`).
- **F3 — Descubribilidad contextual**: "?" junto a KPIs en Análisis y "¿Qué es esto?" en
  la Home (Decisión A).
- **F4 — i18n**: extraer el catálogo a recursos por `Locale`; preparar EN.
- **F5 — KPIs que faltan (depende de `analytics`)**: ratio de Engel (B4), objetivo de
  fondo de emergencia (C6), split de ingreso pasivo creciente (C4). Cada uno **desbloquea**
  el `inTheApp` de su lección, que hasta entonces es referencia conceptual.

---

## 9. Notas de fidelidad y alcance

- La ayuda **describe el producto tal cual es**: si una lección dice "lo ves en Análisis",
  ese número **tiene que existir** ahí. Las lecciones de conceptos aún no calculados se
  marcan explícitamente como referencia, sin botón de acción que lleve a una pantalla vacía.
- No se introduce dominio nuevo por esta vista (Decisión C). El presupuesto mensual
  (C3) sigue siendo `[nuevo-dominio]` propiedad de `SPEC.md`, no de este documento.
- El contenido es **traducible** (Decisión E) y respeta el `Locale` del usuario
  ([`../contexts/mvp.md`](../contexts/mvp.md) → `users`).
- Accesibilidad: las lecciones son texto real (no imágenes con texto); los iconos son
  decorativos y nunca el único portador de significado (`HOME-DESIGN-SPEC §1.2`).

---

## 10. Estado de implementación

> **Pendiente.** Este documento es el spec previo a implementación, fiel a la filosofía
> spec-driven del proyecto: **no se escribe código hasta validar §7**. Las Decisiones de
> §7 están **RESUELTAS**; falta tu visto bueno para arrancar F0.
