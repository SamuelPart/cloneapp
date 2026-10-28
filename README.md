# Clean IPTV

Aplicación Android de ejemplo en Kotlin para explorar una estructura de streaming IPTV. Este repositorio contiene una base de navegación y pantallas; **no es el APK original ni una aplicación de streaming terminada**.

## Estado actual

- Splash y navegación inferior entre Inicio, Canales, Películas, Favoritos y Ajustes.
- Modelos de datos y contratos Retrofit para canales, películas y EPG.
- Configuración local del endpoint con `ConfigManager`.
- Dependencias de ExoPlayer incluidas para una futura integración HLS/DASH.
- Las pantallas todavía muestran estados vacíos: no hay adaptadores, repositorios, autenticación, EPG ni reproducción conectados.

Las URL iniciales usan el dominio reservado `example.invalid`; reemplázalas por un servidor propio que implemente los endpoints definidos en `app/src/main/java/com/myiptv/clean/data/api/ApiService.kt`. No incluyas contraseñas, tokens ni claves en el repositorio.

## Abrir en Android Studio

1. En la pantalla inicial, selecciona **Get from VCS** (o **File → New → Project from Version Control**).
2. Selecciona **Git** y pega `https://github.com/SamuelPart/cloneapp.git`.
3. Clona el repositorio y abre la carpeta raíz, donde están `settings.gradle` y `gradlew`.
4. Usa JDK 17 para Gradle y permite que Android Studio descargue Gradle y sincronice el proyecto.
5. Selecciona un emulador/dispositivo con Android 5.0 o posterior y pulsa **Run**.

También puedes clonar desde terminal:

```bash
git clone https://github.com/SamuelPart/cloneapp.git
cd cloneapp
./gradlew :app:assembleDebug
```

## Requisitos

- Android Studio compatible con Android Gradle Plugin 8.2.2.
- JDK 17.
- Android SDK 34 instalado desde SDK Manager.
- Conexión a Internet la primera vez para descargar Gradle y dependencias.

## Estructura

- `app/src/main/java`: código Kotlin organizado por paquetes.
- `app/src/main/res`: layouts, navegación, menú y valores.
- `app/src/main/AndroidManifest.xml`: permisos y actividades.
- `gradle/wrapper`: versión fija de Gradle para sincronizar de forma reproducible.
