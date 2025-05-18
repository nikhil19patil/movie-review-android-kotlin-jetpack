package com.example.moviereviewapp.data.paging

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.moviereviewapp.data.Movie
import com.example.moviereviewapp.data.api.MovieDTO
import com.example.moviereviewapp.data.api.MovieResponse
import com.example.moviereviewapp.data.api.TMDBService
import com.example.moviereviewapp.data.local.MovieDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import io.mockk.coEvery
import io.mockk.mockk

@OptIn(ExperimentalCoroutinesApi::class)
class MoviePagingSourceTest {
    private lateinit var pagingSource: MoviePagingSource
    private val mockTMDBService: TMDBService = mockk()
    private val mockMovieDao: MovieDao = mockk()
    private val mockContext: Context = mockk()
    private val mockConnectivityManager: ConnectivityManager = mockk()
    private val mockNetwork: Network = mockk()
    private val mockNetworkCapabilities: NetworkCapabilities = mockk()
    private val testApiKey = "test_api_key"

    @Before
    fun setup() {
        coEvery { mockContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockConnectivityManager
        coEvery { mockConnectivityManager.activeNetwork } returns mockNetwork
        coEvery { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
        coEvery { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        pagingSource = MoviePagingSource(mockTMDBService, mockMovieDao, testApiKey, mockContext)
    }

    @Test
    fun `load returns success when API call succeeds`() = runTest {
        // Given
        val movies = listOf(
            MovieDTO(
                id = 1,
                title = "Test Movie 1",
                overview = "Overview 1",
                poster_path = "/test1.jpg",
                release_date = "2024-01-01",
                vote_average = 8.5
            ),
            MovieDTO(
                id = 2,
                title = "Test Movie 2",
                overview = "Overview 2",
                poster_path = "/test2.jpg",
                release_date = "2024-01-02",
                vote_average = 7.5
            )
        )
        val response = MovieResponse(
            page = 1,
            results = movies,
            total_pages = 2,
            total_results = 2
        )
        coEvery { mockTMDBService.getPopularMovies(testApiKey, 1) } returns response

        // When
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = 1,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        // Then
        assertTrue(result is PagingSource.LoadResult.Page)
        val pageResult = result as PagingSource.LoadResult.Page
        assertEquals(2, pageResult.data.size)
        assertEquals(null, pageResult.prevKey)
        assertEquals(2, pageResult.nextKey)
    }

    @Test
    fun `load returns error when API call fails`() = runTest {
        // Given
        val exception = RuntimeException("API Error")
        coEvery { mockTMDBService.getPopularMovies(testApiKey, 1) } throws exception

        // When
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = 1,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        // Then
        assertTrue(result is PagingSource.LoadResult.Error)
        val errorResult = result as PagingSource.LoadResult.Error
        assertEquals(exception, errorResult.throwable)
    }

    @Test
    fun `load returns empty page when API returns empty list`() = runTest {
        // Given
        val response = MovieResponse(
            page = 1,
            results = emptyList(),
            total_pages = 1,
            total_results = 0
        )
        coEvery { mockTMDBService.getPopularMovies(testApiKey, 1) } returns response

        // When
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = 1,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        // Then
        assertTrue(result is PagingSource.LoadResult.Page)
        val pageResult = result as PagingSource.LoadResult.Page
        assertTrue(pageResult.data.isEmpty())
        assertNull(pageResult.prevKey)
        assertNull(pageResult.nextKey)
    }

    @Test
    fun `load returns error when API returns null response`() = runTest {
        // Given
        coEvery { mockTMDBService.getPopularMovies(testApiKey, 1) } returns MovieResponse(
            page = 1,
            results = emptyList(),
            total_pages = 1,
            total_results = 0
        )

        // When
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = 1,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        // Then
        assertTrue(result is PagingSource.LoadResult.Error)
        val errorResult = result as PagingSource.LoadResult.Error
        assertTrue(errorResult.throwable is NullPointerException)
    }

    @Test
    fun `load handles network error`() = runTest {
        // Given
        // Simulate no network
        coEvery { mockConnectivityManager.activeNetwork } returns null

        // When
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = 1,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        // Then
        assertTrue(result is PagingSource.LoadResult.Error)
        val errorResult = result as PagingSource.LoadResult.Error
        assertTrue(errorResult.throwable is Exception)
        assertEquals("No network connection available", errorResult.throwable.message)
    }

    @Test
    fun `load handles invalid page number`() = runTest {
        // Given
        val response = MovieResponse(
            page = 0,
            results = emptyList(),
            total_pages = 1,
            total_results = 0
        )
        coEvery { mockTMDBService.getPopularMovies(testApiKey, 0) } returns response

        // When
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = 0,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        // Then
        assertTrue(result is PagingSource.LoadResult.Error)
        val errorResult = result as PagingSource.LoadResult.Error
        assertTrue(errorResult.throwable is IllegalArgumentException)
    }

    @Test
    fun `getRefreshKey returns null for empty state`() {
        // Given
        val state = PagingState<Int, Movie>(
            pages = emptyList(),
            anchorPosition = null,
            config = PagingConfig(20),
            leadingPlaceholderCount = 0
        )

        // When
        val result = pagingSource.getRefreshKey(state)

        // Then
        assertNull(result)
    }

    @Test
    fun `getRefreshKey returns null for invalid anchor position`() {
        // Given
        val state = PagingState<Int, Movie>(
            pages = listOf(
                PagingSource.LoadResult.Page(
                    data = listOf(
                        Movie(
                            id = "1",
                            title = "Test Movie",
                            overview = "Test Overview",
                            posterPath = "/test.jpg",
                            releaseDate = "2024-01-01",
                            voteAverage = 8.5
                        )
                    ),
                    prevKey = null,
                    nextKey = 2
                )
            ),
            anchorPosition = -1,
            config = PagingConfig(20),
            leadingPlaceholderCount = 0
        )

        // When
        val result = pagingSource.getRefreshKey(state)

        // Then
        assertNull(result)
    }
} 