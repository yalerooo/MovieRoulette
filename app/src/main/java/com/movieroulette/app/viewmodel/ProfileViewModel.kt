package com.movieroulette.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieroulette.app.data.model.Movie
import com.movieroulette.app.data.model.UserProfile
import com.movieroulette.app.data.remote.SupabaseConfig
import com.movieroulette.app.data.repository.AuthRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ProfileViewModel : ViewModel() {
    
    private val authRepository = AuthRepository()
    
    private val _statsState = MutableStateFlow<StatsState>(StatsState.Loading)
    val statsState: StateFlow<StatsState> = _statsState
    
    private val _recentMoviesState = MutableStateFlow<RecentMoviesState>(RecentMoviesState.Loading)
    val recentMoviesState: StateFlow<RecentMoviesState> = _recentMoviesState
    
    private val _favoriteMoviesState = MutableStateFlow<FavoriteMoviesState>(FavoriteMoviesState.Loading)
    val favoriteMoviesState: StateFlow<FavoriteMoviesState> = _favoriteMoviesState
    
    sealed class StatsState {
        object Loading : StatsState()
        data class Success(val moviesRated: Int, val moviesWatched: Int) : StatsState()
        data class Error(val message: String) : StatsState()
    }
    
    sealed class RecentMoviesState {
        object Loading : RecentMoviesState()
        data class Success(val movies: List<MovieStats>) : RecentMoviesState()
        data class Error(val message: String) : RecentMoviesState()
    }
    
    sealed class FavoriteMoviesState {
        object Loading : FavoriteMoviesState()
        data class Success(val movies: List<FavoriteMovie>) : FavoriteMoviesState()
        data class Error(val message: String) : FavoriteMoviesState()
    }
    
    fun loadUserStats() {
        viewModelScope.launch {
            try {
                authRepository.ensureSessionValid()
                val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: run {
                    _statsState.value = StatsState.Error("Usuario no autenticado")
                    return@launch
                }
                
                // Obtener todos los grupos del usuario
                val userGroups = SupabaseConfig.client
                    .from("group_members")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<GroupMemberResponse>()
                
                val groupIds = userGroups.map { it.group_id }
                
                if (groupIds.isEmpty()) {
                    _statsState.value = StatsState.Success(0, 0)
                    return@launch
                }
                
                // Obtener películas únicas valoradas desde movie_ratings
                val ratedMovies = SupabaseConfig.client
                    .from("movie_ratings")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<MovieRatingResponse>()
                
                // Contar películas únicas por tmdb_id (necesitamos obtener el movie_id y luego el tmdb_id)
                val movieIds = ratedMovies.map { it.movie_id }.distinct()
                
                // Obtener tmdb_ids únicos de las películas valoradas
                val uniqueRatedMovies = if (movieIds.isNotEmpty()) {
                    val moviesData = SupabaseConfig.client
                        .from("movies")
                        .select {
                            filter {
                                isIn("id", movieIds)
                            }
                        }
                        .decodeList<MovieDataResponse>()
                    moviesData.map { it.tmdb_id }.distinct().size
                } else {
                    0
                }
                
                // Obtener películas únicas vistas desde movie_viewers (solo las que este usuario realmente vio)
                val viewerRecords = SupabaseConfig.client
                    .from("movie_viewers")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<com.movieroulette.app.data.model.MovieViewer>()
                
                // Obtener tmdb_ids únicos de las películas que el usuario vio
                val watchedMovieIds = viewerRecords.map { it.movieId }.distinct()
                val uniqueWatchedMovies = if (watchedMovieIds.isNotEmpty()) {
                    val moviesData = SupabaseConfig.client
                        .from("movies")
                        .select {
                            filter {
                                isIn("id", watchedMovieIds)
                            }
                        }
                        .decodeList<MovieDataResponse>()
                    moviesData.map { it.tmdb_id }.distinct().size
                } else {
                    0
                }
                
                _statsState.value = StatsState.Success(
                    moviesRated = uniqueRatedMovies,
                    moviesWatched = uniqueWatchedMovies
                )
                
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading stats", e)
                _statsState.value = StatsState.Error(e.message ?: "Error desconocido")
            }
        }
    }
    
    fun loadRecentMovies() {
        viewModelScope.launch {
            try {
                authRepository.ensureSessionValid()
                val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: run {
                    _recentMoviesState.value = RecentMoviesState.Error("Usuario no autenticado")
                    return@launch
                }
                
                // Obtener todas las películas que este usuario vio desde movie_viewers
                val viewerRecords = SupabaseConfig.client
                    .from("movie_viewers")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<com.movieroulette.app.data.model.MovieViewer>()
                
                if (viewerRecords.isEmpty()) {
                    _recentMoviesState.value = RecentMoviesState.Success(emptyList())
                    return@launch
                }
                
                // Obtener los IDs de las películas
                val movieIds = viewerRecords.map { it.movieId }.distinct()
                
                // Obtener información de las películas desde la tabla movies
                val moviesData = SupabaseConfig.client
                    .from("movies")
                    .select {
                        filter {
                            isIn("id", movieIds)
                        }
                    }
                    .decodeList<MovieDataResponse>()
                
                // Combinar con las fechas en que el usuario las marcó como vistas
                val viewerMap = viewerRecords.associateBy { it.movieId }
                
                val uniqueMovies = moviesData
                    .mapNotNull { movie ->
                        viewerMap[movie.id]?.let { viewer ->
                            MovieStats(
                                tmdb_id = movie.tmdb_id,
                                title = movie.title,
                                poster_path = movie.poster_path,
                                updated_at = viewer.markedAt ?: ""
                            )
                        }
                    }
                    .sortedByDescending { it.updated_at }
                    .take(4)
                
                _recentMoviesState.value = RecentMoviesState.Success(uniqueMovies)
                
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading recent movies", e)
                _recentMoviesState.value = RecentMoviesState.Error(e.message ?: "Error desconocido")
            }
        }
    }
    
    fun loadFavoriteMovies() {
        viewModelScope.launch {
            try {
                authRepository.ensureSessionValid()
                val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: run {
                    _favoriteMoviesState.value = FavoriteMoviesState.Error("Usuario no autenticado")
                    return@launch
                }
                
                val favorites = SupabaseConfig.client
                    .from("user_favorite_movies")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<FavoriteMovie>()
                    .sortedByDescending { it.created_at }
                    .take(6)
                
                _favoriteMoviesState.value = FavoriteMoviesState.Success(favorites)
                
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading favorites", e)
                _favoriteMoviesState.value = FavoriteMoviesState.Error(e.message ?: "Error desconocido")
            }
        }
    }
    
    fun addFavorite(tmdbId: Int, title: String, posterPath: String?) {
        viewModelScope.launch {
            try {
                authRepository.ensureSessionValid()
                Log.d("ProfileViewModel", "Adding favorite: $title (tmdbId: $tmdbId)")
                val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id
                
                if (userId == null) {
                    Log.e("ProfileViewModel", "Cannot add favorite: user not logged in")
                    return@launch
                }
                
                // Verificar que no exceda el límite de 6
                val currentFavorites = SupabaseConfig.client
                    .from("user_favorite_movies")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<FavoriteMovie>()
                
                if (currentFavorites.size >= 6) {
                    Log.w("ProfileViewModel", "Cannot add more than 6 favorites")
                    return@launch
                }
                
                // Insertar la nueva favorita
                SupabaseConfig.client
                    .from("user_favorite_movies")
                    .insert(
                        buildJsonObject {
                            put("user_id", userId)
                            put("tmdb_id", tmdbId)
                            put("title", title)
                            if (posterPath != null) {
                                put("poster_path", posterPath)
                            }
                        }
                    )
                
                Log.d("ProfileViewModel", "Favorite added successfully, reloading list...")
                loadFavoriteMovies()
                
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error adding favorite: ${e.message}", e)
            }
        }
    }
    
    fun removeFavorite(tmdbId: Int) {
        viewModelScope.launch {
            try {
                authRepository.ensureSessionValid()
                val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return@launch
                
                SupabaseConfig.client
                    .from("user_favorite_movies")
                    .delete {
                        filter {
                            eq("user_id", userId)
                            eq("tmdb_id", tmdbId)
                        }
                    }
                
                loadFavoriteMovies()
                
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error removing favorite", e)
            }
        }
    }
    
    // Métodos para cargar perfil de otros usuarios
    suspend fun loadUserProfileById(userId: String): UserProfile? {
        return try {
            authRepository.ensureSessionValid()
            SupabaseConfig.client.from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<UserProfile>()
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Error loading user profile: ${e.message}")
            null
        }
    }
    
    suspend fun loadUserStatsByUserId(userId: String) {
        try {
            authRepository.ensureSessionValid()
            _statsState.value = StatsState.Loading
            
            // Películas valoradas
            val ratedMoviesResponse = SupabaseConfig.client.from("movie_ratings")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<MovieRatingResponse>()
            
            val moviesRated = ratedMoviesResponse.size
            
            // Películas vistas
            val watchedMoviesResponse = SupabaseConfig.client.from("movie_status")
                .select {
                    filter {
                        eq("status", "watched")
                    }
                }
                .decodeList<MovieStatusResponse>()
            
            val moviesWatched = watchedMoviesResponse.size
            
            _statsState.value = StatsState.Success(moviesRated, moviesWatched)
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Error loading stats: ${e.message}")
            _statsState.value = StatsState.Error(e.message ?: "Error desconocido")
        }
    }
    
    suspend fun loadRecentMoviesByUserId(userId: String) {
        Log.d("ProfileViewModel", "=== loadRecentMoviesByUserId CALLED - START ===")
        Log.d("ProfileViewModel", "=== loadRecentMoviesByUserId called with userId: $userId ===")
        try {
            authRepository.ensureSessionValid()
            Log.d("ProfileViewModel", "Inside try block")
            _recentMoviesState.value = RecentMoviesState.Loading
            Log.d("ProfileViewModel", "State set to Loading")
            
            Log.d("ProfileViewModel", "Loading recent movies for user: $userId")
            
            // Obtener las películas que este usuario específico vio desde movie_viewers
            val viewerRecords = SupabaseConfig.client
                .from("movie_viewers")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<com.movieroulette.app.data.model.MovieViewer>()
            
            Log.d("ProfileViewModel", "User has watched ${viewerRecords.size} movies")
            
            if (viewerRecords.isEmpty()) {
                _recentMoviesState.value = RecentMoviesState.Success(emptyList())
                return
            }
            
            // Ordenar por fecha y tomar las 6 más recientes
            val recentViewers = viewerRecords
                .sortedByDescending { it.markedAt ?: "" }
                .take(6)
            
            // Obtener IDs únicos de películas
            val movieIds = recentViewers.map { it.movieId }.distinct()
            
            // Obtener datos de las películas
            val moviesData = SupabaseConfig.client.from("movies")
                .select {
                    filter {
                        isIn("id", movieIds)
                    }
                }
                .decodeList<MovieDataResponse>()
            
            // Mapear a la estructura de MovieStats manteniendo el orden
            val movieMap = moviesData.associateBy { it.id }
            val viewerMap = recentViewers.associateBy { it.movieId }
            
            val recentMovies = recentViewers.mapNotNull { viewer ->
                movieMap[viewer.movieId]?.let { movie ->
                    MovieStats(
                        tmdb_id = movie.tmdb_id,
                        title = movie.title,
                        poster_path = movie.poster_path,
                        updated_at = viewer.markedAt ?: ""
                    )
                }
            }
            
            Log.d("ProfileViewModel", "Loaded ${recentMovies.size} recent movies")
            _recentMoviesState.value = RecentMoviesState.Success(recentMovies)
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Error loading recent movies: ${e.message}", e)
            _recentMoviesState.value = RecentMoviesState.Error(e.message ?: "Error desconocido")
        }
    }
    
    suspend fun loadFavoriteMoviesByUserId(userId: String) {
        Log.d("ProfileViewModel", "=== loadFavoriteMoviesByUserId called with userId: $userId ===")
        try {
            authRepository.ensureSessionValid()
            _favoriteMoviesState.value = FavoriteMoviesState.Loading
            Log.d("ProfileViewModel", "Favorites state set to Loading")
            
            Log.d("ProfileViewModel", "Loading favorite movies for user: $userId")
            
            val favorites = SupabaseConfig.client.from("user_favorite_movies")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<FavoriteMovie>()
            
            Log.d("ProfileViewModel", "Loaded ${favorites.size} favorite movies")
            _favoriteMoviesState.value = FavoriteMoviesState.Success(favorites)
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Error loading favorites: ${e.message}", e)
            _favoriteMoviesState.value = FavoriteMoviesState.Error(e.message ?: "Error desconocido")
        }
    }
}

@Serializable
data class GroupMemberResponse(
    val group_id: String
)

@Serializable
data class MovieRatingResponse(
    val id: String,
    val movie_id: String,
    val user_id: String,
    val rating: Double,
    val created_at: String? = null
)

@Serializable
data class MovieDataResponse(
    val id: String,
    val tmdb_id: Int,
    val title: String,
    val poster_path: String?
)

@Serializable
data class MovieStatusResponse(
    val id: String,
    val movie_id: String,
    val group_id: String,
    val status: String,
    val finished_watching_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class MovieStats(
    val tmdb_id: Int,
    val title: String,
    val poster_path: String?,
    val updated_at: String
)

@Serializable
data class FavoriteMovie(
    val id: String? = null,
    val user_id: String? = null,
    val tmdb_id: Int,
    val title: String,
    val poster_path: String?,
    val created_at: String? = null
)
