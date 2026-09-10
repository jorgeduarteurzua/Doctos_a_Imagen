package com.hallazgos.informes.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.hallazgos.informes.domain.model.Lateralidad
import com.hallazgos.informes.domain.model.RegionGroup
import com.hallazgos.informes.domain.model.Sexo
import java.io.File
import java.io.FileOutputStream

/**
 * Genera un PDF (tamaño A4) con:
 *  - Página 1: la imagen del informe visual.
 *  - Páginas siguientes: el listado de hallazgos por región, con datos y origen.
 *
 * Usa PdfDocument nativo de Android (sin dependencias externas).
 */
object PdfExporter {

    // A4 en puntos (72 dpi): 595 x 842
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGEN = 40f

    fun exportarPdf(
        context: Context,
        grupos: List<RegionGroup>,
        sexo: Sexo
    ): Uri {
        val doc = PdfDocument()

        // --- Página 1: imagen del informe ---
        val informe = ReportComposer.componer(context, grupos, sexo, anchoPx = 1400)
        dibujarPaginaImagen(doc, informe)
        informe.recycle()

        // --- Páginas de texto: hallazgos ---
        dibujarPaginasHallazgos(doc, grupos)

        // Guardar y compartir
        val dir = File(context.cacheDir, "images")
        if (!dir.exists()) dir.mkdirs()
        val archivo = File(dir, "hallazgos_${System.currentTimeMillis()}.pdf")
        FileOutputStream(archivo).use { out -> doc.writeTo(out) }
        doc.close()

        return FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", archivo
        )
    }

    private fun dibujarPaginaImagen(doc: PdfDocument, informe: Bitmap) {
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
        val canvas = page.canvas
        canvas.drawColor(AndroidColor.WHITE)

        dibujarEncabezado(canvas, "Informe visual de hallazgos")

        val dispW = PAGE_W - MARGEN * 2
        val dispTop = 70f
        val dispH = PAGE_H - dispTop - MARGEN
        val escala = minOf(dispW / informe.width, dispH / informe.height)
        val w = informe.width * escala
        val h = informe.height * escala
        val left = (PAGE_W - w) / 2f
        val destino = RectF(left, dispTop, left + w, dispTop + h)
        val paint = Paint().apply { isAntiAlias = true; isFilterBitmap = true }
        canvas.drawBitmap(informe, null, destino, paint)

        doc.finishPage(page)
    }

    private fun dibujarPaginasHallazgos(doc: PdfDocument, grupos: List<RegionGroup>) {
        val tituloPaint = Paint().apply {
            color = AndroidColor.parseColor("#1A237E"); textSize = 14f; isAntiAlias = true; isFakeBoldText = true
        }
        val textoPaint = Paint().apply {
            color = AndroidColor.parseColor("#37474F"); textSize = 11f; isAntiAlias = true
        }
        val detallePaint = Paint().apply {
            color = AndroidColor.parseColor("#607D8B"); textSize = 10f; isAntiAlias = true
        }

        var numPagina = 2
        var page = nuevaPaginaTexto(doc, numPagina)
        var canvas = page.canvas
        var y = 90f
        val anchoUtil = PAGE_W - MARGEN * 2

        fun saltarPaginaSiHaceFalta(espacioNecesario: Float) {
            if (y + espacioNecesario > PAGE_H - MARGEN) {
                doc.finishPage(page)
                numPagina++
                page = nuevaPaginaTexto(doc, numPagina)
                canvas = page.canvas
                y = 90f
            }
        }

        if (grupos.isEmpty()) {
            canvas.drawText("No se detectaron hallazgos automáticamente.", MARGEN, y, textoPaint)
        }

        val numeros = ReportComposer.numerar(grupos)

        grupos.forEach { grupo ->
            saltarPaginaSiHaceFalta(40f)
            val n = numeros[grupo.region] ?: 0
            // Círculo con número (correlaciona con el marcador de la imagen).
            val colorGrupo = SeveridadColorPdf(grupo)
            val pc = Paint().apply { isAntiAlias = true; this.color = colorGrupo }
            canvas.drawCircle(MARGEN + 8f, y - 4f, 9f, pc)
            val numPaint = Paint().apply {
                this.color = AndroidColor.WHITE; textSize = 11f; isAntiAlias = true
                isFakeBoldText = true; textAlign = Paint.Align.CENTER
            }
            canvas.drawText(n.toString(), MARGEN + 8f, y, numPaint)
            canvas.drawText(grupo.region.etiqueta, MARGEN + 24f, y, tituloPaint)
            y += 18f

            grupo.findings.forEach { f ->
                val lineas = envolver(f.descripcion, textoPaint, anchoUtil - 12f)
                saltarPaginaSiHaceFalta(lineas.size * 14f + 16f)
                lineas.forEach { linea ->
                    canvas.drawText("• $linea", MARGEN + 24f, y, textoPaint)
                    y += 14f
                }
                val detalles = buildList {
                    when (f.lateralidad) {
                        Lateralidad.DERECHO -> add("lado derecho")
                        Lateralidad.IZQUIERDO -> add("lado izquierdo")
                        Lateralidad.BILATERAL -> add("bilateral")
                        Lateralidad.NO_APLICA -> {}
                    }
                    f.suvMax?.let { add("SUVmax $it") }
                    f.tamanoMm?.let { add("${it.toInt()} mm") }
                    add("origen: ${f.sourceFileName}")
                }
                if (detalles.isNotEmpty()) {
                    canvas.drawText(detalles.joinToString(" · "), MARGEN + 30f, y, detallePaint)
                    y += 16f
                }
            }
            y += 10f
        }

        // Aviso al pie de la última página
        saltarPaginaSiHaceFalta(30f)
        val aviso = Paint().apply {
            color = AndroidColor.parseColor("#B71C1C"); textSize = 9f; isAntiAlias = true
        }
        canvas.drawText(
            "Ayuda de visualización generada localmente. No es un diagnóstico ni reemplaza la valoración médica.",
            MARGEN, PAGE_H - MARGEN, aviso
        )

        doc.finishPage(page)
    }

    private fun nuevaPaginaTexto(doc: PdfDocument, numero: Int): PdfDocument.Page {
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, numero).create())
        page.canvas.drawColor(AndroidColor.WHITE)
        dibujarEncabezado(page.canvas, "Detalle de hallazgos")
        return page
    }

    private fun dibujarEncabezado(canvas: Canvas, titulo: String) {
        val barra = Paint().apply { color = AndroidColor.parseColor("#1565C0") }
        canvas.drawRect(0f, 0f, PAGE_W.toFloat(), 50f, barra)
        val t = Paint().apply {
            color = AndroidColor.WHITE; textSize = 18f; isAntiAlias = true; isFakeBoldText = true
        }
        canvas.drawText(titulo, MARGEN, 33f, t)
    }

    private fun envolver(texto: String, paint: Paint, maxAncho: Float): List<String> {
        val palabras = texto.split(" ")
        val lineas = mutableListOf<String>()
        var actual = StringBuilder()
        for (p in palabras) {
            val prueba = if (actual.isEmpty()) p else "$actual $p"
            if (paint.measureText(prueba) > maxAncho && actual.isNotEmpty()) {
                lineas.add(actual.toString())
                actual = StringBuilder(p)
            } else {
                actual = StringBuilder(prueba)
            }
        }
        if (actual.isNotEmpty()) lineas.add(actual.toString())
        return lineas
    }

    /** Color ARGB (Android) de la severidad máxima del grupo. */
    private fun SeveridadColorPdf(grupo: RegionGroup): Int {
        val c = SeveridadColor.de(grupo.severidadMaxima)
        return AndroidColor.argb(
            (c.alpha * 255).toInt(), (c.red * 255).toInt(),
            (c.green * 255).toInt(), (c.blue * 255).toInt()
        )
    }
}
