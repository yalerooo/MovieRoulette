package com.movieroulette.app.ui.screens.movies

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
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
    val searchState by viewModel.searchState.collectAsState()
    val addMovieState by viewModel.addMovieState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(Unit) {
        viewModel.loadPopularMovies()
    }
    
    LaunchedEffect(addMovieState) {
        when (addMovieState) {
            is MovieViewModel.AddMovieState.Success -> {
                navController.navigateUp()
                viewModel.resetAddMovieState()
            }
            is MovieViewModel.AddMovieState.Error -> {
                snackbarHostState.showSnackbar(
                    message = (addMovieState as MovieViewModel.AddMovieState.Error).message,
                    duration = SnackbarDuration.Short
                )
                viewModel.resetAddMovieState()
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buscar Película") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
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
                placeholder = { Text("Buscar película...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            // Results
            when (val state = searchState) {
                is MovieViewModel.SearchState.Loading -> LoadingScreen()
                is MovieViewModel.SearchState.Empty -> {
                    EmptyState(
                        title = "No hay resultados",
                        message = "Intenta con otro título",
                        emoji = "🔍"
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
                                onClick = { viewModel.addMovieToGroup(groupId, movie.id) },
                                isLoading = addMovieState is MovieViewModel.AddMovieState.Loading
                            )
                        }
                    }
                }
                else -> {}
            }
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
