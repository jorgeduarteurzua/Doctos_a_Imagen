package com.hallazgos.informes.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.hallazgos.informes.domain.model.BodyRegion
import com.hallazgos.informes.domain.model.Lateralidad
import com.hallazgos.informes.domain.model.Sexo
import com.hallazgos.informes.domain.model.VistaCorporal

/**
 * Dibuja una figura humana con anatomía interna estilizada (no fotorrealista
 * pero clara), en vista frontal o posterior. La lógica vive aquí para poder
 * reutilizarla tanto en pantalla (Canvas de Compose) como al exportar a PNG.
 *
 * Todo el dibujo se hace dentro de un rectángulo [area] recibido, en píxeles,
 * usando las coordenadas relativas (0..1) de cada estructura.
 */
object BodyMap {

    // Paleta
    private val piel = Color(0xFFF1C9A5)
    private val pielSombra = Color(0xFFE0AE86)
    private val contorno = Color(0xFF8D6E63)
    private val hueso = Color(0xFFF5EBDC)
    private val huesoBorde = Color(0xFFCBB994)
    private val pulmon = Color(0xFFF3B8C2)
    private val corazon = Color(0xFFC7625B)
    private val higado = Color(0xFF9C5B4C)
    private val organoBazo = Color(0xFF8E5D8A)
    private val estomago = Color(0xFFE7A97F)
    private val intestino = Color(0xFFD9986B)
    private val vejiga = Color(0xFFE7CE7A)
    private val rinon = Color(0xFF9A5A47)
    private val lineaMedia = Color(0x33607D8B)

    /** Proporción ancho:alto de una sola figura. */
    const val FIGURE_ASPECT = 0.42f

    /**
     * Dibuja la figura completa dentro de [area].
     * @return mapa de región -> punto (px) donde quedó el marcador, para callouts.
     */
    fun DrawScope.dibujarFigura(
        area: Rect,
        sexo: Sexo,
        vista: VistaCorporal,
        regionesConMarcador: List<Pair<BodyRegion, Lateralidad>>,
        colores: Map<BodyRegion, Color>,
        dibujarPlaceholder: Boolean = true
    ): Map<BodyRegion, Offset> {
        // Solo dibujamos la silueta/órganos por código cuando NO hay una
        // ilustración de fondo (placeholder de Opción A).
        if (dibujarPlaceholder) {
            dibujarSilueta(area, sexo, vista)
            if (vista == VistaCorporal.FRONTAL) {
                dibujarOrganosFrontales(area, sexo)
            } else {
                dibujarEstructurasPosteriores(area)
            }
        }

        val posiciones = mutableMapOf<BodyRegion, Offset>()
        for ((region, lat) in regionesConMarcador) {
            val centro = puntoRegion(region, lat, area)
            posiciones[region] = centro
            val color = colores[region] ?: SeveridadColor.alta
            dibujarMarcador(centro, area.width, color)
        }
        return posiciones
    }

    /** Punto absoluto (px) de una región dentro del área, ajustando lateralidad. */
    fun puntoRegion(region: BodyRegion, lateralidad: Lateralidad?, area: Rect): Offset {
        var rx = region.x
        if (region.lateralizable) {
            // Vista de frente: el lado DERECHO del paciente está a la izquierda de la imagen.
            val signo = if (region.vista == VistaCorporal.FRONTAL) 1f else -1f
            when (lateralidad) {
                Lateralidad.DERECHO -> rx -= 0.11f * signo
                Lateralidad.IZQUIERDO -> rx += 0.11f * signo
                else -> {}
            }
        }
        return Offset(area.left + rx * area.width, area.top + region.y * area.height)
    }

    private fun DrawScope.px(area: Rect, rx: Float, ry: Float) =
        Offset(area.left + rx * area.width, area.top + ry * area.height)

    // ----------------------------------------------------------------- Silueta

    private fun DrawScope.dibujarSilueta(area: Rect, sexo: Sexo, vista: VistaCorporal) {
        val w = area.width
        val anchoHombro = if (sexo == Sexo.HOMBRE) 0.20f else 0.17f
        val anchoCadera = if (sexo == Sexo.HOMBRE) 0.155f else 0.19f
        val anchoCintura = if (sexo == Sexo.HOMBRE) 0.13f else 0.115f

        val cuerpo = Path().apply {
            // Cabeza
            val cabeza = px(area, 0.5f, 0.075f)
            addOval(
                Rect(
                    cabeza.x - w * 0.085f, cabeza.y - area.height * 0.055f,
                    cabeza.x + w * 0.085f, cabeza.y + area.height * 0.05f
                )
            )
        }
        drawPath(cuerpo, piel)
        drawPath(cuerpo, contorno, style = Stroke(width = w * 0.008f))

        // Tronco + cuello + extremidades como un contorno continuo
        val tronco = Path().apply {
            moveTo(px(area, 0.5f - 0.035f, 0.12f).x, px(area, 0f, 0.12f).y)   // cuello izq
            lineTo(px(area, 0.5f - anchoHombro, 0.17f).x, px(area, 0f, 0.17f).y) // hombro izq
            // brazo izquierdo
            lineTo(px(area, 0.5f - anchoHombro - 0.06f, 0.20f).x, px(area, 0f, 0.20f).y)
            lineTo(px(area, 0.5f - anchoHombro - 0.045f, 0.47f).x, px(area, 0f, 0.47f).y)
            lineTo(px(area, 0.5f - anchoHombro + 0.005f, 0.47f).x, px(area, 0f, 0.47f).y)
            lineTo(px(area, 0.5f - anchoCintura, 0.42f).x, px(area, 0f, 0.42f).y)
            // cadera y pierna izquierda
            lineTo(px(area, 0.5f - anchoCadera, 0.6f).x, px(area, 0f, 0.6f).y)
            lineTo(px(area, 0.5f - anchoCadera + 0.01f, 0.98f).x, px(area, 0f, 0.98f).y)
            lineTo(px(area, 0.5f - 0.03f, 0.98f).x, px(area, 0f, 0.98f).y)
            lineTo(px(area, 0.5f - 0.02f, 0.62f).x, px(area, 0f, 0.62f).y)
            // pierna derecha
            lineTo(px(area, 0.5f + 0.02f, 0.62f).x, px(area, 0f, 0.62f).y)
            lineTo(px(area, 0.5f + 0.03f, 0.98f).x, px(area, 0f, 0.98f).y)
            lineTo(px(area, 0.5f + anchoCadera - 0.01f, 0.98f).x, px(area, 0f, 0.98f).y)
            lineTo(px(area, 0.5f + anchoCadera, 0.6f).x, px(area, 0f, 0.6f).y)
            lineTo(px(area, 0.5f + anchoCintura, 0.42f).x, px(area, 0f, 0.42f).y)
            // brazo derecho
            lineTo(px(area, 0.5f + anchoHombro - 0.005f, 0.47f).x, px(area, 0f, 0.47f).y)
            lineTo(px(area, 0.5f + anchoHombro + 0.045f, 0.47f).x, px(area, 0f, 0.47f).y)
            lineTo(px(area, 0.5f + anchoHombro + 0.06f, 0.20f).x, px(area, 0f, 0.20f).y)
            lineTo(px(area, 0.5f + anchoHombro, 0.17f).x, px(area, 0f, 0.17f).y)
            lineTo(px(area, 0.5f + 0.035f, 0.12f).x, px(area, 0f, 0.12f).y)   // cuello der
            close()
        }
        drawPath(tronco, piel)
        drawPath(tronco, contorno, style = Stroke(width = w * 0.008f))

        // Sombreado sutil en el centro para dar volumen
        drawLine(
            pielSombra.copy(alpha = 0.35f),
            px(area, 0.5f, 0.13f), px(area, 0.5f, 0.6f),
            strokeWidth = w * 0.05f
        )

        if (vista == VistaCorporal.POSTERIOR && sexo == Sexo.MUJER) {
            // recogido de pelo, guiño estético para diferenciar la vista posterior
            drawCircle(contorno.copy(alpha = 0.5f), w * 0.035f, px(area, 0.5f, 0.06f))
        }
    }

    // -------------------------------------------------------- Órganos frontales

    private fun DrawScope.dibujarOrganosFrontales(area: Rect, sexo: Sexo) {
        val w = area.width

        // Pulmones
        val pulIzq = Path().apply {
            moveTo(px(area, 0.5f - 0.02f, 0.23f).x, px(area, 0f, 0.23f).y)
            cubicTo(
                px(area, 0.5f - 0.16f, 0.25f).x, px(area, 0f, 0.25f).y,
                px(area, 0.5f - 0.15f, 0.36f).x, px(area, 0f, 0.36f).y,
                px(area, 0.5f - 0.05f, 0.37f).x, px(area, 0f, 0.37f).y
            )
            close()
        }
        val pulDer = Path().apply {
            moveTo(px(area, 0.5f + 0.02f, 0.23f).x, px(area, 0f, 0.23f).y)
            cubicTo(
                px(area, 0.5f + 0.16f, 0.25f).x, px(area, 0f, 0.25f).y,
                px(area, 0.5f + 0.15f, 0.36f).x, px(area, 0f, 0.36f).y,
                px(area, 0.5f + 0.05f, 0.37f).x, px(area, 0f, 0.37f).y
            )
            close()
        }
        drawPath(pulIzq, pulmon); drawPath(pulDer, pulmon)
        drawPath(pulIzq, corazon.copy(alpha = 0.4f), style = Stroke(width = w * 0.004f))
        drawPath(pulDer, corazon.copy(alpha = 0.4f), style = Stroke(width = w * 0.004f))

        // Corazón
        dibujarOrganoOval(area, 0.46f, 0.33f, 0.06f, 0.05f, corazon)

        // Hígado (grande, a la derecha del paciente = izquierda de la imagen)
        val higadoPath = Path().apply {
            moveTo(px(area, 0.32f, 0.40f).x, px(area, 0f, 0.40f).y)
            lineTo(px(area, 0.5f, 0.40f).x, px(area, 0f, 0.40f).y)
            lineTo(px(area, 0.48f, 0.47f).x, px(area, 0f, 0.47f).y)
            lineTo(px(area, 0.34f, 0.48f).x, px(area, 0f, 0.48f).y)
            close()
        }
        drawPath(higadoPath, higado)

        // Estómago y bazo
        dibujarOrganoOval(area, 0.57f, 0.42f, 0.05f, 0.04f, estomago)
        dibujarOrganoOval(area, 0.62f, 0.43f, 0.03f, 0.03f, organoBazo)

        // Intestino: espiral estilizada
        val intestinoPath = Path().apply {
            moveTo(px(area, 0.38f, 0.51f).x, px(area, 0f, 0.51f).y)
            cubicTo(
                px(area, 0.4f, 0.62f).x, px(area, 0f, 0.62f).y,
                px(area, 0.6f, 0.62f).x, px(area, 0f, 0.62f).y,
                px(area, 0.62f, 0.51f).x, px(area, 0f, 0.51f).y
            )
            cubicTo(
                px(area, 0.58f, 0.57f).x, px(area, 0f, 0.57f).y,
                px(area, 0.42f, 0.57f).x, px(area, 0f, 0.57f).y,
                px(area, 0.38f, 0.51f).x, px(area, 0f, 0.51f).y
            )
            close()
        }
        drawPath(intestinoPath, intestino)
        // Marco del colon
        drawPath(intestinoPath, higado.copy(alpha = 0.5f), style = Stroke(width = w * 0.006f))

        // Vejiga
        dibujarOrganoOval(area, 0.5f, 0.60f, 0.04f, 0.03f, vejiga)

        // Útero (solo mujer)
        if (sexo == Sexo.MUJER) {
            dibujarOrganoOval(area, 0.5f, 0.585f, 0.03f, 0.025f, corazon.copy(alpha = 0.7f))
        }
    }

    private fun DrawScope.dibujarOrganoOval(
        area: Rect, cx: Float, cy: Float, rx: Float, ry: Float, color: Color
    ) {
        val c = px(area, cx, cy)
        drawOval(
            color = color,
            topLeft = Offset(c.x - rx * area.width, c.y - ry * area.height),
            size = androidx.compose.ui.geometry.Size(rx * 2 * area.width, ry * 2 * area.height)
        )
    }

    // ---------------------------------------------------- Estructuras posteriores

    private fun DrawScope.dibujarEstructurasPosteriores(area: Rect) {
        val w = area.width

        // Columna vertebral
        val topY = 0.14f
        val botY = 0.6f
        val n = 20
        for (i in 0..n) {
            val y = topY + (botY - topY) * i / n
            val c = px(area, 0.5f, y)
            drawRoundRect(
                color = hueso,
                topLeft = Offset(c.x - w * 0.022f, c.y - area.height * 0.008f),
                size = androidx.compose.ui.geometry.Size(w * 0.044f, area.height * 0.014f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.01f)
            )
            drawRoundRect(
                color = huesoBorde,
                topLeft = Offset(c.x - w * 0.022f, c.y - area.height * 0.008f),
                size = androidx.compose.ui.geometry.Size(w * 0.044f, area.height * 0.014f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.01f),
                style = Stroke(width = w * 0.003f)
            )
        }

        // Costillas (arcos a ambos lados de la columna)
        for (i in 0 until 8) {
            val y = 0.22f + i * 0.022f
            for (signo in listOf(-1f, 1f)) {
                val path = Path().apply {
                    val start = px(area, 0.5f, y)
                    moveTo(start.x, start.y)
                    quadraticBezierTo(
                        px(area, 0.5f + signo * 0.14f, y + 0.01f).x, px(area, 0f, y + 0.01f).y,
                        px(area, 0.5f + signo * 0.13f, y + 0.05f).x, px(area, 0f, y + 0.05f).y
                    )
                }
                drawPath(path, huesoBorde, style = Stroke(width = w * 0.006f))
            }
        }

        // Pelvis
        val pelvis = Path().apply {
            moveTo(px(area, 0.36f, 0.58f).x, px(area, 0f, 0.58f).y)
            cubicTo(
                px(area, 0.4f, 0.66f).x, px(area, 0f, 0.66f).y,
                px(area, 0.6f, 0.66f).x, px(area, 0f, 0.66f).y,
                px(area, 0.64f, 0.58f).x, px(area, 0f, 0.58f).y
            )
        }
        drawPath(pelvis, hueso.copy(alpha = 0.9f), style = Stroke(width = w * 0.03f))

        // Riñones
        dibujarOrganoOval(area, 0.42f, 0.47f, 0.03f, 0.045f, rinon)
        dibujarOrganoOval(area, 0.58f, 0.47f, 0.03f, 0.045f, rinon)
    }

    // ------------------------------------------------------------- Marcadores

    private fun DrawScope.dibujarMarcador(centro: Offset, w: Float, color: Color) {
        val radio = w * 0.035f
        drawCircle(color.copy(alpha = 0.28f), radio * 2.1f, centro)
        drawCircle(color, radio, centro)
        drawCircle(Color.White, radio, centro, style = Stroke(width = w * 0.008f))
        // El número se dibuja en la capa de anotaciones (texto nativo), aquí solo el punto.
    }

    /** Línea media punteada de referencia. */
    fun DrawScope.dibujarLineaMedia(area: Rect) {
        val w = area.width
        drawLine(
            color = lineaMedia,
            start = px(area, 0.5f, 0.12f),
            end = px(area, 0.5f, 0.6f),
            strokeWidth = w * 0.004f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 0.02f, w * 0.02f))
        )
    }
}
