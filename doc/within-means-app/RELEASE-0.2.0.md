# 📋 Informe de entrega — Within Means 0.2.0

> Fecha: 2026-06-10 · `versionCode=2` / `versionName=0.2.0`
> Artefactos: `dist/within-means-0.2.0-release.apk` (24 MB) · `dist/within-means-0.2.0-release.aab` (15 MB)
> Build R8 (minify + shrink) **firmado** (V2, `CN=Within Means`). APK **verificado** (`apksigner`).

## Resumen ejecutivo

Iteración sobre 0.1.0 centrada en el **registro de movimientos**: el editor completo se llevó a la
fidelidad plena del diseño (§4.5) y la entrada de importe pasó a una **calculadora con aritmética**,
compartida entre el editor y QuickAdd.

## Novedades de este ciclo (sobre 0.1.0)

### ✍️ Editor de movimiento — fidelidad plena (§4.5)
- Topbar fullover: **cerrar / título / borrar** (papelera en `neg`, solo al editar) + **diálogo de borrado**
  (`DeleteTransactionCommand`).
- **Segmented** de tipo (Gasto/Ingreso/Ahorro), fijo al editar.
- **Importe grande** coloreado por la **categoría seleccionada** (con fallback al color del tipo) y símbolo
  de moneda real.
- **Picker de categoría en chips horizontales con `CatIcon`** (icono + nombre, resaltado por su color).
- **Barra inferior** sticky con CTA «Añadir/Guardar».

### 🧮 Calculadora de importe (nueva)
- `AmountCalculator` — motor puro y testeable: expresión con `+ − × ÷` y precedencia, evaluación con
  `BigDecimal` (sin *float drift*, división redondeada a 2, protección div/0), tope de ≤9 enteros / ≤2
  decimales por operando.
- **Edición estilo nativo**: al abrir sobre un importe existente, el primer **dígito/punto lo reemplaza**
  y un **operador continúa** desde él (`⌫` lo edita en sitio).
- `CalculatorSheet` (editor): botón **C**, display que muestra el **número tal como se teclea** (`€85`,
  `€85.5`) y el **resultado en vivo** al operar; CTA «Usar {importe}».
- **QuickAdd** reutiliza el mismo `CalcKeypad` y `AmountCalculator`: su keypad ahora hace cálculos, con
  resultado en vivo coloreado por tipo.

## Calidad técnica

- **Suite verde**: módulo `apps:android` **105 tests**; toda la suite multiplataforma (`shared`, `users`,
  `categories`, `transactions`, `analytics`) pasa con `./gradlew test`.
- **Tests añadidos este ciclo**: borrado en el editor (elimina + no-op en creación); calculadora pura
  (precedencia, decimales sin drift, div/0, caps de operando, cero/operador líder, reemplazo de operador,
  semilla, reemplazo del valor sembrado, continuar con operador, editar con `⌫`); QuickAdd (aritmética
  con precedencia + renombrados a `expression`).
- **Bug corregido**: en `CalculatorSheet` el resultado/CTA se calculaban fuera de la lambda del
  `ModalBottomSheet` y no leían `State` → el importe se quedaba congelado al teclear. `remember(expression)`
  resuscribe el composable por tecla.

## Empaquetado

- `versionCode = 2`, `versionName = "0.2.0"` en `apps/android/build.gradle.kts`.
- `./gradlew :apps:android:assembleRelease :apps:android:bundleRelease` → APK 24 MB + AAB 15 MB.
- Firma de release desde `keystore.properties` (gitignorado); `apksigner verify` ⇒ **OK**.

> ⚠️ **Pendiente de verificación**: *smoke test* del build R8 en device (instalar el APK release y comprobar
> intro → PIN → creación/serialización de DB → Home sin crashes). Requiere reinstalar (borra datos del
> build debug actual), por eso queda a confirmación.
