package com.movieroulette.app.ui.screens.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.movieroulette.app.data.model.UserProfile
import com.movieroulette.app.ui.screens.movies.MovieInfoDialog
import com.movieroulette.app.viewmodel.FavoriteMovie
import com.movieroulette.app.viewmodel.FriendsViewModel
import com.movieroulette.app.viewmodel.MovieStats
import com.movieroulette.app.viewmodel.MovieViewModel
import com.movieroulette.app.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    navController: NavController,
    profileViewModel: ProfileViewModel = viewModel(),
    movieViewModel: MovieViewModel = viewModel(),
    friendsViewModel: FriendsViewModel = viewModel()
) {
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedMovieTmdbId by remember { mutableStateOf<Int?>(null) }
    var isFollowing by remember { mutableStateOf(false) }
    var followsBack by remember { mutableStateOf(false) }
    
    val statsState by profileViewModel.statsState.collectAsState()
    val recentMoviesState by profileViewModel.recentMoviesState.collectAsState()
    val favoriteMoviesState by profileViewModel.favoriteMoviesState.collectAsState()
    val creditsState by movieViewModel.movieCreditsState.collectAsState()
    
    LaunchedEffect(userId) {
        android.util.Log.d("UserProfileScreen", "=== LaunchedEffect triggered for userId: $userId ===")
        isLoading = true
        try {
            userProfile = profileViewModel.loadUserProfileById(userId)
            android.util.Log.d("UserProfileScreen", "Profile loaded: ${userProfile?.username}")
            
            // Verificar estado de seguimiento
            isFollowing = friendsViewModel.checkIfFollowing(userId)
            followsBack = friendsViewModel.checkIfFollowsBack(userId)
            
            profileViewModel.loadUserStatsByUserId(userId)
            profileViewModel.loadRecentMoviesByUserId(userId)
            profileViewModel.loadFavoriteMoviesByUserId(userId)
            isLoading = false
            android.util.Log.d("UserProfileScreen", "All data loaded successfully")
        } catch (e: Exception) {
            android.util.Log.e("UserProfileScreen", "Error loading profile: ${e.message}", e)
            error = e.message ?: "Error al cargar el perfil"
            isLoading = false
        }
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(userProfile?.username ?: "Perfil") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
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
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            error ?: "Error desconocido",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            userProfile != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier.size(120.dp)
                    ) {
                        if (userProfile?.avatarUrl != null) {
                            AsyncImage(
                                model = userProfile?.avatarUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    
                    // Nombre de usuario
                    Text(
                        text = userProfile?.username ?: "",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Botones de acción
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Botón de seguir
                        Button(
                            onClick = {
                                if (isFollowing) {
                                    friendsViewModel.unfollowUser(userId)
                                } else {
                                    friendsViewModel.followUser(userId)
                                }
                                isFollowing = !isFollowing
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                if (isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isFollowing) "Siguiendo" else "Seguir",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        // Botón de chat (solo si se siguen mutuamente)
                        if (isFollowing && followsBack) {
                            Button(
                                onClick = {
                                    navController.navigate("chat/$userId")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(
                                    Icons.Outlined.ChatBubble,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Chat",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    
                    // Estadísticas
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (val stats = statsState) {
                                is ProfileViewModel.StatsState.Success -> {
                                    StatItem(
                                        label = "Valoradas",
                                        value = stats.moviesRated.toString()
                                    )
                                    
                                    Divider(
                                        modifier = Modifier
                                            .height(60.dp)
                                            .width(1.dp),
                                        color = Color.White.copy(alpha = 0.2f)
                                    )
                                    
                                    StatItem(
                                        label = "Vistas",
                                        value = stats.moviesWatched.toString()
                                    )
                                }
                                else -> {
                                    StatItem(label = "Valoradas", value = "0")
                                    Divider(
                                        modifier = Modifier
                                            .height(60.dp)
                                            .width(1.dp),
                                        color = Color.White.copy(alpha = 0.2f)
                                    )
                                    StatItem(label = "Vistas", value = "0")
                                }
                            }
                        }
                    }
                    
                    // Películas recientes
                    Text(
                        text = "Películas recientes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    when (val recent = recentMoviesState) {
                        is ProfileViewModel.RecentMoviesState.Success -> {
                            if (recent.movies.isEmpty()) {
                                Text(
                                    "No hay películas recientes",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp)
                                )
                            } else {
                                // 2 películas por fila
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    recent.movies.chunked(2).forEach { rowMovies ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            rowMovies.forEach { movie ->
                                                Box(modifier = Modifier.weight(1f)) {
                                                    MoviePosterCard(
                                                        title = movie.title,
                                                        posterPath = movie.poster_path,
                                                        onClick = { selectedMovieTmdbId = movie.tmdb_id }
                                                    )
                                                }
                                            }
                                            // Relleno si solo hay 1 película en la fila
                                            if (rowMovies.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else -> {}
                    }
                    
                    // Películas favoritas
                    Text(
                        text = "Películas favoritas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    when (val favorites = favoriteMoviesState) {
                        is ProfileViewModel.FavoriteMoviesState.Success -> {
                            if (favorites.movies.isEmpty()) {
                                Text(
                                    "No hay películas favoritas",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp)
                                )
                            } else {
                                // 3 películas por fila
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    favorites.movies.chunked(3).forEach { rowMovies ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            rowMovies.forEach { movie ->
                                                Box(modifier = Modifier.weight(1f)) {
                                                    FavoritePosterCard(
                                                        title = movie.title,
                                                        posterPath = movie.poster_path,
                                                        onClick = { selectedMovieTmdbId = movie.tmdb_id }
                                                    )
                                                }
                                            }
                                            // Relleno si faltan películas en la fila
                                            repeat(3 - rowMovies.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
        
        // Popup de información de película
        selectedMovieTmdbId?.let { tmdbId ->
            MovieInfoDialog(
                tmdbId = tmdbId,
                onDismiss = { selectedMovieTmdbId = null },
                viewModel = movieViewModel,
                creditsState = creditsState
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}

@Composable
private fun MoviePosterCard(
    title: String,
    posterPath: String?,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (posterPath != null) {
                AsyncImage(
                    model = "https://image.tmdb.org/t/p/w500$posterPath",
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Movie,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoritePosterCard(
    title: String,
    posterPath: String?,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (posterPath != null) {
                AsyncImage(
                    model = "https://image.tmdb.org/t/p/w500$posterPath",
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Movie,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
