# Clean IPTV

Aplicación Android en Kotlin y Jetpack Compose para móvil y Android TV. El catálogo de películas y series usa metadatos de TMDb; los canales se cargan desde la lista pública de TDTChannels. TMDb no proporciona vídeo: cada película o serie necesita que el usuario configure una fuente de reproducción directa o un embed iframe autorizado.

## Fuentes y comportamiento

- **Películas y series:** TMDb aporta títulos, géneros, búsqueda, fichas, reparto, temporadas, episodios, pósteres e imágenes. Sin una API Key válida, la app muestra estados vacíos/configuración; no sustituye resultados por contenido de muestra.
- **Canales:** se descarga y analiza `https://www.tdtchannels.com/lists/tv.m3u8`. Se muestran los canales con enlace HTTP/HTTPS reproducible y sus metadatos M3U (categoría, logo e ID EPG cuando estén presentes). La programación EPG no está integrada.
- **Reproducción de vídeo:** admite enlaces directos compatibles con ExoPlayer (incluidos HLS/DASH) y páginas/URLs de embed iframe mediante WebView. Las fuentes de películas y series se guardan en el almacenamiento privado del dispositivo; no hay catálogo de enlaces de vídeo incluido.
- **Descargas e historial:** todavía no están implementados y muestran estados vacíos, no datos ficticios.
- **Favoritos:** se mantienen en memoria durante la sesión actual.
- **Android TV:** navegación lateral, foco visible y controles aptos para mando; móvil usa navegación inferior.

Usa únicamente emisiones abiertas y enlaces para los que tengas autorización. Respeta las condiciones de TDTChannels y los proveedores de cada contenido. La app no intenta eludir DRM, geobloqueos, autenticación ni otras restricciones. Para admitir algunas fuentes públicas, Android permite tráfico HTTP sin cifrar; prefiere HTTPS cuando esté disponible y ten en cuenta que HTTP puede ser interceptado.

## Configurar TMDb

La app lee una API Key v3 desde `local.properties` (recomendado para desarrollo local) o desde la variable de entorno `TMDB_API_KEY`:

```properties
# Añadir al archivo local.properties de la raíz, sin borrar sdk.dir.
tmdb.api.key=TU_CLAVE_V3_DE_TMDB
```

`local.properties` está excluido por `.gitignore`; no incluyas claves ni tokens en Git. Tras sincronizar Gradle, la clave se usa para las solicitudes de metadatos. La clave cliente queda incorporada en el APK y no es un secreto seguro para distribuir una app pública; restringe la clave según las opciones de TMDb o usa un backend propio. La clave compartida anteriormente quedó expuesta en la conversación: revócala y genera otra antes de usarla.

Este producto utiliza la API de TMDb, pero no está avalado ni certificado por TMDb.

## Añadir una fuente de vídeo

1. Abre una película o serie del catálogo TMDb.
2. Pulsa **Añadir enlace** en la ficha, o **Añadir enlace** junto a un episodio para guardar una fuente distinta por episodio.
3. Selecciona **Directo** para una URL de vídeo compatible o **Embed iframe** para una URL/código iframe.
4. Guarda el enlace. Se conserva solo en el dispositivo y se puede quitar desde la misma ficha.

No hay enlaces de películas/series preconfigurados: TMDb entrega metadatos e imágenes, no derechos ni fuentes de reproducción.

## Abrir en Android Studio

1. Selecciona **Get from VCS** (o **File → New → Project from Version Control**).
2. Selecciona **Git** y pega `https://github.com/SamuelPart/cloneapp.git`.
3. Clona el repositorio y abre la carpeta raíz, donde están `settings.gradle` y `gradlew`.
4. Usa JDK 17 o 21 para Gradle y permite que Android Studio sincronice el proyecto.
5. Selecciona un emulador o dispositivo con Android 5.0 o posterior y pulsa **Run**.

También puedes compilar desde terminal:

```bash
git clone https://github.com/SamuelPart/cloneapp.git
cd cloneapp
./gradlew :app:assembleDebug
```

## Requisitos

- Android Studio compatible con Android Gradle Plugin 8.5.2.
- JDK 17 o 21 para ejecutar Gradle; el bytecode de la app usa Java 17.
- Android SDK 34.
- Conexión a Internet para Gradle, TMDb y la lista de TDTChannels.

## Estructura

- `app/src/main/java/com/myiptv/clean/ui/compose`: interfaz, navegación, reproducción, componentes y ViewModels.
- `app/src/main/java/com/myiptv/clean/data/api`: clientes de TMDb y TDTChannels.
- `app/src/main/java/com/myiptv/clean/data/repository`: carga de datos TMDb/TDTChannels y fuentes de reproducción locales.
- `app/src/main/java/com/myiptv/clean/data/model`: modelos de respuesta TMDb.
- `app/src/main/java/com/myiptv/clean/ui/welcome`: bienvenida inicial.
- `app/src/main/res`: recursos y pantallas heredadas.
- `gradle/wrapper`: versión fija de Gradle.
