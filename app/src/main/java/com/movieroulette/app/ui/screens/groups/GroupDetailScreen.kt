package com.movieroulette.app.ui.screens.groups

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.movieroulette.app.data.model.MovieWithDetails
import com.movieroulette.app.data.model.toPosterUrl
import com.movieroulette.app.data.model.getGenreIds
import com.movieroulette.app.data.model.GenreConstants
import com.movieroulette.app.data.remote.SupabaseConfig
import io.github.jan.supabase.auth.auth
import com.movieroulette.app.ui.components.*
import com.movieroulette.app.ui.navigation.Screen
import com.movieroulette.app.ui.screens.movies.MovieRatingsScreen
import com.movieroulette.app.viewmodel.MovieViewModel
import com.movieroulette.app.viewmodel.GroupViewModel
import com.movieroulette.app.viewmodel.RatingViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    navController: NavController,
    groupId: String,
    viewModel: MovieViewModel = viewModel(),
    groupViewModel: GroupViewModel = viewModel()
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var isSpinning by remember { mutableStateOf(false) }
    var selectedMovie by remember { mutableStateOf<MovieWithDetails?>(null) }
    var currentMovieIndex by remember { mutableStateOf(0) }
    var hasSelectedMovie by remember { mutableStateOf(false) } // Nueva variable para saber si el usuario eligió
    var showGenreFilter by remember { mutableStateOf(false) }
    var selectedGenreFilter by remember { mutableStateOf<Int?>(null) }
    var showMovieDialog by remember { mutableStateOf(false) }
    var selectedMovieTmdbId by remember { mutableStateOf<Int?>(null) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var selectedMovieForRating by remember { mutableStateOf<Pair<String, String>?>(null) } // movieId, movieTitle
    var showRatingsDialog by remember { mutableStateOf(false) }
    var selectedMovieForRatings by remember { mutableStateOf<MovieWithDetails?>(null) } // movie
    var showViewersSelectionDialog by remember { mutableStateOf(false) }
    var selectedMovieForViewers by remember { mutableStateOf<MovieWithDetails?>(null) }
    
    // Usar estados separados por pestaña
    val pendingMoviesState by viewModel.pendingMoviesState.collectAsState()
    val watchingMoviesState by viewModel.watchingMoviesState.collectAsState()
    val watchedMoviesState by viewModel.watchedMoviesState.collectAsState()
    val creditsState by viewModel.movieCreditsState.collectAsState()
    
    val scope = rememberCoroutineScope()
    
    var allMovies by remember { mutableStateOf<List<MovieWithDetails>>(emptyList()) }
    var inviteCode by remember { mutableStateOf("") }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    // Obtener el nombre del grupo desde el caché del ViewModel (persiste durante navegación)
    val groupsState by groupViewModel.groupsState.collectAsState()
    val groupName = remember(groupsState, groupId) {
        groupViewModel.getGroupName(groupId)
    }
    
    // Pre-cargar todas las pestañas al inicio para evitar el efecto de carga al cambiar
    LaunchedEffect(Unit) {
        // Asegurar sesión válida antes de cargar datos
        groupViewModel.authRepository.ensureSessionValid()
        
        viewModel.loadGroupMovies(groupId, status = "pending")
        viewModel.loadGroupMovies(groupId, status = "watching")
        viewModel.loadGroupMovies(groupId, status = "watched")
        
        val result = viewModel.movieRepository.getGroupMovies(groupId, null)
        if (result.isSuccess) {
            allMovies = result.getOrNull() ?: emptyList()
        }
        
        // Cargar código de invitación
        val groupResult = groupViewModel.groupRepository.getUserGroups()
        if (groupResult.isSuccess) {
            val group = groupResult.getOrNull()?.find { it.id == groupId }
            inviteCode = group?.inviteCode ?: ""
        }
    }
    
    // Verificar películas watched sin puntuar al entrar al grupo
    LaunchedEffect(groupId) {
        val currentUserId = viewModel.movieRepository.getCurrentUserId()
        if (currentUserId != null) {
            // Obtener películas watched del grupo
            val watchedResult = viewModel.movieRepository.getGroupMovies(groupId, "watched")
            if (watchedResult.isSuccess) {
                val watchedMovies = watchedResult.getOrNull() ?: emptyList()
                
                // Encontrar la primera película sin puntuar y que no haya mostrado el popup
                for (movie in watchedMovies) {
                    // PRIMERO verificar si el usuario está en movie_viewers (si la vio)
                    val hasWatchedResult = viewModel.movieRepository.hasUserWatchedMovie(movie.id, currentUserId)
                    val hasWatched = hasWatchedResult.getOrNull() ?: false
                    
                    // Solo continuar si el usuario vio la película
                    if (!hasWatched) {
                        continue // Saltar a la siguiente película
                    }
                    
                    val ratingResult = viewModel.movieRepository.getUserRating(movie.id, currentUserId)
                    
                    // Verificar si el usuario ha descartado el prompt para esta película (en cualquier grupo)
                    val hasDismissedResult = viewModel.movieRepository.hasUserDismissedRatingPrompt(movie.id)
                    val hasDismissed = hasDismissedResult.getOrNull() ?: false
                    
                    // Mostrar popup solo si:
                    // 1. El usuario VIO la película (está en movie_viewers)
                    // 2. No tiene puntuación del usuario
                    // 3. No ha descartado el prompt anteriormente (dismissed_rating_prompts)
                    if (ratingResult.isSuccess && ratingResult.getOrNull() == null && !hasDismissed) {
                        selectedMovieForRating = movie.id to movie.title
                        showRatingDialog = true
                        break // Solo mostrar el diálogo para la primera película
                    }
                }
            }
        }
    }
    
    // Diálogo para preguntar si quiere puntuar ahora
    if (showRatingDialog && selectedMovieForRating != null) {
        AlertDialog(
            onDismissRequest = { 
                showRatingDialog = false
                selectedMovieForRating = null
            },
            title = { Text(stringResource(com.movieroulette.app.R.string.rate_now_title)) },
            text = { 
                Text(
                    stringResource(
                        com.movieroulette.app.R.string.rate_now_message,
                        selectedMovieForRating?.second ?: ""
                    )
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedMovieForRating?.let { (movieId, _) ->
                            scope.launch {
                                // No marcar como descartado si va a puntuar
                                // Solo cerrar el dialog y navegar
                                showRatingDialog = false
                                selectedMovieForRating = null
                                navController.navigate(Screen.RateMovie.createRoute(movieId, groupId))
                            }
                        }
                    }
                ) {
                    Text(stringResource(com.movieroulette.app.R.string.rate_now))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedMovieForRating?.let { (movieId, _) ->
                            scope.launch {
                                // Marcar como descartado en dismissed_rating_prompts
                                viewModel.movieRepository.dismissRatingPrompt(movieId)
                                // Cerrar dialog después de guardar
                                showRatingDialog = false
                                selectedMovieForRating = null
                            }
                        }
                    }
                ) {
                    Text(stringResource(com.movieroulette.app.R.string.rate_later))
                }
            }
        )
    }
    
    // Nota: Las notificaciones se muestran cuando se marca la película como "watched"
    // No redirigimos automáticamente al entrar al grupo
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(groupName.ifEmpty { stringResource(com.movieroulette.app.R.string.group) }, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(com.movieroulette.app.R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.GroupMembers.createRoute(groupId)) }) {
                        Icon(Icons.Default.Group, stringResource(com.movieroulette.app.R.string.members))
                    }
                    IconButton(onClick = { navController.navigate(Screen.EditGroup.createRoute(groupId)) }) {
                        Icon(Icons.Default.Edit, stringResource(com.movieroulette.app.R.string.edit_group_title))
                    }
                    IconButton(onClick = { navController.navigate(Screen.SearchMovie.createRoute(groupId)) }) {
                        Icon(Icons.Default.Add, stringResource(com.movieroulette.app.R.string.add_movie))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when (selectedTab) {
                    0 -> RouletteContent(groupId, viewModel, pendingMoviesState, isSpinning, selectedMovie, currentMovieIndex, hasSelectedMovie,
                        { isSpinning = it }, { selectedMovie = it }, { currentMovieIndex = it }, { hasSelectedMovie = it }, scope,
                        selectedGenreFilter, { selectedGenreFilter = it }, { showGenreFilter = true })
                    1 -> MoviesListContent(
                        groupId = groupId,
                        status = "pending",
                        moviesState = pendingMoviesState,
                        viewModel = viewModel,
                        scope = scope,
                        navController = navController,
                        onMovieClick = { tmdbId ->
                            selectedMovieTmdbId = tmdbId
                            showMovieDialog = true
                        },
                        context = context,
                        groupName = groupName,
                        onShowRatingDialog = { movieId, movieTitle ->
                            selectedMovieForRating = movieId to movieTitle
                            showRatingDialog = true
                        },
                        onShowRatingsDialog = { movie ->
                            selectedMovieForRatings = movie
                            showRatingsDialog = true
                        }
                    )
                    2 -> MoviesListContent(
                        groupId = groupId,
                        status = "watching",
                        moviesState = watchingMoviesState,
                        viewModel = viewModel,
                        scope = scope,
                        navController = navController,
                        onMovieClick = { tmdbId ->
                            selectedMovieTmdbId = tmdbId
                            showMovieDialog = true
                        },
                        context = context,
                        groupName = groupName,
                        onShowRatingDialog = { movieId, movieTitle ->
                            selectedMovieForRating = movieId to movieTitle
                            showRatingDialog = true
                        },
                        onShowRatingsDialog = { movie ->
                            selectedMovieForRatings = movie
                            showRatingsDialog = true
                        },
                        onMovieFinished = { movie ->
                            selectedMovieForViewers = movie
                            showViewersSelectionDialog = true
                        }
                    )
                    3 -> MoviesListContent(
                        groupId = groupId,
                        status = "watched",
                        moviesState = watchedMoviesState,
                        viewModel = viewModel,
                        scope = scope,
                        navController = navController,
                        onMovieClick = { tmdbId ->
                            selectedMovieTmdbId = tmdbId
                            showMovieDialog = true
                        },
                        context = context,
                        groupName = groupName,
                        onShowRatingDialog = { movieId, movieTitle ->
                            selectedMovieForRating = movieId to movieTitle
                            showRatingDialog = true
                        },
                        onShowRatingsDialog = { movie ->
                            selectedMovieForRatings = movie
                            showRatingsDialog = true
                        }
                    )
                }
            }
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TabButton(Icons.Default.Refresh, stringResource(com.movieroulette.app.R.string.choose), selected = selectedTab == 0, onClick = { selectedTab = 0 })
                    TabButton(Icons.AutoMirrored.Filled.List, stringResource(com.movieroulette.app.R.string.pending), selected = selectedTab == 1, onClick = { selectedTab = 1 })
                    TabButton(Icons.Default.PlayArrow, stringResource(com.movieroulette.app.R.string.watching), selected = selectedTab == 2, onClick = { selectedTab = 2 })
                    TabButton(Icons.Default.Check, stringResource(com.movieroulette.app.R.string.watched), selected = selectedTab == 3, onClick = { selectedTab = 3 })
                }
            }
        }
    }
    
    // Movie Detail Dialog
    if (showMovieDialog && selectedMovieTmdbId != null) {
        com.movieroulette.app.ui.screens.movies.MovieInfoDialog(
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
    
    // Movie Ratings Dialog
    if (showRatingsDialog && selectedMovieForRatings != null) {
        Dialog(
            onDismissRequest = { 
                showRatingsDialog = false
                selectedMovieForRatings = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            MovieRatingsScreen(
                movie = selectedMovieForRatings!!,
                movieId = selectedMovieForRatings!!.id,
                groupId = groupId,
                onDismiss = {
                    showRatingsDialog = false
                    selectedMovieForRatings = null
                },
                onRateClick = {
                    val movieToRate = selectedMovieForRatings!!
                    showRatingsDialog = false
                    selectedMovieForRatings = null
                    navController.navigate(Screen.RateMovie.createRoute(movieToRate.id, groupId))
                },
                movieViewModel = viewModel,
                ratingViewModel = viewModel()
            )
        }
    }
    
    // Viewers Selection Dialog
    if (showViewersSelectionDialog && selectedMovieForViewers != null) {
        ViewersSelectionDialog(
            movie = selectedMovieForViewers!!,
            groupId = groupId,
            onDismiss = {
                showViewersSelectionDialog = false
                selectedMovieForViewers = null
            },
            onConfirm = { selectedUserIds ->
                scope.launch {
                    Log.d("GroupDetailScreen", "Confirming viewers selection: ${selectedUserIds.size} users")
                    // IMPORTANTE: Primero agregar viewers, LUEGO cambiar estado
                    // El cambio de estado a "watched" dispara createRatingPromptsForGroup
                    // que necesita que los viewers ya estén en la DB
                    
                    // 1. Agregar viewers a la base de datos
                    val addViewersResult = viewModel.addMovieViewers(selectedMovieForViewers!!.id, selectedUserIds)
                    
                    if (addViewersResult.isSuccess) {
                        Log.d("GroupDetailScreen", "Viewers added successfully, now updating status to watched")
                        // 2. Cambiar estado a "watched" (esto crea los rating prompts para los viewers)
                        viewModel.removeMovieFromState(selectedMovieForViewers!!.id, "watching")
                        viewModel.updateMovieStatus(selectedMovieForViewers!!.id, groupId, "watched")
                        
                        // 3. Mostrar diálogo de puntuación solo si el usuario actual está en la lista
                        val currentUserId = com.movieroulette.app.data.remote.SupabaseConfig.client.auth.currentUserOrNull()?.id
                        if (currentUserId != null && selectedUserIds.contains(currentUserId)) {
                            selectedMovieForRating = selectedMovieForViewers!!.id to selectedMovieForViewers!!.title
                            showRatingDialog = true
                        }
                    } else {
                        Log.e("GroupDetailScreen", "Failed to add viewers: ${addViewersResult.exceptionOrNull()?.message}")
                        // TODO: Mostrar error al usuario
                    }
                    
                    showViewersSelectionDialog = false
                    selectedMovieForViewers = null
                }
            },
            groupViewModel = groupViewModel
        )
    }

}

@Composable
fun RouletteContent(
    groupId: String, viewModel: MovieViewModel, moviesState: MovieViewModel.MoviesState,
    isSpinning: Boolean, selectedMovie: MovieWithDetails?, currentMovieIndex: Int, hasSelectedMovie: Boolean,
    onSpinningChange: (Boolean) -> Unit, onSelectedMovieChange: (MovieWithDetails?) -> Unit,
    onCurrentMovieIndexChange: (Int) -> Unit, onHasSelectedMovieChange: (Boolean) -> Unit, scope: kotlinx.coroutines.CoroutineScope,
    selectedGenreFilter: Int?, onGenreFilterChange: (Int?) -> Unit, onShowGenreFilter: () -> Unit
) {
    var showFilters by remember { mutableStateOf(false) }
    
    val allMovies = (moviesState as? MovieViewModel.MoviesState.Success)?.movies ?: emptyList()
    
    // Filter movies by genre if selected
    val movies = if (selectedGenreFilter != null) {
        allMovies.filter { movie ->
            movie.getGenreIds().contains(selectedGenreFilter)
        }
    } else {
        allMovies
    }
    
    var autoPlayIndex by remember { mutableStateOf(0) }
    
    // Auto-cambio de miniaturas cada 5 segundos cuando no está girando ni hay película seleccionada por el usuario
    LaunchedEffect(movies.size, isSpinning, hasSelectedMovie) {
        if (movies.isNotEmpty() && !isSpinning && !hasSelectedMovie) {
            while (true) {
                delay(5000)
                autoPlayIndex = (autoPlayIndex + 1) % movies.size
                onSelectedMovieChange(movies[autoPlayIndex])
            }
        }
    }
    
    // Mostrar película aleatoria inicial
    LaunchedEffect(movies.size) {
        if (movies.isNotEmpty() && selectedMovie == null && !isSpinning) {
            autoPlayIndex = (0 until movies.size).random()
            onSelectedMovieChange(movies[autoPlayIndex])
            onHasSelectedMovieChange(false) // No fue seleccionada por el usuario
        }
    }
    
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(Modifier.size(width = 280.dp, height = 420.dp), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = currentMovieIndex,
                    transitionSpec = {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(150)
                        ) + fadeIn(tween(150)) togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { -it },
                            animationSpec = tween(150)
                        ) + fadeOut(tween(150))
                    },
                    label = "card_slide"
                ) { targetIndex ->
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(12.dp),
                        colors = if (movies.isEmpty() || (!isSpinning && selectedMovie == null))
                            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        else CardDefaults.cardColors()
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            when {
                                movies.isEmpty() -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Movie,
                                            contentDescription = null,
                                            modifier = Modifier.size(80.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                movies.isNotEmpty() && isSpinning && targetIndex >= 0 -> {
                                    AsyncImage(movies[targetIndex % movies.size].toPosterUrl(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                }
                                selectedMovie != null -> {
                                    Crossfade(
                                        targetState = selectedMovie,
                                        animationSpec = tween(1600, easing = FastOutSlowInEasing),
                                        label = "movie_crossfade"
                                    ) { movie ->
                                        AsyncImage(movie.toPosterUrl(), movie.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    }
                                }
                                movies.isNotEmpty() -> {
                                    AsyncImage(movies.first().toPosterUrl(), movies.first().title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            when {
                hasSelectedMovie && selectedMovie != null -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(selectedMovie!!.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    val movieToUpdate = selectedMovie!!
                                    // Update optimista - remover de la lista inmediatamente
                                    viewModel.removeMovieFromState(movieToUpdate.id, "pending")
                                    onSelectedMovieChange(null)
                                    onHasSelectedMovieChange(false)
                                    // Actualizar en servidor
                                    scope.launch {
                                        viewModel.updateMovieStatus(movieToUpdate.id, groupId, "watching")
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) { 
                                Text(
                                    stringResource(com.movieroulette.app.R.string.watch_now),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                ) 
                            }
                            OutlinedButton(
                                onClick = { 
                                    onSelectedMovieChange(null)
                                    onHasSelectedMovieChange(false)
                                    onCurrentMovieIndexChange(0)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) { 
                                Text(
                                    stringResource(com.movieroulette.app.R.string.another),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                ) 
                            }
                        }
                    }
                }
                movies.isEmpty() -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = if (selectedGenreFilter != null) {
                                "No hay películas de ${GenreConstants.getGenreName(selectedGenreFilter)}"
                            } else {
                                stringResource(com.movieroulette.app.R.string.no_movies_in_roulette)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selectedGenreFilter != null) {
                            TextButton(onClick = { onGenreFilterChange(null) }) {
                                Text(stringResource(com.movieroulette.app.R.string.remove_filter))
                            }
                        }
                    }
                }
                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Button with filter icon - centered layout
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = {
                                    if (!isSpinning) {
                                        onSelectedMovieChange(null)
                                        onCurrentMovieIndexChange(0)
                                        onSpinningChange(true)
                                        scope.launch {
                                            repeat(25) { 
                                                onCurrentMovieIndexChange(it)
                                                delay(120)
                                            }
                                            onSelectedMovieChange(movies.random())
                                            onHasSelectedMovieChange(true)
                                            onSpinningChange(false)
                                        }
                                    }
                                },
                                enabled = !isSpinning,
                                modifier = Modifier.height(56.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    if (isSpinning) stringResource(com.movieroulette.app.R.string.loading) 
                                    else stringResource(com.movieroulette.app.R.string.choose_movie), 
                                    style = MaterialTheme.typography.titleMedium, 
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            // Filter toggle button - positioned to the right
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 12.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = { showFilters = !showFilters },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = stringResource(com.movieroulette.app.R.string.filter_by_genre),
                                        tint = if (showFilters || selectedGenreFilter != null) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        
                        // Horizontal scrollable genre filters
                        AnimatedVisibility(
                            visible = showFilters,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            GenreFilterRow(
                                selectedGenre = selectedGenreFilter,
                                onGenreSelected = { genreId ->
                                    onGenreFilterChange(genreId)
                                    if (genreId != null) {
                                        showFilters = false // Hide after selection
                                    }
                                }
                            )
                        }
                        
                        // Selected genre chip (only shows when a filter is active and filters are hidden)
                        if (selectedGenreFilter != null && !showFilters) {
                            FilterChip(
                                selected = true,
                                onClick = { onGenreFilterChange(null) },
                                label = { 
                                    Text(
                                        GenreConstants.getGenreName(selectedGenreFilter),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.Black
                                    ) 
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(com.movieroulette.app.R.string.remove_filter),
                                        modifier = Modifier.size(18.dp),
                                        tint = Color.Black
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = Color.Black
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = true,
                                    borderColor = MaterialTheme.colorScheme.primary,
                                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                                    borderWidth = 1.dp,
                                    selectedBorderWidth = 2.dp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MoviesListContent(
    groupId: String,
    status: String,
    moviesState: MovieViewModel.MoviesState,
    viewModel: MovieViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
    navController: NavController,
    onMovieClick: (Int) -> Unit,
    context: android.content.Context,
    groupName: String,
    onShowRatingDialog: (String, String) -> Unit = { _, _ -> }, // movieId, movieTitle
    onShowRatingsDialog: (MovieWithDetails) -> Unit = { }, // movie
    onMovieFinished: (MovieWithDetails) -> Unit = { } // movie
) {
    var showDeleteDialog by remember { mutableStateOf<MovieWithDetails?>(null) }
    var currentUserId by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        currentUserId = viewModel.movieRepository.getCurrentUserId()
    }
    when (moviesState) {
        is MovieViewModel.MoviesState.Loading -> LoadingScreen()
        is MovieViewModel.MoviesState.Empty -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Movie,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(com.movieroulette.app.R.string.no_movies),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        is MovieViewModel.MoviesState.Success -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(moviesState.movies, key = { it.id }) { movie ->
                    MovieCard(
                        movie = movie,
                        status = status,
                        currentUserId = currentUserId,
                        viewModel = viewModel,
                        onMovieClick = { onMovieClick(movie.tmdbId) },
                        onLongClick = {
                            if ((status == "pending" && movie.addedBy == currentUserId) || status == "watched") {
                                showDeleteDialog = movie
                            }
                        },
                        onWatchNow = {
                            viewModel.removeMovieFromState(movie.id, "pending")
                            scope.launch { 
                                viewModel.updateMovieStatus(movie.id, groupId, "watching")
                            }
                        },
                        onFinished = {
                            // Mostrar diálogo de selección de usuarios
                            onMovieFinished(movie)
                        },
                        onBackToPending = {
                            viewModel.removeMovieFromState(movie.id, if (status == "watching") "watching" else "watched")
                            scope.launch { 
                                viewModel.updateMovieStatus(movie.id, groupId, "pending")
                            }
                        },
                        onRate = {
                            navController.navigate(Screen.RateMovie.createRoute(movie.id, groupId))
                        },
                        onViewRatings = {
                            onShowRatingsDialog(movie)
                        }
                    )
                }
            }
        }
        is MovieViewModel.MoviesState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(moviesState.message ?: "", color = MaterialTheme.colorScheme.error) }
    }
    
    // Diálogo de confirmación para eliminar
    showDeleteDialog?.let { movie ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(com.movieroulette.app.R.string.delete_movie)) },
            text = { Text(stringResource(com.movieroulette.app.R.string.delete_movie_confirm_message, movie.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val result = viewModel.deleteMovie(movie.id)
                            if (result.isSuccess) {
                                viewModel.loadGroupMovies(groupId, status)
                            }
                            showDeleteDialog = null
                        }
                    }
                ) {
                    Text(stringResource(com.movieroulette.app.R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(com.movieroulette.app.R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MovieCard(
    movie: MovieWithDetails,
    status: String,
    currentUserId: String?,
    viewModel: MovieViewModel,
    onMovieClick: () -> Unit,
    onLongClick: () -> Unit,
    onWatchNow: () -> Unit = {},
    onFinished: () -> Unit = {},
    onBackToPending: () -> Unit = {},
    onRate: () -> Unit = {},
    onViewRatings: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    var hasWatched by remember { mutableStateOf<Boolean?>(null) }
    
    // Cargar hasWatched al inicio para que esté listo cuando se expanda el menú
    LaunchedEffect(movie.id, currentUserId, status) {
        if (status == "watched" && currentUserId != null) {
            val result = viewModel.movieRepository.hasUserWatchedMovie(movie.id, currentUserId)
            hasWatched = result.getOrNull() ?: false
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.65f)
            .combinedClickable(
                onClick = onMovieClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Poster Image
            AsyncImage(
                model = movie.toPosterUrl(),
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Shadow overlay for text readability - starts from middle to bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.7f),
                                Color.Black.copy(alpha = 0.95f)
                            ),
                            startY = 0f,
                            endY = 800f
                        )
                    )
            )
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section - Added by (if available)
                movie.addedByUsername?.let { username ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(
                            text = stringResource(com.movieroulette.app.R.string.added_by, username),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Bottom section - Title, Rating, and Actions
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Title
                    Text(
                        text = movie.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Year, Runtime and Rating in same row
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Year and Runtime
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = movie.releaseDate?.take(4) ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            if (movie.runtime != null && movie.runtime > 0) {
                                Text(
                                    text = "• ${movie.runtime}m",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                        
                        // Rating (if watched) - aligned to the right
                        if (status == "watched") {
                            movie.averageRating?.let { rating ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.clickable { onViewRatings() }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = String.format("%.1f", rating),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                    
                    // Expand/Collapse Arrow
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { expanded = !expanded },
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { expanded = !expanded },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (expanded) "Hide actions" else "Show actions",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    // Action Buttons (expandable)
                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            when (status) {
                                "pending" -> {
                                    Button(
                                        onClick = {
                                            expanded = false
                                            onWatchNow()
                                        },
                                        modifier = Modifier.fillMaxWidth().height(42.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Text(
                                            text = stringResource(com.movieroulette.app.R.string.watch_now).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                
                                "watching" -> {
                                    Button(
                                        onClick = {
                                            expanded = false
                                            onFinished()
                                        },
                                        modifier = Modifier.fillMaxWidth().height(42.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Text(
                                            text = stringResource(com.movieroulette.app.R.string.watched_checkmark).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            expanded = false
                                            onBackToPending()
                                        },
                                        modifier = Modifier.fillMaxWidth().height(42.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.primary
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text(
                                            text = stringResource(com.movieroulette.app.R.string.back_to_roulette).uppercase(),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                
                                "watched" -> {
                                    // Solo mostrar botón de Rate si el usuario vio la película
                                    if (hasWatched == true) {
                                        Button(
                                            onClick = {
                                                expanded = false
                                                onViewRatings()
                                            },
                                            modifier = Modifier.fillMaxWidth().height(42.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        ) {
                                            Text(
                                                text = "PUNTUACIONES",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    } else if (hasWatched == false) {
                                        // Botón para marcar como vista
                                        OutlinedButton(
                                            onClick = {
                                                expanded = false
                                                onFinished()
                                            },
                                            modifier = Modifier.fillMaxWidth().height(42.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.primary
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text(
                                                text = "VISTA",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    
                                    OutlinedButton(
                                        onClick = {
                                            expanded = false
                                            onBackToPending()
                                        },
                                        modifier = Modifier.fillMaxWidth().height(42.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.primary
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text(
                                            text = stringResource(com.movieroulette.app.R.string.back_to_roulette_full).uppercase(),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        )
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

@Composable
fun TabButton(
    icon: ImageVector, label: String, count: Int = 0, selected: Boolean, onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(85.dp).clip(RoundedCornerShape(12.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick, Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                if (count > 0) {
                    Badge(Modifier.align(Alignment.TopEnd).offset(6.dp, (-6).dp)) {
                        Text(count.toString(), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
fun MovieRatingsDialog(
    movieId: String,
    groupId: String,
    onDismiss: () -> Unit,
    movieViewModel: MovieViewModel,
    ratingViewModel: RatingViewModel,
    navController: NavController
) {
    val moviesState by movieViewModel.moviesState.collectAsState()
    val ratingsState by ratingViewModel.ratingsState.collectAsState()
    val scope = rememberCoroutineScope()
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var currentUserId by remember { mutableStateOf<String?>(null) }
    var viewers by remember { mutableStateOf<List<com.movieroulette.app.data.model.MovieViewerWithProfile>>(emptyList()) }
    var isLoadingViewers by remember { mutableStateOf(true) }
    var hasWatched by remember { mutableStateOf(false) }
    var userRating by remember { mutableStateOf<com.movieroulette.app.data.model.MovieRating?>(null) }
    var refreshKey by remember { mutableStateOf(0) }
    
    LaunchedEffect(refreshKey) {
        movieViewModel.loadGroupMovies(groupId, null)
        ratingViewModel.loadMovieRatings(movieId)
        
        // Obtener el ID del usuario actual
        currentUserId = movieViewModel.movieRepository.getCurrentUserId()
        
        // Verificar si el usuario ha visto la película y si tiene puntuación
        currentUserId?.let { userId ->
            val result = movieViewModel.movieRepository.hasUserWatchedMovie(movieId, userId)
            hasWatched = result.getOrNull() ?: false
            
            // Obtener la puntuación del usuario si existe
            val ratingResult = movieViewModel.movieRepository.getUserRating(movieId, userId)
            userRating = ratingResult.getOrNull()
        }
        
        // Cargar viewers desde el principio
        isLoadingViewers = true
        val result = movieViewModel.getMovieViewers(movieId)
        result.onSuccess { viewersList ->
            viewers = viewersList
        }
        isLoadingViewers = false
    }
    
    val movie = (moviesState as? MovieViewModel.MoviesState.Success)?.movies?.find { it.id == movieId }
    
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.8f),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
            movie?.let {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Movie header with poster as border
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                        ) {
                            // Poster background
                            AsyncImage(
                                model = it.toPosterUrl(),
                                contentDescription = it.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            
                            // Gradient fade to show rating info
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.5f),
                                                Color.Black.copy(alpha = 0.8f),
                                                MaterialTheme.colorScheme.surface
                                            ),
                                            startY = 0f,
                                            endY = 850f
                                        )
                                    )
                            )
                            
                            // Movie info at bottom
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Text(
                                    text = it.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Average rating
                                it.averageRating?.let { avg ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Text(
                                            text = String.format("%.1f/10", avg),
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "(${it.totalRatings ?: 0} ${stringResource(com.movieroulette.app.R.string.ratings_label)})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                            
                            // Close button
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(com.movieroulette.app.R.string.back),
                                    tint = Color.White
                                )
                            }
                        }
                        
                        // Sección de viewers
                        if (!isLoadingViewers && viewers.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Vieron esta película",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(viewers) { viewer ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (viewer.avatarUrl != null) {
                                                AsyncImage(
                                                    model = viewer.avatarUrl,
                                                    contentDescription = viewer.username,
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = viewer.username.take(1).uppercase(),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Text(
                                                text = viewer.displayName ?: viewer.username,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 70.dp),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                                
                                HorizontalDivider(
                                    modifier = Modifier.padding(top = 16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                        
                        // Ratings list as table
                        when (val state = ratingsState) {
                            is RatingViewModel.RatingsState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            is RatingViewModel.RatingsState.Success -> {
                                if (state.ratings.isEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = stringResource(com.movieroulette.app.R.string.no_ratings_yet),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        
                                        // Botón para puntuar si el usuario vio la película pero no tiene puntuación
                                        if (hasWatched && userRating == null) {
                                            Spacer(modifier = Modifier.height(24.dp))
                                            OutlinedButton(
                                                onClick = {
                                                    onDismiss()
                                                    navController.navigate(Screen.RateMovie.createRoute(movieId, groupId))
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.primary
                                                ),
                                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                            ) {
                                                Text(
                                                    text = "PUNTUAR",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        items(state.ratings, key = { it.id }) { rating ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // User info
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    // Avatar
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (rating.avatarUrl != null) {
                                                            AsyncImage(
                                                                model = rating.avatarUrl,
                                                                contentDescription = "Avatar",
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        } else {
                                                            Icon(
                                                                imageVector = Icons.Default.Person,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                    
                                                    Column(
                                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text(
                                                            text = rating.username ?: stringResource(com.movieroulette.app.R.string.user_default),
                                                            style = MaterialTheme.typography.bodyLarge,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        
                                                        // Comment
                                                        rating.comment?.let { comment ->
                                                            if (comment.isNotBlank()) {
                                                                Text(
                                                                    text = comment,
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                
                                                // Rating and delete button
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    // Rating
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
                                                            text = "${rating.rating.toInt()}",
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    
                                                    // Delete button
                                                    if (rating.userId == currentUserId) {
                                                        IconButton(
                                                            onClick = { showDeleteDialog = true },
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = stringResource(com.movieroulette.app.R.string.delete),
                                                                tint = MaterialTheme.colorScheme.error,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        
                                        // Botón para puntuar si el usuario vio la película pero no tiene puntuación
                                        if (hasWatched && userRating == null) {
                                            item {
                                                OutlinedButton(
                                                    onClick = {
                                                        onDismiss()
                                                        navController.navigate(Screen.RateMovie.createRoute(movieId, groupId))
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 16.dp)
                                                        .height(48.dp),
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        contentColor = MaterialTheme.colorScheme.primary
                                                    ),
                                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                                ) {
                                                    Text(
                                                        text = "PUNTUAR",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            is RatingViewModel.RatingsState.Error -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = state.message ?: "",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(com.movieroulette.app.R.string.delete_rating)) },
            text = { Text(stringResource(com.movieroulette.app.R.string.delete_rating_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            showDeleteDialog = false
                            val result = movieViewModel.deleteRating(movieId)
                            if (result.isSuccess) {
                                // Recargar las puntuaciones y películas después de eliminar
                                kotlinx.coroutines.delay(100)
                                ratingViewModel.loadMovieRatings(movieId)
                                movieViewModel.loadGroupMovies(groupId, "pending")
                                movieViewModel.loadGroupMovies(groupId, "watching")
                                movieViewModel.loadGroupMovies(groupId, "watched")
                                movieViewModel.loadGroupMovies(groupId, null) // Recargar todas
                                // Forzar recarga del estado hasWatched en el diálogo
                                refreshKey++
                            }
                        }
                    }
                ) {
                    Text(stringResource(com.movieroulette.app.R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(com.movieroulette.app.R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun GenreFilterRow(
    selectedGenre: Int?,
    onGenreSelected: (Int?) -> Unit
) {
    val genreIcons = mapOf(
        GenreConstants.ACTION to Icons.Outlined.LocalFireDepartment,
        GenreConstants.COMEDY to Icons.Outlined.TheaterComedy,
        GenreConstants.DRAMA to Icons.Outlined.Theaters,
        GenreConstants.HORROR to Icons.Outlined.Nightlight,
        GenreConstants.SCIENCE_FICTION to Icons.Outlined.Science,
        GenreConstants.ROMANCE to Icons.Outlined.Favorite,
        GenreConstants.THRILLER to Icons.Outlined.Warning,
        GenreConstants.ANIMATION to Icons.Outlined.Palette,
        GenreConstants.ADVENTURE to Icons.Outlined.Explore,
        GenreConstants.FANTASY to Icons.Outlined.AutoAwesome
    )
    
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // All movies chip
        item {
            FilterChip(
                selected = selectedGenre == null,
                onClick = { onGenreSelected(null) },
                label = { 
                    Text(
                        stringResource(com.movieroulette.app.R.string.all_movies),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selectedGenre == null) FontWeight.Bold else FontWeight.Medium
                    ) 
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.Black,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedGenre == null,
                    borderColor = MaterialTheme.colorScheme.outline,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 2.dp
                )
            )
        }
        
        // Genre chips
        items(GenreConstants.popularGenres.size) { index ->
            val (genreId, genreName) = GenreConstants.popularGenres[index]
            FilterChip(
                selected = selectedGenre == genreId,
                onClick = { onGenreSelected(genreId) },
                label = { 
                    Text(
                        genreName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selectedGenre == genreId) FontWeight.Bold else FontWeight.Medium
                    ) 
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    selectedLabelColor = Color.Black,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedGenre == genreId,
                    borderColor = MaterialTheme.colorScheme.outline,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 2.dp
                )
            )
        }
    }
}

@Composable
fun ViewersSelectionDialog(
    movie: MovieWithDetails,
    groupId: String,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
    groupViewModel: GroupViewModel
) {
    val membersState by groupViewModel.membersState.collectAsState()
    val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id
    var selectedUserIds by remember { mutableStateOf(setOf<String>()) }
    
    // Cargar miembros al mostrar el diálogo
    LaunchedEffect(groupId) {
        groupViewModel.loadMembers(groupId)
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Título
                Text(
                    text = "¿Quiénes vieron la película?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Selecciona los miembros que vieron la película:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Lista de miembros
                when (val state = membersState) {
                    is GroupViewModel.MembersState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is GroupViewModel.MembersState.Success -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                        ) {
                            items(state.members) { member ->
                                val isSelected = selectedUserIds.contains(member.userId)
                                val isCurrentUser = member.userId == currentUserId
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedUserIds = if (isSelected) {
                                                selectedUserIds - member.userId
                                            } else {
                                                selectedUserIds + member.userId
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selectedUserIds = if (checked) {
                                                selectedUserIds + member.userId
                                            } else {
                                                selectedUserIds - member.userId
                                            }
                                        }
                                    )
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    // Avatar
                                    if (member.avatarUrl != null) {
                                        AsyncImage(
                                            model = member.avatarUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = member.username.take(1).uppercase(),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = member.displayName ?: member.username,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isCurrentUser) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "(Tú)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        if (member.displayName != null) {
                                            Text(
                                                text = "@${member.username}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                
                                if (member != state.members.last()) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 64.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    is GroupViewModel.MembersState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Error al cargar miembros",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar")
                    }
                    
                    Button(
                        onClick = {
                            if (selectedUserIds.isNotEmpty()) {
                                onConfirm(selectedUserIds.toList())
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedUserIds.isNotEmpty()
                    ) {
                        Text("Confirmar")
                    }
                }
            }
        }
    }
}
