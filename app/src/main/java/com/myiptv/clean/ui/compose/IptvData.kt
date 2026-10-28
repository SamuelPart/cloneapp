package com.myiptv.clean.ui.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.ui.graphics.vector.ImageVector

enum class MediaKind { MOVIE, SERIES }

data class MediaItem(
    val id: String,
    val title: String,
    val kind: MediaKind,
    val year: Int,
    val rating: Double,
    val genres: List<String>,
    val synopsis: String,
    val duration: String,
    val quality: String,
    val colorA: Long,
    val colorB: Long,
    val streamUrl: String? = null
)

data class ProgramItem(
    val start: String,
    val end: String,
    val title: String,
    val description: String
)

data class LiveChannel(
    val id: String,
    val name: String,
    val category: String,
    val logoLetters: String,
    val currentProgram: String,
    val nextProgram: String,
    val startTime: String,
    val endTime: String,
    val accent: Long,
    val schedule: List<ProgramItem>
)

data class ContentCategory(
    val title: String,
    val icon: ImageVector,
    val accent: Long
)

object DemoCatalog {
    val categories = listOf(
        ContentCategory("Películas", Icons.Filled.Movie, 0xFF8E5CF7L),
        ContentCategory("Series", Icons.Filled.Theaters, 0xFF34B6A5L),
        ContentCategory("En vivo", Icons.Filled.LiveTv, 0xFFE45773L),
        ContentCategory("Deportes", Icons.Filled.SportsSoccer, 0xFF3D9BE9L),
        ContentCategory("Música", Icons.Filled.MusicNote, 0xFFDE9A43L),
        ContentCategory("Niños", Icons.Filled.ChildCare, 0xFFEF6BB5L)
    )

    val media = listOf(
        MediaItem("m-orbita", "Órbita 9", MediaKind.MOVIE, 2025, 8.4, listOf("Ciencia ficción", "Aventura"), "Una misión de rescate descubre una señal que podría cambiar el destino de la humanidad.", "1 h 52 min", "4K", 0xFF32116FL, 0xFF121B43L),
        MediaItem("m-horizonte", "Último horizonte", MediaKind.MOVIE, 2024, 7.9, listOf("Drama", "Aventura"), "Dos desconocidos recorren una costa remota mientras intentan empezar de nuevo.", "1 h 46 min", "HD", 0xFFB95139L, 0xFF321D45L),
        MediaItem("s-ciudad", "Ciudad de neón", MediaKind.SERIES, 2025, 9.1, listOf("Suspenso", "Drama"), "En una ciudad que nunca duerme, cada secreto deja una huella luminosa.", "2 temporadas", "4K", 0xFF3B1D72L, 0xFF111A37L),
        MediaItem("m-mar", "El color del mar", MediaKind.MOVIE, 2023, 8.0, listOf("Documental", "Naturaleza"), "Una travesía visual por ecosistemas marinos y las personas que los protegen.", "1 h 28 min", "4K", 0xFF007B83L, 0xFF062C52L),
        MediaItem("m-noche", "La noche violeta", MediaKind.MOVIE, 2024, 7.6, listOf("Misterio", "Drama"), "Una fotógrafa encuentra pistas ocultas en imágenes tomadas durante una noche de lluvia.", "1 h 39 min", "HD", 0xFF6638A1L, 0xFF1A1638L),
        MediaItem("s-camino", "El camino al norte", MediaKind.SERIES, 2024, 8.7, listOf("Aventura", "Familia"), "Una familia convierte un viaje inesperado en una nueva forma de estar juntos.", "3 temporadas", "HD", 0xFF42865EL, 0xFF172C42L),
        MediaItem("m-latido", "Latido profundo", MediaKind.MOVIE, 2022, 7.8, listOf("Música", "Drama"), "Una joven compositora busca su propio ritmo entre dos generaciones.", "1 h 34 min", "HD", 0xFFAC426B, 0xFF3B1B47L),
        MediaItem("m-azul", "Planeta azul", MediaKind.MOVIE, 2025, 9.0, listOf("Documental", "Naturaleza"), "Un retrato cercano de la vida que prospera bajo la superficie del océano.", "1 h 41 min", "4K", 0xFF087CA7L, 0xFF082C58L),
        MediaItem("s-archivo", "El archivo secreto", MediaKind.SERIES, 2023, 8.2, listOf("Misterio", "Suspenso"), "Un equipo de archivistas reconstruye historias que alguien intentó borrar.", "1 temporada", "HD", 0xFF8A633CL, 0xFF2D203BL),
        MediaItem("m-estrellas", "Bajo las estrellas", MediaKind.MOVIE, 2021, 7.7, listOf("Romance", "Drama"), "Una conversación casual da paso a una historia que atraviesa varios veranos.", "1 h 42 min", "HD", 0xFFCE7757L, 0xFF55264AL)
    )

    val channels = listOf(
        LiveChannel("ch-noticias", "Noticias 24", "Noticias", "24", "Resumen del día", "Punto de encuentro", "20:00", "21:00", 0xFFEB5169L, listOf(
            ProgramItem("20:00", "21:00", "Resumen del día", "Las noticias más importantes, con contexto y análisis."),
            ProgramItem("21:00", "22:00", "Punto de encuentro", "Conversaciones con invitados de actualidad."),
            ProgramItem("22:00", "23:00", "Panorama", "Una mirada amplia a los temas de la semana.")
        )),
        LiveChannel("ch-deportes", "Mundo Deportes", "Deportes", "MD", "La previa", "En juego", "20:30", "21:30", 0xFF318ED1L, listOf(
            ProgramItem("20:30", "21:30", "La previa", "Análisis y preparación de los encuentros de hoy."),
            ProgramItem("21:30", "23:30", "En juego", "Cobertura deportiva en directo."),
            ProgramItem("23:30", "00:00", "El resumen", "Lo mejor de la jornada.")
        )),
        LiveChannel("ch-cultura", "Cultura Viva", "Cultura", "CV", "Ideas que inspiran", "Escena abierta", "20:00", "21:00", 0xFF8E63D7L, listOf(
            ProgramItem("20:00", "21:00", "Ideas que inspiran", "Historias de ciencia, arte e innovación."),
            ProgramItem("21:00", "22:00", "Escena abierta", "Música y conversaciones desde el estudio."),
            ProgramItem("22:00", "23:00", "Rutas", "Un recorrido por paisajes y tradiciones.")
        )),
        LiveChannel("ch-musica", "Ritmo TV", "Música", "RT", "Sesión acústica", "Nuevos sonidos", "20:15", "21:15", 0xFFD08D3FL, listOf(
            ProgramItem("20:15", "21:15", "Sesión acústica", "Interpretaciones en vivo en formato íntimo."),
            ProgramItem("21:15", "22:00", "Nuevos sonidos", "Artistas emergentes y sus canciones."),
            ProgramItem("22:00", "23:00", "Ritmo global", "Selección musical internacional.")
        )),
        LiveChannel("ch-familia", "Familia Play", "Niños", "FP", "Aventuras pequeñas", "Cuentos del mundo", "20:00", "20:45", 0xFFE268A6L, listOf(
            ProgramItem("20:00", "20:45", "Aventuras pequeñas", "Relatos y juegos para compartir en familia."),
            ProgramItem("20:45", "21:15", "Cuentos del mundo", "Historias animadas de distintos lugares."),
            ProgramItem("21:15", "22:00", "Planeta curioso", "Preguntas y respuestas para aprender.")
        )),
        LiveChannel("ch-cocina", "Cocina en casa", "Entretenimiento", "CC", "Mesa de temporada", "Recetas simples", "20:00", "21:00", 0xFF4AAB85L, listOf(
            ProgramItem("20:00", "21:00", "Mesa de temporada", "Ingredientes frescos y buenas conversaciones."),
            ProgramItem("21:00", "22:00", "Recetas simples", "Platos cotidianos paso a paso."),
            ProgramItem("22:00", "23:00", "Sabores", "Tradiciones culinarias de la región.")
        ))
    )

    val channelCategories = listOf("Todos", "Noticias", "Deportes", "Entretenimiento", "Música", "Niños", "Cultura")
    val movieGenres = listOf("Todos", "Aventura", "Drama", "Ciencia ficción", "Misterio", "Naturaleza")
    val recentSearches = listOf("Órbita 9", "Naturaleza", "Noticias 24")
}
