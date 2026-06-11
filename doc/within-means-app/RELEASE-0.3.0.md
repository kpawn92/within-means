# 📋 Informe de entrega — Within Means 0.3.0

> Fecha: 2026-06-11 · `versionCode=4` / `versionName=0.3.0`
> Artefactos: `dist/within-means-0.3.0-release.apk` (25 MB) · `dist/within-means-0.3.0-release.aab` (16 MB)
> Build R8 (minify + shrink) **firmado** (V2, `CN=Within Means`). APK **verificado** (`apksigner`).

## Resumen ejecutivo

Ciclo de **identidad visual + UX + captura de movimientos**. Se fija una **regla de color de marca**
(ingreso = azul, gasto = rojo) en toda la app, se pulen las interacciones (ripples, gestos, navegación) y
se añade **hora del movimiento** como cambio de dominio aditivo (sin romper el Event Store ni la analítica).
Además: duplicar movimiento, borrar categoría sin uso, +130 iconos y la lente Fijo/Variable en Análisis.

## Novedades de este ciclo (sobre 0.2.1)

### 🎨 Regla de color: ingreso = AZUL, gasto = ROJO
- Tokens nuevos `income`/`expense` (+ *soft* y *container*) en `ui/theme/Tokens.kt`; `pos`/`neg`
  (verde/terracota) pasan a ser tokens de **estado** (ok/warn), no de ingreso/gasto.
- Aplicada en Home (hero, «Reciente»), Movimientos (filas + total de grupo), Análisis (trío), QuickAdd,
  editor y recurrentes. El color **nunca va solo**: se conservan los signos `+`/`−`/`→`.
- **Tarjeta por signo**: el «Balance del mes», el hero «Disponible» (dentro del plan / atención) y la
  «tasa de ahorro» se tiñen **azul si positivo / rojo si negativo**.
- Spec de diseño: `doc/within-means-app/HOME-DESIGN-SPEC.md` (§1, regla canónica).

### 🕑 Fecha + hora del movimiento (cambio de dominio aditivo)
- `Transaction` gana `time: LocalTime?` (opcional). `register`/`edit`/`rehydrate` y los eventos
  `TransactionRegistered`/`TransactionEdited` lo llevan como `String?` **con default null** → los eventos
  ya serializados siguen deserializando.
- Nueva columna `time TEXT` (nullable) + **migración aditiva idempotente** (`ensureColumn`) → sin borrar
  datos en instalaciones existentes. `date` sigue siendo `LocalDate`, así analítica/agrupación/validación
  «no futuro» quedan intactas.
- UI: **editor** con fila «Fecha · Hora» (`TimePicker` M3, hora = ahora por defecto) y **QuickAdd** con
  chips 📅 fecha / 🕐 hora.

### 🧰 Más UX
- **Duplicar movimiento**: icono de copia en el editor → convierte el movimiento en borrador nuevo
  (mismos datos, fecha = hoy) para revisar y guardar como copia.
- **Borrar categoría** solo si **no tiene movimientos** (comprobación cross-context vía `SearchTransactionsQuery`
  en la capa de app); si está en uso, el botón no se muestra.
- **+130 iconos** de categoría agrupados por temas (manteniendo los ids existentes).
- **Análisis — lente Fijo/Variable**: el desglose pasa a tres lentes **Categoría / Necesidad / Tipo**
  (Fijo/Variable ya se calculaba en el motor; solo faltaba exponerlo).
- **Ajustes**: botón **atrás**.

### ✨ Pulido de interacción
- **Barra inferior**: ripple redondeado (no el recuadro cuadrado) y la pestaña activa deja de ser
  re-pulsable (evita re-navegaciones inesperadas).
- **Selector de color/iconos** del editor: ripple recortado a su forma.
- **`WmSegmented`**: estado **bloqueado** visible (atenuado, sin ripple) — el tipo es fijo al editar — y
  **cambio por gesto de deslizar**, además de tocar.

## Calidad técnica

- **Tests de dominio en verde**: `transactions`, `categories`, `analytics`, `users` (`jvmTest`).
- **Compatibilidad hacia atrás**: el campo `time` es nullable y *defaulted* en eventos y persistencia →
  no rompe el Event Store ni requiere reinstalar.
- **Arquitectura respetada**: la coordinación cross-context (uso de categoría) vive en la capa de app vía
  buses, no acopla los módulos KMP. Ver `memory/respect_architecture.md`.

## Empaquetado

- `versionCode = 4`, `versionName = "0.3.0"` en `apps/android/build.gradle.kts`.
- `./gradlew :apps:android:assembleRelease :apps:android:bundleRelease` → APK 25 MB + AAB 16 MB.
- Firma de release desde `keystore.properties` (gitignorado); `apksigner verify` ⇒ **OK** (V2, `CN=Within Means`).

> ⚠️ **Pendiente de verificación**: *smoke test* del build R8 en device. El emulador se desconectó al
> instalar el APK de release, por lo que no se pudo lanzar. El mismo código se probó como **debug** durante
> el ciclo sin crashes. Para verificar (reinstala, distinta clave de firma):
> `adb uninstall within.means.android && adb install dist/within-means-0.3.0-release.apk`
