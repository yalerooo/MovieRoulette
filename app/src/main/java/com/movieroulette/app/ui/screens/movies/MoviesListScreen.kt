package com.movieroulette.app.ui.screens.movies

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.movieroulette.app.data.model.toPosterUrl
import com.movieroulette.app.ui.components.*
import com.movieroulette.app.ui.navigation.Screen
import com.movieroulette.app.viewmodel.MovieViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesListScreen(
    navController: NavController,
    groupId: String,
    status: String,
    viewModel: MovieViewModel = viewModel()
) {
    val moviesState by viewModel.moviesState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadGroupMovies(groupId, status)
    }
    
    val title = when(status) {
        "pending" -> "En Ruleta"
        "watching" -> "Viendo"
        "watched" -> "Vistas"
        else -> "Películas"
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Atrás")
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
                .padding(paddingValues)
        ) {
            when (val state = moviesState) {
                is MovieViewModel.MoviesState.Loading -> LoadingScreen()
                is MovieViewModel.MoviesState.Empty -> {
                    EmptyState(
                        title = "No hay películas",
                        message = "Añade películas al grupo",
                        emoji = "🎬"
                    )
                }
                is MovieViewModel.MoviesState.Success -> {
                    val scope = rememberCoroutineScope()
                    
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.movies, key = { it.id }) { movie ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Movie Poster
                                    AsyncImage(
                                        model = movie.toPosterUrl(),
                                        contentDescription = movie.title,
                                        modifier = Modifier
                                            .width(80.dp)
                                            .height(120.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    
                                    // Movie Info and Actions
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = movie.title,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        
                                        movie.averageRating?.let {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "⭐ $it/10",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                TextButton(
                                                    onClick = {
                                                        navController.navigate(
                                                            Screen.MovieRatings.createRoute(movie.id, groupId)
                                                        )
                                                    }
                                                ) {
                                                    Text("Ver puntuaciones")
                                                }
                                            }
                                        }
                                        
                                        // Action Buttons based on status
                                        when (status) {
                                            "pending" -> {
                                                Button(
                                                    onClick = {
                                                        scope.launch {
                                                            viewModel.updateMovieStatus(movie.id, groupId, "watching")
                                                            viewModel.loadGroupMovies(groupId, status)
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("Ver Ahora")
                                                }
                                            }
                                            "watching" -> {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            scope.launch {
                                                                viewModel.updateMovieStatus(movie.id, groupId, "watched")
                                                                // Navigate to rate movie screen
                                                                navController.navigate(
                                                                    Screen.RateMovie.createRoute(movie.id, groupId)
                                                                )
                                                            }
                                                        },
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text("✓ Vista")
                                                    }
                                                    OutlinedButton(
                                                        onClick = {
                                                            scope.launch {
                                                                viewModel.updateMovieStatus(movie.id, groupId, "pending")
                                                                viewModel.loadGroupMovies(groupId, status)
                                                            }
                                                        },
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text("← Ruleta")
                                                    }
                                                }
                                            }
                                            "watched" -> {
                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            navController.navigate(
                                                                Screen.RateMovie.createRoute(movie.id, groupId)
                                                            )
                                                        },
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text("⭐ Puntuar")
                                                    }
                                                    OutlinedButton(
                                                        onClick = {
                                                            scope.launch {
                                                                viewModel.updateMovieStatus(movie.id, groupId, "pending")
                                                                viewModel.loadGroupMovies(groupId, status)
                                                            }
                                                        },
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text("Volver a Ruleta")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                is MovieViewModel.MoviesState.Error -> {
                    ErrorView(message = state.message)
                }
            }
        }
    }
}
