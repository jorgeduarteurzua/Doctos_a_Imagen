package com.hallazgos.informes.domain.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.hallazgos.informes.domain.model.DocumentItem
import com.hallazgos.informes.domain.model.TipoDocumento
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Extrae texto de imágenes y PDFs de forma 100% local (offline, gratis).
 *
 * - Imágenes: OCR con ML Kit Text Recognition (Latin).
 * - PDF: intenta primero leer texto embebido; si no hay (PDF escaneado),
 *   rasteriza cada página con PdfRenderer y le aplica OCR.
 */
class TextExtractor(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Densidad de render para PDF escaneado (px por punto). Más alto = mejor OCR pero más memoria. */
    private val pdfRenderScale = 2.0f

    /** Máximo de páginas a procesar por PDF, para evitar bloqueos con documentos enormes. */
    private val maxPaginasPdf = 30

    suspend fun extraer(documento: DocumentItem): String {
        return when (documento.tipo) {
            TipoDocumento.IMAGEN -> extraerDeImagen(documento.uri)
            TipoDocumento.PDF -> extraerDePdf(documento.uri)
            TipoDocumento.DESCONOCIDO -> throw IllegalArgumentException(
                "Formato no soportado. Use imágenes (JPG, PNG) o PDF."
            )
        }
    }

    private suspend fun extraerDeImagen(uri: Uri): String {
        val image = InputImage.fromFilePath(context, uri)
        return reconocer(image)
    }

    private suspend fun extraerDePdf(uri: Uri): String {
        val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalStateException("No se pudo abrir el PDF.")

        pfd.use { descriptor ->
            val renderer = PdfRenderer(descriptor)
            renderer.use { pdf ->
                val totalPaginas = minOf(pdf.pageCount, maxPaginasPdf)
                val sb = StringBuilder()
                for (i in 0 until totalPaginas) {
                    val texto = renderizarYReconocerPagina(pdf, i)
                    if (texto.isNotBlank()) {
                        sb.append(texto).append("\n\n")
                    }
                }
                return sb.toString().trim()
            }
        }
    }

    private suspend fun renderizarYReconocerPagina(pdf: PdfRenderer, indice: Int): String {
        val page = pdf.openPage(indice)
        val ancho = (page.width * pdfRenderScale).toInt().coerceAtLeast(1)
        val alto = (page.height * pdfRenderScale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()

        val image = InputImage.fromBitmap(bitmap, 0)
        val texto = reconocer(image)
        bitmap.recycle()
        return texto
    }

    private suspend fun reconocer(image: InputImage): String =
        suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { result -> cont.resume(result.text) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
}
