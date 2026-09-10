package com.hallazgos.informes.domain.model

import android.net.Uri

/**
 * Tipo de sexo del esquema corporal elegido por el usuario.
 */
enum class Sexo {
    HOMBRE,
    MUJER
}

/**
 * Tipo de documento seleccionado por el usuario.
 */
enum class TipoDocumento {
    IMAGEN,
    PDF,
    DESCONOCIDO
}

/**
 * Un documento seleccionado por el usuario para analizar.
 */
data class DocumentItem(
    val uri: Uri,
    val nombre: String,
    val tipo: TipoDocumento
)

/**
 * Estado del procesamiento de un documento individual dentro del lote.
 */
sealed interface EstadoDocumento {
    val documento: DocumentItem

    data class Pendiente(override val documento: DocumentItem) : EstadoDocumento
    data class Procesando(override val documento: DocumentItem) : EstadoDocumento

    data class Completado(
        override val documento: DocumentItem,
        val textoExtraido: String,
        val findings: List<Finding>
    ) : EstadoDocumento

    data class Error(
        override val documento: DocumentItem,
        val mensaje: String
    ) : EstadoDocumento
}
