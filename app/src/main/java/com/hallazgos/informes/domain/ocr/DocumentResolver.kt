package com.hallazgos.informes.domain.ocr

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.hallazgos.informes.domain.model.DocumentItem
import com.hallazgos.informes.domain.model.TipoDocumento

/**
 * Resuelve metadatos (nombre y tipo) de un Uri seleccionado por el usuario.
 */
object DocumentResolver {

    fun resolver(context: Context, uri: Uri): DocumentItem {
        val nombre = obtenerNombre(context, uri)
        val tipo = obtenerTipo(context, uri, nombre)
        return DocumentItem(uri = uri, nombre = nombre, tipo = tipo)
    }

    private fun obtenerNombre(context: Context, uri: Uri): String {
        var nombre: String? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        nombre = cursor.getString(idx)
                    }
                }
            }
        } catch (_: Exception) {
            // Ignorado: caeremos al nombre por defecto.
        }
        return nombre ?: (uri.lastPathSegment ?: "documento")
    }

    private fun obtenerTipo(context: Context, uri: Uri, nombre: String): TipoDocumento {
        val mime = try {
            context.contentResolver.getType(uri)
        } catch (_: Exception) {
            null
        }
        return when {
            mime != null && mime.startsWith("image/") -> TipoDocumento.IMAGEN
            mime == "application/pdf" -> TipoDocumento.PDF
            nombre.lowercase().endsWith(".pdf") -> TipoDocumento.PDF
            nombre.lowercase().matches(Regex(".*\\.(jpg|jpeg|png|webp|bmp|heic|heif)$")) -> TipoDocumento.IMAGEN
            else -> TipoDocumento.DESCONOCIDO
        }
    }
}
