# 📋 Informe de entrega — Within Means 0.5.0

> Fecha: 2026-06-13 · `versionCode=6` / `versionName=0.5.0`
> Artefactos: `dist/within-means-0.5.0-release.apk` (~26 MB) · `dist/within-means-0.5.0-release.aab` (~18 MB)
> Build R8 (minify + shrink) **firmado** (V1+V2, `CN=Within Means`). APK **verificado** (`apksigner`).
> ✅ **Smoke test del build de release superado** en emulador (Pixel 9): arranque + onboarding sin crashes.

## Resumen ejecutivo

Ciclo del **Concepto**: una primitiva nueva que hace registrable y consultable *en qué* se gastó
("¿cuánto en cerveza?", "¿cuánto en papá?", "¿cuánto en el carro de la ruta1 a la ruta2?") **sin
explotar el número de categorías** y **sin pedirle al usuario que elija categoría**. El concepto se
escribe en lenguaje natural, infiere su categoría sola y alimenta los desgloses. Encima se construye la
**captura por lotes** ("vaciar la cesta": varias líneas en un commit), los **accesos rápidos** (widget
Glance + App Shortcuts) y un **lavado de cara visual** del Home. Regla rectora de siempre: **simplicidad
primero** — superficie mínima por defecto, profundidad opt-in, jerga financiera escondida tras lenguaje llano.

## Novedades de este ciclo (sobre 0.4.0)

### 🏷️ Conceptos (contexto acotado nuevo)
- Nuevo bounded context `concepts` (KMP, espejo de `categories`): agregado `Concept` con **clave
  normalizada** (`ConceptKey`: trim · minúsculas · sin tildes manteniendo ñ · sin emoji/puntuación ·
  espacios colapsados) → `"Cerveza" = "cerveza" = "  CERVEZA "` suman juntas.
- **Idempotente por `(kind, key)`**: escribir un concepto ya existente no duplica, solo lo reutiliza.
- Persistencia propia (SQLDelight + SQLCipher, cifrada como el resto), tabla puente
  `TransactionConcept` para enlazar movimiento↔conceptos (máx. 8 por movimiento).

### ⚡ Captura con conceptos (en las dos superficies)
- **`QuickAddSheet` (héroe del FAB)** y **`TransactionEditScreen` (editor)** comparten el flujo vía
  `MovementCaptureService` (orquesta `concepts` + `categories` + `transactions` desde `apps/android`, sin
  acoplar módulos KMP).
- Campo **"¿En qué fue?"** + chips de conceptos frecuentes (más usados primero). La **categoría ya no se
  pide**: se infiere del primer concepto que la tenga, con override opcional, y si nada resuelve cae en
  **"Otros"** (find-or-create). La UI nunca obliga a elegir categoría.

### 🧺 Captura por lotes — "vaciar la cesta" (F4b)
- Modo **Uno / Lista** opt-in en `QuickAddSheet`. En Lista, un campo `concepto monto` (Enter añade fila y
  mantiene el teclado) parsea respetando nombres con espacios (`carro ruta1 a ruta2 78`), muestra **preview
  de la categoría inferida por fila** (lectura pura, no crea el concepto), un **total corrido** y un único
  **`Guardar N · $total`**.
- Las N líneas se registran como **N movimientos independientes** que comparten un `batchRef` opaco
  (provenance, patrón de los refs reservados). En **Movimientos** se agrupan en **"🧺 Compra · N · total"**
  con **Deshacer** (diálogo de confirmación → borra el lote completo).
- Caso de la spec verificado E2E: `patata 90 / pan 15 / detergente 70 / carro ruta1 a ruta2 78` →
  4 movimientos, donut repartido entre 3 categorías **sin elegir ni una a mano**.

### 🔎 Consulta por concepto
- Búsqueda por etiqueta de concepto en **Movimientos** (responde "¿cuánto gasté en X?" con total visible).
- `FindConceptBreakdownQuery`/`InRange` en `analytics` + lente **"Conceptos"** en Análisis, con aviso de
  **no-partición** (los totales por concepto pueden exceder el total del periodo: un movimiento puede
  llevar varios conceptos).

### 🧠 Aprendizaje (inferencia de categoría)
- `ConceptCategorySuggester`: match por nombre de categoría + sinónimos → grupo de Engel
  (`gasolina`→Transporte, `cerveza`→Comida, `detergente`→Hogar…). Un concepto nuevo **nace ya mapeado**.
- Seed day-1 de conceptos desde las categorías del usuario (suscriptor de `UserDefaultCreated`).
- Re-aprendizaje **opt-in** como tarjeta inline en *Detalles* del editor (no intrusivo).

### 📲 Widget y accesos rápidos (F7)
- **Widget "Añadir rápido"** (Jetpack **Glance** + Material You): dos baldosas **− Gasto / + Ingreso** que
  hacen deep-link al editor con el tipo preseleccionado. Nunca toca la BD cifrada — solo arranca la app
  (bloqueada), que enruta a QuickAdd tras el desbloqueo.
- **App Shortcuts** estáticos (`shortcuts.xml`) + dinámicos (`ShortcutManagerCompat`, conceptos top).
- Deep-link `transactions/new?type=&concept=`; el `QueryBus` se resuelve **perezosamente tras el desbloqueo**
  (resolverlo en composición construía los repos antes de `unlock()` → crash, corregido).

### 🎨 Lavado visual del Home (estándares modernos)
- **Rojo de gasto** llevado de un brick anaranjado/neón a un **rojo tonal limpio** (claro `#BE3636` /
  oscuro `#EC9A92`), con contraste AA, que casa con el azul de ingreso.
- **Heroes con acabado premium**: `WmCard` admite `brush` + `elevated` → **degradado lineal diagonal**
  (`heroBrush`) + **sombra suave** en vez de un bloque plano con filo de 1dp. Aplica a presupuesto y balance,
  preservando la semántica azul/rojo.

### 📚 Ayuda
- Dos lecciones nuevas en la vista de ayuda explicando el concepto ("Di en qué fue") y el lote
  ("Apunta la compra entera").

## Calidad técnica

- **Compila limpio** y **build R8 de release** correcto y firmado.
- **Arquitectura respetada** ([[respect_architecture]]): `concepts` es un módulo KMP aislado; la
  orquestación cross-context (`MovementCaptureService`, suscriptores) vive en `apps/android` y resuelve
  servicios **lazy** vía `get<>()` para no romper el ciclo de Koin del EventBus.
- **Guardia de cableado:** `ConceptsModuleWiringTest` arranca Koin con el módulo + fakes y resuelve cada
  definición; caza la clase de bug que los tests unitarios no ven (p. ej. registrar un handler con
  `Clock = Clock.System` por `singleOf`, que hacía fallar todo el `CommandBus`). Se añadieron además tests
  de parser/lote y de `previewCategoryId` (lectura pura).
- **Migración de persistencia:** columnas `batch_ref` + tabla puente `TransactionConcept` + BD de conceptos;
  `ensureColumn`/`ensure table` idempotentes. Las instalaciones existentes pueblan conceptos por uso (el
  seed day-1 solo dispara en onboarding).

## Empaquetado

- `versionCode = 6`, `versionName = "0.5.0"` en `apps/android/build.gradle.kts`.
- `./gradlew :apps:android:assembleRelease :apps:android:bundleRelease` → APK ~26 MB + AAB ~18 MB.
- Firma de release desde `keystore.properties` (gitignorado); `apksigner verify` ⇒ **OK** (V1+V2,
  `CN=Within Means, OU=Mobile, O=WithinMeans, L=Madrid, C=ES`).

## Verificación

✅ **Smoke test del build R8 firmado** en emulador (Pixel 9, API 36) + verificación E2E en debug del ciclo nuevo:
- Arranque del release firmado → onboarding ("Tu dinero, de un vistazo") sin crashes.
- QuickAdd con conceptos: "cerveza" → chip + subtítulo "en cerveza"; guardado → "Otros" inferido.
- Captura por lotes: cesta de 4 líneas → "4 movimientos · Total $253.00" → guardar reparte el donut.
- Movimientos: agrupación "🧺 Compra · 4 mov · $253" + **Deshacer** borra el lote.
- **Widget colocado en el escritorio y probado en vivo**: renderiza, y "− Gasto" abre el editor con el tipo
  preseleccionado tras el desbloqueo.

### Pendiente conocido (no bloqueante)
- Warm-start del deep-link (solo arranque en frío cubierto).
- Seed de conceptos para instalaciones existentes (hoy por uso; el seed day-1 solo dispara en onboarding).
- Post-MVP: `TileService` (Quick Settings) y merge/sinónimos manual de conceptos.

## Spec-driven
- Especificación: `doc/within-means-app/CONCEPTS-SPEC.md` (decisiones cerradas D0.1–D0.4, fases F1–F7 + F4b,
  no-partición §5, clave normalizada §3). Acabado visual del Home: `doc/within-means-app/HOME-DESIGN-SPEC.md`
  (rojo tonal §1, hero con degradado/elevación §4-C).
