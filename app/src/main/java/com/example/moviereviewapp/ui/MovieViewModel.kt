package com.example.moviereviewapp.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.moviereviewapp.data.Movie
import com.example.moviereviewapp.data.MovieRepository
import com.example.moviereviewapp.data.Review
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovieViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {
    private val _movies = MutableStateFlow<PagingData<Movie>>(PagingData.empty())
    val movies: StateFlow<PagingData<Movie>> = _movies.asStateFlow()

    private val _selectedMovie = MutableStateFlow<Movie?>(null)
    val selectedMovie: StateFlow<Movie?> = _selectedMovie.asStateFlow()

    private var currentPagingData: PagingData<Movie>? = null
    
    // Store scroll position
    private var lastScrollPosition = 0

    init {
        loadMovies()
    }

    private fun loadMovies() {
        viewModelScope.launch {
            try {
                repository.getMovies()
                    .cachedIn(viewModelScope)
                    .collect { pagingData ->
                        currentPagingData = pagingData
                        _movies.value = pagingData
                    }
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            }
        }
    }

    fun selectMovie(movieId: String) {
        viewModelScope.launch {
            try {
                _selectedMovie.value = null // Clear previous selection
                repository.getMovieById(movieId).collect { movie ->
                    _selectedMovie.value = movie
                }
            } catch (e: Exception) {
                Log.e("MovieViewModel", "Error selecting movie", e)
                _selectedMovie.value = null
            }
        }
    }

    fun clearSelectedMovie() {
        _selectedMovie.value = null
    }

    fun saveScrollPosition(position: Int) {
        lastScrollPosition = position
    }

    fun getLastScrollPosition(): Int = lastScrollPosition

    fun addReview(movieId: String, review: Review) {
        viewModelScope.launch {
            try {
                repository.addReview(movieId, review)
                // Refresh the selected movie to show the new review
                selectMovie(movieId)
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            }
        }
    }
} 