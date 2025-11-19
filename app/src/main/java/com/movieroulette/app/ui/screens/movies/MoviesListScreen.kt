package com.movieroulette.app.ui.screens.movies

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.movieroulette.app.data.model.toPosterUrl
import com.movieroulette.app.data.model.TMDBMovie
import com.movieroulette.app.ui.components.*
import com.movieroulette.app.ui.navigation.Screen
import com.movieroulette.app.viewmodel.MovieViewModel
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MoviesListScreen(
    navController: NavController,
    groupId: String,
    status: String,
    viewModel: MovieViewModel = viewModel()
) {
    val moviesState by viewModel.moviesState.collectAsState()
    val creditsState by viewModel.movieCreditsState.collectAsState()
    var selectedMovieTmdbId by remember { mutableStateOf<Int?>(null) }
    var showMovieDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.loadGroupMovies(groupId, status)
    }
    
    val title = when(status) {
        "pending" -> stringResource(com.movieroulette.app.R.string.in_roulette)
        "watching" -> stringResource(com.movieroulette.app.R.string.watching)
        "watched" -> stringResource(com.movieroulette.app.R.string.watched)
        else -> stringResource(com.movieroulette.app.R.string.movies)
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = moviesState) {
                is MovieViewModel.MoviesState.Loading -> LoadingScreen()
                is MovieViewModel.MoviesState.Empty -> {
                    EmptyState(
                        title = stringResource(com.movieroulette.app.R.string.no_movies),
                        message = stringResource(com.movieroulette.app.R.string.add_movies_to_group)
                    )
                }
                is MovieViewModel.MoviesState.Success -> {
                    val scope = rememberCoroutineScope()
                    
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.movies, key = { it.id }) { movie ->
                            ModernMovieCard(
                                movie = movie,
                                status = status,
                                groupId = groupId,
                                navController = navController,
                                viewModel = viewModel,
                                onMovieClick = {
                                    selectedMovieTmdbId = movie.tmdbId
                                    showMovieDialog = true
                                },
                                scope = scope
                            )
                        }
                    }
                }
                is MovieViewModel.MoviesState.Error -> {
                    ErrorView(message = state.message ?: "")
                }
            }
            
            // Movie Detail Dialog
            if (showMovieDialog && selectedMovieTmdbId != null) {
                MovieInfoDialog(
                    tmdbId = selectedMovieTmdbId!!,
                    onDismiss = {
                        showMovieDialog = false
                        selectedMovieTmdbId = null
                        viewModel.resetCreditsState()
                    },
                    viewModel = viewModel,
                    creditsState = creditsState
                )
            }
        }
    }
}

@Composable
fun ModernMovieCard(
    movie: com.movieroulette.app.data.model.MovieWithDetails,
    status: String,
    groupId: String,
    navController: NavController,
    viewModel: MovieViewModel,
    onMovieClick: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onMovieClick),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // Poster con gradiente
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight()
            ) {
                AsyncImage(
                    model = movie.toPosterUrl(),
                    contentDescription = movie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Gradiente overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                ),
                                startY = 0f,
                                endY = 600f
                            )
                        )
                )
                
                // Botón Info sobre el poster
                IconButton(
                    onClick = onMovieClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Ver información",
                        tint = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                CircleShape
                            )
                            .padding(4.dp)
                    )
                }
            }
            
            // Contenido
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Título y rating
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = movie.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    
                    movie.averageRating?.let { rating ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "$rating",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            TextButton(
                                onClick = {
                                    navController.navigate(
                                        Screen.MovieRatings.createRoute(movie.id, groupId)
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = stringResource(com.movieroulette.app.R.string.view_ratings),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
                
                // Botones de acción
                when (status) {
                    "pending" -> {
                        FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    viewModel.updateMovieStatus(movie.id, groupId, "watching")
                                    viewModel.loadGroupMovies(groupId, status)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(com.movieroulette.app.R.string.watch_now))
                        }
                    }
                    
                    "watching" -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    scope.launch {
                                        viewModel.updateMovieStatus(movie.id, groupId, "watched")
                                        // No navegar automáticamente - el usuario puede puntuar cuando quiera
                                        viewModel.loadGroupMovies(groupId, "watching")
                                        viewModel.loadGroupMovies(groupId, "watched")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(com.movieroulette.app.R.string.watched_checkmark),
                                    fontSize = 13.sp
                                )
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
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(com.movieroulette.app.R.string.back_to_roulette),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    
                    "watched" -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    navController.navigate(
                                        Screen.RateMovie.createRoute(movie.id, groupId)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(com.movieroulette.app.R.string.rate))
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
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(com.movieroulette.app.R.string.back_to_roulette_full))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PersonCardLocal(
    name: String,
    role: String? = null,
    profilePath: String?
) {
    Column(
        modifier = Modifier.width(90.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Avatar with subtle border
        Box(
            modifier = Modifier.size(90.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (profilePath != null) {
                        AsyncImage(
                            model = "https://image.tmdb.org/t/p/w185$profilePath",
                            contentDescription = name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
        
        // Name
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            lineHeight = MaterialTheme.typography.bodySmall.fontSize * 1.2
        )
        
        // Role (character name)
        if (role != null) {
            Text(
                text = role,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                lineHeight = MaterialTheme.typography.labelSmall.fontSize * 1.2
            )
        }
    }
}

@Composable
fun MovieInfoDialog(
    tmdbId: Int,
    onDismiss: () -> Unit,
    viewModel: MovieViewModel,
    creditsState: MovieViewModel.CreditsState
) {
    var movieDetails by remember { mutableStateOf<TMDBMovie?>(null) }
    var movieId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(tmdbId) {
        isLoading = true
        viewModel.loadMovieCredits(tmdbId)
        
        // Buscar el movieId en la BD usando tmdbId
        val moviesState = viewModel.moviesState.value
        if (moviesState is MovieViewModel.MoviesState.Success) {
            movieId = moviesState.movies.find { it.tmdbId == tmdbId }?.id
        }
        
        val result = viewModel.movieRepository.getMovieDetails(tmdbId)
        result.onSuccess { details ->
            movieDetails = TMDBMovie(
                id = details.id,
                title = details.title,
                originalTitle = details.originalTitle,
                overview = details.overview,
                posterPath = details.posterPath,
                backdropPath = details.backdropPath,
                releaseDate = details.releaseDate,
                voteAverage = details.voteAverage,
                voteCount = details.voteCount,
                popularity = 0.0,
                genreIds = emptyList(),
                originalLanguage = null
            )
        }
        isLoading = false
    }
    
    // Show loading or the shared MovieDetailDialog
    if (isLoading || movieDetails == null) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    } else {
        MovieDetailDialog(
            movie = movieDetails!!,
            onDismiss = onDismiss,
            onAddMovie = null,
            isLoading = false,
            creditsState = creditsState,
            movieId = movieId,
            movieViewModel = viewModel
        )
    }
}
