package com.example.moviereviewapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.moviereviewapp.ui.MovieViewModel
import com.example.moviereviewapp.ui.components.ReviewCard
import com.example.moviereviewapp.ui.components.ReviewDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    movieId: String,
    onBackClick: () -> Unit,
    viewModel: MovieViewModel = hiltViewModel()
) {
    val movie by viewModel.selectedMovie.collectAsState()
    var showReviewDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(movieId) {
        isLoading = true
        error = null
        viewModel.selectMovie(movieId)
    }

    LaunchedEffect(movie) {
        isLoading = false
        if (movie == null) {
            error = "Unable to load movie details. Please check your internet connection."
        } else {
            error = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(movie?.title ?: "Movie Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = error ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.selectMovie(movieId) }) {
                            Text("Retry")
                        }
                    }
                }
                movie != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AsyncImage(
                            model = movie?.posterPath,
                            contentDescription = movie?.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                            contentScale = ContentScale.Fit
                        )

                        Text(
                            text = movie?.title ?: "",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        // Display genres
                        movie?.genres?.let { genres ->
                            if (genres.isNotEmpty()) {
                                Text(
                                    text = "Genres: ${genres.joinToString { it.name }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }

                        // Display runtime
                        movie?.runtime?.let { runtime ->
                            Text(
                                text = "Runtime: ${runtime} minutes",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Text(
                            text = movie?.overview ?: "",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        // Display cast
                        movie?.credits?.cast?.take(5)?.let { cast ->
                            if (cast.isNotEmpty()) {
                                Text(
                                    text = "Cast:",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                cast.forEach { actor ->
                                    Text(
                                        text = "• ${actor.name} as ${actor.character}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Release Date: ${movie?.releaseDate}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Rating: ${movie?.voteAverage}/10",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Reviews",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Button(onClick = { showReviewDialog = true }) {
                                Text("Add Review")
                            }
                        }

                        movie?.reviews?.forEach { review ->
                            ReviewCard(review = review)
                        }
                    }
                }
            }
        }

        if (showReviewDialog) {
            ReviewDialog(
                onDismiss = { showReviewDialog = false },
                onReviewSubmitted = { review ->
                    viewModel.addReview(movieId, review)
                    showReviewDialog = false
                }
            )
        }
    }
}

