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
    val genreIds: List<Int>
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
