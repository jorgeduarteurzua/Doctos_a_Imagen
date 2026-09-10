package com.hallazgos.informes.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Aviso visible de que la app es una ayuda de visualización, no un diagnóstico.
 */
@Composable
fun DisclaimerCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFFE65100)
            )
            Spacer(Modifier.size(8.dp))
            Text(
                "Esta app resalta y organiza hallazgos mencionados en los informes como ayuda de " +
                    "visualización. No es un diagnóstico ni reemplaza la interpretación de un profesional médico.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5D4037)
            )
        }
    }
}
