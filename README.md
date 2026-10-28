# Clean IPTV

Aplicación Android en Kotlin con interfaz Compose adaptable a móvil y Android TV. Los metadatos, pósteres, fondos y fichas de películas y series pueden cargarse desde TMDb; los canales IPTV, la guía EPG y la reproducción siguen siendo demostrativos. **No contiene un proveedor IPTV conectado ni streams reales.**

## Estado de la aplicación

- Bienvenida, inicio, canales con detalle y guía EPG, películas y detalle, series con temporadas y episodios.
- La conexión a TMDb puede proporcionar títulos populares, géneros, búsqueda, sinopsis, reparto, temporadas/episodios, pósteres e imágenes de fondo. Si falta la clave o no hay conexión, la interfaz conserva datos de demostración.
- Favoritos, búsqueda, ajustes, reproductor de muestra, descargas e historial.
- Tema oscuro Material 3, componentes reutilizables y ViewModels.
- En TV/tablets anchas: navegación lateral y controles con foco visible; en móvil: navegación inferior y controles táctiles.
- Los canales, la programación EPG, las descargas y el historial son demostrativos. TMDb solo entrega metadatos e imágenes: no proporciona streams. Para reproducir contenido real se necesita configurar una fuente IPTV con sus URLs HLS/DASH; la EPG y las descargas aún no están conectadas.

No incluyas contraseñas, tokens ni claves en el repositorio.

## Configurar TMDb

La app lee la clave V3 de TMDb desde `local.properties` (recomendado para desarrollo local) o desde la variable de entorno `TMDB_API_KEY`:

```properties
# En el archivo local.properties de la raíz del proyecto; no lo subas a Git.
tmdb.api.key=TU_CLAVE_V3_DE_TMDB
```

Si Android Studio ya creó `local.properties`, añade la línea sin borrar `sdk.dir`. Después de sincronizar Gradle, `BuildConfig.TMDB_API_KEY` se utiliza para llamar a la API de TMDb. El archivo `local.properties` está excluido por `.gitignore`.

La clave cliente se incorpora al APK y puede extraerse de él; no es un secreto seguro para distribuir una app pública. Para producción, limita/restringe la clave según las opciones de TMDb o usa un backend propio. Como la clave compartida anteriormente quedó expuesta en la conversación, revócala y genera otra antes de usarla.

La aplicación utiliza la API de TMDb, pero no está avalada ni certificada por TMDb.

## Abrir en Android Studio

1. Selecciona **Get from VCS** (o **File → New → Project from Version Control**).
2. Selecciona **Git** y pega `https://github.com/SamuelPart/cloneapp.git`.
3. Clona el repositorio y abre la carpeta raíz, donde están `settings.gradle` y `gradlew`.
4. Usa JDK 17 o 21 para Gradle y permite que Android Studio sincronice el proyecto.
5. Selecciona un emulador o dispositivo con Android 5.0 o posterior y pulsa **Run**.

También puedes clonar y compilar desde terminal:

```bash
git clone https://github.com/SamuelPart/cloneapp.git
cd cloneapp
./gradlew :app:assembleDebug
```

## Requisitos

- Android Studio compatible con Android Gradle Plugin 8.5.2.
- JDK 17 o 21 para ejecutar Gradle; no uses JDK 25 con Gradle 8.7. El bytecode de la app sigue configurado para Java 17.
- Android SDK 34 instalado desde SDK Manager.
- Conexión a Internet la primera vez para descargar Gradle y dependencias.

En Kali Linux, si tienes instalado OpenJDK 21, puedes probarlo en la terminal así:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

Selecciona esa misma carpeta como **Gradle JDK** en Android Studio. Gradle 8.7 no es compatible con Java 25.

## Estructura

- `app/src/main/java/com/myiptv/clean/ui/compose`: interfaz Compose, navegación, componentes, ViewModels y datos de muestra.
- `app/src/main/java/com/myiptv/clean/ui/welcome`: bienvenida inicial.
- `app/src/main/java/com/myiptv/clean/data`: cliente, modelos y repositorio de TMDb; contratos IPTV de ejemplo.
- `app/src/main/res`: recursos y pantallas del flujo de navegación anterior.
- `app/src/main/AndroidManifest.xml`: actividades, permisos y compatibilidad Android TV.
- `gradle/wrapper`: versión fija de Gradle para sincronizar de forma reproducible.
