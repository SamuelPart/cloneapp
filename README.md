# Clean IPTV - Versión Limpia y Segura

Aplicación de streaming IPTV basada en la arquitectura de la app original pero sin malware, telemetría ni VPN malicioso.

## 🎯 Características

- ✅ Sistema de configuración dinámica (similar al original)
- ✅ Streaming HLS/DASH con ExoPlayer
- ✅ Canales en vivo
- ✅ Catálogo de películas
- ✅ Sistema de favoritos
- ✅ Búsqueda de contenido
- ✅ Guía de programación (EPG)
- ✅ Caching inteligente
- ✅ UI/UX similar (Material Design)

## ❌ Lo que NO tiene (malicioso de la original)

- ❌ Telemetría y recolección de datos
- ❌ VPN malicioso
- ❌ Permisos peligrosos (REQUEST_INSTALL, GET_TASKS)
- ❌ Ofuscación de código
- ❌ Actividad oculta
- ❌ Dominios sospechosos

## 🏗️ Arquitectura

Similar a la app original pero limpio:
- MVVM con ViewBinding
- Configuración dinámica desde JSON
- Sistema de streaming robusto
- Navegación con Navigation Component
- Base de datos local con Room

## 📦 Instalación

1. Clona el repositorio
2. Abre en Android Studio
3. Sincroniza Gradle
4. Ejecuta en dispositivo/emulador

## ⚙️ Configuración

Edita el archivo de configuración o usa el servidor por defecto.

## 📝 Licencia

MIT License - Código abierto y personalizable

## 🚀 Próximos pasos

1. Configura tu servidor de contenido
2. Personaliza colores y tema
3. Agrega tus propias fuentes de contenido (TMDB, YouTube, etc.)
4. Prueba y despliega