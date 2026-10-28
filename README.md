# SamPlay

Aplicación Android en Kotlin y Jetpack Compose para móvil y Android TV. Los metadatos de películas y series pueden obtenerse de TMDb; los canales proceden de TDTChannels e IPTV-org. TMDb no proporciona vídeos: las fuentes de películas y series se añaden al catálogo M3U del proyecto.

## Fuentes y comportamiento

- **Películas y series:** TMDb aporta títulos, géneros, fichas, reparto, temporadas, episodios e imágenes cuando hay una API Key configurada. El M3U también puede incluir títulos propios sin ID de TMDb. Sin datos disponibles, la app muestra estados vacíos en vez de contenido ficticio.
- **Vídeos:** el archivo `app/src/main/assets/samplay_catalog.m3u` se entrega vacío. Admite enlaces HTTP/HTTPS directos compatibles con ExoPlayer (incluidos HLS y DASH) y URLs o código de iframe/embed. Los vídeos que se hayan guardado localmente en versiones anteriores siguen sirviendo como respaldo, pero ya no hay botones para añadir o cambiar enlaces en las fichas. Si existen ambas fuentes para el mismo título o episodio, prevalece la del M3U.
- **Canales:** TDTChannels se carga desde `https://www.tdtchannels.com/lists/tv.m3u8`. IPTV-org aporta canales y streams directos de distintos países. Solo se muestran entradas con nombre y URL HTTP/HTTPS directa; no se extraen enlaces de páginas embed ni se eluden bloqueos. Las señales dependen de sus proveedores y no hay programación EPG integrada.
- **Descargas e historial:** todavía no están implementados y muestran estados vacíos.
- **Favoritos:** se mantienen en memoria durante la sesión actual.

Usa únicamente emisiones y enlaces para los que tengas autorización. La app no intenta eludir DRM, geobloqueos, autenticación ni otras restricciones. Para admitir algunas fuentes públicas, Android permite tráfico HTTP sin cifrar; prefiere HTTPS cuando esté disponible.

## Interfaz y reproductor

SamPlay usa el mismo estilo negro con tarjetas panorámicas, carrusel y filas horizontales en móvil y TV. En móvil, los destacados se apilan en vertical; la barra inferior y el menú superior dan acceso a las secciones. En TV se conserva el menú lateral izquierdo y el foco para el mando. «Tendencias esta semana» y «Documentales» usan datos reales de TMDb. Kids y Anime requieren TMDb; Kids no sustituye la supervisión parental.

En vídeos directos, toca el centro del vídeo para pausar o reanudar y mostrar los controles. En TV, el botón central del mando hace lo mismo cuando el vídeo tiene el foco; los botones, la barra de progreso y las teclas de dirección conservan sus funciones. Los controles se ocultan después de unos segundos durante la reproducción y vuelven a aparecer al interactuar.

Un embed se abre en WebView y depende de los controles y la compatibilidad de su proveedor. Como cada iframe puede pertenecer a otro dominio y tener su propio reproductor, SamPlay no puede garantizar que un toque o el mando pausen todos los embeds desde fuera. El botón Atrás permite salir.

Las imágenes de las fichas aparecen suavemente al cargar y mantienen un fondo de reserva si fallan. La tipografía [Inter](https://github.com/rsms/inter/tree/66647c0bbbe41a850d79d9c76fb13add3378940f) se distribuye bajo SIL Open Font License 1.1; la licencia está en `app/src/main/assets/licenses/Inter-OFL.txt`.

## Añadir vídeos al catálogo M3U

1. Abre `app/src/main/assets/samplay_catalog.m3u` y conserva la primera línea `#EXTM3U`.
2. Añade una entrada `#EXTINF` seguida, en la línea siguiente, por el enlace directo o el iframe. Sustituye todos los valores de ejemplo de abajo por datos reales y autorizados.
3. Vuelve a compilar e instalar la aplicación. El archivo se empaqueta dentro del APK; editarlo en el proyecto no actualiza una instalación ya compilada.

Una película sin identificación de TMDb se muestra como un título propio del M3U:

```m3u
#EXTINF:-1 media-type="movie",TÍTULO DE LA PELÍCULA
https://TU_DOMINIO/RUTA/VIDEO.mp4
```

Para relacionar una película con una ficha de TMDb, indica su ID **numérico** y el tipo. Puedes encontrarlo en la URL de la ficha de TMDb, después de `/movie/` para películas o `/tv/` para series. `tvg-logo`, `year`, `genre` y `description` son opcionales; sustituye sus valores por los tuyos o elimínalos:

```m3u
#EXTINF:-1 media-type="movie" tmdb-id="ID_NUMÉRICO_TMDB" year="AÑO" tvg-logo="https://TU_DOMINIO/POSTER.jpg" genre="GÉNERO",TÍTULO DE LA PELÍCULA
https://TU_DOMINIO/RUTA/VIDEO.m3u8
```

Para una serie, proporciona el mismo ID y `series-title` en todos sus episodios, y un número real de `season` y `episode` en cada entrada. Sin `tmdb-id`, los episodios con el mismo `series-title` se agrupan como serie propia del M3U:

```m3u
#EXTINF:-1 media-type="series" tmdb-id="ID_NUMÉRICO_TMDB" series-title="TÍTULO DE LA SERIE" season="1" episode="1",TÍTULO DEL EPISODIO
https://TU_DOMINIO/RUTA/EPISODIO.mp4
```

Para un embed, usa `playback-type="iframe"` con la URL de la página del reproductor:

```m3u
#EXTINF:-1 media-type="movie" playback-type="iframe",TÍTULO DE LA PELÍCULA
https://TU_DOMINIO/RUTA/EMBED
```

También se puede poner una etiqueta `<iframe src="https://TU_DOMINIO/RUTA/EMBED"></iframe>` en la línea del enlace; la app extrae únicamente su `src`. Solo se aceptan URLs HTTP/HTTPS. Una URL de página embed sin etiqueta debe llevar `playback-type="iframe"`; de otro modo se trata como enlace directo.

En vez de `tmdb-id` y `media-type`, puedes usar `tvg-id="tmdb_movie_ID"`, `tvg-id="tmdb_tv_ID"` o `tvg-id="tmdb_tv_ID_sTEMPORADA_eEPISODIO"`, sustituyendo cada marcador por sus números. La app **no empareja títulos por nombre** con TMDb para evitar reproducir una fuente equivocada. En duplicados para el mismo título o episodio se usa la primera fuente válida. Ajustes muestra el estado del catálogo y si se ignoraron entradas por formato incorrecto.

El catálogo queda dentro del APK. No incluyas contraseñas, credenciales ni enlaces privados que no puedas distribuir; tampoco los subas a Git por accidente. No se suministran fuentes de películas ni series con la aplicación.

## Configurar TMDb

La app lee una API Key v3 desde `local.properties` o desde la variable de entorno `TMDB_API_KEY`:

```properties
# Añadir al archivo local.properties de la raíz, sin borrar sdk.dir.
tmdb.api.key=TU_CLAVE_V3_DE_TMDB
```

`local.properties` está excluido por `.gitignore`; no incluyas claves ni tokens en Git. La clave cliente queda incorporada en el APK y no es un secreto seguro para distribuir una app pública; restríngela según las opciones de TMDb o usa un backend propio. Si una clave se ha compartido públicamente, revócala y genera otra.

Este producto utiliza la API de TMDb, pero no está avalado ni certificado por TMDb.

## API local opcional para M3U directo autorizado

`tools/authorized_m3u_api.py` es una herramienta separada del catálogo empaquetado de SamPlay. Carga un M3U/M3U8 local y expone IDs con una redirección 302 a cada stream directo HTTP/HTTPS reconocido; no resuelve embeds ni elude bloqueos.

```bash
python3 tools/authorized_m3u_api.py /ruta/a/catalogo-autorizado.m3u8
```

La API escucha solo en `127.0.0.1:8765` y ofrece `/catalog`, `/health` y `/play?id=<id>`. Ningún método garantiza que una fuente esté disponible en una región o que el proveedor mantenga el enlace.

## Abrir en Android Studio

1. Selecciona **Get from VCS** o **File → New → Project from Version Control** y clona `https://github.com/SamuelPart/cloneapp.git`.
2. Abre la carpeta raíz, donde están `settings.gradle` y `gradlew`.
3. Usa JDK 17 o 21 y permite que Android Studio sincronice Gradle. Se requiere Android SDK 34.
4. Selecciona un dispositivo o emulador con Android 5.0 o posterior y pulsa **Run**.

También puedes ejecutar `./gradlew :app:assembleDebug` desde la raíz. Se necesita conexión a Internet para obtener dependencias y consultar las fuentes de datos.

## Estructura

- `app/src/main/assets/samplay_catalog.m3u`: catálogo de fuentes de películas y series, inicialmente vacío.
- `app/src/main/java/com/myiptv/clean/ui/compose`: interfaz, navegación y ViewModels.
- `app/src/main/java/com/myiptv/clean/ui/player`: interacción táctil y con mando del reproductor directo.
- `app/src/main/java/com/myiptv/clean/data/repository`: catálogos TMDb, canales, M3U y respaldo de fuentes locales.
- `app/src/main/java/com/myiptv/clean/ui/welcome`: bienvenida inicial.
- `app/src/main/res`: recursos de la aplicación.
- `tools/authorized_m3u_api.py`: API local opcional para entradas directas de un M3U autorizado.
