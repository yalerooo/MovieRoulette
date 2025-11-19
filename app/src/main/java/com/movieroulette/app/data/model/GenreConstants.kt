package com.movieroulette.app.data.model

object GenreConstants {
    // TMDB Genre IDs
    const val ACTION = 28
    const val ADVENTURE = 12
    const val ANIMATION = 16
    const val COMEDY = 35
    const val CRIME = 80
    const val DOCUMENTARY = 99
    const val DRAMA = 18
    const val FAMILY = 10751
    const val FANTASY = 14
    const val HISTORY = 36
    const val HORROR = 27
    const val MUSIC = 10402
    const val MYSTERY = 9648
    const val ROMANCE = 10749
    const val SCIENCE_FICTION = 878
    const val TV_MOVIE = 10770
    const val THRILLER = 53
    const val WAR = 10752
    const val WESTERN = 37

    val genreMap = mapOf(
        ACTION to "Acción",
        ADVENTURE to "Aventura",
        ANIMATION to "Animación",
        COMEDY to "Comedia",
        CRIME to "Crimen",
        DOCUMENTARY to "Documental",
        DRAMA to "Drama",
        FAMILY to "Familiar",
        FANTASY to "Fantasía",
        HISTORY to "Historia",
        HORROR to "Terror",
        MUSIC to "Música",
        MYSTERY to "Misterio",
        ROMANCE to "Romance",
        SCIENCE_FICTION to "Ciencia Ficción",
        TV_MOVIE to "TV",
        THRILLER to "Thriller",
        WAR to "Guerra",
        WESTERN to "Western"
    )

    fun getGenreName(id: Int): String {
        return genreMap[id] ?: "Desconocido"
    }

    // Popular genres for filter
    val popularGenres = listOf(
        ACTION to "Acción",
        COMEDY to "Comedia",
        DRAMA to "Drama",
        HORROR to "Terror",
        SCIENCE_FICTION to "Ciencia Ficción",
        ROMANCE to "Romance",
        THRILLER to "Thriller",
        ANIMATION to "Animación",
        ADVENTURE to "Aventura",
        FANTASY to "Fantasía"
    )
}
