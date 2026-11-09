# SamPlay: anuncios, monedas y comprobaciones antes de publicar

## Configuración

- Se conservan Android 5.0 / API 21 y los sabores `play` y `github`. La versión de la aplicación es **9 (1.2.6)**.
- Google Mobile Ads **23.6.0** y UMP **3.2.0** se eligieron por compatibilidad con API 21. Google declaró obsoleta la rama 23.x y anunció su retirada para el **30 de junio de 2027**. Hay que planificar su sustitución sin subir el mínimo de Android sin una decisión explícita.
- Por decisión del propietario, **Debug y Release usan los mismos IDs reales de SamPlay**. Antes de probar, registrar el teléfono como dispositivo de prueba en **AdMob → Configuración → Dispositivos de prueba → Añadir dispositivo de prueba**, usando el ID de publicidad del teléfono. También se puede evaluar el modo de prueba para una versión de la app si está disponible en la cuenta. Esperar a que la configuración se propague (puede tardar hasta 24 horas) y comprobar la etiqueta «Test Ad» / «Test Mode» antes de interactuar. **No pulsar anuncios reales propios.** Si se usa mediación, cada red externa necesita su propia configuración de prueba. Emuladores Android se reconocen como dispositivos de prueba de Google, pero no sustituyen un teléfono físico.
- La app no solicita anuncios desde su código en Android TV. La comprobación de edad móvil es neutral y opcional; si se omite o indica menos de 18 años, no se inician UMP ni solicitudes de anuncios desde la aplicación. Solo se guarda la categoría, no la fecha de nacimiento. Esta categoría corresponde a la instalación y no distingue entre las personas que comparten el dispositivo.
- Kids no tiene ubicaciones AdMob y se conserva el contexto Kids al abrir sus fichas y recomendaciones; en reproducción también se excluyen los géneros identificados como infantiles o familiares. Los metadatos pueden ser incompletos: esto **no garantiza** clasificar todo el contenido dirigido a niños. Revisar el público objetivo real, los dispositivos compartidos y todas las políticas de Familias antes de publicar.
- UMP actualiza el consentimiento en lanzamientos aptos y muestra el formulario solo en navegación segura, no durante bienvenida, Kids o reproducción. En Ajustes se ofrecen opciones de privacidad cuando corresponda. Si no puede solicitar anuncios, la app sigue funcionando.
- Inicio: anuncio de apertura solo en bienvenida, después de uso previo, al terminar la animación y solo si ya está cargado. Tocar el león o pulsar OK entra sin esperar. Límite local de una muestra por hora. Un pase de Inicio o de toda la app lo desactiva.
- Banner: parte superior de películas, series, búsqueda y favoritos, con espacio y separación de controles. No en reproductor, canales en directo, Kids ni fichas. Nativo: bloque identificado en el inicio móvil con AdChoices. Cada ubicación respeta el pase de su sección o el pase general.
- Antes de reproducir películas, episodios y canales aptos desde sus fichas en móvil, si hay un anuncio recompensado previamente cargado, se ofrece de forma explícita y voluntaria. El usuario puede reproducir gratis sin verlo. Si no hay anuncio, falla o caducó, la reproducción continúa sin esperar. La vista previa y el cambio de canales desde la lista empiezan directamente, sin invitación ni anuncios que interrumpan la señal. Los dos formatos recompensados se descartan después de **una hora**; app-open mantiene su límite de cuatro horas.

## Monedas y canjes

Las monedas son virtuales, locales a la instalación, no transferibles, sin valor monetario y no se sincronizan. Se pierden al desinstalar o borrar los datos; `allowBackup` está desactivado. Las fechas dependen del reloj y la zona horaria del dispositivo; no son un sistema antifraude ni una cuenta de servidor.

- **+10 monedas** únicamente por cada callback `onUserEarnedReward` legítimo de un anuncio recompensado completado. Cada impresión solo se acredita una vez. El saldo usa validación en el dispositivo, no verificación de servidor (SSV).
- **+2** al abrir o volver a primer plano una vez por día local.
- **+10 adicionales** cada cinco días consecutivos de apertura (días 5, 10, etc.). Un día perdido reinicia la racha.
- **+5** por acumular 20 minutos de reproducción en primer plano, una vez al día. VLC cuenta cuando informa reproducción sin pausa ni buffering. En embeds, solo se cuenta vídeo accesible al bot y cuyo tiempo avanza; los iframe de otro origen o proveedores que no expongan su estado pueden no contabilizarse. No se cuenta por tener la pantalla abierta ni por anuncios SamPlay.
- **+3** por agregar un favorito realmente nuevo, una vez al día. Los favoritos ya existentes o que se quiten y vuelvan a añadir no se consideran nuevos; sus identificadores y alias se recuerdan localmente.
- **10 monedas**: omitir la invitación en la reproducción actual de una película/episodio o durante una hora para el canal seleccionado. La invitación también se puede rechazar gratis; no se cobra si ya no hay un anuncio o existe un pase aplicable.
- **40 monedas**: 24 horas para una sección seleccionada (Inicio, Películas, Series, Canales, Búsqueda o Mi lista). **100 monedas**: 24 horas en toda la app. Los pases no se vuelven a cobrar mientras sigan activos; se pide confirmación.
- Los canjes solo afectan a los anuncios integrados por SamPlay. **No eliminan publicidad que venga insertada en las señales o los sitios externos.** La recompensa que pudiera seguir activa de la versión anterior se respeta hasta su vencimiento.

Configurar en AdMob una recompensa coherente de 10 monedas en ambas unidades. Las recompensas no están garantizadas si AdMob no sirve un anuncio ni se ofrece compensación por rechazarlo. Revisar las políticas de recompensas de Google y comprobar todos los formatos en dispositivos reales.

La app incluye `DELAY_APP_MEASUREMENT_INIT` como precaución. Hay que comprobar el manifiesto fusionado y el tráfico previo al consentimiento de esta versión del SDK; estas medidas no demuestran por sí solas cumplimiento de Familias ni ausencia de inicialización automática.

## Antes de publicar

1. Declarar en Play Console que la app contiene anuncios y revisar público objetivo, clasificación y políticas de Familias.
2. Crear/publicar mensajes de consentimiento en **AdMob → Privacidad y mensajes** y probar las regiones pertinentes y las opciones de privacidad.
3. Revisar política de privacidad publicada y Seguridad de los datos de Play para reflejar SDK, identificadores, comunicaciones y monedas locales. El borrador histórico del repositorio no se ha modificado y no debe asumirse actualizado.
4. Si corresponde, indicar `https://mandratv.blogspot.com` como web del desarrollador en Play. `/app-ads.txt` responde con el registro de Google mediante redirección a `/ads.txt`: `google.com, pub-8124327134735952, DIRECT, f08c47fec0942fa0`. Comprobar el estado real de verificación en AdMob después de publicar y vincular la ficha.
5. Compilar y firmar `:app:bundlePlayRelease` con JDK 17 y la clave de subida existente. Para GitHub se mantiene su variante APK. No compartir claves ni contraseñas.
6. En teléfonos **registrados para pruebas**, verificar las cinco fuentes de monedas, límites diarios, racha, pausas y segundo plano, cambios de día, los tres canjes, su vencimiento, formatos recompensados completos/omitidos/no disponibles, consentimiento y navegación a películas, episodios y canales. Probar también menores/edad desconocida, Kids y el mando y reproducción de una TV real. Revisar manifiesto fusionado y dependencias de ambos sabores.

Referencias: [dispositivos de prueba](https://support.google.com/admob/answer/9691433), [anuncios de prueba](https://developers.google.com/admob/android/test-ads), [recompensados](https://developers.google.com/admob/android/rewarded) y [políticas de recompensas](https://support.google.com/admob/answer/7313578). No se puede garantizar que AdMob llene bloques, verifique el archivo o apruebe la aplicación.
