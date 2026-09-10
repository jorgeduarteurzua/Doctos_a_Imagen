package com.hallazgos.informes.domain.analysis

import com.hallazgos.informes.domain.model.BodyRegion
import com.hallazgos.informes.domain.model.Finding
import com.hallazgos.informes.domain.model.Lateralidad
import com.hallazgos.informes.domain.model.Severidad

/**
 * Motor de análisis por reglas (offline). Recibe el texto plano extraído de un
 * documento y devuelve una lista de hallazgos.
 *
 * IMPORTANTE: es una ayuda heurística basada en palabras clave, NO una
 * interpretación clínica. Es conservador y siempre conserva el texto original.
 */
class FindingsAnalyzer {

    // SUVmax: acepta "SUV max 8.5", "SUVmax: 8,5", "SUV de 12", etc.
    private val regexSuv = Regex(
        "suv\\s*(?:m[aá]x)?\\.?\\s*(?:de|:|=)?\\s*(\\d{1,3}(?:[.,]\\d{1,2})?)",
        RegexOption.IGNORE_CASE
    )

    // Tamaños: "12 mm", "1.5 cm", "2,3 cm"
    private val regexTamanoMm = Regex("(\\d{1,3}(?:[.,]\\d{1,2})?)\\s*mm", RegexOption.IGNORE_CASE)
    private val regexTamanoCm = Regex("(\\d{1,2}(?:[.,]\\d{1,2})?)\\s*cm", RegexOption.IGNORE_CASE)

    fun analizar(texto: String, sourceFileName: String): List<Finding> {
        if (texto.isBlank()) return emptyList()

        val oraciones = segmentar(texto)
        val findings = mutableListOf<Finding>()

        for (oracion in oraciones) {
            val normal = oracion.lowercase()

            val region = detectarRegion(normal) ?: continue

            val negado = estaNegado(normal)
            val tieneActividad = Diccionarios.terminosActividad.any { normal.contains(it) }

            val suv = extraerSuv(oracion)
            val tamano = extraerTamanoMm(oracion)

            // Si la frase está negada, no la reportamos aunque tenga cifras
            // (ej. "sin adenopatías mayores de 10 mm").
            // Si no está negada, reportamos cuando hay actividad descrita o una
            // medida concreta (SUV/tamaño) asociada a la región.
            val relevante = !negado && (tieneActividad || suv != null || tamano != null)
            if (!relevante) continue

            val lateralidad = detectarLateralidad(normal, region)
            val severidad = calcularSeveridad(normal, tieneActividad, negado, suv)

            findings.add(
                Finding(
                    region = region,
                    lateralidad = lateralidad,
                    descripcion = limpiar(oracion),
                    suvMax = suv,
                    tamanoMm = tamano,
                    severidad = severidad,
                    sourceFileName = sourceFileName,
                    rawText = oracion.trim()
                )
            )
        }
        return findings
    }

    private fun segmentar(texto: String): List<String> {
        // Divide por saltos de línea y por punto seguido de espacio/mayúscula.
        return texto
            .replace("\r", "\n")
            .split(Regex("(?<=[.;:])\\s+|\\n+"))
            .map { it.trim() }
            .filter { it.length >= 4 }
    }

    private fun detectarRegion(oracionNormal: String): BodyRegion? {
        for ((region, terminos) in Diccionarios.regiones) {
            if (terminos.any { oracionNormal.contains(it) }) {
                return region
            }
        }
        return null
    }

    private fun estaNegado(oracionNormal: String): Boolean {
        return Diccionarios.negaciones.any { oracionNormal.contains(it) }
    }

    private fun detectarLateralidad(oracionNormal: String, region: BodyRegion): Lateralidad {
        if (!region.lateralizable) return Lateralidad.NO_APLICA
        val derecho = Regex("\\bderech[oa]s?\\b").containsMatchIn(oracionNormal)
        val izquierdo = Regex("\\bizquierd[oa]s?\\b").containsMatchIn(oracionNormal)
        val bilateral = oracionNormal.contains("bilateral") || oracionNormal.contains("ambos") || oracionNormal.contains("ambas")
        return when {
            bilateral -> Lateralidad.BILATERAL
            derecho && izquierdo -> Lateralidad.BILATERAL
            derecho -> Lateralidad.DERECHO
            izquierdo -> Lateralidad.IZQUIERDO
            else -> Lateralidad.NO_APLICA
        }
    }

    private fun extraerSuv(oracion: String): Double? {
        val m = regexSuv.find(oracion) ?: return null
        return m.groupValues[1].replace(",", ".").toDoubleOrNull()
    }

    private fun extraerTamanoMm(oracion: String): Double? {
        regexTamanoMm.find(oracion)?.let {
            return it.groupValues[1].replace(",", ".").toDoubleOrNull()
        }
        regexTamanoCm.find(oracion)?.let {
            val cm = it.groupValues[1].replace(",", ".").toDoubleOrNull()
            return cm?.times(10.0)
        }
        return null
    }

    private fun calcularSeveridad(
        oracionNormal: String,
        tieneActividad: Boolean,
        negado: Boolean,
        suv: Double?
    ): Severidad {
        if (negado || !tieneActividad) return Severidad.INFORMATIVA

        val intensa = Diccionarios.terminosIntensidadAlta.any { oracionNormal.contains(it) }
        val baja = Diccionarios.terminosBaja.any { oracionNormal.contains(it) }

        return when {
            (suv != null && suv >= 8.0) || intensa -> Severidad.ALTA
            baja -> Severidad.BAJA
            suv != null && suv >= 3.0 -> Severidad.MEDIA
            else -> Severidad.MEDIA
        }
    }

    private fun limpiar(oracion: String): String {
        return oracion.trim()
            .replace(Regex("\\s+"), " ")
            .replaceFirstChar { it.uppercase() }
    }
}
