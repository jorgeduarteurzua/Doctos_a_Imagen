package com.hallazgos.informes.domain.consolidation

import com.hallazgos.informes.domain.model.Finding
import com.hallazgos.informes.domain.model.RegionGroup

/**
 * Consolida los hallazgos provenientes de TODOS los documentos del lote:
 * - deduplica hallazgos casi idénticos (misma región, lado y descripción similar),
 * - agrupa por región anatómica,
 * - ordena los grupos por severidad máxima y las regiones internamente por severidad.
 */
class FindingsConsolidator {

    fun consolidar(todos: List<Finding>): List<RegionGroup> {
        if (todos.isEmpty()) return emptyList()

        val deduplicados = deduplicar(todos)

        return deduplicados
            .groupBy { it.region }
            .map { (region, findings) ->
                RegionGroup(
                    region = region,
                    findings = findings.sortedBy { it.severidad.ordinal }
                )
            }
            .sortedWith(
                compareBy<RegionGroup> { it.severidadMaxima.ordinal }
                    .thenBy { it.region.etiqueta }
            )
    }

    private fun deduplicar(todos: List<Finding>): List<Finding> {
        val resultado = mutableListOf<Finding>()
        for (f in todos) {
            val duplicado = resultado.any { existente ->
                existente.region == f.region &&
                    existente.lateralidad == f.lateralidad &&
                    similar(existente.descripcion, f.descripcion)
            }
            if (!duplicado) {
                resultado.add(f)
            }
        }
        return resultado
    }

    /**
     * Similitud simple por solapamiento de tokens (índice de Jaccard).
     * Dos descripciones se consideran el mismo hallazgo si comparten
     * la mayoría de sus palabras significativas.
     */
    private fun similar(a: String, b: String): Boolean {
        if (a.equals(b, ignoreCase = true)) return true
        val ta = tokens(a)
        val tb = tokens(b)
        if (ta.isEmpty() || tb.isEmpty()) return false
        val interseccion = ta.intersect(tb).size.toDouble()
        val union = ta.union(tb).size.toDouble()
        return (interseccion / union) >= 0.8
    }

    private fun tokens(s: String): Set<String> =
        s.lowercase()
            .split(Regex("[^a-záéíóúñü0-9]+"))
            .filter { it.length > 2 }
            .toSet()
}
