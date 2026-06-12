# Desbloqueo con huella — Especificación (spec-driven)

> **Alcance:** añadir **ingreso por huella** (biometría) como **vía alternativa** de
> desbloqueo de la app, **sin sustituir** el PIN. El PIN sigue siendo la fuente de
> verdad para derivar la clave de cifrado de la base de datos (SQLCipher) y la **única
> vía de recuperación**. La huella es un atajo cómodo para no teclear el PIN en cada
> arranque, no un segundo factor ni un mecanismo de cifrado nuevo.
>
> Solo afecta a `apps/android` (capa de infraestructura + UI). **No toca ningún
> bounded context KMP** (`shared`, `users`, `categories`, `transactions`, `analytics`):
> el control de acceso vive en la app Android, no en el dominio. Ver
> [[respect_architecture]] y [[cross-context-subscribers]].
>
> Donde choque con [`SPEC.md`](SPEC.md), manda `SPEC.md` para el sistema de diseño;
> este documento manda para el **flujo de desbloqueo y su seguridad**.
>
> Convención de etiquetas (igual que `SPEC.md`): `[existe]`, `[restyle]`, `[nuevo-ui]`,
> `[nuevo-dominio]`, `[mock]`.

---

## 1. Visión y principios

El usuario abre la app varias veces al día. Teclear 4 dígitos cada vez es fricción
pura. La huella elimina esa fricción **sin bajar el nivel de seguridad**: la base de
datos sigue cifrada con AES-256 y la clave se sigue derivando del PIN; la biometría
solo **custodia el PIN** detrás del sensor del dispositivo.

Principios, **en orden de prioridad**:

0. **Simplicidad ante todo (regla #0).** El onboarding **no cambia**: el usuario novato
   nunca ve biometría hasta que la busca. La huella es **opt-in desde Ajustes** (§2).
   Cuando está activa, abrir la app es un gesto: el sensor se dispara solo (§5).
   Ver [[simplicity_first_for_user]].
1. **El PIN nunca desaparece.** La huella es complemento, no reemplazo. En cualquier
   pantalla de desbloqueo hay siempre una salida a PIN. Si la biometría falla, se
   invalida o el hardware no está, el usuario **no queda fuera de sus datos**: cae al
   PIN de forma transparente.
2. **Cero claves nuevas en el cifrado de la DB.** No se introduce un segundo esquema
   de derivación. La passphrase de SQLCipher sigue siendo
   `HMAC-SHA256(masterKey, utf8(pin))` ([`PassphraseProvider.kt`](../../apps/android/src/main/kotlin/within/means/android/persistence/PassphraseProvider.kt)).
   La huella solo descifra el **PIN guardado**, que luego entra al pipeline `unlock(pin)`
   **ya existente** ([`DatabaseUnlocker.kt`](../../apps/android/src/main/kotlin/within/means/android/persistence/DatabaseUnlocker.kt)).
3. **Seguridad por defecto.** Solo biometría **fuerte** (`BIOMETRIC_STRONG`, clase 3).
   Autenticación requerida **por cada uso** de la clave. Si el usuario **enrola una huella
   nueva** en el sistema, la clave biométrica se **invalida** y se exige PIN: una huella
   añadida por un tercero no debe dar acceso (§6, decisión D).
4. **Honesta con el alcance.** Esta spec describe **lo que se va a construir**. Hoy
   **no existe** ningún código biométrico en el repo (PIN-only). Todo lo aquí descrito
   es `[nuevo-dominio]` salvo lo marcado `[existe]`.

> Principio rector: **la huella custodia el PIN, no la base de datos.** Quien controla
> el cifrado sigue siendo el PIN + la master key del Keystore. La biometría es una
> cerradura cómoda delante de una caja fuerte que no cambia.

---

## 2. Ubicación y activación `[nuevo-ui]`

- **No hay paso de biometría en el onboarding.** El primer arranque sigue siendo
  Welcome → PIN → Preferencias → Home, sin cambios.
- **Activación: fila nueva en Ajustes** — **"Desbloquear con huella"** con un `Switch`,
  junto a "Cambiar PIN"
  ([`SettingsScreen.kt`](../../apps/android/src/main/kotlin/within/means/android/ui/settings/SettingsScreen.kt)).
- La fila **solo aparece** si el dispositivo tiene hardware biométrico **y** al menos
  una huella enrolada (`BiometricManager.canAuthenticate(BIOMETRIC_STRONG) == SUCCESS`).
  Si no hay hardware o no hay huellas, la fila **se oculta** (no se muestra deshabilitada
  con jerga: simplicidad-ante-todo). Opcional: si hay hardware pero **sin** huellas
  enroladas, mostrar la fila deshabilitada con subtítulo "Añade una huella en los ajustes
  del teléfono".
- Al **activar** el switch → se lanza `BiometricPrompt` en modo cifrado; al confirmar la
  huella, se **envuelve el PIN actual** y se persiste (§4, flujo enroll). Para envolver
  hace falta que la app esté **desbloqueada** (el PIN ya está en memoria de sesión, o se
  pide una vez).
- Al **desactivar** el switch → se borra el vault (ciphertext + IV) y la clave del Keystore
  (§4). Vuelve a PIN puro en el siguiente arranque.

---

## 3. Modelo de seguridad

```
                       ┌─────────────────── Android Keystore (hardware) ──────────────────┐
                       │                                                                   │
  PIN (4 díg.) ──┐     │  master_key (HMAC-SHA256, no exportable)   [existe]               │
                 ├─────┼─► passphrase = HMAC(master_key, PIN) ─► SQLCipher AES-256         │
                 │     │                                                                   │
                 │     │  biometric_key (AES-256-GCM)              [nuevo-dominio]          │
                 │     │   · setUserAuthenticationRequired(true)                           │
                 └─────┼──► · setInvalidatedByBiometricEnrollment(true)                    │
   wrap (enroll)       │     · solo desbloqueable con BIOMETRIC_STRONG                      │
                       └───────────────────────────────────────────────────────────────────┘
                                         │ Cipher autenticado por BiometricPrompt
                                         ▼
   EncryptedSharedPreferences:  { pinCiphertext, iv }   [nuevo-dominio]
                                         │ decrypt (unlock)
                                         ▼
                                   PIN ─► unlock(PIN)  [existe]
```

- **`master_key`** `[existe]`: HMAC-SHA256 no exportable, ya gestionada por
  [`KeystoreManager.kt`](../../apps/android/src/main/kotlin/within/means/android/persistence/KeystoreManager.kt).
  **No se toca.** Deriva la passphrase del PIN. La huella **no** interactúa con esta clave.
- **`biometric_key`** `[nuevo-dominio]`: clave AES-256-GCM **nueva**, alias
  `within_means.biometric_key`, generada en el Keystore con:
  - `KeyProperties.PURPOSE_ENCRYPT or PURPOSE_DECRYPT`
  - `setUserAuthenticationRequired(true)` + `setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG)`
    → la clave **exige** una autenticación biométrica fresca **por cada operación** (timeout 0).
  - `setInvalidatedByBiometricEnrollment(true)` → si se **añade/cambia** una huella en el
    sistema, la clave se **destruye sola**. El descifrado lanza
    `KeyPermanentlyInvalidatedException` → se borra el vault y se cae a PIN (§6-D).
- **Vault** `[nuevo-dominio]`: `pinCiphertext` (PIN cifrado AES-GCM) + `iv`, guardados en
  **EncryptedSharedPreferences** (mismo esquema AES-GCM que
  [`OnboardingState.kt`](../../apps/android/src/main/kotlin/within/means/android/persistence/OnboardingState.kt)).
  Doble capa: el ciphertext ya es inútil sin la `biometric_key` del hardware; las EncSP
  evitan además su lectura/manipulación off-device.

> **Por qué guardar el PIN y no la passphrase** (decisión C, §6): reusar `unlock(pin)`
> sin tocar la derivación, y **no** sacar nunca la clave real de SQLCipher (32 bytes)
> al flujo biométrico. La superficie de cambio se reduce a "obtener el PIN por otra vía".

---

## 4. Componentes nuevos (infraestructura) `[nuevo-dominio]`

Todos en `apps/android/.../persistence/` salvo indicación. Nada en módulos KMP.

| Componente | Rol |
|---|---|
| `BiometricVault` | Genera/gestiona `biometric_key`; `wrap(pin, Cipher)` y `unwrap(Cipher): String`; persiste/borra `{ciphertext, iv}` en EncSP; expone `isEnrolled`. |
| `BiometricGate` | Envuelve el **`BiometricPrompt` nativo del framework** (`android.hardware.biometrics`, sin librería externa). Construye el prompt con `CryptoObject(Cipher)`, lo presenta y resuelve un coroutine con el `Cipher` autenticado (`suspendCancellableCoroutine`). |
| `BiometricAvailability` | Consulta el `BiometricManager` nativo: `canAuthenticate(BIOMETRIC_STRONG)`. Mapea a `{ AVAILABLE, NONE_ENROLLED, NO_HARDWARE, UNSUPPORTED }` para decidir UI (§2). |

- **Sin dependencias nuevas** (decisión G): se usa el `BiometricPrompt`/`BiometricManager`
  **nativos del framework**, no `androidx.biometric`. Funcionan con el `ComponentActivity`
  actual sin migrar a `FragmentActivity`. El vault reusa `androidx.security.crypto`
  (`EncryptedSharedPreferences`), que **ya** es dependencia (la usa `OnboardingState`).
- **Gate de versión**: la feature se habilita en **API 30+** (Android 11). Ahí
  `setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG)` y los `Authenticators`
  están disponibles de forma uniforme. `minSdk` sigue en 23; por debajo de 30 la fila de
  Ajustes y el prompt **no aparecen** (app PIN-only, sin regresión). `BiometricVault` se
  construye en cualquier API (las llamadas al Keystore están aisladas y anotadas
  `@RequiresApi(R)`; `clear`/`isEnrolled`/`store`/`retrievePin` son agnósticas).
- **Permiso**: `<uses-permission android:name="android.permission.USE_BIOMETRIC" />` en
  el `AndroidManifest.xml` de la app.
- **DI (Koin)**: `BiometricVault` y `BiometricAvailability` se registran como singletons en
  [`AppModule.kt`](../../apps/android/src/main/kotlin/within/means/android/di/AppModule.kt);
  `BiometricGate` necesita la activity en foreground, así que se crea **por pantalla** (no
  singleton) vía `rememberBiometricSession()` con el `Context` actual. No se resuelve
  eagermente en ningún módulo global. Ver [[koin_cycle_for_event_subscribers]].

**Flujo enroll (activar en Ajustes):**
1. Switch ON → `BiometricVault.generateKeyIfNeeded()` → `Cipher` en modo `ENCRYPT`.
2. `BiometricGate.authenticate(cipher)` → `BiometricPrompt` ("Confirma tu huella para
   activar el desbloqueo rápido").
3. Éxito → `cipher.doFinal(pin.toByteArray())` → guardar `{ciphertext, iv}` en EncSP →
   `biometricEnabled = true`.
4. Cancelar/fallar → no se persiste nada, switch vuelve a OFF.

**Flujo unlock (abrir la app):**
1. Arranque con `biometricEnabled == true` y `BiometricAvailability == Available`.
2. `Cipher` en modo `DECRYPT` inicializado con el `iv` guardado.
3. `BiometricGate.authenticate(cipher)` → prompt automático.
4. Éxito → `pin = String(cipher.doFinal(ciphertext))` → `unlocker.unlock(pin)` →
   force-touch query (igual que [`UnlockViewModel.kt:46`](../../apps/android/src/main/kotlin/within/means/android/ui/unlock/UnlockViewModel.kt#L46)) → Home.
5. Fallo/cancelación/lockout/invalidación → **cae a PIN** (§5, §6).

---

## 5. Cambios de UI

### 5.1 Pantalla de desbloqueo `[restyle]`
[`UnlockScreen.kt`](../../apps/android/src/main/kotlin/within/means/android/ui/unlock/UnlockScreen.kt) /
[`UnlockViewModel.kt`](../../apps/android/src/main/kotlin/within/means/android/ui/unlock/UnlockViewModel.kt)

- Si la huella está activa y disponible: al entrar (`LaunchedEffect`) se **dispara
  `BiometricPrompt` automáticamente**. El teclado de PIN queda detrás, listo.
- El prompt del sistema ya ofrece su propio botón de cancelar; al cancelar, queda visible
  un enlace/botón **"Usar PIN"** que da foco al teclado numérico **existente**.
- El usuario **siempre** puede ignorar la huella y teclear el PIN directamente.
- `UnlockUiState` gana: `biometricAvailable: Boolean`, `biometricError: String?`.
  El path PIN actual (`updatePin`, `submit`) **no cambia**.

### 5.2 Ajustes `[nuevo-ui]`
- Fila **"Desbloquear con huella"** con `Switch` + subtítulo corto ("Entra con tu huella
  en vez del PIN"). Visibilidad según §2.
- Nuevo `BiometricSettingsViewModel`: orquesta enroll/disable y refleja el estado real
  del vault.

### 5.3 Microcopia (ES, sin jerga)
- Prompt activar: **"Confirma tu huella para activar el desbloqueo rápido"**.
- Prompt entrar: título **"Within Means"**, subtítulo **"Desbloquea con tu huella"**,
  negativo **"Usar PIN"**.
- Huella inválida tras enrolar otra: **"Tus huellas cambiaron. Entra con tu PIN y vuelve
  a activar el desbloqueo rápido si quieres."**

---

## 6. Decisiones (RESOLVED)

- **A. Activación solo opt-in desde Ajustes** (no en onboarding). El primer arranque no
  gana pasos; la huella se descubre cuando se busca. *Confirmada por el usuario.*
- **B. Huella primero, PIN como salida permanente.** En unlock, el sensor se dispara
  solo; "Usar PIN" siempre visible. *Confirmada.*
- **C. Se guarda el PIN envuelto, no la passphrase.** Reusa `unlock(pin)` sin tocar la
  derivación y mantiene la clave de SQLCipher fuera del flujo biométrico. *Confirmada.*
- **D. La clave biométrica se invalida al enrolar una huella nueva**
  (`setInvalidatedByBiometricEnrollment(true)`). Decisión de seguridad: una huella
  añadida por un tercero no abre la app. Al detectar `KeyPermanentlyInvalidatedException`
  se borra el vault, se apaga `biometricEnabled` y se exige PIN.
- **E. Cambiar el PIN desactiva la huella.** Tras un `changePin` exitoso
  ([`DatabaseUnlocker.changePin`](../../apps/android/src/main/kotlin/within/means/android/persistence/DatabaseUnlocker.kt#L66)),
  el PIN envuelto queda obsoleto. En vez de re-envolver silenciosamente (exigiría otro
  prompt biométrico a mitad de flujo), se **borra el vault** y se muestra un aviso de una
  línea para re-activar la huella en Ajustes. Más simple y sin estados intermedios frágiles.
- **F. Solo `BIOMETRIC_STRONG` (clase 3).** Nada de credencial de dispositivo ni biometría
  débil: la clave del Keystore exige clase 3.
- **G. Implementación nativa, sin librerías.** A petición explícita ("que sea nativo") se
  usa el `BiometricPrompt`/`BiometricManager` del framework en vez de
  `androidx.biometric`. Consecuencia: feature gateada a **API 30+** (§4); por debajo, la
  app sigue PIN-only. *Confirmada por el usuario.*

---

## 7. Casos límite y fallback

| Situación | Comportamiento |
|---|---|
| Hardware sin sensor / sin huellas enroladas | La fila de Ajustes se oculta (o deshabilita con nota). Nunca se promete algo inexistente. |
| Huella nueva enrolada en el sistema | `KeyPermanentlyInvalidatedException` al descifrar → borrar vault, `biometricEnabled = false`, caer a PIN con aviso (D). |
| Demasiados intentos (lockout temporal/permanente) | `BiometricPrompt` reporta `ERROR_LOCKOUT*` → mostrar "Usar PIN"; el PIN no depende del sensor. |
| Usuario cancela el prompt | Queda en la pantalla de PIN, sin error rojo. |
| Cambia el PIN | Vault borrado, huella desactivada, aviso para re-activar (E). |
| Wipe de datos de la app | Se destruyen master key, biometric key y vault: la DB queda ilegible (igual que hoy). No hay regresión. |
| Reinicio del teléfono sin desbloquear aún | La clave biométrica sigue disponible tras el primer desbloqueo del dispositivo; si no, PIN. |

---

## 8. Plan de implementación (fases)

- **F0 — Andamiaje** `[nuevo-dominio]`: dependencia `androidx.biometric`, permiso en
  Manifest, `BiometricAvailability`. Sin UI todavía. *Verificable:* la app compila y
  `canAuthenticate` reporta el estado correcto en un dispositivo de prueba.
- **F1 — Vault**: `BiometricVault` (generar clave, wrap/unwrap, persistir/borrar) +
  `BiometricGate`. Tests instrumentados de round-trip wrap→unwrap con huella (emulador
  con biometría).
- **F2 — Activación en Ajustes**: fila + `Switch` + `BiometricSettingsViewModel` + flujo
  enroll/disable. *Verificable:* activar guarda vault; desactivar lo borra.
- **F3 — Unlock con huella**: integrar prompt automático en `UnlockScreen`/`UnlockViewModel`,
  con "Usar PIN" siempre disponible. *Verificable:* abrir la app entra con huella; cancelar
  cae a PIN.
- **F4 — Casos límite**: manejar invalidación (D), lockout, `changePin` (E), microcopia
  final. *Verificable:* enrolar una huella nueva fuerza PIN y limpia el vault.

> Cada fase debe poder mergearse sin romper el flujo PIN: la biometría es siempre un
> camino **adicional** sobre el unlock existente.

---

## 9. Qué NO cambia (para evitar regresiones)

- Derivación de passphrase y `KeystoreManager` `[existe]` — intactos.
- `unlock(pin)` / `changePin` / `lock` de `DatabaseUnlocker` `[existe]` — la huella los
  **invoca**, no los reescribe.
- Onboarding, `OnboardingState`, `PinPolicy` `[existe]` — sin cambios.
- Ningún módulo KMP, ningún bus, ningún evento de dominio. El control de acceso es
  responsabilidad de la app Android. Ver [[respect_architecture]].

**Cambio colateral (no biométrico):** "Bloquear ahora" y "Cambiar PIN" ahora **reinician
el proceso** (`relaunchToLock` en `MainActivity`) en vez de navegar a Unlock dentro de la
sesión. Motivo: los `single<XDatabase>` de `PersistenceModule` cachean el handle de la DB;
al cerrar (`lock`) o re-keyear (`changePin`) en el mismo proceso, esos singletons quedan
**stale** y la siguiente consulta reactiva crashea (`already-closed object`). El reinicio
fuerza un arranque en frío con grafo limpio y, de paso, borra el estado descifrado de
memoria. Descubierto al probar el desbloqueo por huella en sesión; afectaba igual al
re-desbloqueo con PIN.

---

## 10. Notas de fidelidad y alcance

- **Estado:** implementado de forma nativa (sin librerías) en `apps/android`:
  `BiometricAvailability`, `BiometricVault`, `BiometricGate` (persistence) +
  `BiometricSession`/`rememberBiometricSession` (ui/unlock), fila en Ajustes y prompt
  automático en `UnlockScreen`. Antes de esto el repo era **PIN-only**. La feature solo
  se activa en API 30+ y siempre como vía **adicional** al PIN.
- La huella **no es un segundo factor** ni cifra la base de datos: es una custodia cómoda
  del PIN. La seguridad criptográfica de los datos sigue anclada al PIN + master key.
- Solo Android. El target `jvmMain`/Desktop (post-MVP) no entra en esta spec.
- Esta spec manda para el flujo de desbloqueo; para colores, tipografía y componentes,
  manda [`SPEC.md`](SPEC.md).
