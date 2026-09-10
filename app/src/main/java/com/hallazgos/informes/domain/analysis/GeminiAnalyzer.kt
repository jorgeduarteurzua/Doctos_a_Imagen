package com.hallazgos.informes.domain.analysis

import com.hallazgos.informes.domain.model.BodyRegion
import com.hallazgos.informes.domain.model.Finding
import com.hallazgos.informes.domain.model.Lateralidad
import com.hallazgos.informes.domain.model.Severidad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Analiza el texto de un informe usando la API de Gemini (nube). A diferencia
 * del motor por reglas, un LLM "entiende" el texto y detecta hallazgos de
 * cualquier órgano aunque la redacción sea inusual.
 *
 * Devuelve la misma estructura [Finding] que el analizador local, así que el
 * resto de la app (consolidación, imagen, PDF) no cambia.
 *
 * Requiere conexión e implica enviar el texto del informe a Google.
 */
class GeminiAnalyzer(private val apiKey: String) {

    // "flash-lite-latest": modelo ligero y rápido, con free tier más holgado.
    // Verificado como accesible y estable con la API key de AI Studio.
    private val modelo = "gemini-flash-lite-latest"
    private val endpoint =
        "https://generativelanguage.googleapis.com/v1beta/models/$modelo:generateContent"

    /** Reintentos ante servidor ocupado (503) o límite de tasa (429). */
    private val maxReintentos = 3

    /**
     * Analiza el texto y devuelve los hallazgos. Lanza excepción si falla la
     * llamada (para que el ViewModel pueda caer al análisis local).
     */
    suspend fun analizar(texto: String, sourceFileName: String): List<Finding> =
        withContext(Dispatchers.IO) {
            if (texto.isBlank()) return@withContext emptyList()

            val cuerpo = construirRequest(texto)
            val respuesta = llamarApiConReintentos(cuerpo)
            val jsonHallazgos = extraerJsonDeRespuesta(respuesta)
            parsearHallazgos(jsonHallazgos, sourceFileName)
        }

    // ------------------------------------------------------------- Request

    private fun construirRequest(texto: String): String {
        val regionesValidas = BodyRegion.entries.joinToString(", ") { it.name }

        val instruccion = """
            Eres un asistente que extrae hallazgos de informes médicos de imagenología
            (por ejemplo PET/CT) en español. Analiza el texto del informe y devuelve
            SOLO los hallazgos relevantes (lesiones, captación/actividad metabólica
            anormal, nódulos, adenopatías, litiasis, etc.).

            Reglas:
            - NO incluyas hallazgos negados o normales (ej. "sin evidencia de", "metabolismo normal", "sin alteraciones").
            - Para cada hallazgo asigna la región anatómica más adecuada de esta lista EXACTA (usa el valor tal cual): $regionesValidas
            - Si ninguna región encaja, usa OTRA.
            - "lateralidad" debe ser uno de: DERECHO, IZQUIERDO, BILATERAL, NO_APLICA.
            - "severidad" debe ser uno de: ALTA, MEDIA, BAJA, INFORMATIVA (estima según intensidad/SUV; ALTA si SUV alto o descrito intenso).
            - "suvMax" numérico si el texto lo menciona, si no null.
            - "tamanoMm" numérico en milímetros si el texto lo menciona (convierte cm a mm), si no null.
            - "descripcion" una frase clara y breve del hallazgo en español.

            Devuelve EXCLUSIVAMENTE un array JSON. Cada elemento debe tener estas claves:
            region, descripcion, lateralidad, severidad, suvMax, tamanoMm.
            Ejemplo del formato esperado:
            [{"region":"PULMON","descripcion":"Nódulo en lóbulo medio","lateralidad":"DERECHO","severidad":"MEDIA","suvMax":2.2,"tamanoMm":20}]
            Si no hay hallazgos relevantes, devuelve [].

            Texto del informe:
            ---
            $texto
            ---
        """.trimIndent()

        // Pedimos JSON por mime-type + instrucción (sin responseSchema, que
        // ralentiza mucho al modelo). El parseo tolera formato con o sin ```json.
        val root = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", instruccion)))
            }))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1)
                put("responseMimeType", "application/json")
            })
        }
        return root.toString()
    }

    // ------------------------------------------------------------- HTTP

    /** Llama a la API reintentando ante timeout, 503 (ocupado) o 429 (límite de tasa). */
    private suspend fun llamarApiConReintentos(cuerpo: String): String {
        var ultimoError: ReintentableException? = null
        for (intento in 1..maxReintentos) {
            try {
                return llamarApi(cuerpo)
            } catch (e: ReintentableException) {
                ultimoError = e
                if (intento < maxReintentos) {
                    delay(2000L * intento) // espera creciente: 2s, 4s...
                }
            }
        }
        val motivo = if (ultimoError?.message == "timeout") {
            "el servidor tardó demasiado en responder (revisa tu conexión o inténtalo de nuevo)"
        } else {
            "el servidor de Gemini está ocupado"
        }
        throw RuntimeException("No se pudo contactar a Gemini tras varios intentos: $motivo.")
    }

    /** Excepción para errores transitorios que conviene reintentar. */
    private class ReintentableException(mensaje: String) : Exception(mensaje)

    private fun llamarApi(cuerpo: String): String {
        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30000
            readTimeout = 120000  // los LLM pueden tardar en generar; damos margen
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-goog-api-key", apiKey)
        }

        val codigo: Int
        val respuesta: String
        try {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(cuerpo) }
            codigo = conn.responseCode
            val stream = if (codigo in 200..299) conn.inputStream else conn.errorStream
            respuesta = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
        } catch (e: java.net.SocketTimeoutException) {
            // Tiempo de espera agotado: transitorio, conviene reintentar.
            throw ReintentableException("timeout")
        } finally {
            conn.disconnect()
        }

        if (codigo == 503 || codigo == 429) {
            throw ReintentableException("Servidor de Gemini ocupado ($codigo). Reintentando...")
        }
        if (codigo !in 200..299) {
            throw RuntimeException("Error de Gemini ($codigo): ${resumenError(respuesta)}")
        }
        return respuesta
    }

    private fun resumenError(respuesta: String): String {
        return try {
            JSONObject(respuesta).optJSONObject("error")?.optString("message") ?: respuesta.take(200)
        } catch (_: Exception) {
            respuesta.take(200)
        }
    }

    // ------------------------------------------------------------- Parseo

    /** Extrae el texto JSON generado por el modelo de la respuesta de la API. */
    private fun extraerJsonDeRespuesta(respuesta: String): String {
        val root = JSONObject(respuesta)
        val candidates = root.optJSONArray("candidates")
            ?: throw RuntimeException("Respuesta sin candidatos.")
        if (candidates.length() == 0) throw RuntimeException("Respuesta vacía del modelo.")
        val content = candidates.getJSONObject(0).getJSONObject("content")
        val parts = content.getJSONArray("parts")
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            sb.append(parts.getJSONObject(i).optString("text", ""))
        }
        return sb.toString()
    }

    private fun parsearHallazgos(json: String, sourceFileName: String): List<Finding> {
        val limpio = json.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val array = try {
            JSONArray(limpio)
        } catch (_: Exception) {
            return emptyList()
        }

        val findings = mutableListOf<Finding>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val region = mapRegion(obj.optString("region"))
            val descripcion = obj.optString("descripcion").trim()
            if (descripcion.isBlank()) continue

            findings.add(
                Finding(
                    region = region,
                    lateralidad = mapLateralidad(obj.optString("lateralidad")),
                    descripcion = descripcion.replaceFirstChar { it.uppercase() },
                    suvMax = obj.optDoubleOrNull("suvMax"),
                    tamanoMm = obj.optDoubleOrNull("tamanoMm"),
                    severidad = mapSeveridad(obj.optString("severidad")),
                    sourceFileName = sourceFileName,
                    rawText = descripcion
                )
            )
        }
        return findings
    }

    private fun JSONObject.optDoubleOrNull(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val v = optDouble(key, Double.NaN)
        return if (v.isNaN()) null else v
    }

    private fun mapRegion(valor: String): BodyRegion =
        BodyRegion.entries.firstOrNull { it.name.equals(valor.trim(), ignoreCase = true) }
            ?: BodyRegion.OTRA

    private fun mapLateralidad(valor: String): Lateralidad =
        Lateralidad.entries.firstOrNull { it.name.equals(valor.trim(), ignoreCase = true) }
            ?: Lateralidad.NO_APLICA

    private fun mapSeveridad(valor: String): Severidad =
        Severidad.entries.firstOrNull { it.name.equals(valor.trim(), ignoreCase = true) }
            ?: Severidad.MEDIA
}
