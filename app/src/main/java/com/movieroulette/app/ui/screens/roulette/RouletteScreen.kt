package com.movieroulette.app.ui.screens.roulette

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.movieroulette.app.R
import com.movieroulette.app.data.model.MovieWithDetails
import com.movieroulette.app.data.model.toPosterUrl
import com.movieroulette.app.ui.components.PrimaryButton
import com.movieroulette.app.ui.components.SecondaryButton
import com.movieroulette.app.viewmodel.MovieViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouletteScreen(
    navController: NavController,
    groupId: String,
    viewModel: MovieViewModel = viewModel()
) {
    var isSpinning by remember { mutableStateOf(false) }
    var selectedMovie by remember { mutableStateOf<MovieWithDetails?>(null) }
    var currentMovieIndex by remember { mutableStateOf(0) }
    
    val moviesState by viewModel.moviesState.collectAsState()
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        viewModel.loadGroupMovies(groupId, status = "pending")
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.roulette)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Roulette Display with card sliding animation
            val movies = (moviesState as? MovieViewModel.MoviesState.Success)?.movies ?: emptyList()
            
            Box(
                modifier = Modifier
                    .size(width = 250.dp, height = 370.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = if (isSpinning) currentMovieIndex else -1,
                    transitionSpec = {
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(durationMillis = 150)
                        ) + fadeIn(animationSpec = tween(150)) togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth },
                            animationSpec = tween(durationMillis = 150)
                        ) + fadeOut(animationSpec = tween(150))
                    },
                    label = "card_slide"
                ) { targetIndex ->
                    if (movies.isNotEmpty() && isSpinning && targetIndex >= 0) {
                        // Show cycling movie poster during spin
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            AsyncImage(
                                model = movies[targetIndex % movies.size].toPosterUrl(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else if (selectedMovie != null) {
                        // Show selected movie poster
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            AsyncImage(
                                model = selectedMovie!!.toPosterUrl(),
                                contentDescription = selectedMovie!!.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        // Show placeholder
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Movie,
                                    contentDescription = null,
                                    modifier = Modifier.size(120.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            if (selectedMovie != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.movie_selected),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = selectedMovie!!.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                PrimaryButton(
                    text = stringResource(R.string.watch_now),
                    onClick = {
                        scope.launch {
                            selectedMovie?.let { movie ->
                                viewModel.updateMovieStatus(movie.id, groupId, "watching")
                                navController.navigateUp()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                SecondaryButton(
                    text = stringResource(R.string.spin_again),
                    onClick = {
                        selectedMovie = null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                val movies = (moviesState as? MovieViewModel.MoviesState.Success)?.movies ?: emptyList()
                
                if (movies.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.no_pending_movies),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.add_movies_to_use_roulette),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    PrimaryButton(
                        text = if (isSpinning) stringResource(R.string.spinning) else stringResource(R.string.spin_roulette),
                        onClick = {
                            if (!isSpinning && movies.isNotEmpty()) {
                                isSpinning = true
                                
                                scope.launch {
                                    // Cycle through movie posters - they will slide like cards
                                    repeat(25) { 
                                        currentMovieIndex++
                                        delay(120) // Slightly slower for better card effect
                                    }
                                    
                                    // Select random movie from pending list
                                    val randomMovie = movies.random()
                                    selectedMovie = randomMovie
                                    isSpinning = false
                                    currentMovieIndex = -1 // Reset for next spin
                                }
                            }
                        },
                        enabled = !isSpinning && movies.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
