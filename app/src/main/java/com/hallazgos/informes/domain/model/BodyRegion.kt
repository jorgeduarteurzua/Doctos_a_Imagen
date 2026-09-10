package com.hallazgos.informes.domain.model

/**
 * Lado / lateralidad de un hallazgo.
 */
enum class Lateralidad {
    DERECHO,
    IZQUIERDO,
    BILATERAL,
    NO_APLICA
}

/**
 * Vista del cuerpo en la que se dibuja de forma natural una región.
 * FRONTAL para vísceras y órganos anteriores; POSTERIOR para columna,
 * riñones y estructuras de la espalda.
 */
enum class VistaCorporal {
    FRONTAL,
    POSTERIOR
}

/**
 * Regiones anatómicas que la app sabe reconocer y ubicar en el esquema corporal.
 *
 * [x] e [y] son coordenadas RELATIVAS (0f..1f) sobre el lienzo del esquema,
 * usadas por el renderizador para colocar el marcador. (0,0) es la esquina
 * superior izquierda; (1,1) la inferior derecha.
 *
 * Cuando la región es lateralizable, [x] representa la línea media y el
 * renderizador desplaza el marcador a izquierda/derecha según la lateralidad.
 */
enum class BodyRegion(
    val etiqueta: String,
    val x: Float,
    val y: Float,
    val lateralizable: Boolean,
    val vista: VistaCorporal = VistaCorporal.FRONTAL
) {
    // Coordenadas (x,y) relativas 0..1, ajustadas a las ilustraciones anatómicas
    // incluidas (cuerpo_{sexo}_{vista}). Vista de frente: el lado DERECHO del
    // paciente aparece a la IZQUIERDA de la imagen. Ajusta estos valores si
    // cambias las ilustraciones de fondo.
    CEREBRO("Cerebro", 0.5f, 0.055f, false),
    CABEZA_CUELLO("Cabeza y cuello", 0.5f, 0.115f, true),
    TIROIDES("Tiroides", 0.5f, 0.135f, true),
    GANGLIOS_CUELLO("Ganglios cervicales", 0.5f, 0.125f, true),
    PULMON("Pulmón", 0.5f, 0.205f, true),
    MEDIASTINO("Mediastino", 0.5f, 0.215f, false),
    GANGLIOS_MEDIASTINO("Ganglios mediastínicos", 0.5f, 0.20f, false),
    MAMA("Mama", 0.5f, 0.20f, true),
    CORAZON("Corazón", 0.53f, 0.215f, false),
    HIGADO("Hígado", 0.42f, 0.27f, false),
    VESICULA("Vesícula biliar", 0.46f, 0.28f, false),
    BAZO("Bazo", 0.58f, 0.27f, false),
    ESTOMAGO("Estómago", 0.57f, 0.26f, false),
    PANCREAS("Páncreas", 0.5f, 0.29f, false),
    RINON("Riñón", 0.5f, 0.275f, true, VistaCorporal.POSTERIOR),
    GLANDULA_SUPRARRENAL("Glándula suprarrenal", 0.5f, 0.25f, true, VistaCorporal.POSTERIOR),
    GANGLIOS_ABDOMEN("Ganglios abdominales", 0.5f, 0.32f, false),
    INTESTINO("Intestino", 0.5f, 0.35f, false),
    COLON("Colon", 0.5f, 0.34f, false),
    VEJIGA("Vejiga", 0.5f, 0.42f, false),
    PROSTATA("Próstata", 0.5f, 0.435f, false),
    UTERO("Útero", 0.5f, 0.425f, false),
    OVARIO("Ovario", 0.5f, 0.42f, true),
    GANGLIOS_PELVIS("Ganglios pélvicos", 0.5f, 0.42f, true),
    HUESO("Hueso", 0.5f, 0.40f, false, VistaCorporal.POSTERIOR),
    COLUMNA("Columna", 0.5f, 0.28f, false, VistaCorporal.POSTERIOR),
    PIEL_TEJIDOS("Piel / tejidos blandos", 0.5f, 0.35f, true),
    OTRA("Otra región", 0.5f, 0.65f, false);

    companion object {
        /** Regiones que solo aplican al esquema masculino. */
        val soloMasculino = setOf(PROSTATA)

        /** Regiones que solo aplican al esquema femenino. */
        val soloFemenino = setOf(UTERO, OVARIO)
    }
}
