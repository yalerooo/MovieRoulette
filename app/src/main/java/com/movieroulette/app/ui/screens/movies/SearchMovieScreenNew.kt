package com.movieroulette.app.ui.screens.movies

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.movieroulette.app.R
import com.movieroulette.app.data.model.TMDBGenre
import com.movieroulette.app.data.model.TMDBMovie
import com.movieroulette.app.ui.components.*
import com.movieroulette.app.viewmodel.MovieViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchMovieScreenNew(
    navController: NavController,
    groupId: String,
    viewModel: MovieViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMovie by remember { mutableStateOf<TMDBMovie?>(null) }
    var showMovieDialog by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    
    val searchState by viewModel.searchState.collectAsState()
    val discoverState by viewModel.discoverState.collectAsState()
    val genresState by viewModel.genresState.collectAsState()
    val addMovieState by viewModel.addMovieState.collectAsState()
    val creditsState by viewModel.movieCreditsState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Filtros
    var selectedSortBy by remember { mutableStateOf("popularity.desc") }
    var selectedGenres by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var selectedYear by remember { mutableStateOf<Int?>(null) }
    var selectedMinRating by remember { mutableDoubleStateOf(0.0) }
    
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    // Cargar géneros y películas populares al inicio
    LaunchedEffect(Unit) {
        viewModel.loadGenres()
        viewModel.discoverMovies(MovieViewModel.MovieFilters())
    }

    // Detectar scroll al final para cargar más
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                val state = discoverState
                if (state is MovieViewModel.DiscoverState.Success && state.hasMore) {
                    val totalItems = state.movies.size
                    if (lastVisibleIndex != null && lastVisibleIndex >= totalItems - 4) {
                        viewModel.loadMoreMovies()
                    }
                }
            }
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
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        val hasActiveFilters = selectedGenres.isNotEmpty() || selectedYear != null || selectedMinRating > 0
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filtros",
                            tint = if (hasActiveFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
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
            // Barra de búsqueda
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    if (it.isNotBlank()) {
                        viewModel.searchMovies(it)
                    } else {
                        // Volver a discover cuando se borra la búsqueda
                        viewModel.discoverMovies(
                            MovieViewModel.MovieFilters(
                                sortBy = selectedSortBy,
                                genres = selectedGenres.takeIf { it.isNotEmpty() }?.toList(),
                                year = selectedYear,
                                minRating = selectedMinRating.takeIf { it > 0 }
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.search_movie_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            viewModel.discoverMovies(
                                MovieViewModel.MovieFilters(
                                    sortBy = selectedSortBy,
                                    genres = selectedGenres.takeIf { it.isNotEmpty() }?.toList(),
                                    year = selectedYear,
                                    minRating = selectedMinRating.takeIf { it > 0 }
                                )
                            )
                        }) {
                            Icon(Icons.Default.Clear, "Limpiar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Panel de filtros con animación
            AnimatedVisibility(
                visible = showFilters,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                FiltersPanel(
                    genresState = genresState,
                    selectedSortBy = selectedSortBy,
                    selectedGenres = selectedGenres,
                    selectedYear = selectedYear,
                    selectedMinRating = selectedMinRating,
                    onSortByChanged = { selectedSortBy = it },
                    onGenresChanged = { selectedGenres = it },
                    onYearChanged = { selectedYear = it },
                    onMinRatingChanged = { selectedMinRating = it },
                    onApplyFilters = {
                        scope.launch {
                            viewModel.discoverMovies(
                                MovieViewModel.MovieFilters(
                                    sortBy = selectedSortBy,
                                    genres = selectedGenres.takeIf { it.isNotEmpty() }?.toList(),
                                    year = selectedYear,
                                    minRating = selectedMinRating.takeIf { it > 0 }
                                )
                            )
                            showFilters = false
                        }
                    },
                    onClearFilters = {
                        selectedSortBy = "popularity.desc"
                        selectedGenres = emptySet()
                        selectedYear = null
                        selectedMinRating = 0.0
                        viewModel.discoverMovies(MovieViewModel.MovieFilters())
                        showFilters = false
                    }
                )
            }

            // Resultados
            val currentState = if (searchQuery.isBlank()) discoverState else searchState
            
            when (currentState) {
                is MovieViewModel.SearchState.Loading, 
                is MovieViewModel.DiscoverState.Loading -> LoadingScreen()
                
                is MovieViewModel.SearchState.Empty,
                is MovieViewModel.DiscoverState.Empty -> {
                    EmptyState(
                        title = stringResource(R.string.no_results_title),
                        message = stringResource(R.string.try_another_title),
                        showIcon = false
                    )
                }
                
                is MovieViewModel.SearchState.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = gridState,
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(
                            items = currentState.movies,
                            key = { index, movie -> "${movie.id}_$index" }
                        ) { index, movie ->
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
                
                is MovieViewModel.DiscoverState.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = gridState,
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(
                            items = currentState.movies,
                            key = { index, movie -> "${movie.id}_$index" }
                        ) { index, movie ->
                            MoviePosterCard(
                                movie = movie,
                                onClick = {
                                    selectedMovie = movie
                                    showMovieDialog = true
                                },
                                isLoading = false
                            )
                        }
                        
                        // Loading indicator al final
                        if (currentState.hasMore) {
                            item(
                                key = "loading_indicator",
                                span = { GridItemSpan(2) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                    }
                }
                
                else -> {}
            }
        }

        // Diálogo de detalle de película
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FiltersPanel(
    genresState: MovieViewModel.GenresState,
    selectedSortBy: String,
    selectedGenres: Set<Int>,
    selectedYear: Int?,
    selectedMinRating: Double,
    onSortByChanged: (String) -> Unit,
    onGenresChanged: (Set<Int>) -> Unit,
    onYearChanged: (Int?) -> Unit,
    onMinRatingChanged: (Double) -> Unit,
    onApplyFilters: () -> Unit,
    onClearFilters: () -> Unit
) {
    var showYearPicker by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Contenido scrolleable
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filtros",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onClearFilters) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Limpiar")
                    }
                }

                // Ordenar por
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Sort,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Ordenar por",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SortChip("Popularidad", "popularity.desc", selectedSortBy, onSortByChanged)
                        SortChip("Mejor valoradas", "vote_average.desc", selectedSortBy, onSortByChanged)
                        SortChip("Más recientes", "release_date.desc", selectedSortBy, onSortByChanged)
                        SortChip("Título", "title.asc", selectedSortBy, onSortByChanged)
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // Año y Valoración en la misma fila
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Año
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Año",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        OutlinedButton(
                            onClick = { showYearPicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            colors = if (selectedYear != null) {
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else {
                                ButtonDefaults.outlinedButtonColors()
                            },
                            border = if (selectedYear != null) {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                ButtonDefaults.outlinedButtonBorder
                            }
                        ) {
                            Text(
                                text = selectedYear?.toString() ?: "Todos",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            if (selectedYear != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Limpiar",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onYearChanged(null) }
                                )
                            }
                        }
                        
                        if (showYearPicker) {
                            YearPickerDialog(
                                selectedYear = selectedYear,
                                onYearSelected = { 
                                    onYearChanged(it)
                                    showYearPicker = false
                                },
                                onDismiss = { showYearPicker = false }
                            )
                        }
                    }

                    // Rating mínimo
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Rating",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (selectedMinRating > 0) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = String.format("%.1f+", selectedMinRating),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                        
                        Slider(
                            value = selectedMinRating.toFloat(),
                            onValueChange = { onMinRatingChanged(it.toDouble()) },
                            valueRange = 0f..9f,
                            steps = 17,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("9+", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // Géneros
                if (genresState is MovieViewModel.GenresState.Success) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Movie,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Géneros",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (selectedGenres.isNotEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "${selectedGenres.size}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            genresState.genres.forEach { genre ->
                                GenreChip(
                                    genre = genre,
                                    isSelected = selectedGenres.contains(genre.id),
                                    onToggle = {
                                        onGenresChanged(
                                            if (selectedGenres.contains(genre.id)) {
                                                selectedGenres - genre.id
                                            } else {
                                                selectedGenres + genre.id
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // Botón aplicar (fijo en la parte inferior)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Button(
                    onClick = onApplyFilters,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Aplicar filtros",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun YearPickerDialog(
    selectedYear: Int?,
    onYearSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val currentYear = 2025
    val years = (1900..currentYear).toList().reversed()
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedYear?.let { years.indexOf(it) } ?: 0
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selecciona un año") },
        text = {
            LazyColumn(
                state = listState,
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(years.size) { index ->
                    val year = years[index]
                    TextButton(
                        onClick = { onYearSelected(year) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = year.toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (year == selectedYear) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (year == selectedYear) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun SortChip(
    label: String,
    value: String,
    selectedValue: String,
    onSelect: (String) -> Unit
) {
    FilterChip(
        selected = selectedValue == value,
        onClick = { onSelect(value) },
        label = { Text(label) },
        leadingIcon = if (selectedValue == value) {
            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
fun GenreChip(
    genre: TMDBGenre,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onToggle,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        label = { Text(genre.name) },
        leadingIcon = if (isSelected) {
            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
        } else null
    )
}
