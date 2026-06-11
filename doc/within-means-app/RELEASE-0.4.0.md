# 📋 Informe de entrega — Within Means 0.4.0

> Fecha: 2026-06-11 · `versionCode=5` / `versionName=0.4.0`
> Artefactos: `dist/within-means-0.4.0-release.apk` (~24 MB) · `dist/within-means-0.4.0-release.aab` (~16 MB)
> Build R8 (minify + shrink) **firmado** (V2, `CN=Within Means`). APK **verificado** (`apksigner`).
> ✅ **Smoke test en device superado** (onboarding → Home → Ajustes → vista de ayuda, sin crashes).

## Resumen ejecutivo

Ciclo de **educación financiera en producto**. Se añade una **vista de ayuda ("Aprende a mover tu
dinero")** que enseña a usar la app y a mover el dinero con técnicas de finanzas personales, **ancladas a
los clasificadores que el dominio ya modela** (esencial/discrecional, fijo/variable, productivo, ingreso
activo/pasivo). No introduce dominio nuevo: es contenido + navegación. Diseño **minimalista, profesional y
cálido**, con la **simplicidad como regla rectora**: superficie mínima por defecto, profundidad opt-in.

## Novedades de este ciclo (sobre 0.3.0)

### 📚 Vista de ayuda y educación financiera
- Nueva pantalla **`HelpScreen`** accesible desde **Ajustes → "Aprende a mover tu dinero"**.
- **Simplicidad primero (regla #0):** al abrir solo se ve la **ruta guiada de 4 pasos** (Anota lo que entra
  · Aparta tu ahorro · Anota lo que gastas · Mira cómo vas), un colapsable **"¿Quieres profundizar?"
  cerrado** y un descargo amable. Nada se impone.
- **Profundidad opt-in:** al abrir el colapsable aparecen 3 bibliotecas de lecciones plegadas —
  *Lee tus números*, *Técnicas para mover mejor el dinero* y *El flujo completo* — como acordeones.
- **Lecciones ancladas al dominio:** cada técnica se mapea a un dato real (tasa de ahorro, esencial vs
  discrecional, fijo vs variable, gasto productivo, ingreso activo/pasivo, fondo de emergencia, 50/30/20,
  págate primero, Kakebo). Lo aún no calculado se presenta como referencia, sin prometer pantalla.
- **Sin jerga en pantalla (§6.3):** los títulos usan lenguaje cotidiano ("Cuánto te queda al mes",
  "Dinero que trabaja por ti"); el término técnico aparece solo como letra pequeña ("también llamado…").
- **Deep-links accionables:** los pasos y lecciones llevan a las pantallas reales (registro rápido,
  Análisis, Categorías).

### 🎨 Lenguaje visual (minimalista profesional y cálido)
- Reutiliza el sistema de diseño existente (`WmCard`, tokens, radios) — **sin tokens ni componentes nuevos**.
- Filas con hairline (no tarjetas con sombra), paleta neutra, acento de marca solo en acciones,
  iconografía/emoji en badge suave, acordeón con motion amable, botones tonales discretos.

### 📝 Spec-driven
- Especificación previa: `doc/within-means-app/HELP-EDUCATION-SPEC.md` (regla #0, superficie mínima §4,
  contenido anclado §6, jerga→llano §6.3, lenguaje visual §4.5).

## Calidad técnica

- **Compila limpio** (`:apps:android:compileDebugKotlin`) y **build R8 de release** correcto.
- **Arquitectura respetada:** la vista es UI pura en `apps/android/ui/help/`; no toca los módulos KMP ni
  introduce dominio. El contenido se modela como dato (`Lesson`/`LessonLibrary`) → testeable y traducible.
- **Sin cambios de persistencia ni de Event Store** → no requiere migración ni reinstalar en instalaciones
  existentes.

## Empaquetado

- `versionCode = 5`, `versionName = "0.4.0"` en `apps/android/build.gradle.kts`.
- `./gradlew :apps:android:assembleRelease :apps:android:bundleRelease` → APK ~24 MB + AAB ~16 MB.
- Firma de release desde `keystore.properties` (gitignorado); `apksigner verify` ⇒ **OK** (V2, `CN=Within Means`).

## Verificación

✅ **Smoke test del build R8 de release** en emulador (Pixel 9, API 36), realizado con install del APK firmado:
- Onboarding completo (intro → PIN → preferencias) sin crashes.
- Home, Ajustes y la nueva **vista de ayuda** renderizan correctamente.
- Ruta guiada → abre el registro rápido (deep-link OK).
- Colapsable "¿Quieres profundizar?" → bibliotecas con lecciones plegadas.
- Acordeón de lección → bloques Qué es / Por qué importa / En la app / La idea + "también llamado" + acción.
