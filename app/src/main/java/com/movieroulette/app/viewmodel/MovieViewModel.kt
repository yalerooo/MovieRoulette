package com.movieroulette.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieroulette.app.data.model.MovieWithDetails
import com.movieroulette.app.data.model.TMDBMovie
import com.movieroulette.app.data.model.TMDBMovieDetails
import com.movieroulette.app.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MovieViewModel : ViewModel() {
    
    val movieRepository = MovieRepository()
    
    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()
    
    private val _moviesState = MutableStateFlow<MoviesState>(MoviesState.Loading)
    val moviesState: StateFlow<MoviesState> = _moviesState.asStateFlow()
    
    private val _addMovieState = MutableStateFlow<AddMovieState>(AddMovieState.Idle)
    val addMovieState: StateFlow<AddMovieState> = _addMovieState.asStateFlow()
    
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
                SearchState.Error(result.exceptionOrNull()?.message ?: "Error al buscar películas")
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
                SearchState.Error(result.exceptionOrNull()?.message ?: "Error al cargar películas")
            }
        }
    }
    
    fun addMovieToGroup(groupId: String, tmdbId: Int) {
        viewModelScope.launch {
            _addMovieState.value = AddMovieState.Loading
            
            // First get movie details
            val detailsResult = movieRepository.getMovieDetails(tmdbId)
            if (detailsResult.isFailure) {
                _addMovieState.value = AddMovieState.Error("Error al obtener detalles")
                return@launch
            }
            
            val details = detailsResult.getOrNull()!!
            val result = movieRepository.addMovieToGroup(groupId, details)
            
            _addMovieState.value = if (result.isSuccess) {
                AddMovieState.Success
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Error al añadir película"
                if (errorMsg.contains("duplicate") || errorMsg.contains("unique")) {
                    AddMovieState.Error("Esta película ya está en el grupo")
                } else {
                    AddMovieState.Error(errorMsg)
                }
            }
        }
    }
    
    fun loadGroupMovies(groupId: String, status: String? = null) {
        viewModelScope.launch {
            _moviesState.value = MoviesState.Loading
            
            val result = movieRepository.getGroupMovies(groupId, status)
            _moviesState.value = if (result.isSuccess) {
                val movies = result.getOrNull() ?: emptyList()
                if (movies.isEmpty()) {
                    MoviesState.Empty
                } else {
                    MoviesState.Success(movies)
                }
            } else {
                MoviesState.Error(result.exceptionOrNull()?.message ?: "Error al cargar películas")
            }
        }
    }
    
    fun updateMovieStatus(movieId: String, groupId: String, newStatus: String) {
        viewModelScope.launch {
            movieRepository.updateMovieStatus(movieId, groupId, newStatus)
        }
    }
    
    fun addRating(movieId: String, rating: Double, comment: String?) {
        viewModelScope.launch {
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
        data class Error(val message: String) : SearchState()
    }
    
    sealed class MoviesState {
        object Loading : MoviesState()
        object Empty : MoviesState()
        data class Success(val movies: List<MovieWithDetails>) : MoviesState()
        data class Error(val message: String) : MoviesState()
    }
    
    sealed class AddMovieState {
        object Idle : AddMovieState()
        object Loading : AddMovieState()
        object Success : AddMovieState()
        data class Error(val message: String) : AddMovieState()
    }
}
