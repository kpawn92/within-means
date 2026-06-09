# ============================================================================
# within means — reglas R8/ProGuard para el build de release.
# isMinifyEnabled + isShrinkResources están activos; estas reglas evitan que
# R8 elimine/renombre clases que se resuelven por reflexión o por JNI.
# ============================================================================

# --- SQLCipher (net.zetetic) -------------------------------------------------
# El driver carga código nativo vía JNI; sus clases no deben ofuscarse.
-keep class net.zetetic.database.** { *; }
-keep class net.sqlcipher.** { *; }
-dontwarn net.zetetic.**
-dontwarn net.sqlcipher.**

# --- kotlinx.serialization ---------------------------------------------------
# Mantiene los serializers generados y los metadatos que necesitan.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
# Los eventos de dominio @Serializable se (de)serializan desde el Event Store
# por nombre de clase; conserva la clase y su companion con el serializer.
-keep,includedescriptorclasses class within.means.**$Companion { *; }
-keep @kotlinx.serialization.Serializable class within.means.** { *; }

# --- Koin --------------------------------------------------------------------
# Koin resuelve por tipo; conserva constructores de los componentes inyectados.
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# --- Kermit (logging) --------------------------------------------------------
-dontwarn co.touchlab.kermit.**

# --- androidx.security.crypto / Google Tink ---------------------------------
# EncryptedSharedPreferences usa Tink, que referencia anotaciones compile-only
# (javax.annotation, errorprone). No están en runtime: silenciar para R8.
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
-dontwarn javax.annotation.**
-dontwarn javax.annotation.concurrent.**
-dontwarn com.google.errorprone.annotations.**

# --- Modelo de dominio (defensivo) ------------------------------------------
# Mantiene los miembros de los agregados/VOs por si algún path los toca por
# reflexión (kotlinx.serialization de eventos anidados, etc.).
-keepclassmembers class within.means.** {
    <init>(...);
}
