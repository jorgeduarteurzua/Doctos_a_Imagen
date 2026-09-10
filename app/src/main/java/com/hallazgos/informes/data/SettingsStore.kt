package com.hallazgos.informes.data

import android.content.Context
import com.hallazgos.informes.BuildConfig

/**
 * Almacena las preferencias del usuario: si usa el análisis con IA en la nube
 * y su API key de Gemini.
 *
 * La API key efectiva se resuelve así:
 *  1. La que el usuario haya guardado manualmente en Ajustes (tiene prioridad).
 *  2. Si no hay, la incrustada en el build (BuildConfig.GEMINI_API_KEY), que
 *     viene de local.properties. Así todos los dispositivos la traen sin
 *     ingresarla a mano.
 *
 * Nota de privacidad: la API key se guarda solo en el dispositivo. El análisis
 * con IA envía el texto del informe a Google (ver aviso en la UI).
 */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("ajustes", Context.MODE_PRIVATE)

    /** Key incrustada en el build (puede estar vacía si no se configuró). */
    private val keyDelBuild: String = BuildConfig.GEMINI_API_KEY.trim()

    /**
     * Activación de la IA. Si el usuario nunca tocó el ajuste pero hay key en
     * el build, se considera activada por defecto.
     */
    var usarIA: Boolean
        get() = prefs.getBoolean(KEY_USAR_IA, keyDelBuild.isNotBlank())
        set(value) = prefs.edit().putBoolean(KEY_USAR_IA, value).apply()

    /** Key introducida por el usuario (vacía si nunca puso una). */
    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    /** Key efectiva: la del usuario si existe, si no la del build. */
    val apiKeyEfectiva: String
        get() = apiKey.ifBlank { keyDelBuild }

    /** True si el análisis con IA está activado y hay una key utilizable. */
    val iaDisponible: Boolean
        get() = usarIA && apiKeyEfectiva.isNotBlank()

    /** True si el build trae una key incrustada (no requiere que el usuario la ponga). */
    val hayKeyDelBuild: Boolean
        get() = keyDelBuild.isNotBlank()

    companion object {
        private const val KEY_USAR_IA = "usar_ia"
        private const val KEY_API_KEY = "api_key"
    }
}
