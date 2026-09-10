# Hallazgos de Informes

App Android que analiza uno o varios documentos médicos (imágenes o PDFs, por ejemplo informes de PET-CT) y muestra los hallazgos de forma clara: una lista organizada por región anatómica y un esquema corporal (hombre o mujer) con marcadores en las zonas mencionadas.

Todo funciona **en el dispositivo, sin conexión y sin costo**. No se envían datos a ningún servidor.

> **Aviso importante:** esta app es una **ayuda de visualización**, no un diagnóstico. No reemplaza la interpretación de un profesional médico. Los hallazgos se extraen con reglas basadas en palabras clave y pueden ser incompletos.

## Qué hace

1. Seleccionas varias imágenes y/o PDFs.
2. Cada archivo se procesa:
   - **Imágenes** → OCR con ML Kit (offline).
   - **PDFs** → se rasterizan las páginas y se les aplica OCR.
3. Un motor de reglas en español detecta región anatómica, lateralidad, valores SUV, tamaños y actividad metabólica.
4. Los hallazgos de todos los documentos se consolidan, se agrupan por región y se muestran en:
   - Un **informe visual** con dos figuras (frontal y posterior), marcadores numerados, callouts y leyenda.
   - Una **lista** con el detalle y el archivo de origen (trazabilidad).
5. Puedes **compartir la imagen PNG** o **exportar un PDF** con la imagen y el listado de hallazgos.

## Requisitos

- **Android Studio** (incluye el JDK y el SDK necesarios). Todo gratis.
- Un teléfono Android (API 26 / Android 8.0 o superior) o un emulador.
- No requiere cuentas, API keys ni servicios de pago.

## Cómo abrir y ejecutar

1. Abre **Android Studio**.
2. `File > Open` y selecciona esta carpeta del proyecto.
3. Espera a que Gradle sincronice (la primera vez descarga dependencias).
4. Conecta un teléfono con **depuración USB** activada, o crea un emulador.
5. Pulsa **Run** (el botón ▶). La primera ejecución descarga el modelo de OCR de ML Kit.

### Compilar desde línea de comandos (opcional)

En Windows, con Android Studio instalado en la ruta por defecto:

```powershell
cmd /c "set `"JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`" && gradlew.bat assembleDebug"
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Estructura del proyecto

```
app/src/main/java/com/hallazgos/informes/
├── MainActivity.kt              Punto de entrada y navegación entre etapas
├── domain/
│   ├── model/                   Modelos: Finding, BodyRegion, DocumentItem, Sexo...
│   ├── ocr/                     Extracción de texto (ML Kit + PdfRenderer)
│   ├── analysis/                Motor de reglas y diccionarios en español
│   └── consolidation/           Fusión de hallazgos de varios documentos
├── render/                      Dibujo del esquema corporal y export a PNG
└── ui/                          Pantallas Compose (selección, progreso, resultados)
```

## Ilustraciones anatómicas (mejorar el realismo)

La app **ya incluye** 4 ilustraciones anatómicas en `app/src/main/res/drawable/`:

```
cuerpo_hombre_frontal.png
cuerpo_hombre_posterior.png
cuerpo_mujer_frontal.png
cuerpo_mujer_posterior.png
```

Las detecta automáticamente y las usa como fondo de cada figura; encima dibuja los marcadores, números y líneas de conexión. Si eliminas alguna, para esa figura se usa el dibujo por código como respaldo (no hay que tocar código).

### Reemplazar las ilustraciones

Para usar otras imágenes, sustituye esos archivos manteniendo los mismos nombres. Recomendaciones:
- Una figura por archivo (frontal o posterior por separado), centrada.
- Preferible fondo transparente o blanco.
- Proporción vertical (más alta que ancha).

### Ajustar la posición de los marcadores

Los marcadores se colocan según coordenadas relativas (x, y de 0 a 1) definidas en `domain/model/BodyRegion.kt`. Si al cambiar las ilustraciones un marcador no cae sobre el órgano correcto, edita la `x`/`y` de esa región. `x=0.5` es el centro; `y=0` es arriba (cabeza) y `y=1` es abajo (pies). Las coordenadas actuales están calibradas para las ilustraciones incluidas.

## Análisis con IA (Gemini) y API key

La app puede usar Gemini para interpretar los informes (modo opcional). La API key se resuelve así:

1. **Key incrustada en el build (recomendada para uso propio):** añade en `local.properties` una línea:

   ```
   GEMINI_API_KEY=tu_clave_de_gemini
   ```

   El archivo `local.properties` NO se versiona (está en `.gitignore`). Todos los APK que compiles traerán esa clave, así no hay que ingresarla en cada dispositivo. El análisis con IA queda activado por defecto.

2. **Key introducida por el usuario:** si alguien escribe su propia clave en Ajustes, esa tiene prioridad sobre la del build.

> Seguridad: la key incrustada puede extraerse del APK y todos los dispositivos comparten la misma cuota (free tier). Es adecuado para uso personal o un grupo pequeño de confianza. Para distribución pública, lo correcto es un backend propio (proxy) que guarde la key; no incrustarla.

Si no hay key (ni en el build ni del usuario), la app usa el análisis local por reglas, que funciona sin conexión.

## Exportar a PDF

Desde la pantalla de resultados, el botón **Exportar PDF** genera un documento con:
- Página 1: la imagen del informe visual.
- Páginas siguientes: el listado de hallazgos por región (descripción, lado, SUVmax, tamaño y archivo de origen).

El PDF se genera con `PdfDocument` nativo (sin dependencias externas) y se abre el selector para compartirlo o guardarlo.

## Cómo ampliar el análisis

El motor de reglas es un diccionario ampliable. Para reconocer más términos o regiones:

- Añade sinónimos en `domain/analysis/Diccionarios.kt` (listas `regiones`, `terminosActividad`, etc.).
- Añade nuevas regiones en `domain/model/BodyRegion.kt` con sus coordenadas relativas `x`/`y` para ubicar el marcador en el esquema.

## Limitaciones conocidas (V1)

- El análisis es heurístico (palabras clave), no comprensión clínica real.
- El OCR puede fallar con fotos de baja calidad o manuscritos.
- El esquema corporal es esquemático, no anatómicamente exacto.
- PDFs muy largos se limitan a las primeras 30 páginas por rendimiento.

## Posible evolución (V2)

Integrar un modelo de lenguaje (LLM) para interpretar el informe con más precisión. Esto requeriría conexión, una API key (algunos servicios tienen capa gratuita limitada) y aceptar el envío de datos a un servicio externo, con las implicaciones de privacidad que eso conlleva.
