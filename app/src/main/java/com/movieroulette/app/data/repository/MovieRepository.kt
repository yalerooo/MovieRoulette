package com.movieroulette.app.data.repository

import com.movieroulette.app.data.model.*
import com.movieroulette.app.data.remote.SupabaseConfig
import com.movieroulette.app.data.remote.TMDBClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

class MovieRepository {
    
    private val supabase = SupabaseConfig.client
    private val tmdbApi = TMDBClient.api
    
    fun getCurrentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
    }
    
    // TMDB Operations
    suspend fun searchMovies(query: String): Result<List<TMDBMovie>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = tmdbApi.searchMovies(query)
                Result.success(response.results)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun getMovieDetails(tmdbId: Int): Result<TMDBMovieDetails> {
        return withContext(Dispatchers.IO) {
            try {
                val details = tmdbApi.getMovieDetails(tmdbId)
                Result.success(details)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun getPopularMovies(): Result<List<TMDBMovie>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = tmdbApi.getPopularMovies()
                Result.success(response.results)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // Supabase Operations
    suspend fun addMovieToGroup(groupId: String, tmdbMovie: TMDBMovieDetails): Result<Movie> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: return@withContext Result.failure(Exception("User not logged in"))
                
                val genresJson = Json.encodeToString(
                    kotlinx.serialization.builtins.ListSerializer(TMDBGenre.serializer()),
                    tmdbMovie.genres
                )
                
                // Add movie
                val movie = supabase.from("movies")
                    .insert(
                        buildJsonObject {
                            put("group_id", groupId)
                            put("tmdb_id", tmdbMovie.id)
                            put("title", tmdbMovie.title)
                            put("original_title", tmdbMovie.originalTitle)
                            put("overview", tmdbMovie.overview)
                            tmdbMovie.posterPath?.let { put("poster_path", it) }
                            tmdbMovie.backdropPath?.let { put("backdrop_path", it) }
                            tmdbMovie.releaseDate?.let { put("release_date", it) }
                            tmdbMovie.runtime?.let { put("runtime", it) }
                            put("genres", genresJson)
                            put("added_by", userId)
                        }
                    ) {
                        select()
                    }
                    .decodeSingle<Movie>()
                
                // Create initial status
                supabase.from("movie_status")
                    .insert(
                        buildJsonObject {
                            put("movie_id", movie.id)
                            put("group_id", groupId)
                            put("status", "pending")
                        }
                    )
                
                Result.success(movie)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun getGroupMovies(groupId: String, status: String? = null): Result<List<MovieWithDetails>> {
        return withContext(Dispatchers.IO) {
            try {
                val query = supabase.from("movies_with_ratings")
                    .select {
                        filter {
                            eq("group_id", groupId)
                            status?.let { eq("status", it) }
                        }
                    }
                
                val movies = query.decodeList<MovieWithDetails>()
                Result.success(movies)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun getMoviesByStatus(groupId: String, status: String): Result<List<MovieWithDetails>> {
        return getGroupMovies(groupId, status)
    }
    
    suspend fun updateMovieStatus(movieId: String, groupId: String, newStatus: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val updateData = buildJsonObject {
                    put("status", newStatus)
                    
                    when (newStatus) {
                        "watching" -> {
                            put("started_watching_at", "now()")
                        }
                        "watched", "dropped" -> {
                            put("finished_watching_at", "now()")
                        }
                    }
                }
                
                supabase.from("movie_status")
                    .update(updateData) {
                        filter {
                            eq("movie_id", movieId)
                            eq("group_id", groupId)
                        }
                    }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun deleteMovie(movieId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                supabase.from("movies")
                    .delete {
                        filter {
                            eq("id", movieId)
                        }
                    }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun addRating(movieId: String, rating: Double, comment: String?): Result<MovieRating> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: return@withContext Result.failure(Exception("User not logged in"))
                
                // Delete existing rating if exists, then insert new one
                try {
                    supabase.from("movie_ratings")
                        .delete {
                            filter {
                                eq("movie_id", movieId)
                                eq("user_id", userId)
                            }
                        }
                } catch (e: Exception) {
                    // Ignore if no existing rating
                }
                
                val ratingData = supabase.from("movie_ratings")
                    .insert(
                        buildJsonObject {
                            put("movie_id", movieId)
                            put("user_id", userId)
                            put("rating", rating)
                            if (comment != null) {
                                put("comment", comment)
                            }
                        }
                    ) {
                        select()
                    }
                    .decodeSingle<MovieRating>()
                
                Result.success(ratingData)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun getMovieRatings(movieId: String): Result<List<MovieRating>> {
        return withContext(Dispatchers.IO) {
            try {
                // First, get the ratings
                val ratings = supabase.from("movie_ratings")
                    .select {
                        filter {
                            eq("movie_id", movieId)
                        }
                    }
                    .decodeList<MovieRating>()
                
                // Then, fetch usernames for each rating
                val ratingsWithUsernames = ratings.map { rating ->
                    try {
                        val profile = supabase.from("profiles")
                            .select {
                                filter {
                                    eq("id", rating.userId)
                                }
                            }
                            .decodeSingle<Profile>()
                        
                        rating.copy(username = profile.username, avatarUrl = profile.avatarUrl)
                    } catch (e: Exception) {
                        rating.copy(username = "Usuario")
                    }
                }
                
                Result.success(ratingsWithUsernames)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun deleteRating(movieId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: return@withContext Result.failure(Exception("User not logged in"))
                
                supabase.from("movie_ratings")
                    .delete {
                        filter {
                            eq("movie_id", movieId)
                            eq("user_id", userId)
                        }
                    }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun getRandomPendingMovie(groupId: String): Result<MovieWithDetails?> {
        return withContext(Dispatchers.IO) {
            try {
                val movies = getMoviesByStatus(groupId, "pending").getOrNull()
                if (movies.isNullOrEmpty()) {
                    Result.success(null)
                } else {
                    Result.success(movies.random())
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
