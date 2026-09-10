package com.hallazgos.informes.domain.analysis

import com.hallazgos.informes.domain.model.BodyRegion

/**
 * Diccionarios en español para el motor de reglas.
 *
 * Cada región tiene una lista de sinónimos/términos que, si aparecen en una
 * oración, sugieren esa región. El orden importa: se evalúan de más específico
 * a más general para evitar falsos positivos (ej. "ganglios mediastínicos"
 * antes que "mediastino").
 */
object Diccionarios {

    /** Términos que indican actividad metabólica / captación (relevante en PET-CT). */
    val terminosActividad = listOf(
        "hipermetabólico", "hipermetabolico", "hipermetabolismo", "hipermetabólica", "hipermetabolica",
        "hipercaptación", "hipercaptacion", "hipercaptante", "hipercaptador",
        "captación", "captacion", "captante", "capta",
        "metabólicamente activo", "metabolicamente activo", "metabólicamente activa",
        "avidez", "ávido", "avido", "ávida", "avida",
        "actividad metabólica", "actividad metabolica",
        "foco", "focos", "depósito", "deposito",
        // Términos que describen lesiones aunque no mencionen actividad
        "nódulo", "nodulo", "nódulos", "nodulos", "lesión", "lesion", "lesiones",
        "masa", "masas", "tumor", "tumoral", "neoplasia", "neoplásico", "neoplasico",
        "metástasis", "metastasis", "metastásico", "metastasico", "metastásica",
        "adenopatía", "adenopatia", "adenopatías", "adenopatias",
        "engrosamiento", "infiltración", "infiltracion", "implante", "implantes",
        "litiasis", "cálculo", "calculo", "quiste", "nodular"
    )

    /** Términos que refuerzan severidad alta. */
    val terminosIntensidadAlta = listOf(
        "intenso", "intensa", "marcado", "marcada", "elevado", "elevada",
        "importante", "significativo", "significativa", "ávida", "avida",
        "ávido", "avido", "franca", "franco", "avidez marcada", "gran"
    )

    /** Términos que sugieren baja relevancia o duda. */
    val terminosBaja = listOf(
        "leve", "tenue", "discreto", "discreta", "dudoso", "dudosa",
        "inespecífico", "inespecifico", "fisiológico", "fisiologico", "fisiológica", "fisiologica",
        "probablemente benigno", "sin relevancia", "mínimo", "minimo", "mínima", "minima",
        "ligero", "ligera", "sutil"
    )

    /**
     * Términos de negación. Si preceden de cerca a un término de actividad,
     * el hallazgo se considera negado (no se reporta como activo).
     */
    val negaciones = listOf(
        "sin evidencia de", "no se observa", "no se observan", "no se identifica",
        "no se identifican", "ausencia de", "sin signos de", "sin captación", "sin captacion",
        "no hay", "negativo para", "sin hallazgos", "no se aprecia", "no se aprecian",
        "descarta", "sin alteraciones", "no se detecta", "no se detectan",
        "sin evidencia", "no se visualiza", "no se visualizan", "sin lesiones",
        "metabolismo normal", "conservado", "conservados", "conservada", "conservadas",
        "sin adenopatías", "sin adenopatias", "sin masas", "sin nódulos", "sin nodulos",
        "normometabólico", "normometabolico", "de aspecto normal", "dentro de límites normales",
        "dentro de limites normales", "no se reconoce", "no se reconocen"
    )

    /**
     * Mapa de región -> términos. Ordenado de específico a general.
     */
    val regiones: List<Pair<BodyRegion, List<String>>> = listOf(
        BodyRegion.GANGLIOS_CUELLO to listOf("ganglio cervical", "ganglios cervicales", "adenopatía cervical", "adenopatias cervicales", "cadena cervical", "linfonodo cervical", "linfonodos cervicales", "supraclavicular", "yugular"),
        BodyRegion.GANGLIOS_MEDIASTINO to listOf("ganglio mediastínico", "ganglios mediastínicos", "ganglios mediastinicos", "adenopatía mediastínica", "adenopatias mediastinicas", "hiliar", "hiliares", "subcarinal", "paratraqueal", "linfonodo mediastínico", "linfonodos mediastinicos", "axilar", "axilares", "linfonodo axilar", "linfonodos axilares", "prevascular"),
        BodyRegion.GANGLIOS_ABDOMEN to listOf("ganglio retroperitoneal", "ganglios retroperitoneales", "adenopatía retroperitoneal", "adenopatias abdominales", "paraaórtico", "paraaortico", "mesentérico", "mesenterico", "linfonodo retroperitoneal", "linfonodos retroperitoneales", "portocava", "interaortocava", "celíaco", "celiaco"),
        BodyRegion.GANGLIOS_PELVIS to listOf("ganglio pélvico", "ganglios pélvicos", "ganglios pelvicos", "iliaco", "ilíaco", "inguinal", "inguinales", "adenopatía inguinal", "linfonodo iliaco", "linfonodos iliacos", "obturador", "obturatriz"),
        BodyRegion.GLANDULA_SUPRARRENAL to listOf("suprarrenal", "suprarrenales", "adrenal", "adrenales"),
        BodyRegion.TIROIDES to listOf("tiroides", "tiroideo", "tiroidea", "nódulo tiroideo", "tiroideos"),
        BodyRegion.CEREBRO to listOf("cerebro", "cerebral", "encéfalo", "encefalo", "intracraneal", "frontal", "parietal", "temporal", "occipital", "cerebelo", "cerebeloso", "cortical", "subcortical", "lóbulo frontal", "lobulo frontal"),
        BodyRegion.MAMA to listOf("mama", "mamario", "mamaria", "mamas", "glándula mamaria", "mamario", "retroareolar"),
        BodyRegion.CORAZON to listOf("corazón", "corazon", "cardíaco", "cardiaco", "miocardio", "miocárdico", "pericardio", "pericárdico", "pericardico"),
        BodyRegion.MEDIASTINO to listOf("mediastino", "mediastínico", "mediastinico", "mediastínica"),
        BodyRegion.PULMON to listOf(
            "pulmón", "pulmon", "pulmonar", "pulmones",
            "lóbulo pulmonar", "nódulo pulmonar", "lobar", "lingula", "língula",
            // Lóbulos pulmonares (el "lóbulo medio" solo existe en el pulmón)
            "lóbulo medio", "lobulo medio", "lóbulo superior", "lobulo superior",
            "lóbulo inferior", "lobulo inferior",
            // Formas de describir lesiones pulmonares en PET/CT
            "opacidad parenquimatosa", "opacidad", "parénquima pulmonar", "parenquima pulmonar",
            "tracción pleural", "traccion pleural", "pleural", "pleura",
            "broncogénica", "broncogenica", "lepídico", "lepidico", "bronquio", "bronquios", "tráquea", "traquea"
        ),
        BodyRegion.HIGADO to listOf("hígado", "higado", "hepático", "hepatico", "hepática", "hepatica", "lesión hepática", "hepatobiliar", "segmento hepático", "lóbulo hepático", "lobulo hepatico"),
        BodyRegion.VESICULA to listOf("vesícula", "vesicula", "biliar", "colédoco", "coledoco", "vías biliares", "vias biliares"),
        BodyRegion.BAZO to listOf("bazo", "esplénico", "esplenico", "esplénica", "esplenica", "esplénicos"),
        BodyRegion.PANCREAS to listOf("páncreas", "pancreas", "pancreático", "pancreatico", "pancreática", "pancreatica", "cabeza pancreática", "cola del páncreas"),
        BodyRegion.ESTOMAGO to listOf("estómago", "estomago", "gástrico", "gastrico", "gástrica", "gastrica", "gastroesofágico", "cardias", "antro gástrico"),
        BodyRegion.RINON to listOf("riñón", "rinon", "renal", "riñones", "rinones", "nefrolitiasis", "litiasis renal", "cálculo renal", "calculo renal", "pielocalicial", "cortical renal", "parénquima renal", "ureteral", "uréter", "ureter"),
        BodyRegion.VEJIGA to listOf("vejiga", "vesical", "vesículo", "vesicula urinaria"),
        BodyRegion.PROSTATA to listOf("próstata", "prostata", "prostático", "prostatico", "prostática", "prostatica", "prostáticos"),
        BodyRegion.UTERO to listOf("útero", "utero", "uterino", "uterina", "endometrio", "endometrial", "cérvix", "cervix", "cuello uterino", "miometrio", "miometrial"),
        BodyRegion.OVARIO to listOf("ovario", "ovárico", "ovarico", "ovárica", "ovarica", "anexial", "anexo", "anexos", "anexos uterinos"),
        BodyRegion.COLON to listOf("colon", "colónico", "colonico", "rectosigmoides", "recto", "rectal", "sigmoides", "sigmoideo", "ciego", "cecal", "ascendente", "descendente", "transverso"),
        BodyRegion.INTESTINO to listOf("intestino", "intestinal", "asa intestinal", "asas intestinales", "yeyuno", "íleon", "ileon", "duodeno", "duodenal", "delgado", "yeyunal", "ileal"),
        BodyRegion.COLUMNA to listOf("columna", "vertebral", "vértebra", "vertebra", "cuerpo vertebral", "l1", "l2", "l3", "l4", "l5", "d1", "d12", "t1", "t12", "sacro", "sacra", "lumbar", "dorsal", "torácica vertebral", "coxis"),
        BodyRegion.HUESO to listOf("hueso", "óseo", "oseo", "ósea", "osea", "esquelético", "esqueletico", "costilla", "costal", "costillas", "fémur", "femur", "húmero", "humero", "pelvis ósea", "iliaco óseo", "esternón", "esternon", "escápula", "escapula", "clavícula", "clavicula", "óseos", "medular óseo", "cresta iliaca"),
        BodyRegion.CABEZA_CUELLO to listOf("cuello", "amígdala", "amigdala", "faringe", "laringe", "parótida", "parotida", "base de lengua", "nasofaringe", "orofaringe", "submandibular", "glándula salival", "seno piriforme"),
        BodyRegion.PIEL_TEJIDOS to listOf("piel", "cutáneo", "cutaneo", "cutánea", "subcutáneo", "subcutaneo", "subcutánea", "tejido blando", "tejidos blandos", "muscular", "partes blandas", "intramuscular")
    )
}
