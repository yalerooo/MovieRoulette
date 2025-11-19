package com.movieroulette.app.ui.screens.friends

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.movieroulette.app.data.model.DecryptedChatMessage
import com.movieroulette.app.data.model.MessageStatus
import com.movieroulette.app.data.model.MessageType
import com.movieroulette.app.data.model.Movie
import com.movieroulette.app.data.model.TMDBMovie
import com.movieroulette.app.ui.screens.movies.MovieDetailDialog
import com.movieroulette.app.viewmodel.ChatViewModel
import com.movieroulette.app.viewmodel.GroupViewModel
import com.movieroulette.app.viewmodel.MovieViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    userId: String,
    navController: NavController,
    viewModel: ChatViewModel = viewModel(),
    movieViewModel: MovieViewModel = viewModel(),
    groupViewModel: GroupViewModel = viewModel()
) {
    val context = LocalContext.current
    val messagesState by viewModel.messagesState.collectAsState()
    val otherUserProfile by viewModel.otherUserProfile.collectAsState()
    var messageText by remember { mutableStateOf("") }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showMovieSelector by remember { mutableStateOf(false) }
    var showMovieDetail by remember { mutableStateOf<TMDBMovie?>(null) }
    var pendingMovieId by remember { mutableStateOf<Int?>(null) } // Para buscar película al hacer click
    val listState = rememberLazyListState()
    
    // Estados para búsqueda de películas
    val movieSearchState by movieViewModel.searchState.collectAsState()
    val groupsState by groupViewModel.groupsState.collectAsState()
    val creditsState by movieViewModel.movieCreditsState.collectAsState()
    val addMovieState by movieViewModel.addMovieState.collectAsState()
    
    val movieSearchResults = when (val state = movieSearchState) {
        is MovieViewModel.SearchState.Success -> state.movies
        else -> emptyList()
    }
    val isSearching = movieSearchState is MovieViewModel.SearchState.Loading
    
    val userGroups = when (val state = groupsState) {
        is GroupViewModel.GroupsState.Success -> state.groups
        else -> emptyList()
    }
    
    // Cargar grupos del usuario
    LaunchedEffect(Unit) {
        groupViewModel.loadUserGroups()
    }
    
    // Cuando se cargan resultados de búsqueda y hay un pendingMovieId, mostrar el diálogo
    LaunchedEffect(movieSearchState, pendingMovieId) {
        if (pendingMovieId != null && movieSearchState is MovieViewModel.SearchState.Success) {
            val movie = movieSearchResults.firstOrNull { it.id == pendingMovieId }
                ?: movieSearchResults.firstOrNull() // Fallback
            movie?.let {
                showMovieDetail = it
                pendingMovieId = null // Resetear
            }
        }
    }
    
    // Cargar créditos cuando se muestra el detalle de una película
    LaunchedEffect(showMovieDetail) {
        showMovieDetail?.let { movie ->
            movieViewModel.loadMovieCredits(movie.id)
        }
    }
    
    // Launcher para seleccionar imagen
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        uri?.let {
            viewModel.sendImage(context, it, userId)
        }
    }
    
    LaunchedEffect(userId) {
        viewModel.initializeChat(context, userId)
    }
    
    // Scroll al último mensaje cuando se cargan
    LaunchedEffect(messagesState) {
        if (messagesState is ChatViewModel.MessagesState.Success) {
            val messages = (messagesState as ChatViewModel.MessagesState.Success).messages
            if (messages.isNotEmpty()) {
                listState.scrollToItem(messages.size - 1)
            }
        }
    }
    
    // Auto-scroll cuando llegan nuevos mensajes
    LaunchedEffect((messagesState as? ChatViewModel.MessagesState.Success)?.messages?.size) {
        if (messagesState is ChatViewModel.MessagesState.Success) {
            val messages = (messagesState as ChatViewModel.MessagesState.Success).messages
            if (messages.isNotEmpty() && listState.layoutInfo.visibleItemsInfo.isNotEmpty()) {
                val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                if (messages.size - lastVisibleIndex <= 5) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }
        }
    }
    
    // Detectar cuando el usuario llega al principio para cargar mensajes antiguos
    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (listState.firstVisibleItemIndex == 0 && messagesState is ChatViewModel.MessagesState.Success) {
            viewModel.loadOlderMessages(userId)
        }
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.clickable { 
                            navController.navigate("user_profile/$userId")
                        }
                    ) {
                        if (otherUserProfile?.avatarUrl != null) {
                            AsyncImage(
                                model = otherUserProfile?.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = otherUserProfile?.username ?: "Cargando...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón de adjuntar con menú desplegable
                    Box {
                        IconButton(
                            onClick = { showAttachmentMenu = !showAttachmentMenu },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Añadir imagen o película",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showAttachmentMenu,
                            onDismissRequest = { showAttachmentMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Imagen") },
                                onClick = {
                                    showAttachmentMenu = false
                                    imagePickerLauncher.launch("image/*")
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Image, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Película") },
                                onClick = {
                                    showAttachmentMenu = false
                                    showMovieSelector = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Movie, contentDescription = null)
                                }
                            )
                        }
                    }
                    
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Mensaje") },
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        maxLines = 4
                    )
                    
                    FilledIconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(messageText.trim(), userId)
                                messageText = ""
                            }
                        },
                        modifier = Modifier.size(56.dp),
                        enabled = messageText.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, "Enviar")
                    }
                }
            }
        }
    ) { paddingValues ->
        when (val state = messagesState) {
            is ChatViewModel.MessagesState.Loading -> {
                // Mostrar pantalla vacía mientras se carga (sin spinner)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Text(
                            "Chat cifrado de extremo a extremo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Tus mensajes están protegidos con cifrado E2E",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            is ChatViewModel.MessagesState.Success -> {
                if (state.messages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Text(
                                "Chat cifrado de extremo a extremo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Tus mensajes están protegidos con cifrado E2E",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        state = listState
                    ) {
                        items(state.messages) { message ->
                            MessageBubble(
                                message = message,
                                onMovieClick = { tmdbId, title ->
                                    // Guardar el ID y buscar la película
                                    pendingMovieId = tmdbId
                                    movieViewModel.searchMovies(title)
                                }
                            )
                        }
                    }
                }
            }
            is ChatViewModel.MessagesState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Error",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
    
    // Diálogo selector de películas
    if (showMovieSelector) {
        Dialog(
            onDismissRequest = { showMovieSelector = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Buscar Película",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showMovieSelector = false }) {
                            Icon(Icons.Default.Close, "Cerrar")
                        }
                    }
                    
                    // Search bar
                    var searchQuery by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            if (it.length >= 3) {
                                movieViewModel.searchMovies(it)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        placeholder = { Text("Buscar...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Results
                    when {
                        isSearching -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        movieSearchResults.isEmpty() && searchQuery.length >= 3 -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No se encontraron películas")
                            }
                        }
                        movieSearchResults.isNotEmpty() -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(movieSearchResults) { movie ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                showMovieSelector = false
                                                // Enviar película al chat inmediatamente (sin mostrar popup)
                                                viewModel.sendMovie(movie.id, movie.title, movie.posterPath, userId)
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            // Poster
                                            AsyncImage(
                                                model = movie.posterPath?.let { "https://image.tmdb.org/t/p/w92$it" },
                                                contentDescription = movie.title,
                                                modifier = Modifier
                                                    .width(60.dp)
                                                    .height(90.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            
                                            // Info
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = movie.title,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (!movie.releaseDate.isNullOrEmpty()) {
                                                    Text(
                                                        text = movie.releaseDate.take(4),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                if (!movie.overview.isNullOrEmpty()) {
                                                    Text(
                                                        text = movie.overview,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        maxLines = 2,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Busca una película",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Diálogo detalle de película
    showMovieDetail?.let { movie ->
        // Mostrar selector de grupo si el usuario tiene grupos
        var showGroupSelector by remember { mutableStateOf(false) }
        
        MovieDetailDialog(
            movie = movie,
            onDismiss = {
                showMovieDetail = null
                movieViewModel.resetCreditsState()
            },
            onAddMovie = { showGroupSelector = true }, // Siempre mostrar el botón
            isLoading = addMovieState is MovieViewModel.AddMovieState.Loading,
            creditsState = creditsState
        )
        
        // Diálogo selector de grupo con diseño moderno
        if (showGroupSelector) {
            Dialog(
                onDismissRequest = { showGroupSelector = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.7f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Header con título y botón cerrar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Agregar a grupo",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Selecciona dónde añadir la película",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { showGroupSelector = false },
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Divider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        
                        // Lista de grupos
                        if (userGroups.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Group,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "No tienes grupos",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Crea un grupo primero para agregar películas",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(userGroups) { group ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                movieViewModel.addMovieToGroup(group.id, movie.id)
                                                showGroupSelector = false
                                                showMovieDetail = null
                                            },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        elevation = CardDefaults.cardElevation(
                                            defaultElevation = 2.dp,
                                            pressedElevation = 8.dp
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                // Imagen cuadrada del grupo con bordes redondeados
                                                Box(
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(
                                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (group.imageUrl != null) {
                                                        AsyncImage(
                                                            model = group.imageUrl,
                                                            contentDescription = group.name,
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    } else {
                                                        Icon(
                                                            Icons.Default.Group,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                }
                                                
                                                // Nombre del grupo
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = group.name,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "Toca para agregar",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                            
                                            // Icono de flecha
                                            Icon(
                                                Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
}

@Composable
fun MessageBubble(
    message: DecryptedChatMessage,
    onMovieClick: ((tmdbId: Int, title: String) -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isMine) 16.dp else 4.dp,
                bottomEnd = if (message.isMine) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isMine) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Mostrar contenido según el tipo de mensaje
                when (message.messageType) {
                    MessageType.IMAGE -> {
                        // Mostrar imagen
                        if (message.imageUrl != null) {
                            AsyncImage(
                                model = message.imageUrl,
                                contentDescription = "Imagen compartida",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(
                            text = message.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (message.isMine)
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    MessageType.MOVIE -> {
                        // Mostrar película (clickeable)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = message.movieTmdbId != null) {
                                    message.movieTmdbId?.let { tmdbId ->
                                        onMovieClick?.invoke(tmdbId, message.message.removePrefix("🎬 "))
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (message.isMine)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Poster de la película o icono por defecto
                                if (message.moviePosterUrl != null) {
                                    AsyncImage(
                                        model = message.moviePosterUrl,
                                        contentDescription = message.message,
                                        modifier = Modifier
                                            .width(50.dp)
                                            .height(75.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Movie,
                                        contentDescription = null,
                                        modifier = Modifier.size(50.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = message.message.removePrefix("🎬 "),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (message.isMine)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Toca para ver detalles",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (message.isMine)
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = if (message.isMine)
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                    MessageType.TEXT -> {
                        // Mensaje de texto normal
                        Text(
                            text = message.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (message.isMine)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formatTime(message.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (message.isMine)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    
                    // Mostrar icono de estado solo para mis mensajes
                    if (message.isMine) {
                        val icon = getStatusIcon(message.status)
                        val description = getStatusDescription(message.status)
                        
                        Icon(
                            imageVector = icon,
                            contentDescription = description,
                            modifier = Modifier.size(16.dp),
                            tint = if (message.status == MessageStatus.READ)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Obtiene el icono correspondiente al estado del mensaje
 */
private fun getStatusIcon(status: MessageStatus): ImageVector {
    return when (status) {
        MessageStatus.SENDING -> Icons.Default.Schedule // Reloj
        MessageStatus.SENT -> Icons.Default.Check // Un check (enviado)
        MessageStatus.READ -> Icons.Default.DoneAll // Dos checks (leído)
    }
}

/**
 * Obtiene la descripción del estado del mensaje
 */
private fun getStatusDescription(status: MessageStatus): String {
    return when (status) {
        MessageStatus.SENDING -> "Enviando"
        MessageStatus.SENT -> "Enviado"
        MessageStatus.READ -> "Leído"
    }
}

private fun formatTime(timestamp: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = inputFormat.parse(timestamp)
        date?.let { outputFormat.format(it) } ?: timestamp
    } catch (e: Exception) {
        timestamp
    }
}
