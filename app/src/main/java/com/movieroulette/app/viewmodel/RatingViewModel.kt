package com.movieroulette.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieroulette.app.data.model.MovieRating
import com.movieroulette.app.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RatingViewModel : ViewModel() {
    
    private val movieRepository = MovieRepository()
    
    private val _ratingsState = MutableStateFlow<RatingsState>(RatingsState.Idle)
    val ratingsState: StateFlow<RatingsState> = _ratingsState.asStateFlow()
    
    fun loadMovieRatings(movieId: String) {
        viewModelScope.launch {
            _ratingsState.value = RatingsState.Loading
            
            val result = movieRepository.getMovieRatings(movieId)
            _ratingsState.value = if (result.isSuccess) {
                RatingsState.Success(result.getOrNull() ?: emptyList())
            } else {
                RatingsState.Error(result.exceptionOrNull()?.message, com.movieroulette.app.R.string.error_load_ratings)
            }
        }
    }
    
    sealed class RatingsState {
        object Idle : RatingsState()
        object Loading : RatingsState()
        data class Success(val ratings: List<MovieRating>) : RatingsState()
        data class Error(val message: String?, val messageResId: Int) : RatingsState()
    }
}
