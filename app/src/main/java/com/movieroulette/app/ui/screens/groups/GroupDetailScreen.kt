package com.movieroulette.app.ui.screens.groups

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.movieroulette.app.data.model.MovieWithDetails
import com.movieroulette.app.data.model.toPosterUrl
import com.movieroulette.app.ui.components.*
import com.movieroulette.app.ui.navigation.Screen
import com.movieroulette.app.viewmodel.MovieViewModel
import com.movieroulette.app.viewmodel.GroupViewModel
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
    var showInviteDialog by remember { mutableStateOf(false) }
    
    val moviesState by viewModel.moviesState.collectAsState()
    val scope = rememberCoroutineScope()
    
    var allMovies by remember { mutableStateOf<List<MovieWithDetails>>(emptyList()) }
    var inviteCode by remember { mutableStateOf("") }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            0 -> viewModel.loadGroupMovies(groupId, status = "pending")
            1 -> viewModel.loadGroupMovies(groupId, status = "pending")
            2 -> viewModel.loadGroupMovies(groupId, status = "watching")
            3 -> viewModel.loadGroupMovies(groupId, status = "watched")
        }
        
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grupo", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { showInviteDialog = true }) {
                        Icon(Icons.Default.PersonAdd, "Invitar")
                    }
                    IconButton(onClick = { navController.navigate(Screen.GroupMembers.createRoute(groupId)) }) {
                        Icon(Icons.Default.Group, "Miembros")
                    }
                    IconButton(onClick = { navController.navigate(Screen.EditGroup.createRoute(groupId)) }) {
                        Icon(Icons.Default.Edit, "Editar grupo")
                    }
                    IconButton(onClick = { navController.navigate(Screen.SearchMovie.createRoute(groupId)) }) {
                        Icon(Icons.Default.Add, "Añadir película")
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
                    0 -> RouletteContent(groupId, viewModel, moviesState, isSpinning, selectedMovie, currentMovieIndex, hasSelectedMovie,
                        { isSpinning = it }, { selectedMovie = it }, { currentMovieIndex = it }, { hasSelectedMovie = it }, scope)
                    else -> {
                        val status = when (selectedTab) {
                            1 -> "pending"
                            2 -> "watching"
                            3 -> "watched"
                            else -> "pending"
                        }
                        MoviesListContent(groupId, status, moviesState, viewModel, scope, navController)
                    }
                }
            }
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TabButton(Icons.Default.Refresh, "Ruleta", selected = selectedTab == 0, onClick = { selectedTab = 0 })
                    TabButton(Icons.Default.List, "Pendiente", selected = selectedTab == 1, onClick = { selectedTab = 1 })
                    TabButton(Icons.Default.PlayArrow, "Viendo", selected = selectedTab == 2, onClick = { selectedTab = 2 })
                    TabButton(Icons.Default.Check, "Vistas", selected = selectedTab == 3, onClick = { selectedTab = 3 })
                }
            }
        }
    }
    
    // Diálogo de invitación
    if (showInviteDialog && inviteCode.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Invitar al Grupo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Comparte este código para que otros se unan:")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = inviteCode,
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(inviteCode))
                        android.widget.Toast.makeText(context, "Código copiado", android.widget.Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Copiar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

}

@Composable
fun RouletteContent(
    groupId: String, viewModel: MovieViewModel, moviesState: MovieViewModel.MoviesState,
    isSpinning: Boolean, selectedMovie: MovieWithDetails?, currentMovieIndex: Int, hasSelectedMovie: Boolean,
    onSpinningChange: (Boolean) -> Unit, onSelectedMovieChange: (MovieWithDetails?) -> Unit,
    onCurrentMovieIndexChange: (Int) -> Unit, onHasSelectedMovieChange: (Boolean) -> Unit, scope: kotlinx.coroutines.CoroutineScope
) {
    val movies = (moviesState as? MovieViewModel.MoviesState.Success)?.movies ?: emptyList()
    
    // Mostrar película aleatoria inicial
    LaunchedEffect(movies.size) {
        if (movies.isNotEmpty() && selectedMovie == null && !isSpinning) {
            onSelectedMovieChange(movies.random())
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
                                movies.isNotEmpty() && isSpinning && targetIndex >= 0 -> {
                                    AsyncImage(movies[targetIndex % movies.size].toPosterUrl(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                }
                                selectedMovie != null -> {
                                    AsyncImage(selectedMovie!!.toPosterUrl(), selectedMovie!!.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                }
                                movies.isNotEmpty() -> {
                                    AsyncImage(movies.first().toPosterUrl(), movies.first().title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                }
                                else -> Text("🎬", style = MaterialTheme.typography.displayLarge)
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
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        viewModel.updateMovieStatus(selectedMovie!!.id, groupId, "watching")
                                        onSelectedMovieChange(null)
                                        onHasSelectedMovieChange(false)
                                        delay(100)
                                        viewModel.loadGroupMovies(groupId, "pending")
                                        viewModel.loadGroupMovies(groupId, "watching")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Ver Ahora") }
                            OutlinedButton(
                                onClick = { 
                                    onSelectedMovieChange(null)
                                    onHasSelectedMovieChange(false)
                                    onCurrentMovieIndexChange(0)
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Otra") }
                        }
                    }
                }
                movies.isEmpty() -> Text("No hay películas en la ruleta", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> {
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
                        modifier = Modifier.fillMaxWidth(0.7f).height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(if (isSpinning) "Eligiendo..." else "Elegir película", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MoviesListContent(
    groupId: String, status: String, moviesState: MovieViewModel.MoviesState,
    viewModel: MovieViewModel, scope: kotlinx.coroutines.CoroutineScope, navController: NavController
) {
    var showDeleteDialog by remember { mutableStateOf<MovieWithDetails?>(null) }
    var currentUserId by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        currentUserId = viewModel.movieRepository.getCurrentUserId()
    }
    when (moviesState) {
        is MovieViewModel.MoviesState.Loading -> LoadingScreen()
        is MovieViewModel.MoviesState.Empty -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text("No hay películas", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        is MovieViewModel.MoviesState.Success -> {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(moviesState.movies, key = { it.id }) { movie ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { },
                                onLongClick = {
                                    // Mostrar diálogo solo si:
                                    // - En "pending" y el usuario agregó la película
                                    // - En "watched" (cualquiera puede eliminar)
                                    if ((status == "pending" && movie.addedBy == currentUserId) || status == "watched") {
                                        showDeleteDialog = movie
                                    }
                                }
                            ),
                        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AsyncImage(
                                model = movie.toPosterUrl(),
                                contentDescription = movie.title,
                                modifier = Modifier.width(80.dp).height(120.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(movie.title, style = MaterialTheme.typography.titleMedium)
                                movie.averageRating?.let {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Star,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text("$it/10", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                        }
                                        TextButton({ navController.navigate(Screen.MovieRatings.createRoute(movie.id, groupId)) }) { Text("Ver") }
                                    }
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    when (status) {
                                        "pending" -> Button(
                                            onClick = { 
                                                scope.launch { 
                                                    viewModel.updateMovieStatus(movie.id, groupId, "watching")
                                                    delay(100)
                                                    viewModel.loadGroupMovies(groupId, "pending")
                                                    viewModel.loadGroupMovies(groupId, "watching")
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(0.8f)
                                        ) { Text("Ver Ahora") }
                                        
                                        "watching" -> Row(
                                            modifier = Modifier.fillMaxWidth(0.95f),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { 
                                                    scope.launch { 
                                                        viewModel.updateMovieStatus(movie.id, groupId, "watched")
                                                        delay(100)
                                                        viewModel.loadGroupMovies(groupId, "watching")
                                                        viewModel.loadGroupMovies(groupId, "watched")
                                                        navController.navigate(Screen.RateMovie.createRoute(movie.id, groupId))
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) { Text("Finalizar") }
                                            OutlinedButton(
                                                onClick = { 
                                                    scope.launch { 
                                                        viewModel.updateMovieStatus(movie.id, groupId, "pending")
                                                        delay(100)
                                                        viewModel.loadGroupMovies(groupId, "watching")
                                                        viewModel.loadGroupMovies(groupId, "pending")
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) { Text("Pausar") }
                                        }
                                        
                                        "watched" -> Button(
                                            onClick = { navController.navigate(Screen.RateMovie.createRoute(movie.id, groupId)) },
                                            modifier = Modifier.fillMaxWidth(0.8f)
                                        ) { Text("Puntuar") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        is MovieViewModel.MoviesState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(moviesState.message, color = MaterialTheme.colorScheme.error) }
    }
    
    // Diálogo de confirmación para eliminar
    showDeleteDialog?.let { movie ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Eliminar Película") },
            text = { Text("¿Estás seguro de que quieres eliminar '${movie.title}' del grupo?") },
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
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
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
