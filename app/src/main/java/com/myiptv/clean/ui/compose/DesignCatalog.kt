package com.myiptv.clean.ui.compose

/** Fictional content supplied with the UI reference. It is never added to the real catalog. */
internal data class DesignTitle(
    val id: String,
    val title: String,
    val kind: String,
    val year: Int,
    val ageRating: String,
    val duration: String,
    val genres: List<String>,
    val match: Int,
    val synopsis: String,
    private val photoId: String
) {
    fun imageUrl(width: Int = 1600): String =
        "https://images.unsplash.com/photo-$photoId?crop=entropy&cs=tinysrgb&fit=crop&fm=jpg&q=80&w=$width"
}

internal data class DesignEntry(val title: DesignTitle, val progress: Float? = null)
internal data class DesignRow(val title: String, val items: List<DesignEntry>)

internal object DesignCatalog {
    private fun title(
        id: String,
        name: String,
        kind: String,
        year: Int,
        ageRating: String,
        duration: String,
        genres: List<String>,
        match: Int,
        synopsis: String,
        photoId: String
    ) = DesignTitle(id, name, kind, year, ageRating, duration, genres, match, synopsis, photoId)

    val titles: Map<String, DesignTitle> = listOf(
        title("night", "Ciudad Eléctrica", "Serie", 2025, "16+", "3 temporadas", listOf("Ciencia ficción", "Thriller"), 98,
            "En una metrópolis que nunca duerme, una mensajera descubre un código oculto en las luces de neón que podría reescribir la memoria de toda la ciudad.", "1514439827219-9137a0b99245"),
        title("forest", "El Bosque Sin Fin", "Película", 2024, "13+", "1 h 54 min", listOf("Misterio", "Drama"), 95,
            "Tres hermanos regresan a la casa familiar y descubren que el sendero tras el jardín cambia cada noche.", "1650464179774-caf42373dc17"),
        title("moto", "Última Entrega", "Película", 2025, "16+", "2 h 08 min", listOf("Acción", "Thriller"), 92,
            "Una noche, una moto y un paquete que nadie debe abrir. Doce horas para cruzar la ciudad.", "1599060052009-24d6d0b0161c"),
        title("river", "Río Abajo", "Documental", 2023, "TP", "1 h 32 min", listOf("Naturaleza"), 89,
            "Un viaje a lo largo de un río que atraviesa cuatro estaciones y diez ecosistemas.", "1475070929565-c985b496cb9f"),
        title("alley", "Callejón 9", "Serie", 2024, "18+", "2 temporadas", listOf("Crimen", "Drama"), 91,
            "Un detective retirado vuelve a un barrio donde todos conocen un secreto menos él.", "1651221186975-61f13b4ab7af"),
        title("bridge", "Horizonte LED", "Película", 2025, "13+", "1 h 47 min", listOf("Ciencia ficción"), 90,
            "Una ingeniera cruza un puente que, según los mapas, no existe.", "1542642839-83adadcbe645"),
        title("towers", "Torres", "Serie", 2022, "16+", "4 temporadas", listOf("Drama", "Suspenso"), 87,
            "Poder, dinero y ambición en el piso más alto de la ciudad.", "1535391879778-3bae11d29a24"),
        title("aerial", "Mapa Nocturno", "Documental", 2024, "TP", "58 min", listOf("Ciudades"), 85,
            "Cinco ciudades vistas desde el aire, desde el atardecer hasta el amanecer.", "1671877321315-e114825a475c"),
        title("street", "Sombras de Lluvia", "Película", 2023, "16+", "1 h 41 min", listOf("Romance", "Drama"), 88,
            "Dos desconocidos comparten un paraguas y una noche entera de conversación.", "1541702467897-41915a07d3a7"),
        title("crowd", "Noche de Mercado", "Serie", 2025, "13+", "1 temporada", listOf("Comedia", "Drama"), 86,
            "Un mercado nocturno, veinte puestos y una rivalidad que lo cambia todo.", "1631044176346-804c33ade61c"),
        title("car", "Ruta 7", "Película", 2024, "13+", "1 h 38 min", listOf("Aventura"), 84,
            "Un viaje por carretera con un destino que nadie quiere admitir.", "1675587069926-2d9a51992420"),
        title("window", "Tras el Cristal", "Serie", 2023, "16+", "2 temporadas", listOf("Misterio"), 83,
            "Una casa aislada, un bosque que observa y una familia que oculta algo.", "1714972593463-3beecd6f7c1c")
    ).associateBy(DesignTitle::id)

    private fun get(id: String): DesignTitle = titles.getValue(id)
    private fun entry(id: String, progress: Float? = null) = DesignEntry(get(id), progress)

    val heroItem: DesignTitle = get("night")
    val carousel: List<DesignTitle> = listOf(get("forest"), get("night"), get("alley"), get("bridge"))

    // Matches the four rows in the supplied React screen, including its fixed second hero.
    fun gridRows(slide: Int): List<List<DesignTitle>> = listOf(
        listOf(carousel[slide.coerceAtLeast(0) % carousel.size], get("moto")),
        listOf(get("river"), get("street"), get("towers"), get("crowd")),
        listOf(get("car"), get("window"), get("aerial"), get("night")),
        listOf(get("alley"), get("bridge"), get("forest"), get("moto"))
    )

    val rows: List<DesignRow> = listOf(
        DesignRow("Continuar viendo", listOf(
            entry("night", 0.62f), entry("alley", 0.25f), entry("bridge", 0.8f),
            entry("window", 0.4f), entry("car", 0.1f)
        )),
        DesignRow("Tendencias esta semana", listOf(
            entry("moto"), entry("forest"), entry("towers"), entry("street"),
            entry("crowd"), entry("aerial"), entry("river")
        )),
        DesignRow("Series originales", listOf(entry("night"), entry("alley"), entry("towers"), entry("crowd"), entry("window"))),
        DesignRow("Documentales", listOf(entry("river"), entry("aerial"), entry("forest"), entry("car")))
    )
}
