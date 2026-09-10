package com.hallazgos.informes.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.hallazgos.informes.domain.model.Sexo
import com.hallazgos.informes.domain.model.VistaCorporal

/**
 * Provee la ilustración anatómica de fondo para cada combinación de
 * sexo + vista. Las imágenes se buscan por nombre en res/drawable, así que la
 * app funciona aunque alguna no exista (usa el dibujo por código como respaldo).
 *
 * La app incluye estas 4 imágenes en `app/src/main/res/drawable/`:
 *
 *   cuerpo_hombre_frontal
 *   cuerpo_hombre_posterior
 *   cuerpo_mujer_frontal
 *   cuerpo_mujer_posterior
 *
 * Para reemplazarlas, mantén los mismos nombres. La posición de los marcadores
 * sobre cada órgano se ajusta en BodyRegion.kt (coordenadas x/y relativas).
 */
object BodyImageProvider {

    /** Cache simple para no decodificar el mismo bitmap repetidamente. */
    private val cache = HashMap<String, Bitmap?>()

    fun obtener(context: Context, sexo: Sexo, vista: VistaCorporal): Bitmap? {
        val nombre = nombreRecurso(sexo, vista)
        cache[nombre]?.let { return it }

        val resId = context.resources.getIdentifier(
            nombre, "drawable", context.packageName
        )
        val bitmap = if (resId != 0) {
            try {
                val opts = BitmapFactory.Options().apply { inScaled = false }
                BitmapFactory.decodeResource(context.resources, resId, opts)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
        cache[nombre] = bitmap
        return bitmap
    }

    private fun nombreRecurso(sexo: Sexo, vista: VistaCorporal): String {
        val s = if (sexo == Sexo.HOMBRE) "hombre" else "mujer"
        val v = if (vista == VistaCorporal.FRONTAL) "frontal" else "posterior"
        return "cuerpo_${s}_${v}"
    }
}
