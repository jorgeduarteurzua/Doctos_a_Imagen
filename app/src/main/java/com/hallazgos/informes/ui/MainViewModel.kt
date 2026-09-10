package com.hallazgos.informes.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hallazgos.informes.data.SettingsStore
import com.hallazgos.informes.domain.analysis.FindingsAnalyzer
import com.hallazgos.informes.domain.analysis.GeminiAnalyzer
import com.hallazgos.informes.domain.consolidation.FindingsConsolidator
import com.hallazgos.informes.domain.model.DocumentItem
import com.hallazgos.informes.domain.model.EstadoDocumento
import com.hallazgos.informes.domain.model.Finding
import com.hallazgos.informes.domain.model.RegionGroup
import com.hallazgos.informes.domain.model.Sexo
import com.hallazgos.informes.domain.ocr.DocumentResolver
import com.hallazgos.informes.domain.ocr.TextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Etapa actual del flujo de la app.
 */
enum class Etapa {
    SELECCION,
    PROCESANDO,
    RESULTADOS
}

/**
 * Estado observable de la pantalla.
 */
data class UiState(
    val etapa: Etapa = Etapa.SELECCION,
    val sexo: Sexo = Sexo.HOMBRE,
    val documentos: List<DocumentItem> = emptyList(),
    val estados: List<EstadoDocumento> = emptyList(),
    val progresoActual: Int = 0,
    val progresoTotal: Int = 0,
    val grupos: List<RegionGroup> = emptyList(),
    val sinHallazgos: Boolean = false,
    val textoSinAnalizar: String = "",
    // Ajustes de IA
    val usarIA: Boolean = false,
    val apiKey: String = "",
    val iaDisponible: Boolean = false,
    val hayKeyDelBuild: Boolean = false,
    // Método realmente usado en el último análisis, para informar al usuario.
    val metodoAnalisis: String = "",
    val avisoIA: String = ""
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val textExtractor = TextExtractor(app.applicationContext)
    private val analyzer = FindingsAnalyzer()
    private val consolidator = FindingsConsolidator()
    private val settings = SettingsStore(app.applicationContext)

    private val _state = MutableStateFlow(
        UiState(
            usarIA = settings.usarIA,
            apiKey = settings.apiKey,
            iaDisponible = settings.iaDisponible,
            hayKeyDelBuild = settings.hayKeyDelBuild
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun seleccionarSexo(sexo: Sexo) {
        _state.update { it.copy(sexo = sexo) }
    }

    fun guardarAjustesIA(usarIA: Boolean, apiKey: String) {
        settings.usarIA = usarIA
        settings.apiKey = apiKey
        _state.update {
            it.copy(
                usarIA = usarIA,
                apiKey = apiKey.trim(),
                iaDisponible = settings.iaDisponible
            )
        }
    }

    fun agregarDocumentos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val ctx = getApplication<Application>().applicationContext
        val nuevos = uris.map { DocumentResolver.resolver(ctx, it) }
        _state.update { current ->
            val existentesUri = current.documentos.map { it.uri }.toSet()
            val combinados = current.documentos + nuevos.filter { it.uri !in existentesUri }
            current.copy(documentos = combinados)
        }
    }

    fun quitarDocumento(documento: DocumentItem) {
        _state.update { current ->
            current.copy(documentos = current.documentos.filterNot { it.uri == documento.uri })
        }
    }

    fun limpiarTodo() {
        _state.value = UiState(sexo = _state.value.sexo)
    }

    fun volverASeleccion() {
        _state.update { it.copy(etapa = Etapa.SELECCION) }
    }

    fun procesar() {
        val documentos = _state.value.documentos
        if (documentos.isEmpty()) return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    etapa = Etapa.PROCESANDO,
                    progresoActual = 0,
                    progresoTotal = documentos.size,
                    estados = documentos.map { d -> EstadoDocumento.Pendiente(d) }
                )
            }

            // Decidimos el método una sola vez para todo el lote.
            val geminiAnalyzer = if (settings.iaDisponible) GeminiAnalyzer(settings.apiKeyEfectiva) else null
            var huboFallbackIA = false
            var errorIA: String? = null

            val todosLosFindings = mutableListOf<Finding>()
            val estadosFinales = mutableListOf<EstadoDocumento>()

            documentos.forEachIndexed { indice, documento ->
                _state.update { it.copy(progresoActual = indice + 1) }
                val resultado = procesarUno(documento, geminiAnalyzer)
                estadosFinales.add(resultado.estado)
                if (resultado.estado is EstadoDocumento.Completado) {
                    todosLosFindings.addAll(resultado.estado.findings)
                }
                if (resultado.usoFallback) {
                    huboFallbackIA = true
                    if (errorIA == null) errorIA = resultado.errorIA
                }
                _state.update { it.copy(estados = estadosFinales.toList()) }
            }

            val grupos = consolidator.consolidar(todosLosFindings)
            val textoCombinado = estadosFinales
                .filterIsInstance<EstadoDocumento.Completado>()
                .joinToString("\n\n") { "— ${it.documento.nombre} —\n${it.textoExtraido}" }

            val metodo = when {
                geminiAnalyzer != null && !huboFallbackIA -> "Análisis con IA (Gemini)"
                geminiAnalyzer != null && huboFallbackIA -> "Análisis local (IA no disponible)"
                else -> "Análisis local"
            }
            val aviso = if (geminiAnalyzer != null && huboFallbackIA) {
                val detalle = errorIA?.let { "\nDetalle: $it" } ?: ""
                "No se pudo usar la IA en la nube. Se usó el análisis local como respaldo.$detalle"
            } else ""

            _state.update {
                it.copy(
                    etapa = Etapa.RESULTADOS,
                    grupos = grupos,
                    sinHallazgos = grupos.isEmpty(),
                    textoSinAnalizar = textoCombinado,
                    metodoAnalisis = metodo,
                    avisoIA = aviso
                )
            }
        }
    }

    /** Resultado interno del procesamiento de un documento. */
    private data class ResultadoDoc(
        val estado: EstadoDocumento,
        val usoFallback: Boolean,
        val errorIA: String? = null
    )

    private suspend fun procesarUno(
        documento: DocumentItem,
        geminiAnalyzer: GeminiAnalyzer?
    ): ResultadoDoc = withContext(Dispatchers.Default) {
        try {
            val texto = textExtractor.extraer(documento)
            if (texto.isBlank()) {
                return@withContext ResultadoDoc(
                    EstadoDocumento.Error(
                        documento,
                        "No se pudo leer texto del archivo (puede ser una imagen sin texto o de baja calidad)."
                    ),
                    usoFallback = false
                )
            }

            // Intento con IA si está disponible; si falla, caigo al análisis local.
            if (geminiAnalyzer != null) {
                try {
                    val findings = geminiAnalyzer.analizar(texto, documento.nombre)
                    return@withContext ResultadoDoc(
                        EstadoDocumento.Completado(documento, texto, findings),
                        usoFallback = false
                    )
                } catch (e: Exception) {
                    val findings = analyzer.analizar(texto, documento.nombre)
                    return@withContext ResultadoDoc(
                        EstadoDocumento.Completado(documento, texto, findings),
                        usoFallback = true,
                        errorIA = e.message
                    )
                }
            }

            val findings = analyzer.analizar(texto, documento.nombre)
            ResultadoDoc(EstadoDocumento.Completado(documento, texto, findings), usoFallback = false)
        } catch (e: Exception) {
            ResultadoDoc(
                EstadoDocumento.Error(
                    documento,
                    e.message ?: "Error desconocido al procesar el archivo."
                ),
                usoFallback = false
            )
        }
    }
}
