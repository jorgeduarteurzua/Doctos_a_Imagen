package com.hallazgos.informes.ui.results

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hallazgos.informes.domain.model.EstadoDocumento
import com.hallazgos.informes.domain.model.Finding
import com.hallazgos.informes.domain.model.Lateralidad
import com.hallazgos.informes.domain.model.RegionGroup
import com.hallazgos.informes.domain.model.Sexo
import com.hallazgos.informes.render.BodyMapExporter
import com.hallazgos.informes.render.PdfExporter
import com.hallazgos.informes.render.SeveridadColor
import com.hallazgos.informes.ui.common.DisclaimerCard

@Composable
fun ResultsScreen(
    grupos: List<RegionGroup>,
    sexo: Sexo,
    estados: List<EstadoDocumento>,
    sinHallazgos: Boolean,
    textoSinAnalizar: String,
    metodoAnalisis: String,
    avisoIA: String,
    onNuevo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Resultados",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (metodoAnalisis.isNotBlank()) {
                    Text(
                        metodoAnalisis,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (avisoIA.isNotBlank()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                    Text(
                        avisoIA,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF5D4037)
                    )
                }
            }
        }

        item { DisclaimerCard() }

        // Errores por archivo (si los hay)
        val errores = estados.filterIsInstance<EstadoDocumento.Error>()
        if (errores.isNotEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "${errores.size} archivo(s) no se pudieron analizar:",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFB71C1C)
                        )
                        errores.forEach {
                            Text(
                                "• ${it.documento.nombre}: ${it.mensaje}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF5D4037)
                            )
                        }
                    }
                }
            }
        }

        if (sinHallazgos) {
            item {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "No se detectaron hallazgos automáticamente.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "Esto puede pasar si el informe usa una redacción poco común o si el " +
                                "texto no se leyó bien. Abajo puedes revisar el texto extraído.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (textoSinAnalizar.isNotBlank()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Text(
                            textoSinAnalizar,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        } else {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    BodyMapView(
                        grupos = grupos,
                        sexo = sexo,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }

            item {
                Text(
                    "Hallazgos por región",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            itemsIndexed(grupos, key = { _, g -> g.region.name }) { indice, grupo ->
                RegionGroupCard(numero = indice + 1, grupo = grupo)
            }
        }

        item {
            Spacer(Modifier.size(8.dp))
            if (!sinHallazgos) {
                Button(
                    onClick = { compartir(context, grupos, sexo) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Compartir imagen")
                }
                Spacer(Modifier.size(8.dp))
                Button(
                    onClick = { compartirPdf(context, grupos, sexo) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Exportar PDF")
                }
                Spacer(Modifier.size(8.dp))
            }
            OutlinedButton(onClick = onNuevo, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Analizar otros documentos")
            }
        }
    }
}

@Composable
private fun RegionGroupCard(numero: Int, grupo: RegionGroup) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(SeveridadColor.de(grupo.severidadMaxima)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        numero.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.size(8.dp))
                Text(
                    grupo.region.etiqueta,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.size(8.dp))
            grupo.findings.forEach { finding ->
                FindingRow(finding)
                Spacer(Modifier.size(6.dp))
            }
        }
    }
}

@Composable
private fun FindingRow(finding: Finding) {
    Column {
        Text(finding.descripcion, style = MaterialTheme.typography.bodyMedium)
        val detalles = buildList {
            add(SeveridadColor.etiqueta(finding.severidad))
            when (finding.lateralidad) {
                Lateralidad.DERECHO -> add("lado derecho")
                Lateralidad.IZQUIERDO -> add("lado izquierdo")
                Lateralidad.BILATERAL -> add("bilateral")
                Lateralidad.NO_APLICA -> {}
            }
            finding.suvMax?.let { add("SUVmax ${it}") }
            finding.tamanoMm?.let { add("${it.toInt()} mm") }
            add("origen: ${finding.sourceFileName}")
        }
        Text(
            detalles.joinToString(" • "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun compartir(context: Context, grupos: List<RegionGroup>, sexo: Sexo) {
    val uri = BodyMapExporter.exportarPng(context, grupos, sexo)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir hallazgos"))
}

private fun compartirPdf(context: Context, grupos: List<RegionGroup>, sexo: Sexo) {
    val uri = PdfExporter.exportarPdf(context, grupos, sexo)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir PDF de hallazgos"))
}
