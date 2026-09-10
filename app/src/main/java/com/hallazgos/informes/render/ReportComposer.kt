package com.hallazgos.informes.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect as AndroidRect
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.hallazgos.informes.domain.model.BodyRegion
import com.hallazgos.informes.domain.model.Lateralidad
import com.hallazgos.informes.domain.model.RegionGroup
import com.hallazgos.informes.domain.model.Sexo
import com.hallazgos.informes.domain.model.VistaCorporal
import com.hallazgos.informes.render.BodyMap.dibujarFigura
import com.hallazgos.informes.render.BodyMap.dibujarLineaMedia

/**
 * Compone la IMAGEN del informe visual: título, dos figuras (frontal y
 * posterior) con marcadores NUMERADOS y una leyenda de relevancia.
 *
 * La correlación con el detalle de hallazgos se hace por NÚMERO (el mismo
 * número aparece en el marcador y en la lista de hallazgos), no por líneas de
 * conexión. Así funciona con cualquier cantidad de hallazgos y aunque la lista
 * quede en otra página del PDF.
 *
 * La lista de hallazgos se dibuja aparte (en pantalla como tarjetas Compose y
 * en el PDF como texto), por lo que esta imagen NO incluye el texto largo.
 */
object ReportComposer {

    /** Numera las regiones (1..n) en el mismo orden en que se muestran. */
    fun numerar(grupos: List<RegionGroup>): LinkedHashMap<BodyRegion, Int> {
        val numeros = LinkedHashMap<BodyRegion, Int>()
        grupos.forEachIndexed { i, g -> numeros[g.region] = i + 1 }
        return numeros
    }

    fun componer(
        context: Context,
        grupos: List<RegionGroup>,
        sexo: Sexo,
        anchoPx: Int = 1400
    ): Bitmap {
        val w = anchoPx.toFloat()
        val numeros = numerar(grupos)

        // Layout: título arriba, figuras, etiquetas de vista, leyenda al final.
        val tituloH = w * 0.075f
        val margen = w * 0.03f
        val figurasH = w * 0.92f            // alto de la zona de figuras
        val etiquetaH = w * 0.05f           // "Vista frontal/posterior"
        val leyendaH = w * 0.10f

        val altoTotal = (tituloH + margen + figurasH + etiquetaH + leyendaH + margen).toInt()

        val bitmap = Bitmap.createBitmap(anchoPx, altoTotal, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        dibujarFondo(canvas, w, altoTotal.toFloat())
        dibujarTitulo(canvas, w, tituloH)

        // Áreas de las dos figuras. Se dejan ~9% de margen a cada extremo del
        // lienzo para colocar los círculos numerados de las líneas guía.
        val figTop = tituloH + margen
        val anchoFigura = w * 0.36f
        val areaFrontal = Rect(
            left = w * 0.09f, top = figTop,
            right = w * 0.09f + anchoFigura, bottom = figTop + figurasH
        )
        val areaPosterior = Rect(
            left = w * 0.55f, top = figTop,
            right = w * 0.55f + anchoFigura, bottom = figTop + figurasH
        )

        val posiciones = dibujarFigurasEnCanvas(
            context, canvas, w, altoTotal.toFloat(), sexo, grupos, areaFrontal, areaPosterior
        )

        // Números con línea guía hacia el margen (evita solape entre marcadores juntos).
        dibujarNumerosConLineaGuia(canvas, posiciones, numeros, grupos, w, areaFrontal, areaPosterior)

        // Leyenda al pie.
        dibujarLeyenda(canvas, w, altoTotal - leyendaH + margen * 0.5f)

        return bitmap
    }

    // ------------------------------------------------------------------- Figuras

    private fun dibujarFigurasEnCanvas(
        context: Context,
        canvas: Canvas,
        w: Float,
        h: Float,
        sexo: Sexo,
        grupos: List<RegionGroup>,
        areaFrontal: Rect,
        areaPosterior: Rect
    ): Map<BodyRegion, Offset> {
        val posiciones = HashMap<BodyRegion, Offset>()

        val frontales = grupos.filter { it.region.vista == VistaCorporal.FRONTAL }
            .map { it.region to (it.findings.firstOrNull()?.lateralidad ?: Lateralidad.NO_APLICA) }
        val posteriores = grupos.filter { it.region.vista == VistaCorporal.POSTERIOR }
            .map { it.region to (it.findings.firstOrNull()?.lateralidad ?: Lateralidad.NO_APLICA) }

        val coloresCompose = grupos.associate {
            it.region to SeveridadColor.de(it.severidadMaxima)
        }

        val imgFrontal = BodyImageProvider.obtener(context, sexo, VistaCorporal.FRONTAL)
        val imgPosterior = BodyImageProvider.obtener(context, sexo, VistaCorporal.POSTERIOR)

        val areaMarcadoresFrontal = dibujarFondoFigura(canvas, imgFrontal, areaFrontal) ?: areaFrontal
        val areaMarcadoresPosterior = dibujarFondoFigura(canvas, imgPosterior, areaPosterior) ?: areaPosterior

        val drawScope = CanvasDrawScope()
        val composeCanvas = ComposeCanvas(canvas)
        drawScope.draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = composeCanvas,
            size = Size(w, h)
        ) {
            if (imgFrontal == null) dibujarLineaMedia(areaFrontal)
            if (imgPosterior == null) dibujarLineaMedia(areaPosterior)
            posiciones.putAll(
                dibujarFigura(
                    areaMarcadoresFrontal, sexo, VistaCorporal.FRONTAL, frontales, coloresCompose,
                    dibujarPlaceholder = imgFrontal == null
                )
            )
            posiciones.putAll(
                dibujarFigura(
                    areaMarcadoresPosterior, sexo, VistaCorporal.POSTERIOR, posteriores, coloresCompose,
                    dibujarPlaceholder = imgPosterior == null
                )
            )
        }

        val etiqueta = Paint().apply {
            color = AndroidColor.parseColor("#455A64")
            textSize = w * 0.026f
            isAntiAlias = true
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Vista frontal", areaFrontal.center.x, areaFrontal.bottom + w * 0.035f, etiqueta)
        canvas.drawText("Vista posterior", areaPosterior.center.x, areaPosterior.bottom + w * 0.035f, etiqueta)

        return posiciones
    }

    private fun dibujarFondoFigura(canvas: Canvas, bitmap: Bitmap?, area: Rect): Rect? {
        if (bitmap == null) return null
        val escala = minOf(area.width / bitmap.width, area.height / bitmap.height)
        val destW = bitmap.width * escala
        val destH = bitmap.height * escala
        val left = area.left + (area.width - destW) / 2f
        val top = area.top + (area.height - destH) / 2f
        val destino = RectF(left, top, left + destW, top + destH)
        val paint = Paint().apply { isAntiAlias = true; isFilterBitmap = true }
        canvas.drawBitmap(bitmap, null, destino, paint)
        return Rect(left, top, left + destW, top + destH)
    }

    /**
     * Para cada marcador dibuja una línea guía hacia el margen exterior de su
     * figura, terminando en un círculo con el número. Los círculos se apilan
     * verticalmente en el margen para no solaparse aunque los marcadores estén
     * muy juntos. La figura frontal saca las guías a la IZQUIERDA; la posterior
     * a la DERECHA.
     */
    private fun dibujarNumerosConLineaGuia(
        canvas: Canvas,
        posiciones: Map<BodyRegion, Offset>,
        numeros: Map<BodyRegion, Int>,
        grupos: List<RegionGroup>,
        w: Float,
        areaFrontal: Rect,
        areaPosterior: Rect
    ) {
        val coloresPorRegion = grupos.associate { it.region to SeveridadColor.de(it.severidadMaxima).toArgbColor() }
        val radioMarcador = w * 0.012f
        val radioNumero = w * 0.022f
        val separacion = radioNumero * 2.4f  // separación vertical entre círculos apilados

        // Separa las regiones por vista, según en qué figura cayó el marcador.
        val frontales = posiciones.keys.filter { it.vista == VistaCorporal.FRONTAL }
        val posteriores = posiciones.keys.filter { it.vista == VistaCorporal.POSTERIOR }

        dibujarGuiasDeFigura(
            canvas, frontales, posiciones, numeros, coloresPorRegion,
            xMargen = w * 0.045f, // margen izquierdo del lienzo
            areaTop = areaFrontal.top, areaBottom = areaFrontal.bottom,
            radioMarcador = radioMarcador, radioNumero = radioNumero, separacion = separacion, w = w
        )
        dibujarGuiasDeFigura(
            canvas, posteriores, posiciones, numeros, coloresPorRegion,
            xMargen = w * 0.955f, // margen derecho del lienzo
            areaTop = areaPosterior.top, areaBottom = areaPosterior.bottom,
            radioMarcador = radioMarcador, radioNumero = radioNumero, separacion = separacion, w = w
        )
    }

    private fun dibujarGuiasDeFigura(
        canvas: Canvas,
        regiones: List<BodyRegion>,
        posiciones: Map<BodyRegion, Offset>,
        numeros: Map<BodyRegion, Int>,
        colores: Map<BodyRegion, Int>,
        xMargen: Float,
        areaTop: Float,
        areaBottom: Float,
        radioMarcador: Float,
        radioNumero: Float,
        separacion: Float,
        w: Float
    ) {
        // Ordena por altura del marcador y reparte las posiciones Y de los
        // círculos para que no se solapen (empuja hacia abajo si hace falta).
        val ordenadas = regiones.sortedBy { posiciones[it]?.y ?: 0f }
        var ultimaY = areaTop
        val yNumero = HashMap<BodyRegion, Float>()
        for (region in ordenadas) {
            val py = posiciones[region]?.y ?: continue
            val y = maxOf(py, ultimaY + separacion).coerceAtMost(areaBottom)
            yNumero[region] = y
            ultimaY = y
        }

        val paintNum = Paint().apply {
            color = AndroidColor.WHITE; textSize = w * 0.026f; isAntiAlias = true
            isFakeBoldText = true; textAlign = Paint.Align.CENTER
        }

        for (region in ordenadas) {
            val punto = posiciones[region] ?: continue
            val n = numeros[region] ?: continue
            val color = colores[region] ?: AndroidColor.RED
            val y = yNumero[region] ?: punto.y

            // Marcador (punto pequeño sobre el órgano)
            val pMarcador = Paint().apply { isAntiAlias = true; this.color = color }
            canvas.drawCircle(punto.x, punto.y, radioMarcador, pMarcador)
            canvas.drawCircle(punto.x, punto.y, radioMarcador,
                Paint().apply { isAntiAlias = true; this.color = AndroidColor.WHITE; style = Paint.Style.STROKE; strokeWidth = w * 0.003f })

            // Línea guía marcador -> círculo del número
            val pLinea = Paint().apply {
                isAntiAlias = true; this.color = color; strokeWidth = w * 0.0035f; alpha = 180
            }
            canvas.drawLine(punto.x, punto.y, xMargen, y, pLinea)

            // Círculo con el número en el margen
            val pc = Paint().apply { isAntiAlias = true; this.color = color }
            canvas.drawCircle(xMargen, y, radioNumero, pc)
            val nb = AndroidRect()
            paintNum.getTextBounds(n.toString(), 0, n.toString().length, nb)
            canvas.drawText(n.toString(), xMargen, y + nb.height() / 2f, paintNum)
        }
    }

    // ------------------------------------------------------------- Fondo / chrome

    private fun dibujarFondo(canvas: Canvas, w: Float, h: Float) {
        val fondo = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, h,
                AndroidColor.parseColor("#EAF2F8"),
                AndroidColor.parseColor("#F7FAFC"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w, h, fondo)
    }

    private fun dibujarTitulo(canvas: Canvas, w: Float, tituloH: Float) {
        val barra = Paint().apply { color = AndroidColor.parseColor("#1565C0") }
        canvas.drawRect(0f, 0f, w, tituloH, barra)
        val t = Paint().apply {
            color = AndroidColor.WHITE
            textSize = w * 0.038f
            isAntiAlias = true
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("INFORME VISUAL DE HALLAZGOS", w / 2f, tituloH * 0.62f, t)
    }

    private fun dibujarLeyenda(canvas: Canvas, w: Float, top: Float) {
        val titulo = Paint().apply {
            color = AndroidColor.parseColor("#1A237E")
            textSize = w * 0.024f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText("Leyenda de relevancia", w * 0.04f, top, titulo)

        val items = listOf(
            SeveridadColor.alta.toArgbColor() to "Alta",
            SeveridadColor.media.toArgbColor() to "Moderada",
            SeveridadColor.baja.toArgbColor() to "Leve",
            SeveridadColor.informativa.toArgbColor() to "Mención"
        )
        val texto = Paint().apply {
            color = AndroidColor.parseColor("#37474F")
            textSize = w * 0.022f
            isAntiAlias = true
        }
        var x = w * 0.04f
        val y = top + w * 0.045f
        items.forEach { (color, etiqueta) ->
            val pc = Paint().apply { isAntiAlias = true; this.color = color }
            canvas.drawCircle(x + w * 0.015f, y - w * 0.007f, w * 0.013f, pc)
            canvas.drawText(etiqueta, x + w * 0.035f, y, texto)
            x += w * 0.035f + texto.measureText(etiqueta) + w * 0.03f
        }
    }

    private fun Color.toArgbColor(): Int = AndroidColor.argb(
        (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()
    )
}
