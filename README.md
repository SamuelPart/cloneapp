# Clean IPTV

Aplicación Android en Kotlin y Jetpack Compose para móvil y Android TV. El catálogo de películas y series usa metadatos de TMDb; los canales se combinan desde TDTChannels y IPTV-org (emisiones de Perú). TMDb no proporciona vídeo: cada película o serie necesita que el usuario configure una fuente de reproducción directa o un embed iframe autorizado.

## Fuentes y comportamiento

- **Películas y series:** TMDb aporta títulos, géneros, búsqueda, fichas, reparto, temporadas, episodios, pósteres e imágenes. Sin una API Key válida, la app muestra estados vacíos/configuración; no sustituye resultados por contenido de muestra.
- **Canales:** TDTChannels se carga desde `https://www.tdtchannels.com/lists/tv.m3u8`. IPTV-org aporta metadatos y enlaces directos desde `https://iptv-org.github.io/api/channels.json` y `streams.json`; se limita a Perú y se omiten canales NSFW y streams marcados `Geo-blocked`. Se usan únicamente URLs HTTP/HTTPS directas del catálogo; no se extraen enlaces de páginas embed ni se eluden bloqueos. La programación EPG no está integrada.
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

## API local para M3U directo autorizado

`tools/authorized_m3u_api.py` carga un archivo EXTINF M3U/M3U8 local y expone un catálogo de IDs con una redirección 302 a la URL HTTP/HTTPS exacta de cada stream directo reconocido por extensión (HLS, DASH, audio o vídeo). Ignora enlaces a páginas web y otros formatos; no abre páginas, resuelve iframes, extrae tokens/cookies, evade anti-bot ni retransmite el flujo.

```bash
python3 tools/authorized_m3u_api.py /ruta/a/catalogo-autorizado.m3u8
```

La API escucha solo en `127.0.0.1:8765`: `GET /catalog`, `GET /health` y `GET /play?id=<id>`. El cliente debe seguir la redirección. No acepta URLs arbitrarias en la petición ni almacena tokens. Un 302 no puede inyectar `Referer`/`User-Agent`; en la app Android se usan únicamente los valores explícitos publicados por IPTV-org para sus streams directos. Ningún método garantiza cero fallos: la disponibilidad, el formato, la región y las condiciones dependen del proveedor.

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
- Conexión a Internet para Gradle, TMDb, TDTChannels e IPTV-org.
- Python 3.10 o posterior, solo si se utiliza la API local opcional para M3U.

## Estructura

- `app/src/main/java/com/myiptv/clean/ui/compose`: interfaz, navegación, reproducción, componentes y ViewModels.
- `app/src/main/java/com/myiptv/clean/data/api`: clientes de TMDb, TDTChannels e IPTV-org.
- `app/src/main/java/com/myiptv/clean/data/repository`: catálogos TMDb/TDTChannels/IPTV-org y fuentes de reproducción locales.
- `app/src/main/java/com/myiptv/clean/data/model`: modelos de respuesta TMDb e IPTV-org.
- `tools/authorized_m3u_api.py`: API local de redirección para entradas directas de un M3U autorizado.
- `app/src/main/java/com/myiptv/clean/ui/welcome`: bienvenida inicial.
- `app/src/main/res`: recursos y pantallas heredadas.
- `gradle/wrapper`: versión fija de Gradle.
