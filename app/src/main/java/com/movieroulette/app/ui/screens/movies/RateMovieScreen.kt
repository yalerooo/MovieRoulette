package com.movieroulette.app.ui.screens.movies

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.movieroulette.app.data.model.toPosterUrl
import com.movieroulette.app.ui.components.PrimaryButton
import com.movieroulette.app.utils.NotificationHelper
import com.movieroulette.app.viewmodel.MovieViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateMovieScreen(
    navController: NavController,
    movieId: String,
    groupId: String,
    viewModel: MovieViewModel = viewModel()
) {
    var rating by remember { mutableStateOf(5f) } // Start at 5 (middle)
    var comment by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val moviesState by viewModel.moviesState.collectAsState()
    val context = LocalContext.current
    var hasWatchedMovie by remember { mutableStateOf<Boolean?>(null) }
    var currentUserId by remember { mutableStateOf<String?>(null) }
    
    // Verificar si el usuario vio la película - recargar cuando cambie movieId
    LaunchedEffect(movieId) {
        viewModel.loadGroupMovies(groupId, null)
        currentUserId = viewModel.movieRepository.getCurrentUserId()
        if (currentUserId != null) {
            val result = viewModel.movieRepository.hasUserWatchedMovie(movieId, currentUserId!!)
            hasWatchedMovie = result.getOrNull() ?: false
        } else {
            hasWatchedMovie = false
        }
    }
    
    val movie = (moviesState as? MovieViewModel.MoviesState.Success)?.movies?.find { it.id == movieId }
    
    // Si el usuario no vio la película, mostrar mensaje y no permitir puntuar
    if (hasWatchedMovie == false) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(com.movieroulette.app.R.string.rate_movie)) },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, stringResource(com.movieroulette.app.R.string.back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "No puedes puntuar esta película",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Solo los usuarios que vieron la película pueden puntuarla",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { navController.navigateUp() }) {
                        Text("Volver")
                    }
                }
            }
        }
        return
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(com.movieroulette.app.R.string.rate_movie)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, stringResource(com.movieroulette.app.R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            movie?.let {
                // Movie Poster with shadow/glow
                Box(
                    modifier = Modifier
                        .height(280.dp)
                        .width(190.dp)
                        .padding(bottom = 16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        AsyncImage(
                            model = it.toPosterUrl(),
                            contentDescription = it.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                
                // Movie Title
                Text(
                    text = it.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Rating Display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(com.movieroulette.app.R.string.your_rating),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format("%.1f", rating),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "/10",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Bottom).padding(bottom = 6.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Slider
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Slider(
                        value = rating,
                        onValueChange = { rating = it },
                        valueRange = 0f..10f,
                        steps = 19, // 0.5 increments (20 intervals - 1)
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "5",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "10",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Comment
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text(stringResource(com.movieroulette.app.R.string.comment_optional)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Save Button
                PrimaryButton(
                    text = stringResource(com.movieroulette.app.R.string.save_rating),
                    onClick = {
                        scope.launch {
                            viewModel.addRating(movieId, rating.toDouble(), comment.ifBlank { null })
                            // Cancelar notificación
                            NotificationHelper.cancelRatingNotification(context, movieId)
                            // Reload movies in all states to update the average rating
                            kotlinx.coroutines.delay(100)
                            viewModel.loadGroupMovies(groupId, "pending")
                            viewModel.loadGroupMovies(groupId, "watching")
                            viewModel.loadGroupMovies(groupId, "watched")
                            navController.navigateUp()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
