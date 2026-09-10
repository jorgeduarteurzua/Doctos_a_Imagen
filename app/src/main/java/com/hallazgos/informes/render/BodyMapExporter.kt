package com.hallazgos.informes.render

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.hallazgos.informes.domain.model.RegionGroup
import com.hallazgos.informes.domain.model.Sexo
import java.io.File
import java.io.FileOutputStream

/**
 * Genera y comparte el informe visual como PNG. Reutiliza ReportComposer para
 * que la imagen compartida sea idéntica a la que se ve en pantalla.
 */
object BodyMapExporter {

    fun exportarPng(
        context: Context,
        grupos: List<RegionGroup>,
        sexo: Sexo,
        anchoPx: Int = 1400
    ): Uri {
        val bitmap = ReportComposer.componer(context, grupos, sexo, anchoPx)
        return guardarYCompartir(context, bitmap)
    }

    private fun guardarYCompartir(context: Context, bitmap: Bitmap): Uri {
        val dir = File(context.cacheDir, "images")
        if (!dir.exists()) dir.mkdirs()
        val archivo = File(dir, "hallazgos_${System.currentTimeMillis()}.png")
        FileOutputStream(archivo).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            archivo
        )
    }
}
