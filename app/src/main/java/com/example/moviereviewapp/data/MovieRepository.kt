package com.example.moviereviewapp.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.moviereviewapp.data.api.MovieDTO
import com.example.moviereviewapp.data.api.MovieDetailsResponse
import com.example.moviereviewapp.data.api.TMDBService
import com.example.moviereviewapp.data.local.MovieDao
import com.example.moviereviewapp.data.local.MovieEntity
import com.example.moviereviewapp.data.paging.MoviePagingSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

interface MovieRepository {
    fun getMovies(): Flow<PagingData<Movie>>
    fun getMovieById(id: String): Flow<Movie?>
    suspend fun addReview(movieId: String, review: Review)
}

@Singleton
class MovieRepositoryImpl @Inject constructor(
    private val tmdbService: TMDBService,
    private val movieDao: MovieDao,
    private val apiKey: String,
    private val context: Context
) : MovieRepository {
    private val reviews = mutableMapOf<String, MutableList<Review>>()

    override fun getMovies(): Flow<PagingData<Movie>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 2
            ),
            pagingSourceFactory = {
                MoviePagingSource(tmdbService, movieDao, apiKey, context)
            }
        ).flow
    }

    override fun getMovieById(id: String): Flow<Movie?> = flow {
        try {
            Log.d("MovieRepository", "Fetching movie details for ID: $id")
            
            // First check if we have the movie in local database
            val localMovie = movieDao.getMovieById(id)
            if (localMovie != null) {
                Log.d("MovieRepository", "Found movie in local database: ${localMovie.title}")
                val movie = localMovie.toMovie()
                val movieWithReviews = movie.copy(reviews = reviews[movie.id] ?: emptyList())
                emit(movieWithReviews)
            }

            // Then check internet connectivity
            if (isNetworkAvailable()) {
                try {
                    Log.d("MovieRepository", "Internet available, attempting to fetch from remote API")
                    val movieDetails = tmdbService.getMovieDetails(id, apiKey)
                    Log.d("MovieRepository", "Successfully fetched movie details from remote: ${movieDetails.title}")
                    
                    val movie = movieDetails.toMovie()
                    val movieWithReviews = movie.copy(reviews = reviews[movie.id] ?: emptyList())
                    
                    // Cache the movie in local database
                    Log.d("MovieRepository", "Caching movie in local database")
                    movieDao.insertMovies(listOf(movie.toMovieEntity()))
                    
                    emit(movieWithReviews)
                } catch (e: Exception) {
                    Log.e("MovieRepository", "Error fetching movie details from remote", e)
                    if (localMovie == null) {
                        Log.e("MovieRepository", "No local data available and remote fetch failed")
                        emit(null)
                    }
                }
            } else {
                Log.d("MovieRepository", "No internet connection available")
                if (localMovie == null) {
                    Log.e("MovieRepository", "No local data available and no internet connection")
                    emit(null)
                }
            }
        } catch (e: Exception) {
            Log.e("MovieRepository", "Error in getMovieById", e)
            emit(null)
        }
    }

    override suspend fun addReview(movieId: String, review: Review) {
        val movieReviews = reviews.getOrPut(movieId) { mutableListOf() }
        movieReviews.add(review)
    }

    private fun MovieDTO.toMovie() = Movie(
        id = id.toString(),
        title = title,
        overview = overview,
        posterPath = "https://image.tmdb.org/t/p/w500$poster_path",
        releaseDate = release_date,
        voteAverage = vote_average
    )

    private fun MovieDetailsResponse.toMovie() = Movie(
        id = id.toString(),
        title = title,
        overview = overview,
        posterPath = "https://image.tmdb.org/t/p/w500$poster_path",
        releaseDate = release_date,
        voteAverage = vote_average,
        runtime = runtime,
        genres = genres.map { apiGenre -> 
            Genre(
                id = apiGenre.id,
                name = apiGenre.name
            )
        },
        credits = credits?.let { apiCredits ->
            Credits(
                cast = apiCredits.cast.map { apiCast ->
                    CastMember(
                        id = apiCast.id,
                        name = apiCast.name,
                        character = apiCast.character,
                        profilePath = apiCast.profile_path
                    )
                },
                crew = apiCredits.crew.map { apiCrew ->
                    CrewMember(
                        id = apiCrew.id,
                        name = apiCrew.name,
                        job = apiCrew.job,
                        profilePath = apiCrew.profile_path
                    )
                }
            )
        }
    )

    private fun MovieEntity.toMovie() = Movie(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        releaseDate = releaseDate,
        voteAverage = voteAverage
    )

    private fun Movie.toMovieEntity() = MovieEntity(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
        page = 0 // Default page for single movie
    )

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && (
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        )
    }
} 