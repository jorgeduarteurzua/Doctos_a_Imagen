package com.hallazgos.informes.domain.model

/**
 * Nivel de relevancia estimado de un hallazgo, usado para colorear el marcador
 * y ordenar la lista. Es heurístico, NO un juicio clínico.
 */
enum class Severidad {
    ALTA,     // captación marcada / SUV alto / términos como "hipermetabólico intenso"
    MEDIA,    // captación / actividad descrita sin cuantificar como intensa
    BAJA,     // mención leve, dudosa o de baja actividad
    INFORMATIVA // hallazgo mencionado sin actividad metabólica evidente
}

/**
 * Un hallazgo individual extraído de un documento.
 *
 * @property region región anatómica detectada
 * @property lateralidad lado detectado (si aplica)
 * @property descripcion frase clara y legible del hallazgo
 * @property suvMax valor SUVmax si se detectó
 * @property tamanoMm tamaño en milímetros si se detectó
 * @property severidad relevancia heurística
 * @property sourceFileName nombre del archivo de origen (trazabilidad)
 * @property rawText fragmento original del informe de donde se extrajo
 */
data class Finding(
    val region: BodyRegion,
    val lateralidad: Lateralidad,
    val descripcion: String,
    val suvMax: Double?,
    val tamanoMm: Double?,
    val severidad: Severidad,
    val sourceFileName: String,
    val rawText: String
)

/**
 * Hallazgos de una misma región, agrupados para la vista consolidada.
 */
data class RegionGroup(
    val region: BodyRegion,
    val findings: List<Finding>
) {
    /** Severidad máxima presente en el grupo, para colorear el marcador. */
    val severidadMaxima: Severidad
        get() = findings.minByOrNull { it.severidad.ordinal }?.severidad ?: Severidad.INFORMATIVA
}
