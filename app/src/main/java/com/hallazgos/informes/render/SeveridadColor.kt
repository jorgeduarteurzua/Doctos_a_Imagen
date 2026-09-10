package com.hallazgos.informes.render

import androidx.compose.ui.graphics.Color
import com.hallazgos.informes.domain.model.Severidad

/**
 * Colores asociados a cada nivel de severidad, usados en marcadores y etiquetas.
 */
object SeveridadColor {
    val alta = Color(0xFFD32F2F)        // rojo
    val media = Color(0xFFF57C00)       // naranja
    val baja = Color(0xFFFBC02D)        // amarillo
    val informativa = Color(0xFF1976D2) // azul

    fun de(severidad: Severidad): Color = when (severidad) {
        Severidad.ALTA -> alta
        Severidad.MEDIA -> media
        Severidad.BAJA -> baja
        Severidad.INFORMATIVA -> informativa
    }

    fun etiqueta(severidad: Severidad): String = when (severidad) {
        Severidad.ALTA -> "Actividad alta"
        Severidad.MEDIA -> "Actividad moderada"
        Severidad.BAJA -> "Actividad leve"
        Severidad.INFORMATIVA -> "Mención"
    }
}
