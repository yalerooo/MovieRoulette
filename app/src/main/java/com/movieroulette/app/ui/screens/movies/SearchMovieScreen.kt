package com.movieroulette.app.ui.screens.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.movieroulette.app.R
import com.movieroulette.app.data.model.TMDBMovie
import com.movieroulette.app.data.model.toPosterUrl
import com.movieroulette.app.ui.components.*
import com.movieroulette.app.viewmodel.MovieViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchMovieScreen(
    navController: NavController,
    groupId: String,
    viewModel: MovieViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMovie by remember { mutableStateOf<TMDBMovie?>(null) }
    var showMovieDialog by remember { mutableStateOf(false) }
    val searchState by viewModel.searchState.collectAsState()
    val addMovieState by viewModel.addMovieState.collectAsState()
    val creditsState by viewModel.movieCreditsState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadPopularMovies()
    }

    LaunchedEffect(addMovieState) {
        when (addMovieState) {
            is MovieViewModel.AddMovieState.Success -> {
                showMovieDialog = false
                selectedMovie = null
                navController.navigateUp()
                viewModel.resetAddMovieState()
            }
            is MovieViewModel.AddMovieState.Error -> {
                snackbarHostState.showSnackbar(
                    message = (addMovieState as MovieViewModel.AddMovieState.Error).message ?: "",
                    duration = SnackbarDuration.Short
                )
                viewModel.resetAddMovieState()
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_movie)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.searchMovies(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(stringResource(R.string.search_movie_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Results
            when (val state = searchState) {
                is MovieViewModel.SearchState.Loading -> LoadingScreen()
                is MovieViewModel.SearchState.Empty -> {
                    EmptyState(
                        title = stringResource(R.string.no_results_title),
                        message = stringResource(R.string.try_another_title),
                        showIcon = false
                    )
                }
                is MovieViewModel.SearchState.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = state.movies,
                            key = { it.id }
                        ) { movie ->
                            MoviePosterCard(
                                movie = movie,
                                onClick = {
                                    selectedMovie = movie
                                    showMovieDialog = true
                                },
                                isLoading = false
                            )
                        }
                    }
                }
                else -> {}
            }
        }

        // Movie Detail Dialog
        if (showMovieDialog && selectedMovie != null) {
            LaunchedEffect(selectedMovie!!.id) {
                viewModel.loadMovieCredits(selectedMovie!!.id)
            }

            MovieDetailDialog(
                movie = selectedMovie!!,
                onDismiss = {
                    showMovieDialog = false
                    selectedMovie = null
                    viewModel.resetCreditsState()
                },
                onAddMovie = {
                    viewModel.addMovieToGroup(groupId, selectedMovie!!.id)
                },
                isLoading = addMovieState is MovieViewModel.AddMovieState.Loading,
                creditsState = creditsState
            )
        }
    }
}

@Composable
fun MoviePosterCard(
    movie: TMDBMovie,
    onClick: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        onClick = onClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(12.dp)
    ) {
        Box {
            AsyncImage(
                model = movie.toPosterUrl(),
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Loading overlay
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun PersonCard(
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

        // Role (character for actors)
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
fun MovieDetailDialog(
    movie: TMDBMovie,
    onDismiss: () -> Unit,
    onAddMovie: (() -> Unit)? = null,
    isLoading: Boolean = false,
    creditsState: MovieViewModel.CreditsState,
    movieId: String? = null,
    movieViewModel: MovieViewModel? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) { // <-- Este Box es crucial para usar .align
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.90f)
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp), // <-- Ahora .align funciona aquí
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                val scrollState = rememberScrollState()

                // Calculate header height based on scroll - slower collapse
                val maxHeaderHeight = 340.dp
                val minHeaderHeight = 120.dp
                val scrollProgress = (scrollState.value / 500f).coerceIn(0f, 1f)
                val headerHeight = maxHeaderHeight - ((maxHeaderHeight - minHeaderHeight) * scrollProgress)

                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                    // Collapsing Header with backdrop/poster - always on top
                    Box( // <-- Este Box también es necesario para el align de abajo
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(headerHeight)
                    ) {
                        // Use backdrop if available, otherwise poster
                        AsyncImage(
                            model = (movie.backdropPath ?: movie.posterPath)?.let {
                                "https://image.tmdb.org/t/p/w780$it"
                            },
                            contentDescription = movie.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Gradient overlay - stronger and smoother
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(
                                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.3f),
                                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f),
                                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.9f)
                                        )
                                    )
                                )
                        )
                        
                        // Close button - always visible at top right
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(
                                    color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = androidx.compose.ui.graphics.Color.White
                            )
                        }

                        // Title and info - always at bottom of current header
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart) // <-- Ahora .align funciona aquí
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = movie.title,
                                style = MaterialTheme.typography.headlineLarge,
                                color = androidx.compose.ui.graphics.Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            // Quick info row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (movie.voteAverage > 0) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = androidx.compose.ui.graphics.Color(0xFFFFD700),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = String.format("%.1f", movie.voteAverage),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = androidx.compose.ui.graphics.Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (!movie.releaseDate.isNullOrBlank()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = movie.releaseDate.take(4),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f)
                                        )
                                    }
                                }

                                if (movie.voteCount > 0) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ThumbUp,
                                            contentDescription = null,
                                            tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "${movie.voteCount}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Scrollable content below the header
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        ) {
                            // Content card
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 8.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                                    verticalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    // Credits section
                                    when (creditsState) {
                                        is MovieViewModel.CreditsState.Success -> {
                                            val credits = creditsState.credits

                                            // Director
                                            val director = credits.crew.firstOrNull { it.job == "Director" }
                                            if (director != null) {
                                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                    Text(
                                                        text = "Director",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    PersonCard(
                                                        name = director.name,
                                                        profilePath = director.profilePath
                                                    )
                                                }
                                            }

                                            // Main cast
                                            val mainCast = credits.cast.take(6)
                                            if (mainCast.isNotEmpty()) {
                                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                    Text(
                                                        text = "Reparto Principal",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .horizontalScroll(rememberScrollState()),
                                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                                    ) {
                                                        mainCast.forEach { cast ->
                                                            PersonCard(
                                                                name = cast.name,
                                                                role = cast.character,
                                                                profilePath = cast.profilePath
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        is MovieViewModel.CreditsState.Loading -> {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                            }
                                        }
                                        else -> {}
                                    }

                                    // Overview
                                    if (!movie.overview.isNullOrBlank()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Text(
                                                text = stringResource(R.string.overview),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = movie.overview,
                                                style = MaterialTheme.typography.bodyMedium,
                                                lineHeight = MaterialTheme.typography.bodyMedium.fontSize * 1.6,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                            )
                                        }
                                    }

                                    // Bottom spacing for buttons
                                    Spacer(modifier = Modifier.height(if (onAddMovie != null) 90.dp else 20.dp))
                                }
                            }
                        }
                    }
                }
                
                // Bottom buttons - fixed at bottom inside Card
                if (onAddMovie != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = onAddMovie,
                                enabled = !isLoading,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text(stringResource(R.string.add_movie))
                                }
                            }
                        }
                    }
                }
            }
        } // <-- Cierre de Box interno del Card
        } // <-- Cierre de Card
    } // <-- Cierre de Box externo
} // <-- Cierre de Dialog


@Composable
fun InfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.White
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}