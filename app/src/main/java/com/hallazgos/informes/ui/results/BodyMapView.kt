package com.hallazgos.informes.ui.results

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.hallazgos.informes.domain.model.RegionGroup
import com.hallazgos.informes.domain.model.Sexo
import com.hallazgos.informes.render.ReportComposer

/**
 * Muestra el informe visual completo (dos figuras, callouts, leyenda) como una
 * imagen. Es exactamente la misma imagen que se comparte, generada por
 * ReportComposer. Se memoriza para no regenerarla en cada recomposición.
 */
@Composable
fun BodyMapView(
    grupos: List<RegionGroup>,
    sexo: Sexo,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap = remember(grupos, sexo) {
        ReportComposer.componer(context, grupos, sexo).asImageBitmap()
    }
    Image(
        bitmap = bitmap,
        contentDescription = "Informe visual de hallazgos",
        modifier = modifier.fillMaxWidth(),
        contentScale = ContentScale.FillWidth
    )
}
