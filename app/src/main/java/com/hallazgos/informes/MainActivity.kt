package com.hallazgos.informes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hallazgos.informes.ui.Etapa
import com.hallazgos.informes.ui.MainViewModel
import com.hallazgos.informes.ui.processing.ProcessingScreen
import com.hallazgos.informes.ui.results.ResultsScreen
import com.hallazgos.informes.ui.selection.SelectionScreen
import com.hallazgos.informes.ui.theme.HallazgosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HallazgosTheme(oscuro = isSystemInDarkTheme()) {
                App()
            }
        }
    }
}

@Composable
private fun App(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold { padding ->
            when (state.etapa) {
                Etapa.SELECCION -> SelectionScreen(
                    sexo = state.sexo,
                    documentos = state.documentos,
                    usarIA = state.usarIA,
                    apiKey = state.apiKey,
                    iaDisponible = state.iaDisponible,
                    hayKeyDelBuild = state.hayKeyDelBuild,
                    onSexoChange = viewModel::seleccionarSexo,
                    onAgregar = viewModel::agregarDocumentos,
                    onQuitar = viewModel::quitarDocumento,
                    onProcesar = viewModel::procesar,
                    onGuardarAjustesIA = viewModel::guardarAjustesIA,
                    modifier = Modifier.padding(padding)
                )

                Etapa.PROCESANDO -> ProcessingScreen(
                    actual = state.progresoActual,
                    total = state.progresoTotal,
                    modifier = Modifier.padding(padding)
                )

                Etapa.RESULTADOS -> ResultsScreen(
                    grupos = state.grupos,
                    sexo = state.sexo,
                    estados = state.estados,
                    sinHallazgos = state.sinHallazgos,
                    textoSinAnalizar = state.textoSinAnalizar,
                    metodoAnalisis = state.metodoAnalisis,
                    avisoIA = state.avisoIA,
                    onNuevo = viewModel::limpiarTodo,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}
