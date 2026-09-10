package com.hallazgos.informes.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Diálogo "Acerca de" con la información de la app.
 */
@Composable
fun AcercaDeDialog(onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Acerca de", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Campo("Aplicación", "Hallazgos — Visualizador de informes médicos")
                Campo("Autor", "Jorge")
                Campo("Fecha", "Septiembre 2026")
                Campo(
                    "Objetivo",
                    "A partir de uno o varios informes o imágenes médicas (por ejemplo PET/CT), " +
                        "extraer los hallazgos y mostrarlos de forma clara en un esquema corporal " +
                        "con marcadores numerados, además de exportarlos como imagen o PDF."
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    "Ayuda de visualización. No es un diagnóstico ni reemplaza la valoración de un profesional médico.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCerrar) { Text("Cerrar") }
        }
    )
}

@Composable
private fun Campo(titulo: String, valor: String) {
    Column {
        Text(titulo, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text(valor, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.size(10.dp))
    }
}
