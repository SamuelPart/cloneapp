# SamPlay

Aplicación Android en Kotlin y Jetpack Compose para móvil y Android TV. Los metadatos de películas y series pueden obtenerse de TMDb; los canales proceden de TDTChannels e IPTV-org. TMDb no proporciona vídeos: las fuentes de películas y series se añaden al catálogo M3U del proyecto.

## Fuentes y comportamiento

- **Películas y series:** TMDb aporta títulos, géneros, fichas, reparto, temporadas, episodios e imágenes cuando hay una API Key configurada. El M3U también puede incluir títulos propios sin ID de TMDb. Sin datos disponibles, la app muestra estados vacíos en vez de contenido ficticio.
- **Vídeos:** `app/src/main/assets/samplay_catalog.m3u` contiene los enlaces proporcionados para *Marsupilami (2026)* y 12 capítulos de *La Brea* (diez de la temporada 1 y dos de la temporada 2). Admite enlaces HTTP/HTTPS directos que pueda reproducir LibVLC (incluidos HLS y DASH) y URLs o código de iframe/embed. Los vídeos guardados localmente en versiones anteriores siguen sirviendo como respaldo; no hay botones para añadir o cambiar enlaces en las fichas. Si existen ambas fuentes para el mismo título o episodio, prevalece la del M3U. No se ha verificado la disponibilidad de estos enlaces.
- **Canales:** TDTChannels se carga desde `https://www.tdtchannels.com/lists/tv.m3u8`. IPTV-org aporta canales y streams directos de distintos países. Solo se muestran entradas con nombre y URL HTTP/HTTPS directa; no se extraen enlaces de páginas embed ni se eluden bloqueos. Las señales dependen de sus proveedores y no hay programación EPG integrada.
- **Descargas e historial:** todavía no están implementados y muestran estados vacíos.
- **Favoritos:** se mantienen en memoria durante la sesión actual.

Usa únicamente emisiones y enlaces para los que tengas autorización. La app no intenta eludir DRM, geobloqueos, autenticación ni otras restricciones. Para admitir algunas fuentes públicas, Android permite tráfico HTTP sin cifrar; prefiere HTTPS cuando esté disponible.

## Interfaz y reproductor

SamPlay usa el mismo estilo negro con tarjetas panorámicas, carrusel y filas horizontales en móvil y TV. En móvil, los destacados se apilan en vertical; la barra inferior y el menú superior dan acceso a las secciones. En TV se conserva el menú lateral izquierdo y el foco para el mando. «Tendencias esta semana» y «Documentales» usan datos reales de TMDb. Kids y Anime requieren TMDb; Kids no sustituye la supervisión parental.

Los canales y vídeos directos utilizan **LibVLC 3.6.4**, integrado en SamPlay; no hace falta instalar la aplicación VLC aparte. Se conservan los campos `userAgent` y `referrer` de los canales cuando el proveedor los facilita. El motor ofrece mayor compatibilidad con formatos de audio, pero el sonido sigue dependiendo de la señal y del dispositivo: no puede garantizarse en todos los canales. Si una señal ofrece varias pistas, usa **Audio** para seleccionar una; si no hay ninguna disponible, se indicará en pantalla.

En móvil, los controles de SamPlay se ocultan tras unos segundos sin interacción, incluso si el vídeo está pausado; el primer toque los muestra y, con ellos visibles, tocar el centro pausa o reanuda. En páginas con controles del proveedor, el toque también llega a la página y sus propios controles pueden comportarse de otra manera. El botón **Pantalla completa** gira el reproductor en horizontal y oculta las barras de estado/notificaciones y navegación; al salir o pulsar Atrás se restauran la orientación solicitada y las barras anteriores. La orientación final también depende de la rotación y el bloqueo del dispositivo. Android puede mostrar las barras temporalmente al deslizar desde el borde. Un reproductor web que solicite su propia pantalla completa utiliza el mismo modo inmersivo en móvil.

En TV, el botón central del mando pausa o reanuda cuando el vídeo tiene el foco; con las flechas puedes revelar los controles y desplazarte entre ellos. La barra y los saltos de 10 segundos aparecen solo cuando VLC permite buscar en la fuente; en emisiones que no admiten pausa, la app lo indica. Al salir de la app se detiene el audio; al regresar se intenta recuperar la posición en los vídeos que permiten búsqueda y el estado de pausa.

Las páginas web y embeds se abren en WebView. Un **bot local basado en reglas** (no un servicio de IA externo) intenta detectar el vídeo, pulsar Play o iniciarlo automáticamente y cerrar u ocultar capas de anuncios reconocibles. Bloquea ventanas emergentes, recursos de varias redes publicitarias conocidas y navegaciones principales fuera del proveedor; los iframes de vídeo en otros dominios siguen permitidos salvo que coincidan con la lista de anuncios. El bloqueo es limitado y puede afectar a algún proveedor que necesite una redirección externa. Si una redirección bloqueada impide cargar la página, la app muestra el dominio y permite autorizarlo explícitamente solo durante esa reproducción; nunca autoriza redes publicitarias conocidas ni rebajas de HTTPS a HTTP. No se incluyen credenciales ni se instala una interfaz JavaScript con acceso nativo.

Cuando el bot puede acceder al elemento de vídeo HTML de la página o de un iframe del mismo origen, muestra los controles de SamPlay para reproducir, pausar y buscar. El botón **Controles web** vuelve a los originales del proveedor. Si el proveedor rechaza el inicio automático por exigir un toque, la app muestra sus controles para que el usuario lo inicie. Si el vídeo está en un iframe de otro origen, protegido o sin una API accesible, se mantienen los controles del proveedor: el bot no puede garantizar inicio automático, pausa, avance ni eliminación completa de anuncios en esos casos. Las páginas externas, sus enlaces y audio no se han probado en un dispositivo. Al pasar al fondo se intenta pausar el vídeo accesible; un iframe que el bot no puede controlar se descarga y vuelve a cargar al regresar, por lo que puede perder su posición. VLC y su selector de audio se usan solo con fuentes directas.

LibVLC añade bibliotecas nativas para varias arquitecturas, por lo que puede aumentar el tamaño del APK. Consulta el [proyecto y la licencia de LibVLC](https://github.com/videolan/vlc-android) antes de distribuir la aplicación.

Las imágenes de las fichas aparecen suavemente al cargar y mantienen un fondo de reserva si fallan. La tipografía [Inter](https://github.com/rsms/inter/tree/66647c0bbbe41a850d79d9c76fb13add3378940f) se distribuye bajo SIL Open Font License 1.1; la licencia está en `app/src/main/assets/licenses/Inter-OFL.txt`.

## Añadir vídeos al catálogo M3U

1. Abre `app/src/main/assets/samplay_catalog.m3u` y conserva la primera línea `#EXTM3U`.
2. Añade una entrada `#EXTINF` seguida, en la línea siguiente, por el enlace directo o el iframe. Sustituye todos los valores de ejemplo de abajo por datos reales y autorizados.
3. Vuelve a compilar e instalar la aplicación. El archivo se empaqueta dentro del APK; editarlo en el proyecto no actualiza una instalación ya compilada.

Puedes usar el formato sencillo incluido en el catálogo. Una película y un capítulo se escriben así, con la URL en texto plano en la siguiente línea:

```m3u
#EXTINF:-1 tvg-logo="" group-title="Peliculas",Marsupilami (2026)
https://nupload.my/watch/60SsUrowOEw2cDkHmN3M0iIojHFkXJ8oFYM3jz37kz7IctGTs

#EXTINF:-1 tvg-logo="" group-title="La Brea Temporada 1",Capítulo 1
https://nupload.my/watch/o3xAJQ7kz7MBPKokvwmPuBO9MmRijQgxxIUPl0zF0WoUmE
```

En series, repite el mismo nombre antes de `Temporada N` y numera cada `Capítulo N`: la app agrupa las temporadas bajo una sola serie. También acepta `Episodio N`, espacios en blanco y enlaces de Markdown. Un `tvg-logo` vacío no genera una imagen ficticia. Con una clave TMDb válida, la app busca los títulos M3U en segundo plano y agrega fichas e imágenes sin cambiar sus enlaces. Si hay una identificación válida, también consulta los episodios de cada temporada; los que no tienen enlace se muestran sin botón de reproducción.

El formato anterior más explícito también sigue funcionando, por ejemplo:

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

También se puede poner una etiqueta `<iframe src="https://TU_DOMINIO/RUTA/EMBED"></iframe>` en la línea del enlace; la app extrae únicamente su `src`. Solo se aceptan URLs HTTP/HTTPS. Las rutas evidentes `/watch/`, `/embed/`, `/iframe/` y las páginas `.html` se reconocen como web; las extensiones de vídeo conocidas, como `.mp4` o `.m3u8`, se reproducen con VLC. Los enlaces ambiguos se consideran directos para conservar el comportamiento previo. Usa `playback-type="iframe"` o `playback-type="direct"` si necesitas indicar el tipo explícitamente.

En vez de `tmdb-id` y `media-type`, puedes usar `tvg-id="tmdb_movie_ID"`, `tvg-id="tmdb_tv_ID"` o `tvg-id="tmdb_tv_ID_sTEMPORADA_eEPISODIO"`, sustituyendo cada marcador por sus números.

Para entradas sencillas **sin ID**, la búsqueda automática compara el tipo y el título completo o el título original, sin distinguir mayúsculas, acentos ni puntuación. Si se indica un año, también debe coincidir; por ejemplo, `Nombre (2026)`. Solo se usa una coincidencia única y se consultan como máximo cinco páginas: si hay ambigüedad o demasiados resultados, se mantiene la entrada local sin inventar datos. Para resolver esos casos, añade el ID real. El atributo opcional `tmdb-match-id="ID_NUMÉRICO_TMDB"` fija solo los metadatos sin cambiar el ID M3U existente; en una serie agrupada basta ponerlo en un episodio, y los IDs contradictorios se rechazan. El catálogo incluido ya identifica así [*Marsupilami*](https://www.themoviedb.org/movie/1145899-marsupilami) (`1145899`, película) y [*La Brea*](https://www.themoviedb.org/tv/128839-la-brea) (`128839`, serie). El formato sencillo sigue siendo válido para nuevos títulos, sin necesidad de añadir el atributo cuando la coincidencia es inequívoca.

En duplicados para el mismo título o episodio se usa la primera fuente válida. Ajustes muestra el estado del catálogo y si se ignoraron entradas por formato incorrecto.

El catálogo queda dentro del APK. No incluyas contraseñas, credenciales ni enlaces privados que no puedas distribuir; tampoco los subas a Git por accidente. Los enlaces presentes fueron facilitados por el propietario de este proyecto y su funcionamiento, disponibilidad y autorización deben comprobarse antes de distribuir la app.

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

También puedes ejecutar `./gradlew :app:assembleDebug` desde la raíz. Se necesita conexión a Internet para obtener dependencias y consultar las fuentes de datos. Para las pruebas opcionales del bot web, instala Node.js 18 o posterior y ejecuta `npm ci` seguido de `npm run test:web-bot`; Node no es necesario para compilar Android.

## Estructura

- `app/src/main/assets/samplay_catalog.m3u`: catálogo de fuentes de películas y series proporcionadas.
- `app/src/main/assets/samplay_embed_bot.js`: bot local de reproducción y cierre de capas de anuncios reconocibles.
- `app/src/main/java/com/myiptv/clean/ui/compose`: interfaz, navegación y ViewModels.
- `app/src/main/java/com/myiptv/clean/ui/player`: interacción táctil y con mando del reproductor directo.
- `app/src/main/java/com/myiptv/clean/data/repository`: catálogos TMDb, canales, M3U y respaldo de fuentes locales.
- `app/src/main/java/com/myiptv/clean/ui/welcome`: bienvenida inicial.
- `app/src/main/res`: recursos de la aplicación.
- `tools/authorized_m3u_api.py`: API local opcional para entradas directas de un M3U autorizado.
