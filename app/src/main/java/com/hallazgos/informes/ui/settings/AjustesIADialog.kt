package com.hallazgos.informes.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Diálogo de ajustes del análisis con IA (Gemini). Permite activar el modo IA
 * y guardar la API key, con un aviso claro de privacidad.
 */
@Composable
fun AjustesIADialog(
    usarIAInicial: Boolean,
    apiKeyInicial: String,
    hayKeyDelBuild: Boolean,
    onGuardar: (Boolean, String) -> Unit,
    onCerrar: () -> Unit
) {
    var usarIA by remember { mutableStateOf(usarIAInicial) }
    var apiKey by remember { mutableStateOf(apiKeyInicial) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Análisis con IA", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Usa IA en la nube (Google Gemini) para interpretar el informe y " +
                        "detectar hallazgos de cualquier órgano, incluso con redacciones poco comunes.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.size(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Activar análisis con IA",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(checked = usarIA, onCheckedChange = { usarIA = it })
                }

                Spacer(Modifier.size(8.dp))
                if (hayKeyDelBuild) {
                    Text(
                        "La app ya incluye una API key configurada. Puedes dejar este campo " +
                            "vacío para usarla, o escribir una propia para reemplazarla.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.size(6.dp))
                }
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(if (hayKeyDelBuild) "API key propia (opcional)" else "API key de Gemini") },
                    singleLine = true,
                    enabled = usarIA,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.size(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(2.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFE65100))
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "Privacidad: al activar la IA, el texto del informe se envía a Google " +
                            "para su análisis. Si prefieres que todo quede en el dispositivo, " +
                            "desactiva esta opción y se usará el análisis local.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.size(8.dp))
                Text(
                    "La API key gratuita se obtiene en Google AI Studio (aistudio.google.com). " +
                        "Se guarda solo en tu teléfono.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onGuardar(usarIA, apiKey) }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cancelar") }
        }
    )
}
