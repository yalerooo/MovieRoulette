package com.movieroulette.app.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.movieroulette.app.R
import com.movieroulette.app.ui.navigation.Screen
import com.movieroulette.app.ui.screens.movies.MovieInfoDialog
import com.movieroulette.app.viewmodel.AuthViewModel
import com.movieroulette.app.viewmodel.GroupViewModel
import com.movieroulette.app.viewmodel.MovieViewModel
import com.movieroulette.app.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    groupViewModel: GroupViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel(),
    movieViewModel: MovieViewModel = viewModel()
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showAvatarDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMovieTmdbId by remember { mutableStateOf<Int?>(null) }
    
    val userProfileState by groupViewModel.userProfileState.collectAsState()
    val userProfile = (userProfileState as? GroupViewModel.UserProfileState.Success)?.profile
    
    // Estados del ProfileViewModel
    val statsState by profileViewModel.statsState.collectAsState()
    val recentMoviesState by profileViewModel.recentMoviesState.collectAsState()
    val favoriteMoviesState by profileViewModel.favoriteMoviesState.collectAsState()
    
    // Estado de créditos del MovieViewModel
    val creditsState by movieViewModel.movieCreditsState.collectAsState()
    
    // Launcher para seleccionar imagen cuando se amplía el avatar
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            navController.navigate(Screen.EditProfile.route)
        }
    }
    
    // Cargar datos al iniciar
    LaunchedEffect(Unit) {
        groupViewModel.loadUserProfile()
        profileViewModel.loadUserStats()
        profileViewModel.loadRecentMovies()
        profileViewModel.loadFavoriteMovies()
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Contenido principal
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (showAvatarDialog) Modifier.blur(20.dp)
                    else Modifier
                ),
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.profile)) },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, "Menu")
                            }
                            
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.settings)) },
                                    onClick = {
                                        showMenu = false
                                        navController.navigate(Screen.Settings.route)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Settings, null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            stringResource(R.string.logout),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showLogoutDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.ExitToApp,
                                            null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                            }
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
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Avatar con opción de editar
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clickable { showAvatarDialog = true }
                ) {
                    if (userProfile?.avatarUrl != null) {
                        AsyncImage(
                            model = userProfile.avatarUrl,
                            contentDescription = stringResource(R.string.avatar),
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
                                    icon = Icons.Default.Star,
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
                                icon = Icons.Default.Check,
                                label = stringResource(R.string.watched),
                                value = stats.moviesWatched.toString()
                            )
                        }
                        else -> {
                            StatItem(
                                icon = Icons.Default.Star,
                                label = "Valoradas",
                                value = "0"
                            )
                            
                            Divider(
                                modifier = Modifier
                                    .height(60.dp)
                                    .width(1.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            )
                            
                            StatItem(
                                icon = Icons.Default.Check,
                                label = stringResource(R.string.watched),
                                value = "0"
                            )
                        }
                    }
                }
            }
            
            // Últimas películas vistas
            when (val recent = recentMoviesState) {
                is ProfileViewModel.RecentMoviesState.Success -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Últimas películas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (recent.movies.isEmpty()) {
                            // Mensaje cuando no hay películas recientes
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Movie,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Este usuario no ha visto ninguna película",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.heightIn(max = 800.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                userScrollEnabled = false
                            ) {
                                items(recent.movies) { movie ->
                                    MoviePosterCard(
                                        title = movie.title,
                                        posterPath = movie.poster_path,
                                        onClick = {
                                            selectedMovieTmdbId = movie.tmdb_id
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                else -> { /* Loading o Error */ }
            }
            
            // Películas favoritas
            when (val favorites = favoriteMoviesState) {
                is ProfileViewModel.FavoriteMoviesState.Success -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Favoritas",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = { navController.navigate(Screen.ManageFavorites.route) }
                            ) {
                                Text("Gestionar")
                            }
                        }
                        
                        if (favorites.movies.isEmpty()) {
                            // Mensaje cuando no hay favoritas
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Este usuario no tiene películas favoritas",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier.heightIn(max = 800.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                userScrollEnabled = false
                            ) {
                                items(favorites.movies) { movie ->
                                    MoviePosterCard(
                                        title = movie.title,
                                        posterPath = movie.poster_path,
                                        onClick = {
                                            selectedMovieTmdbId = movie.tmdb_id
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                else -> { /* Loading o Error */ }
            }
        }
    }
        
        // Diálogo de ampliar avatar
        if (showAvatarDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showAvatarDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(enabled = false) { }
                    ) {
                        if (userProfile?.avatarUrl != null) {
                            AsyncImage(
                                model = userProfile.avatarUrl,
                                contentDescription = stringResource(R.string.avatar),
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
                                    contentDescription = null,
                                    modifier = Modifier.size(120.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        
                        // Botón editar (top-right)
                        IconButton(
                            onClick = {
                                showAvatarDialog = false
                                pickImageLauncher.launch("image/*")
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Diálogo de confirmación de logout
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.logout)) },
            text = { Text("¿Estás seguro de que quieres cerrar sesión?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        authViewModel.signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                ) {
                    Text(
                        stringResource(R.string.logout),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // Diálogo de información de película
    selectedMovieTmdbId?.let { tmdbId ->
        MovieInfoDialog(
            tmdbId = tmdbId,
            onDismiss = { selectedMovieTmdbId = null },
            viewModel = movieViewModel,
            creditsState = creditsState
        )
    }
}

@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun MoviePosterCard(
    title: String,
    posterPath: String?,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.67f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp
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
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
