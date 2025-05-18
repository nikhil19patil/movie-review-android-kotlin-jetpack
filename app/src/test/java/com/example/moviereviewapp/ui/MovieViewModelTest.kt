package com.example.moviereviewapp.ui

import androidx.paging.PagingData
import app.cash.turbine.test
import com.example.moviereviewapp.data.Movie
import com.example.moviereviewapp.data.MovieRepository
import com.example.moviereviewapp.data.Review
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import io.mockk.coVerify
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MovieViewModelTest {
    private lateinit var viewModel: MovieViewModel
    private val mockRepository: MovieRepository = mockk()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { mockRepository.getMovies() } returns flowOf(PagingData.empty())
        viewModel = MovieViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `movies state is initialized with empty PagingData`() = runTest {
        coEvery { mockRepository.getMovies() } returns flowOf(PagingData.empty())
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.movies.test {
            val result = awaitItem()
            assertTrue(result is PagingData<*>)
            awaitComplete()
        }
    }

    @Test
    fun `selectMovie updates selectedMovie state with loading`() = runTest {
        val movieId = "1"
        val movie = Movie(
            id = movieId,
            title = "Test Movie",
            overview = "Test Overview",
            posterPath = "/test.jpg",
            releaseDate = "2024-01-01",
            voteAverage = 8.5
        )
        coEvery { mockRepository.getMovieById(movieId) } returns flowOf(movie)
        viewModel.selectMovie(movieId)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.selectedMovie.test {
            assertNull(awaitItem())
            assertEquals(movie, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `selectMovie handles repository error`() = runTest {
        val movieId = "1"
        coEvery { mockRepository.getMovieById(movieId) } returns flowOf(null)
        viewModel.selectMovie(movieId)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.selectedMovie.test {
            assertNull(awaitItem())
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `clearSelectedMovie clears selectedMovie state`() = runTest {
        val movieId = "1"
        val movie = Movie(
            id = movieId,
            title = "Test Movie",
            overview = "Test Overview",
            posterPath = "/test.jpg",
            releaseDate = "2024-01-01",
            voteAverage = 8.5
        )
        coEvery { mockRepository.getMovieById(movieId) } returns flowOf(movie)
        viewModel.selectMovie(movieId)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.clearSelectedMovie()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.selectedMovie.test {
            val result = awaitItem()
            assertNull(result)
            awaitComplete()
        }
    }

    @Test
    fun `addReview calls repository and updates selectedMovie`() = runTest {
        val movieId = "1"
        val review = Review(
            id = "1",
            author = "Test Author",
            content = "Test Review",
            rating = 5,
            date = "2024-01-01"
        )
        val movie = Movie(
            id = movieId,
            title = "Test Movie",
            overview = "Test Overview",
            posterPath = "/test.jpg",
            releaseDate = "2024-01-01",
            voteAverage = 8.5
        )
        val updatedMovie = movie.copy(reviews = listOf(review))
        coEvery { mockRepository.getMovieById(movieId) } returns flowOf(updatedMovie)
        viewModel.addReview(movieId, review)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { mockRepository.addReview(movieId, review) }
        viewModel.selectedMovie.test {
            val result = awaitItem()
            assertEquals(updatedMovie, result)
            awaitComplete()
        }
    }

    @Test
    fun `addReview handles repository error`() = runTest {
        val movieId = "1"
        val review = Review(
            id = "1",
            author = "Test Author",
            content = "Test Review",
            rating = 5,
            date = "2024-01-01"
        )
        coEvery { mockRepository.getMovieById(movieId) } returns flowOf(null)
        viewModel.addReview(movieId, review)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { mockRepository.addReview(movieId, review) }
        viewModel.selectedMovie.test {
            val result = awaitItem()
            assertNull(result)
            awaitComplete()
        }
    }

    @Test
    fun `selectMovie with empty movieId clears selection`() = runTest {
        viewModel.selectMovie("")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.selectedMovie.test {
            val result = awaitItem()
            assertNull(result)
            awaitComplete()
        }
    }

    @Test
    fun `addReview with empty movieId does nothing`() = runTest {
        val review = Review(
            id = "1",
            author = "Test Author",
            content = "Test Review",
            rating = 5,
            date = "2024-01-01"
        )
        viewModel.addReview("", review)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.selectedMovie.test {
            val result = awaitItem()
            assertNull(result)
            awaitComplete()
        }
    }
} 