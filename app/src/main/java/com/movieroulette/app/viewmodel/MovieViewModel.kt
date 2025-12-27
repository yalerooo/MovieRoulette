package com.movieroulette.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieroulette.app.data.model.MovieWithDetails
import com.movieroulette.app.data.model.TMDBMovie
import com.movieroulette.app.data.model.TMDBMovieDetails
import com.movieroulette.app.data.model.TMDBCreditsResponse
import com.movieroulette.app.data.repository.AuthRepository
import com.movieroulette.app.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MovieViewModel : ViewModel() {
    
    val movieRepository = MovieRepository()
    private val authRepository = AuthRepository()
    
    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()
    
    // Estados para filtros y paginación
    private val _genresState = MutableStateFlow<GenresState>(GenresState.Idle)
    val genresState: StateFlow<GenresState> = _genresState.asStateFlow()
    
    private val _discoverState = MutableStateFlow<DiscoverState>(DiscoverState.Idle)
    val discoverState: StateFlow<DiscoverState> = _discoverState.asStateFlow()
    
    private var currentPage = 1
    private var totalPages = 1
    private var currentFilters: MovieFilters? = null
    private val allMovies = mutableListOf<TMDBMovie>()
    
    // Estados separados para cada pestaña
    private val _pendingMoviesState = MutableStateFlow<MoviesState>(MoviesState.Loading)
    val pendingMoviesState: StateFlow<MoviesState> = _pendingMoviesState.asStateFlow()
    
    private val _watchingMoviesState = MutableStateFlow<MoviesState>(MoviesState.Loading)
    val watchingMoviesState: StateFlow<MoviesState> = _watchingMoviesState.asStateFlow()
    
    private val _watchedMoviesState = MutableStateFlow<MoviesState>(MoviesState.Loading)
    val watchedMoviesState: StateFlow<MoviesState> = _watchedMoviesState.asStateFlow()
    
    private val _moviesState = MutableStateFlow<MoviesState>(MoviesState.Loading)
    val moviesState: StateFlow<MoviesState> = _moviesState.asStateFlow()
    
    private val _addMovieState = MutableStateFlow<AddMovieState>(AddMovieState.Idle)
    val addMovieState: StateFlow<AddMovieState> = _addMovieState.asStateFlow()
    
    private val _movieCreditsState = MutableStateFlow<CreditsState>(CreditsState.Idle)
    val movieCreditsState: StateFlow<CreditsState> = _movieCreditsState.asStateFlow()
    
    fun searchMovies(query: String) {
        if (query.isBlank()) {
            loadPopularMovies()
            return
        }
        
        viewModelScope.launch {
            _searchState.value = SearchState.Loading
            
            val result = movieRepository.searchMovies(query)
            _searchState.value = if (result.isSuccess) {
                val movies = result.getOrNull() ?: emptyList()
                if (movies.isEmpty()) {
                    SearchState.Empty
                } else {
                    SearchState.Success(movies)
                }
            } else {
                SearchState.Error(result.exceptionOrNull()?.message, com.movieroulette.app.R.string.error_search_movies)
            }
        }
    }
    
    fun loadPopularMovies() {
        viewModelScope.launch {
            _searchState.value = SearchState.Loading
            
            val result = movieRepository.getPopularMovies()
            _searchState.value = if (result.isSuccess) {
                SearchState.Success(result.getOrNull() ?: emptyList())
            } else {
                SearchState.Error(result.exceptionOrNull()?.message, com.movieroulette.app.R.string.error_load_movies)
            }
        }
    }
    
    fun loadMovieCredits(tmdbId: Int) {
        viewModelScope.launch {
            _movieCreditsState.value = CreditsState.Loading
            
            val result = movieRepository.getMovieCredits(tmdbId)
            _movieCreditsState.value = if (result.isSuccess) {
                CreditsState.Success(result.getOrNull()!!)
            } else {
                CreditsState.Error(result.exceptionOrNull()?.message)
            }
        }
    }
    
    fun resetCreditsState() {
        _movieCreditsState.value = CreditsState.Idle
    }
    
    fun addMovieToGroup(groupId: String, tmdbId: Int) {
        viewModelScope.launch {
            _addMovieState.value = AddMovieState.Loading
            
            // Ensure session is valid
            authRepository.ensureSessionValid()
            
            // First get movie details
            val detailsResult = movieRepository.getMovieDetails(tmdbId)
            if (detailsResult.isFailure) {
                _addMovieState.value = AddMovieState.Error(null, com.movieroulette.app.R.string.error_get_details)
                return@launch
            }
            
            val details = detailsResult.getOrNull()!!
            val result = movieRepository.addMovieToGroup(groupId, details)
            
            _addMovieState.value = if (result.isSuccess) {
                AddMovieState.Success
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: ""
                if (errorMsg.contains("duplicate") || errorMsg.contains("unique")) {
                    AddMovieState.Error(null, com.movieroulette.app.R.string.error_movie_already_added)
                } else {
                    AddMovieState.Error(result.exceptionOrNull()?.message, com.movieroulette.app.R.string.error_load_movies)
                }
            }
        }
    }
    
    fun loadGroupMovies(groupId: String, status: String? = null) {
        viewModelScope.launch {
            // Actualizar el estado específico de la pestaña
            val targetState = when (status) {
                "pending" -> _pendingMoviesState
                "watching" -> _watchingMoviesState
                "watched" -> _watchedMoviesState
                else -> _moviesState
            }
            
            targetState.value = MoviesState.Loading
            
            // Asegurar que la sesión es válida
            authRepository.ensureSessionValid()
            
            val result = movieRepository.getGroupMovies(groupId, status)
            targetState.value = if (result.isSuccess) {
                val movies = result.getOrNull() ?: emptyList()
                if (movies.isEmpty()) {
                    MoviesState.Empty
                } else {
                    MoviesState.Success(movies)
                }
            } else {
                MoviesState.Error(result.exceptionOrNull()?.message, com.movieroulette.app.R.string.error_load_movies)
            }
            
            // También actualizar el estado general para compatibilidad
            _moviesState.value = targetState.value
        }
    }
    
    fun getMoviesStateForStatus(status: String): StateFlow<MoviesState> {
        return when (status) {
            "pending" -> _pendingMoviesState
            "watching" -> _watchingMoviesState
            "watched" -> _watchedMoviesState
            else -> _moviesState
        }
    }
    
    fun updateMovieStatus(movieId: String, groupId: String, newStatus: String) {
        viewModelScope.launch {
            // Ensure session is valid
            authRepository.ensureSessionValid()
            
            // Actualizar en el servidor
            val result = movieRepository.updateMovieStatus(movieId, groupId, newStatus)
            
            if (result.isSuccess) {
                // Recargar el estado de destino para ver el cambio inmediatamente
                loadGroupMovies(groupId, newStatus)
            } else {
                // Si falla, revertir mostrando error
                loadGroupMovies(groupId, "pending")
                loadGroupMovies(groupId, "watching")
                loadGroupMovies(groupId, "watched")
            }
        }
    }
    
    fun removeMovieFromState(movieId: String, status: String) {
        // Obtener el estado específico de la pestaña actual
        val targetState = when (status) {
            "pending" -> _pendingMoviesState
            "watching" -> _watchingMoviesState
            "watched" -> _watchedMoviesState
            else -> _moviesState
        }
        
        val currentState = targetState.value
        if (currentState is MoviesState.Success) {
            val updatedMovies = currentState.movies.filter { it.id != movieId }
            targetState.value = if (updatedMovies.isEmpty()) {
                MoviesState.Empty
            } else {
                MoviesState.Success(updatedMovies)
            }
        }
        
        // También actualizar el estado general
        _moviesState.value = targetState.value
    }
    
    fun addRating(movieId: String, rating: Double, comment: String?) {
        viewModelScope.launch {
            authRepository.ensureSessionValid()
            movieRepository.addRating(movieId, rating, comment)
        }
    }
    
    suspend fun deleteRating(movieId: String): Result<Unit> {
        return movieRepository.deleteRating(movieId)
    }
    
    suspend fun deleteMovie(movieId: String): Result<Unit> {
        return movieRepository.deleteMovie(movieId)
    }
    
    fun countMoviesByStatus(movies: List<MovieWithDetails>, status: String): Int {
        return movies.count { it.status == status }
    }
    
    fun resetAddMovieState() {
        _addMovieState.value = AddMovieState.Idle
    }
    
    sealed class SearchState {
        object Idle : SearchState()
        object Loading : SearchState()
        object Empty : SearchState()
        data class Success(val movies: List<TMDBMovie>) : SearchState()
        data class Error(val message: String?, val messageResId: Int) : SearchState()
    }
    
    sealed class MoviesState {
        object Loading : MoviesState()
        object Empty : MoviesState()
        data class Success(val movies: List<MovieWithDetails>) : MoviesState()
        data class Error(val message: String?, val messageResId: Int) : MoviesState()
    }
    
    sealed class AddMovieState {
        object Idle : AddMovieState()
        object Loading : AddMovieState()
        object Success : AddMovieState()
        data class Error(val message: String?, val messageResId: Int) : AddMovieState()
    }
    
    sealed class CreditsState {
        object Idle : CreditsState()
        object Loading : CreditsState()
        data class Success(val credits: TMDBCreditsResponse) : CreditsState()
        data class Error(val message: String?) : CreditsState()
    }
    
    // Funciones para manejar movie viewers
    suspend fun addMovieViewers(movieId: String, userIds: List<String>): Result<Unit> {
        return movieRepository.addMovieViewers(movieId, userIds)
    }
    
    suspend fun getMovieViewers(movieId: String): Result<List<com.movieroulette.app.data.model.MovieViewerWithProfile>> {
        return movieRepository.getMovieViewers(movieId)
    }
    
    suspend fun hasUserWatchedMovie(movieId: String, userId: String): Result<Boolean> {
        return movieRepository.hasUserWatchedMovie(movieId, userId)
    }
    
    suspend fun getMovieViewersCount(movieId: String): Result<Int> {
        val result = movieRepository.getMovieViewers(movieId)
        return result.map { it.size }
    }
    
    suspend fun clearMovieViewers(movieId: String): Result<Unit> {
        return movieRepository.clearMovieViewers(movieId)
    }
    
    // Funciones para filtros y discover
    fun loadGenres() {
        viewModelScope.launch {
            _genresState.value = GenresState.Loading
            
            val result = movieRepository.getGenres()
            _genresState.value = if (result.isSuccess) {
                GenresState.Success(result.getOrNull() ?: emptyList())
            } else {
                GenresState.Error(result.exceptionOrNull()?.message)
            }
        }
    }
    
    fun discoverMovies(filters: MovieFilters, loadMore: Boolean = false) {
        viewModelScope.launch {
            if (!loadMore) {
                _discoverState.value = DiscoverState.Loading
                currentPage = 1
                allMovies.clear()
            } else {
                if (currentPage >= totalPages) return@launch
                currentPage++
            }
            
            currentFilters = filters
            
            val result = movieRepository.discoverMovies(
                page = currentPage,
                sortBy = filters.sortBy,
                genreIds = filters.genres,
                year = filters.year,
                minRating = filters.minRating
            )
            
            if (result.isSuccess) {
                val (movies, pages) = result.getOrNull()!!
                totalPages = pages
                
                if (!loadMore) {
                    allMovies.clear()
                }
                allMovies.addAll(movies)
                
                _discoverState.value = if (allMovies.isEmpty()) {
                    DiscoverState.Empty
                } else {
                    DiscoverState.Success(
                        movies = allMovies.toList(),
                        hasMore = currentPage < totalPages,
                        isLoadingMore = false
                    )
                }
            } else {
                _discoverState.value = DiscoverState.Error(result.exceptionOrNull()?.message)
            }
        }
    }
    
    fun loadMoreMovies() {
        currentFilters?.let { filters ->
            discoverMovies(filters, loadMore = true)
        }
    }
    
    fun resetDiscoverState() {
        _discoverState.value = DiscoverState.Idle
        currentPage = 1
        totalPages = 1
        allMovies.clear()
        currentFilters = null
    }
    
    sealed class GenresState {
        object Idle : GenresState()
        object Loading : GenresState()
        data class Success(val genres: List<com.movieroulette.app.data.model.TMDBGenre>) : GenresState()
        data class Error(val message: String?) : GenresState()
    }
    
    sealed class DiscoverState {
        object Idle : DiscoverState()
        object Loading : DiscoverState()
        object Empty : DiscoverState()
        data class Success(val movies: List<TMDBMovie>, val hasMore: Boolean, val isLoadingMore: Boolean = false) : DiscoverState()
        data class Error(val message: String?) : DiscoverState()
    }
    
    data class MovieFilters(
        val sortBy: String = "popularity.desc",
        val genres: List<Int>? = null,
        val year: Int? = null,
        val minRating: Double? = null
    )
}
