# Clean IPTV

Aplicación Android de ejemplo en Kotlin para explorar una interfaz IPTV. Incluye una experiencia Compose adaptable a móvil y Android TV, con navegación y contenido ficticio para previsualizar las pantallas. **No contiene un proveedor IPTV conectado ni streams reales.**

## Estado de la aplicación

- Bienvenida, inicio, canales con detalle y guía EPG, películas y detalle, series con temporadas y episodios.
- Favoritos, búsqueda, ajustes, reproductor de muestra, descargas e historial.
- Tema oscuro Material 3, componentes reutilizables y ViewModels.
- En TV/tablets anchas: navegación lateral y controles con foco visible; en móvil: navegación inferior y controles táctiles.
- Datos de catálogo, programación, descargas e historial son demostrativos. Para reproducir contenido real se necesita configurar una fuente y sus URLs HLS/DASH; la EPG y las descargas aún no están conectadas.
- Se conservan los contratos Retrofit y la configuración de endpoint como base para una futura integración. Las URL iniciales usan el dominio reservado `example.invalid`.

No incluyas contraseñas, tokens ni claves en el repositorio.

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
- `app/src/main/java/com/myiptv/clean/data`: modelos y contratos de API de ejemplo.
- `app/src/main/res`: recursos y pantallas del flujo de navegación anterior.
- `app/src/main/AndroidManifest.xml`: actividades, permisos y compatibilidad Android TV.
- `gradle/wrapper`: versión fija de Gradle para sincronizar de forma reproducible.
