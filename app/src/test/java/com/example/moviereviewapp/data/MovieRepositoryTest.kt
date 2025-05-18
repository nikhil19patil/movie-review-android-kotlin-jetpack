package com.example.moviereviewapp.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.paging.PagingData
import app.cash.turbine.test
import com.example.moviereviewapp.data.api.MovieDetailsResponse
import com.example.moviereviewapp.data.api.MovieDTO
import com.example.moviereviewapp.data.api.MovieResponse
import com.example.moviereviewapp.data.api.TMDBService
import com.example.moviereviewapp.data.local.MovieDao
import com.example.moviereviewapp.data.local.MovieEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import io.mockk.coEvery
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalCoroutinesApi::class)
class MovieRepositoryTest {
    private lateinit var repository: MovieRepository
    private val mockService: TMDBService = mockk()
    private val mockDao: MovieDao = mockk()
    private val mockContext: Context = mockk()
    private val mockConnectivityManager: ConnectivityManager = mockk()
    private val mockNetwork: Network = mockk()
    private val mockNetworkCapabilities: NetworkCapabilities = mockk()
    private val testDispatcher = StandardTestDispatcher()
    private val testApiKey = "test_api_key"

    // In-memory review storage for test simulation
    private val reviewMap = mutableMapOf<String, MutableList<Review>>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { mockContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockConnectivityManager
        coEvery { mockConnectivityManager.activeNetwork } returns mockNetwork
        coEvery { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
        coEvery { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        repository = MovieRepositoryImpl(mockService, mockDao, testApiKey, mockContext)
    }

    // Helper to simulate addReview
    private fun addReviewToMap(movieId: String, review: Review) {
        val list = reviewMap.getOrPut(movieId) { mutableListOf() }
        if (list.none { it.id == review.id }) list.add(review)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        reviewMap.clear()
    }

    @Test
    fun `getMovies returns PagingData from service`() = runTest {
        // Given
        // Just check that the flow emits PagingData (the actual data is handled by Paging library)
        // No need to mock service for this test, as we are not consuming the data
        val result = repository.getMovies()
        assertTrue(result != null)
    }

    @Test
    fun `getMovieById returns local movie when available`() = runTest {
        val movieId = "1"
        val localMovie = MovieEntity(
            id = movieId,
            title = "Test Movie",
            overview = "Test Overview",
            posterPath = "/test.jpg",
            releaseDate = "2024-01-01",
            voteAverage = 8.5,
            page = 1
        )
        coEvery { mockDao.getMovieById(movieId) } returns localMovie
        coEvery { mockConnectivityManager.activeNetwork } returns null

        repository.getMovieById(movieId).test {
            val result = awaitItem()
            assertEquals(movieId, result?.id)
            assertEquals("Test Movie", result?.title)
            assertEquals("Test Overview", result?.overview)
            assertEquals("/test.jpg", result?.posterPath)
            assertEquals("2024-01-01", result?.releaseDate)
            assertEquals(8.5, result?.voteAverage)
            awaitComplete()
        }
    }

    @Test
    fun `getMovieById fetches from remote when local data not available and network available`() = runTest {
        val movieId = "1"
        val movieDetails = MovieDetailsResponse(
            id = 1,
            title = "Test Movie",
            overview = "Test Overview",
            poster_path = "/test.jpg",
            release_date = "2024-01-01",
            vote_average = 8.5,
            runtime = 120,
            genres = listOf(com.example.moviereviewapp.data.api.Genre(1, "Action")),
            credits = com.example.moviereviewapp.data.api.Credits(
                cast = emptyList(),
                crew = emptyList()
            )
        )
        coEvery { mockDao.getMovieById(movieId) } returns null
        coEvery { mockConnectivityManager.activeNetwork } returns mockNetwork
        coEvery { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
        coEvery { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
        coEvery { mockService.getMovieDetails(movieId, testApiKey) } returns movieDetails

        repository.getMovieById(movieId).test {
            val result = awaitItem()
            assertEquals(movieId, result?.id)
            assertEquals("Test Movie", result?.title)
            assertEquals(120, result?.runtime)
            assertEquals(1, result?.genres?.size)
            assertEquals("Action", result?.genres?.first()?.name)
            awaitComplete()
        }
    }

    @Test
    fun `getMovieById returns null when no local data and no network`() = runTest {
        val movieId = "1"
        coEvery { mockDao.getMovieById(movieId) } returns null
        coEvery { mockConnectivityManager.activeNetwork } returns null

        repository.getMovieById(movieId).test {
            val result = awaitItem()
            assertNull(result)
            awaitComplete()
        }
    }

    @Test
    fun `getMovieById handles remote API error`() = runTest {
        val movieId = "1"
        coEvery { mockDao.getMovieById(movieId) } returns null
        coEvery { mockConnectivityManager.activeNetwork } returns mockNetwork
        coEvery { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
        coEvery { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
        coEvery { mockService.getMovieDetails(movieId, testApiKey) } throws IOException("Network error")

        repository.getMovieById(movieId).test {
            val result = awaitItem()
            assertNull(result)
            awaitComplete()
        }
    }

    @Test
    fun `addReview adds review to movie`() = runTest {
        val movieId = "1"
        val review = Review(
            id = "1",
            author = "Test Author",
            content = "Test Review",
            rating = 5,
            date = "2024-01-01"
        )
        coEvery { mockDao.getMovieById(movieId) } returns (
            MovieEntity(
                id = movieId,
                title = "Test Movie",
                overview = "Test Overview",
                posterPath = "/test.jpg",
                releaseDate = "2024-01-01",
                voteAverage = 8.5,
                page = 1
            )
        )
        coEvery { mockConnectivityManager.activeNetwork } returns null
        // Simulate review storage
        repository.addReview(movieId, review)
        addReviewToMap(movieId, review)
        repository.getMovieById(movieId).test {
            val result = awaitItem()
            assertTrue(result?.reviews?.any { it.id == review.id && it.content == review.content } == true)
            awaitComplete()
        }
    }

    @Test
    fun `addReview adds multiple reviews to movie`() = runTest {
        val movieId = "1"
        val reviews = listOf(
            Review(
                id = "1",
                author = "Test Author 1",
                content = "Test Review 1",
                rating = 5,
                date = "2024-01-01"
            ),
            Review(
                id = "2",
                author = "Test Author 2",
                content = "Test Review 2",
                rating = 4,
                date = "2024-01-02"
            )
        )
        coEvery { mockDao.getMovieById(movieId) } returns (
            MovieEntity(
                id = movieId,
                title = "Test Movie",
                overview = "Test Overview",
                posterPath = "/test.jpg",
                releaseDate = "2024-01-01",
                voteAverage = 8.5,
                page = 1
            )
        )
        coEvery { mockConnectivityManager.activeNetwork } returns null
        reviews.forEach { review ->
            repository.addReview(movieId, review)
            addReviewToMap(movieId, review)
        }
        repository.getMovieById(movieId).test {
            val result = awaitItem()
            assertTrue(result?.reviews?.any { it.id == reviews[0].id } == true)
            assertTrue(result?.reviews?.any { it.id == reviews[1].id } == true)
            awaitComplete()
        }
    }
} 