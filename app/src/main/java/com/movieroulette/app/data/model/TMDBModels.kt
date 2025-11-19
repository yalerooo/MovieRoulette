package com.movieroulette.app.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

// TMDB API Response Models
data class TMDBSearchResponse(
    val page: Int,
    val results: List<TMDBMovie>,
    @SerializedName("total_pages")
    val totalPages: Int,
    @SerializedName("total_results")
    val totalResults: Int
)

data class TMDBMovie(
    val id: Int,
    val title: String,
    @SerializedName("original_title")
    val originalTitle: String,
    val overview: String,
    @SerializedName("poster_path")
    val posterPath: String?,
    @SerializedName("backdrop_path")
    val backdropPath: String?,
    @SerializedName("release_date")
    val releaseDate: String?,
    @SerializedName("vote_average")
    val voteAverage: Double,
    @SerializedName("vote_count")
    val voteCount: Int,
    val popularity: Double,
    @SerializedName("genre_ids")
    val genreIds: List<Int>,
    @SerializedName("original_language")
    val originalLanguage: String?
)

data class TMDBMovieDetails(
    val id: Int,
    val title: String,
    @SerializedName("original_title")
    val originalTitle: String,
    val overview: String,
    @SerializedName("poster_path")
    val posterPath: String?,
    @SerializedName("backdrop_path")
    val backdropPath: String?,
    @SerializedName("release_date")
    val releaseDate: String?,
    val runtime: Int?,
    val genres: List<TMDBGenre>,
    @SerializedName("vote_average")
    val voteAverage: Double,
    @SerializedName("vote_count")
    val voteCount: Int,
    val tagline: String?,
    val budget: Long,
    val revenue: Long,
    val status: String
)

@Serializable
data class TMDBGenre(
    val id: Int,
    val name: String
)

// Credits models
data class TMDBCreditsResponse(
    val id: Int,
    val cast: List<TMDBCast>,
    val crew: List<TMDBCrew>
)

data class TMDBCast(
    val id: Int,
    val name: String,
    val character: String,
    @SerializedName("profile_path")
    val profilePath: String?,
    val order: Int
)

data class TMDBCrew(
    val id: Int,
    val name: String,
    val job: String,
    val department: String,
    @SerializedName("profile_path")
    val profilePath: String?
)

// Helper functions
fun TMDBMovie.toPosterUrl(): String? {
    return posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
}

fun TMDBMovie.toBackdropUrl(): String? {
    return backdropPath?.let { "https://image.tmdb.org/t/p/original$it" }
}

fun TMDBMovieDetails.toPosterUrl(): String? {
    return posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
}

fun TMDBMovieDetails.toBackdropUrl(): String? {
    return backdropPath?.let { "https://image.tmdb.org/t/p/original$it" }
}

// Extension functions for Movie and MovieWithDetails
fun Movie.toPosterUrl(): String? {
    return posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
}

fun Movie.toBackdropUrl(): String? {
    return backdropPath?.let { "https://image.tmdb.org/t/p/original$it" }
}

fun MovieWithDetails.toPosterUrl(): String? {
    return posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
}

fun MovieWithDetails.toBackdropUrl(): String? {
    return backdropPath?.let { "https://image.tmdb.org/t/p/original$it" }
}

// Extract genre IDs from genres JSON string
fun MovieWithDetails.getGenreIds(): List<Int> {
    return try {
        if (genres.isNullOrBlank()) return emptyList()
        val genreList = kotlinx.serialization.json.Json.decodeFromString<List<TMDBGenre>>(genres)
        genreList.map { it.id }
    } catch (e: Exception) {
        emptyList()
    }
}

fun Movie.getGenreIds(): List<Int> {
    return try {
        if (genres.isNullOrBlank()) return emptyList()
        val genreList = kotlinx.serialization.json.Json.decodeFromString<List<TMDBGenre>>(genres)
        genreList.map { it.id }
    } catch (e: Exception) {
        emptyList()
    }
}
