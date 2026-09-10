package com.hallazgos.informes.ui.selection

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import com.hallazgos.informes.domain.model.DocumentItem
import com.hallazgos.informes.domain.model.Sexo
import com.hallazgos.informes.domain.model.TipoDocumento
import com.hallazgos.informes.ui.common.DisclaimerCard
import com.hallazgos.informes.ui.settings.AcercaDeDialog
import com.hallazgos.informes.ui.settings.AjustesIADialog

@Composable
fun SelectionScreen(
    sexo: Sexo,
    documentos: List<DocumentItem>,
    usarIA: Boolean,
    apiKey: String,
    iaDisponible: Boolean,
    hayKeyDelBuild: Boolean,
    onSexoChange: (Sexo) -> Unit,
    onAgregar: (List<android.net.Uri>) -> Unit,
    onQuitar: (DocumentItem) -> Unit,
    onProcesar: () -> Unit,
    onGuardarAjustesIA: (Boolean, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        onAgregar(uris)
    }

    var mostrarAjustes by remember { mutableStateOf(false) }
    var mostrarAcercaDe by remember { mutableStateOf(false) }

    if (mostrarAjustes) {
        AjustesIADialog(
            usarIAInicial = usarIA,
            apiKeyInicial = apiKey,
            hayKeyDelBuild = hayKeyDelBuild,
            onGuardar = { u, k ->
                onGuardarAjustesIA(u, k)
                mostrarAjustes = false
            },
            onCerrar = { mostrarAjustes = false }
        )
    }
    if (mostrarAcercaDe) {
        AcercaDeDialog(onCerrar = { mostrarAcercaDe = false })
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Analizar informes",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = { mostrarAcercaDe = true }) {
                Icon(Icons.Default.Info, contentDescription = "Acerca de")
            }
        }
        Spacer(Modifier.size(4.dp))
        Text(
            "Selecciona una o varias imágenes o PDFs.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.size(12.dp))

        // Chip de estado/acceso al análisis con IA.
        AssistChip(
            onClick = { mostrarAjustes = true },
            label = {
                Text(if (iaDisponible) "Análisis con IA: activado" else "Análisis con IA: desactivado")
            },
            leadingIcon = {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, Modifier.size(18.dp))
            },
            colors = if (iaDisponible) {
                AssistChipDefaults.assistChipColors(
                    leadingIconContentColor = MaterialTheme.colorScheme.primary,
                    labelColor = MaterialTheme.colorScheme.primary
                )
            } else AssistChipDefaults.assistChipColors()
        )

        Spacer(Modifier.size(16.dp))

        Text("Esquema corporal", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.size(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = sexo == Sexo.HOMBRE,
                onClick = { onSexoChange(Sexo.HOMBRE) },
                label = { Text("Hombre") }
            )
            FilterChip(
                selected = sexo == Sexo.MUJER,
                onClick = { onSexoChange(Sexo.MUJER) },
                label = { Text("Mujer") }
            )
        }

        Spacer(Modifier.size(16.dp))

        OutlinedButton(
            onClick = { picker.launch(arrayOf("image/*", "application/pdf")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.UploadFile, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Agregar imágenes o PDFs")
        }

        Spacer(Modifier.size(12.dp))

        if (documentos.isEmpty()) {
            DisclaimerCard()
            Spacer(Modifier.size(12.dp))
            Text(
                "Aún no has agregado documentos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                "${documentos.size} documento(s) seleccionado(s)",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.size(8.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(documentos, key = { it.uri.toString() }) { doc ->
                    DocumentoCard(doc = doc, onQuitar = { onQuitar(doc) })
                }
            }
            Spacer(Modifier.size(12.dp))
            Button(
                onClick = onProcesar,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Analizar ${documentos.size} documento(s)")
            }
        }
    }
}

@Composable
private fun DocumentoCard(doc: DocumentItem, onQuitar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icono = when (doc.tipo) {
                TipoDocumento.IMAGEN -> Icons.Default.Image
                TipoDocumento.PDF -> Icons.Default.Description
                TipoDocumento.DESCONOCIDO -> Icons.Default.Description
            }
            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    doc.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2
                )
                Text(
                    when (doc.tipo) {
                        TipoDocumento.IMAGEN -> "Imagen"
                        TipoDocumento.PDF -> "PDF"
                        TipoDocumento.DESCONOCIDO -> "Formato desconocido"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (doc.tipo == TipoDocumento.DESCONOCIDO) Color(0xFFB71C1C)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onQuitar) {
                Icon(Icons.Default.Close, contentDescription = "Quitar")
            }
        }
    }
}
