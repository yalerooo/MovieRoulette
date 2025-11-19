package com.movieroulette.app.data.repository

import android.util.Log
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
    
    suspend fun getMovieCredits(tmdbId: Int): Result<TMDBCreditsResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val credits = tmdbApi.getMovieCredits(tmdbId)
                Result.success(credits)
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
    
    suspend fun discoverMovies(
        page: Int = 1,
        sortBy: String = "popularity.desc",
        genreIds: List<Int>? = null,
        year: Int? = null,
        minRating: Double? = null
    ): Result<Pair<List<TMDBMovie>, Int>> {
        return withContext(Dispatchers.IO) {
            try {
                val genresString = genreIds?.joinToString(",")
                // Si es ordenar por votación, poner mínimo de 500 votos para tener películas conocidas
                val minVotes = when {
                    sortBy == "vote_average.desc" -> 500
                    minRating != null && minRating > 0 -> 100
                    else -> null
                }
                val response = tmdbApi.discoverMovies(
                    page = page,
                    sortBy = sortBy,
                    withGenres = genresString,
                    year = year,
                    minRating = minRating,
                    minVoteCount = minVotes
                )
                Result.success(response.results to response.totalPages)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun getGenres(): Result<List<TMDBGenre>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = tmdbApi.getGenres()
                Result.success(response.genres)
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
                val currentUserId = getCurrentUserId()
                
                // Si el nuevo estado es "pending", eliminar todas las puntuaciones y rating_prompts
                if (newStatus == "pending") {
                    try {
                        supabase.from("movie_ratings")
                            .delete {
                                filter {
                                    eq("movie_id", movieId)
                                }
                            }
                        
                        // Eliminar los rating_prompts cuando vuelve a pending
                        deleteRatingPrompts(movieId, groupId)
                    } catch (e: Exception) {
                        Log.e("MovieRepository", "Error deleting ratings/prompts: ${e.message}")
                        // Continuar incluso si falla la eliminación
                    }
                }
                
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
                
                // ANTES de actualizar, si el nuevo estado NO es "watched", limpiar viewers
                if (newStatus != "watched") {
                    Log.d("MovieRepository", "Movie moving away from watched, clearing viewers")
                    val clearResult = clearMovieViewers(movieId)
                    if (clearResult.isFailure) {
                        Log.e("MovieRepository", "Failed to clear viewers: ${clearResult.exceptionOrNull()?.message}")
                    } else {
                        Log.d("MovieRepository", "Viewers cleared for movie $movieId")
                    }
                }
                
                supabase.from("movie_status")
                    .update(updateData) {
                        filter {
                            eq("movie_id", movieId)
                            eq("group_id", groupId)
                        }
                    }
                
                // Si el nuevo estado es "watched", crear rating_prompts para todos los miembros DESPUÉS de actualizar
                if (newStatus == "watched" && currentUserId != null) {
                    val result = createRatingPromptsForGroup(movieId, groupId, currentUserId)
                    if (result.isFailure) {
                        Log.e("MovieRepository", "Failed to create rating prompts: ${result.exceptionOrNull()?.message}")
                    } else {
                        Log.d("MovieRepository", "Rating prompts created successfully for movie $movieId")
                    }
                }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("MovieRepository", "Error updating movie status: ${e.message}")
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
                
                // Eliminar el registro de dismissed_rating_prompts si existe (ya puntuó)
                try {
                    removeDismissedRatingPrompt(movieId)
                } catch (e: Exception) {
                    Log.w("MovieRepository", "Could not remove dismissed prompt (may not exist): ${e.message}")
                }
                
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
    
    suspend fun getUserRating(movieId: String, userId: String): Result<MovieRating?> {
        return withContext(Dispatchers.IO) {
            try {
                val rating = supabase.from("movie_ratings")
                    .select {
                        filter {
                            eq("movie_id", movieId)
                            eq("user_id", userId)
                        }
                    }
                    .decodeSingleOrNull<MovieRating>()
                
                Result.success(rating)
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
    
    suspend fun getUnratedWatchedMovies(groupId: String): Result<List<MovieWithDetails>> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: return@withContext Result.failure(Exception("User not logged in"))
                
                // Obtener todas las películas vistas del grupo
                val watchedMovies = getMoviesByStatus(groupId, "watched").getOrNull() ?: emptyList()
                
                // Filtrar las que el usuario no ha calificado
                val unratedMovies = watchedMovies.filter { movie ->
                    try {
                        val ratings = supabase.from("movie_ratings")
                            .select {
                                filter {
                                    eq("movie_id", movie.id)
                                    eq("user_id", userId)
                                }
                            }
                            .decodeList<MovieRating>()
                        
                        ratings.isEmpty() // Si no hay rating, incluir esta película
                    } catch (e: Exception) {
                        true // En caso de error, asumir que no está calificada
                    }
                }
                
                Result.success(unratedMovies)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // ==================== Rating Prompts ====================
    
    /**
     * Crea registros de rating_prompts para todos los miembros del grupo cuando alguien marca una película como watched
     * @param movieId ID de la película (movie_status.movie_id)
     * @param groupId ID del grupo
     * @param markedByUserId ID del usuario que marcó la película como vista
     */
    suspend fun createRatingPromptsForGroup(movieId: String, groupId: String, markedByUserId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("MovieRepository", "Creating rating prompts for movie $movieId in group $groupId")
                
                // Obtener solo los usuarios que vieron la película (viewers)
                val viewers = supabase.from("movie_viewers")
                    .select {
                        filter {
                            eq("movie_id", movieId)
                        }
                    }
                    .decodeList<com.movieroulette.app.data.model.MovieViewer>()
                
                Log.d("MovieRepository", "Found ${viewers.size} viewers for this movie")
                
                if (viewers.isEmpty()) {
                    Log.w("MovieRepository", "No viewers found for movie $movieId")
                    return@withContext Result.success(Unit)
                }
                
                // Crear un registro de rating_prompt solo para los usuarios que vieron la película
                // PERO excluir al usuario que marcó la película como vista (no enviarse notificación a sí mismo)
                val prompts = viewers
                    .filter { it.userId != markedByUserId } // Excluir al usuario que hizo la acción
                    .map { viewer ->
                        buildJsonObject {
                            put("group_id", groupId)
                            put("movie_id", movieId)
                            put("user_id", viewer.userId)
                            put("marked_by_user_id", markedByUserId)
                            put("prompt_shown", false)
                        }
                    }
                
                Log.d("MovieRepository", "Inserting ${prompts.size} rating prompts (excluding the user who marked it)")
                
                // Insertar todos los prompts (usar upsert para ignorar duplicados)
                supabase.from("rating_prompts")
                    .upsert(prompts) {
                        // ignoreDuplicates = true (por defecto en upsert)
                    }
                
                Log.d("MovieRepository", "Rating prompts inserted successfully")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("MovieRepository", "Error creating rating prompts: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Verifica si el usuario actual ya vio el popup de rating para una película
     * @param movieId ID de la película
     * @param groupId ID del grupo
     * @return true si ya vio el popup, false si aún no lo ha visto
     */
    suspend fun hasSeenRatingPrompt(movieId: String, groupId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId() ?: return@withContext Result.failure(Exception("User not logged in"))
                
                val prompts = supabase.from("rating_prompts")
                    .select {
                        filter {
                            eq("group_id", groupId)
                            eq("movie_id", movieId)
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<RatingPrompt>()
                
                // Si no existe el registro, significa que no ha visto el prompt
                if (prompts.isEmpty()) {
                    Result.success(false)
                } else {
                    // Si existe, devolver el valor de prompt_shown
                    Result.success(prompts.first().promptShown)
                }
            } catch (e: Exception) {
                Log.e("MovieRepository", "Error checking rating prompt: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Marca el rating prompt como visto para el usuario actual
     * @param movieId ID de la película
     * @param groupId ID del grupo
     */
    suspend fun markRatingPromptShown(movieId: String, groupId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId() ?: return@withContext Result.failure(Exception("User not logged in"))
                
                supabase.from("rating_prompts")
                    .update(
                        buildJsonObject {
                            put("prompt_shown", true)
                            put("updated_at", "now()")
                        }
                    ) {
                        filter {
                            eq("group_id", groupId)
                            eq("movie_id", movieId)
                            eq("user_id", userId)
                        }
                    }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("MovieRepository", "Error marking rating prompt as shown: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Elimina todos los rating_prompts de una película cuando vuelve a pending
     * @param movieId ID de la película
     * @param groupId ID del grupo
     */
    suspend fun deleteRatingPrompts(movieId: String, groupId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                supabase.from("rating_prompts")
                    .delete {
                        filter {
                            eq("group_id", groupId)
                            eq("movie_id", movieId)
                        }
                    }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("MovieRepository", "Error deleting rating prompts: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Verifica si el usuario ha descartado el rating prompt para esta película (en cualquier grupo)
     * @param movieId ID de la película
     * @return true si el usuario descartó el prompt, false si no
     */
    suspend fun hasUserDismissedRatingPrompt(movieId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId() ?: return@withContext Result.failure(Exception("User not logged in"))
                
                val dismissed = supabase.from("dismissed_rating_prompts")
                    .select {
                        filter {
                            eq("user_id", userId)
                            eq("movie_id", movieId)
                        }
                    }
                    .decodeList<DismissedRatingPrompt>()
                
                Result.success(dismissed.isNotEmpty())
            } catch (e: Exception) {
                Log.e("MovieRepository", "Error checking dismissed rating prompt: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Marca que el usuario descartó el rating prompt para esta película
     * @param movieId ID de la película
     */
    suspend fun dismissRatingPrompt(movieId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId() ?: return@withContext Result.failure(Exception("User not logged in"))
                
                supabase.from("dismissed_rating_prompts")
                    .insert(
                        buildJsonObject {
                            put("user_id", userId)
                            put("movie_id", movieId)
                        }
                    )
                
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("MovieRepository", "Error dismissing rating prompt: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Elimina el registro de descarte cuando el usuario puntúa la película
     * @param movieId ID de la película
     */
    suspend fun removeDismissedRatingPrompt(movieId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId() ?: return@withContext Result.failure(Exception("User not logged in"))
                
                supabase.from("dismissed_rating_prompts")
                    .delete {
                        filter {
                            eq("user_id", userId)
                            eq("movie_id", movieId)
                        }
                    }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("MovieRepository", "Error removing dismissed rating prompt: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Agrega múltiples usuarios como viewers de una película
     * @param movieId ID de la película
     * @param userIds Lista de IDs de usuarios que vieron la película
     */
    suspend fun addMovieViewers(movieId: String, userIds: List<String>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("MovieRepository", "Adding ${userIds.size} viewers for movie $movieId")
                Log.d("MovieRepository", "User IDs: ${userIds.joinToString()}")
                
                // Verificar si hay duplicados en la lista
                val uniqueUserIds = userIds.distinct()
                if (uniqueUserIds.size != userIds.size) {
                    Log.w("MovieRepository", "WARNING: Duplicate user IDs detected! Original: ${userIds.size}, Unique: ${uniqueUserIds.size}")
                }
                
                val userId = supabase.auth.currentUserOrNull()?.id ?: return@withContext Result.failure(Exception("User not authenticated"))
                
                // PRIMERO: Limpiar viewers existentes para esta película
                Log.d("MovieRepository", "Clearing existing viewers before adding new ones")
                try {
                    supabase.from("movie_viewers")
                        .delete {
                            filter {
                                eq("movie_id", movieId)
                            }
                        }
                    Log.d("MovieRepository", "Existing viewers cleared")
                } catch (e: Exception) {
                    Log.w("MovieRepository", "No existing viewers to clear or error: ${e.message}")
                }
                
                // SEGUNDO: Insertar los nuevos viewers (usando la lista única)
                val viewersData = uniqueUserIds.map { viewerUserId ->
                    buildJsonObject {
                        put("movie_id", movieId)
                        put("user_id", viewerUserId)
                        put("marked_by", userId)
                    }
                }
                
                Log.d("MovieRepository", "Inserting viewers data: ${viewersData.size} records")
                supabase.from("movie_viewers")
                    .insert(viewersData)
                
                Log.d("MovieRepository", "Successfully added ${uniqueUserIds.size} viewers")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("MovieRepository", "Error adding movie viewers: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Elimina todos los viewers de una película
     * Se usa cuando una película vuelve a pending/watching desde watched
     * @param movieId ID de la película
     */
    suspend fun clearMovieViewers(movieId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("MovieRepository", "Clearing viewers for movie $movieId")
                supabase.from("movie_viewers")
                    .delete {
                        filter {
                            eq("movie_id", movieId)
                        }
                    }
                
                Log.d("MovieRepository", "Successfully cleared viewers for movie $movieId")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("MovieRepository", "Error clearing movie viewers: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Obtiene la lista de usuarios que vieron una película con sus perfiles
     * @param movieId ID de la película
     */
    suspend fun getMovieViewers(movieId: String): Result<List<com.movieroulette.app.data.model.MovieViewerWithProfile>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("MovieRepository", "Fetching viewers for movie $movieId")
                val viewers = supabase.from("movie_viewers")
                    .select {
                        filter {
                            eq("movie_id", movieId)
                        }
                    }
                    .decodeList<com.movieroulette.app.data.model.MovieViewer>()
                
                Log.d("MovieRepository", "Found ${viewers.size} viewers for movie $movieId")
                
                // Obtener perfiles para cada viewer
                val viewersWithProfiles = viewers.mapNotNull { viewer ->
                    try {
                        val profile = supabase.from("profiles")
                            .select {
                                filter {
                                    eq("id", viewer.userId)
                                }
                            }
                            .decodeSingle<UserProfile>()
                        
                        com.movieroulette.app.data.model.MovieViewerWithProfile(
                            id = viewer.id,
                            movieId = viewer.movieId,
                            userId = viewer.userId,
                            username = profile.username,
                            displayName = profile.displayName,
                            avatarUrl = profile.avatarUrl,
                            markedAt = viewer.markedAt
                        )
                    } catch (e: Exception) {
                        Log.e("MovieRepository", "Error getting profile for viewer: ${e.message}")
                        null
                    }
                }
                
                Result.success(viewersWithProfiles)
            } catch (e: Exception) {
                Log.e("MovieRepository", "Error getting movie viewers: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Verifica si un usuario específico ha visto una película
     * @param movieId ID de la película
     * @param userId ID del usuario
     */
    suspend fun hasUserWatchedMovie(movieId: String, userId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val viewers = supabase.from("movie_viewers")
                    .select {
                        filter {
                            eq("movie_id", movieId)
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<com.movieroulette.app.data.model.MovieViewer>()
                
                Result.success(viewers.isNotEmpty())
            } catch (e: Exception) {
                Log.e("MovieRepository", "Error checking if user watched movie: ${e.message}")
                Result.failure(e)
            }
        }
    }
}
