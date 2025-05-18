package com.example.moviereviewapp.data.paging

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.moviereviewapp.data.Movie
import com.example.moviereviewapp.data.api.TMDBService
import com.example.moviereviewapp.data.local.MovieDao
import com.example.moviereviewapp.data.local.MovieEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MoviePagingSource @Inject constructor(
    private val tmdbService: TMDBService,
    private val movieDao: MovieDao,
    private val apiKey: String,
    private val context: Context
) : PagingSource<Int, Movie>() {

    override val keyReuseSupported: Boolean = true

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> {
        return try {
            val page = params.key ?: 1
            Log.d("MoviePagingSource", "Loading page $page")

            if (!isNetworkAvailable()) {
                Log.e("MoviePagingSource", "No network connection available")
                throw Exception("No network connection available")
            }

            val response = withContext(Dispatchers.IO) {
                tmdbService.getPopularMovies(apiKey, page)
            }

            Log.d("MoviePagingSource", "Received response: total_pages=${response.total_pages}, total_results=${response.total_results}")

            if (response.results.isEmpty()) {
                Log.d("MoviePagingSource", "No results found for page $page")
                return LoadResult.Page(
                    data = emptyList(),
                    prevKey = if (page == 1) null else page - 1,
                    nextKey = null
                )
            }

            // Convert DTOs to domain models
            val movies = response.results.map { dto ->
                Movie(
                    id = dto.id.toString(),
                    title = dto.title,
                    overview = dto.overview,
                    posterPath = "https://image.tmdb.org/t/p/w500${dto.poster_path}",
                    releaseDate = dto.release_date,
                    voteAverage = dto.vote_average
                )
            }

            // Cache movies in local database
            withContext(Dispatchers.IO) {
                movieDao.insertMovies(movies.map { movie ->
                    MovieEntity(
                        id = movie.id,
                        title = movie.title,
                        overview = movie.overview,
                        posterPath = movie.posterPath,
                        releaseDate = movie.releaseDate,
                        voteAverage = movie.voteAverage,
                        page = page
                    )
                })
            }

            LoadResult.Page(
                data = movies,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (page >= response.total_pages) null else page + 1
            )
        } catch (e: Exception) {
            Log.e("MoviePagingSource", "Error loading movies", e)
            
            // Try to load from local database
            try {
                val localMovies = withContext(Dispatchers.IO) {
                    movieDao.getMovies().load(
                        PagingSource.LoadParams.Refresh(
                            key = params.key ?: 1,
                            loadSize = params.loadSize,
                            placeholdersEnabled = false
                        )
                    )
                }
                
                when (localMovies) {
                    is LoadResult.Page -> {
                        Log.d("MoviePagingSource", "Loaded ${localMovies.data.size} movies from local database")
                        return LoadResult.Page(
                            data = localMovies.data.map { entity ->
                                Movie(
                                    id = entity.id,
                                    title = entity.title,
                                    overview = entity.overview,
                                    posterPath = entity.posterPath,
                                    releaseDate = entity.releaseDate,
                                    voteAverage = entity.voteAverage
                                )
                            },
                            prevKey = localMovies.prevKey,
                            nextKey = localMovies.nextKey
                        )
                    }
                    is LoadResult.Error -> throw localMovies.throwable
                    else -> LoadResult.Error(Exception("No data available"))
                }
            } catch (dbError: Exception) {
                Log.e("MoviePagingSource", "Error loading from local database", dbError)
                LoadResult.Error(e)
            }
        }
    }
} 