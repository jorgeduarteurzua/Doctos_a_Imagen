package com.hallazgos.informes.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Paleta de marca de la app. Estilo médico moderno: teal profundo como color
 * principal, un azul-índigo de apoyo y un acento coral para lo destacado.
 */
private object Marca {
    val Teal = Color(0xFF00897B)          // principal
    val TealOscuro = Color(0xFF00695C)
    val TealClaro = Color(0xFFB2DFDB)
    val Indigo = Color(0xFF3949AB)        // secundario
    val IndigoClaro = Color(0xFFC5CAE9)
    val Coral = Color(0xFFFF7043)         // acento (terciario)
    val CoralClaro = Color(0xFFFFCCBC)
    val FondoClaro = Color(0xFFF5FBFA)
    val SuperficieClara = Color(0xFFFFFFFF)
    val FondoOscuro = Color(0xFF0F1A19)
    val SuperficieOscura = Color(0xFF17211F)
}

private val ColoresClaros = lightColorScheme(
    primary = Marca.Teal,
    onPrimary = Color.White,
    primaryContainer = Marca.TealClaro,
    onPrimaryContainer = Marca.TealOscuro,
    secondary = Marca.Indigo,
    onSecondary = Color.White,
    secondaryContainer = Marca.IndigoClaro,
    onSecondaryContainer = Color(0xFF1A237E),
    tertiary = Marca.Coral,
    onTertiary = Color.White,
    tertiaryContainer = Marca.CoralClaro,
    onTertiaryContainer = Color(0xFFBF360C),
    background = Marca.FondoClaro,
    onBackground = Color(0xFF1A1C1B),
    surface = Marca.SuperficieClara,
    onSurface = Color(0xFF1A1C1B),
    surfaceVariant = Color(0xFFDCE5E3),
    onSurfaceVariant = Color(0xFF3F4948)
)

private val ColoresOscuros = darkColorScheme(
    primary = Marca.TealClaro,
    onPrimary = Marca.TealOscuro,
    primaryContainer = Marca.TealOscuro,
    onPrimaryContainer = Marca.TealClaro,
    secondary = Marca.IndigoClaro,
    onSecondary = Color(0xFF1A237E),
    tertiary = Marca.CoralClaro,
    onTertiary = Color(0xFFBF360C),
    background = Marca.FondoOscuro,
    onBackground = Color(0xFFE0E3E1),
    surface = Marca.SuperficieOscura,
    onSurface = Color(0xFFE0E3E1),
    surfaceVariant = Color(0xFF3F4948),
    onSurfaceVariant = Color(0xFFBEC9C7)
)

@Composable
fun HallazgosTheme(
    oscuro: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (oscuro) ColoresOscuros else ColoresClaros,
        content = content
    )
}
