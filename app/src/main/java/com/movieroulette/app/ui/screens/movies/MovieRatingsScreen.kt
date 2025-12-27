package com.movieroulette.app.ui.screens.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.movieroulette.app.R
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.movieroulette.app.data.model.MovieRating
import com.movieroulette.app.data.model.toPosterUrl
import com.movieroulette.app.ui.components.LoadingScreen
import com.movieroulette.app.viewmodel.MovieViewModel
import com.movieroulette.app.viewmodel.RatingViewModel
import kotlinx.coroutines.launch
import kotlin.math.max

import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.text.style.TextOverflow
import com.movieroulette.app.data.model.MovieViewerWithProfile
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset

import com.movieroulette.app.data.model.MovieWithDetails

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieRatingsScreen(
    movie: MovieWithDetails? = null,
    movieId: String,
    groupId: String,
    onDismiss: () -> Unit,
    onRateClick: () -> Unit = {},
    movieViewModel: MovieViewModel = viewModel(),
    ratingViewModel: RatingViewModel = viewModel()
) {
    val moviesState by movieViewModel.moviesState.collectAsState()
    val ratingsState by ratingViewModel.ratingsState.collectAsState()
    val scope = rememberCoroutineScope()
    
    // Determine which movie object to display
    // If 'movie' is passed, use it (stable).
    // If not, try to find it in the state.
    val displayMovie = remember(movie, moviesState) {
        movie ?: (moviesState as? MovieViewModel.MoviesState.Success)?.movies?.find { it.id == movieId }
    }
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var currentUserId by remember { mutableStateOf<String?>(null) }
    var selectedRating by remember { mutableStateOf<MovieRating?>(null) }
    var viewers by remember { mutableStateOf<List<MovieViewerWithProfile>>(emptyList()) }
    var isLoadingViewers by remember { mutableStateOf(true) }
    
    // Collapsing header state
    val density = LocalDensity.current
    val maxHeaderHeight = 250.dp
    val minHeaderHeight = 80.dp
    
    val maxHeaderHeightPx = with(density) { maxHeaderHeight.toPx() }
    val minHeaderHeightPx = with(density) { minHeaderHeight.toPx() }
    
    var headerHeightPx by remember { mutableFloatStateOf(maxHeaderHeightPx) }
    
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newHeight = headerHeightPx + delta
                headerHeightPx = newHeight.coerceIn(minHeaderHeightPx, maxHeaderHeightPx)
                return Offset.Zero
            }
        }
    }
    
    val headerHeight = with(density) { headerHeightPx.toDp() }
    
    // Calculate collapse fraction for animations (0.0 = expanded, 1.0 = collapsed)
    val collapseFraction = (maxHeaderHeightPx - headerHeightPx) / (maxHeaderHeightPx - minHeaderHeightPx)
    
    LaunchedEffect(Unit) {
        movieViewModel.loadGroupMovies(groupId, null)
        ratingViewModel.loadMovieRatings(movieId)
        currentUserId = movieViewModel.movieRepository.getCurrentUserId()
        
        // Load viewers
        isLoadingViewers = true
        val result = movieViewModel.getMovieViewers(movieId)
        result.onSuccess { viewersList ->
            viewers = viewersList
        }
        isLoadingViewers = false
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {
            // Ratings List
            when (val state = ratingsState) {
                is RatingViewModel.RatingsState.Loading -> LoadingScreen()
                is RatingViewModel.RatingsState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            bottom = paddingValues.calculateBottomPadding() + 80.dp,
                            start = 16.dp,
                            end = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Spacer for header
                        item {
                            Spacer(modifier = Modifier.height(maxHeaderHeight))
                        }
                        
                        // Viewers Section
                        if (!isLoadingViewers && viewers.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
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
                        }

                        // Rate Button if not rated
                        val userHasRated = state.ratings.any { it.userId == currentUserId }
                        if (!userHasRated && currentUserId != null) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Button(
                                        onClick = onRateClick,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth(0.8f)
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.rate_now))
                                    }
                                }
                            }
                        }

                        if (state.ratings.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.no_ratings_yet),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            item {
                                Text(
                                    text = stringResource(R.string.all_ratings),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            
                            items(state.ratings, key = { it.id }) { rating ->
                                RatingItemCard(
                                    rating = rating,
                                    currentUserId = currentUserId,
                                    onLongClick = {
                                        if (rating.userId == currentUserId) {
                                            showDeleteDialog = true
                                        }
                                    },
                                    onClick = { selectedRating = rating }
                                )
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
            
            // Collapsing Header
            displayMovie?.let { movieData ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeight)
                        .align(Alignment.TopCenter)
                        .clip(RoundedCornerShape(bottomStart = if (collapseFraction < 0.5f) 24.dp else 0.dp, bottomEnd = if (collapseFraction < 0.5f) 24.dp else 0.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    // Background Image
                    AsyncImage(
                        model = movieData.toPosterUrl(),
                        contentDescription = movieData.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (collapseFraction > 0.5f) Modifier.blur(radius = (collapseFraction * 10).dp) else Modifier),
                        contentScale = ContentScale.Crop,
                        alpha = 1f - (collapseFraction * 0.3f) // Darken slightly when collapsed
                    )
                    
                    // Gradient Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.3f + (collapseFraction * 0.5f)),
                                        Color.Black.copy(alpha = 0.7f + (collapseFraction * 0.3f))
                                    )
                                )
                            )
                    )
                    
                    // Content
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Close Button (Always visible)
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .statusBarsPadding()
                                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = Color.White
                            )
                        }
                        
                        // Movie Info
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = movieData.title,
                                style = if (collapseFraction > 0.5f) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = if (collapseFraction > 0.5f) 1 else 2
                            )
                            
                            // Hide details when collapsed
                            if (collapseFraction < 0.5f) {
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                movieData.averageRating?.let { avg ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = String.format("%.1f/10", avg),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "(${movieData.totalRatings ?: 0})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.8f)
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
    
    // Detail Dialog
    selectedRating?.let { rating ->
        RatingDetailDialog(
            rating = rating,
            moviePosterUrl = displayMovie?.toPosterUrl(),
            onDismiss = { selectedRating = null }
        )
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_rating)) },
            text = { Text(stringResource(R.string.delete_rating_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val result = movieViewModel.deleteRating(movieId)
                            if (result.isSuccess) {
                                // Recargar las listas del grupo para actualizar la UI principal
                                movieViewModel.loadGroupMovies(groupId, "pending")
                                movieViewModel.loadGroupMovies(groupId, "watching")
                                movieViewModel.loadGroupMovies(groupId, "watched")
                                // Cerrar la pantalla/diálogo inmediatamente
                                onDismiss()
                            }
                            showDeleteDialog = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RatingItemCard(
    rating: MovieRating,
    currentUserId: String?,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        .size(48.dp)
                        .clip(CircleShape)
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
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = rating.username ?: stringResource(R.string.user_default),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Comment preview
                    rating.comment?.let { comment ->
                        if (comment.isNotBlank()) {
                            Text(
                                text = comment,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            
            // Rating badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (rating.rating % 1.0 == 0.0) rating.rating.toInt().toString() else rating.rating.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RatingDetailDialog(
    rating: MovieRating,
    moviePosterUrl: String?,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Movie Image Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    if (moviePosterUrl != null) {
                        AsyncImage(
                            model = moviePosterUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                    startY = 200f
                                )
                            )
                    )
                    
                    // User Avatar overlapping
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 24.dp, bottom = 16.dp) // Adjusted padding
                            .size(80.dp) // Larger avatar
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(4.dp)
                            .clip(CircleShape)
                    ) {
                        if (rating.avatarUrl != null) {
                            AsyncImage(
                                model = rating.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = rating.username ?: "Usuario",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${if (rating.rating % 1.0 == 0.0) rating.rating.toInt().toString() else rating.rating.toString()}/10",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (!rating.comment.isNullOrBlank()) {
                        Text(
                            text = rating.comment,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 28.sp
                        )
                    } else {
                        Text(
                            text = "Sin comentario escrito",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }
}

