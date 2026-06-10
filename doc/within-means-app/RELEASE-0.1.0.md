# 📋 Informe de entrega — Within Means 0.1.0

> Fecha: 2026-06-10 · `versionCode=1` / `versionName=0.1.0`
> Artefactos: `within-means-0.1.0-release.apk` (25 MB) · `within-means-0.1.0-release.aab` (16 MB)

## Resumen ejecutivo
App de finanzas personales **local y cifrada** (Kotlin Multiplatform + DDD), construida sobre un MVP visual. En este ciclo se llevó de "rediseño visual" a **app funcional completa y empaquetada para release firmado**.

**Magnitud del trabajo** (desde el baseline pre-Diferido):

- **126 archivos**, **+8.655 / −847 líneas**, ~16 commits de feature
- **383 tests** unitarios (verdes en los 6 módulos)
- **12 pantallas** Compose · **5 bounded contexts** (`shared`, `users`, `categories`, `transactions`, `analytics`) + `apps:android`
- Empaquetado **release firmado** (APK 25 MB + AAB 16 MB, R8)

---

## 1. Funcionalidad entregada

### 💸 Dominio financiero

| Feature | Estado |
|---|---|
| **Ahorro / Transferencia** (`TransactionType.TRANSFER`) — neutro al balance, color propio | ✅ |
| **Presupuesto mensual + hero "Disponible" + ritmo/día** (cálculo de ciclo) | ✅ |
| **Inicio del mes configurable** (día 1–28; el ciclo de presupuesto lo respeta) | ✅ |
| **Recurrentes**: crear · materializar (catch-up idempotente) · **gestionar** · **editar** · **desactivar** | ✅ |

### ✍️ Registro de movimientos

- **QuickAdd** (bottom sheet con teclado numérico propio) sobre el FAB
- **Editor completo** (tipo, categoría, fecha, nota, recurrente) — alcanzable desde Movimientos

### 📊 Analítica

- **Periodos** Semana / Mes / Año (analítica por rango)
- **Tasa de ahorro real** comparada con el periodo anterior (▲/▼ pts) — *reemplaza el mock*
- **Evolución del gasto** con **granularidad por periodo** (6 semanas / meses / años)
- Desglose por categoría/tipo + donut "en qué va el mes"

### 🔐 Seguridad

- **PIN de 4 dígitos** + keypad propio · **cifrado SQLCipher** (passphrase = HMAC-SHA256(masterKey en Keystore, PIN))
- **Cambiar PIN** = **re-key real** de la base de datos (sin perder datos)
- **Bloquear ahora** · **Ocultar importes al abrir** (máscara + ojo de revelar)

### ⚙️ Ajustes y onboarding

- **Tema** claro / oscuro / sistema (aplica al instante) · idioma · moneda · plan + alertas
- **Onboarding** rediseñado: carrusel de 3 slides ilustrados + dots + Saltar · "Ver introducción"

### ✨ Motion

- Count-up de cifras, entrada escalonada, guardia de **reduced-motion**

---

## 2. Calidad técnica destacable

- **Arquitectura DDD limpia**: agregados con eventos de dominio, buses CQRS (Command/Query/Event), serialización de eventos por nombre. Sin acoplar `:analytics` con `:users` (el presupuesto se calcula en el adapter).
- **Migraciones in-place sin pérdida de datos** — cerró el gap que arrastraba el proyecto. *(Se descubrió y corrigió en device un bug real: el `user_version` global de SQLCipher rompía el enfoque `.sqm`; se cambió por `ALTER` idempotente guardado.)*
- **Re-key de PIN** con cierre de los 4 drivers → `changePassword` → re-unlock, con recuperación ante fallo.
- **R8/minify** con reglas ProGuard completas (SQLCipher/JNI, serializers, Koin, Tink).

---

## 3. Verificación (Pixel_9, API 36)

- ✅ **R8 release** arranca end-to-end (intro → PIN → creación/serialización de DB → Home), **0 crashes**
- ✅ **Cambiar PIN** round-trip exhaustivo: re-key OK · PIN viejo rechazado · PIN nuevo entra · **datos intactos**
- ✅ Recurrentes (crear→materializar→gestionar→desactivar), tema oscuro, evolución semanal, periodos, unlock 4-puntos
- 383 tests verdes; `assembleRelease` + `bundleRelease` firmados

---

## 4. Artefactos de release

| Archivo | Tamaño | Uso |
|---|---|---|
| `within-means-0.1.0-release.apk` | 25 MB | Sideload / instalación directa |
| `within-means-0.1.0-release.aab` | 16 MB | Subir a Google Play |

Firma **V2 válida** · `versionCode=1` / `versionName=0.1.0`.

🔐 **Keystore de firma** (`within-means-release.jks`, gitignorado) — **haz backup del `.jks` + la contraseña o no podrás publicar actualizaciones en Play.**

### Instalar la APK

```bash
adb install -r ~/Desktop/within-means-0.1.0-release.apk
```

---

## 5. No incluido / limitaciones conocidas

- **"Inicio del mes"** afecta al ciclo de presupuesto, pero el resto de la analítica sigue por mes natural.
- Sin **multi-cuenta**, sin **export/backup**, sin **sincronización** (es local por diseño).
- Sin tests instrumentados (la cobertura es unitaria por capa, fuerte).
- Recurrentes: editar cambia importe/categoría/nota/frecuencia; **el tipo y la fecha de inicio son fijos**.

---

## 6. Estado de entrega (ramas / PRs)

El **APK 0.1.0 contiene TODO** lo anterior, pero **no todo está en `main`**:

| Bloque | En `main` |
|---|---|
| Diferido F2–F5 (Ahorro, Presupuesto, Recurrentes, QuickAdd, Periodos, PIN, Motion) — **PR #1** | ✅ merged |
| Diferido tail (gestión recurrentes, migración in-place, month-start/hide-amounts, lock/intro, editor) — **PR #2** | ✅ merged |
| Tema + tasa de ahorro real — **PR #3** | 🟡 OPEN, sin mergear |
| Editar recurrentes · evolución · cambiar PIN (0.2) | 🔴 rama sin PR (encima de #3) |

> El APK se construyó desde la rama de 0.2, así que va **por delante de `main`**. Para alinear el repo: mergear **PR #3** y abrir un PR para las 3 de 0.2.

---

## 7. Commits de feature (16)

```
change PIN — re-key the encrypted database (SQLCipher rekey)
period-aware spending-evolution granularity (week/month/year)
edit a recurring rule (amount/category/description/frequency)
real previous-period savings-rate comparison (replaces mock)
appearance theme toggle (system/light/dark)
reachable full editor — + action on Movimientos header
additive migration via guarded ALTER instead of .sqm version bump
onboarding intro carousel + Bloquear ahora + Ver introducción
configurable month-start + hide-amounts settings
in-place SQLDelight migrations + month-start/hide-amounts columns
recurring-rule management — deactivate command + manager screen
motion & polish — count-up, staggered entrance, reduced-motion (F5)
4-digit PIN (F4)
Week / Month / Year periods in Analysis (F4)
QuickAdd bottom sheet with numeric keypad (F4)
deferred domain — savings (TRANSFER), monthly budget, recurring rules
```
