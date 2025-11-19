package com.movieroulette.app.ui.navigation

import android.app.Activity
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.movieroulette.app.data.remote.SupabaseConfig
import com.movieroulette.app.data.repository.AuthRepository
import com.movieroulette.app.ui.screens.auth.LoginScreen
import com.movieroulette.app.ui.screens.auth.RegisterScreen
import com.movieroulette.app.ui.screens.auth.SetUsernameScreen
import com.movieroulette.app.ui.screens.auth.SplashScreen
import com.movieroulette.app.ui.screens.groups.GroupsScreen
import com.movieroulette.app.ui.screens.groups.CreateGroupScreen
import com.movieroulette.app.ui.screens.groups.JoinGroupScreen
import com.movieroulette.app.ui.screens.groups.GroupDetailScreen
import com.movieroulette.app.ui.screens.groups.EditGroupScreen
import com.movieroulette.app.ui.screens.groups.GroupMembersScreen
import com.movieroulette.app.ui.screens.movies.SearchMovieScreenNew
import com.movieroulette.app.ui.screens.movies.MovieDetailScreen
import com.movieroulette.app.ui.screens.movies.MoviesListScreen
import com.movieroulette.app.ui.screens.movies.RateMovieScreen
import com.movieroulette.app.ui.screens.movies.MovieRatingsScreen
import com.movieroulette.app.ui.screens.profile.EditProfileScreen
import com.movieroulette.app.ui.screens.profile.ManageFavoritesScreen
import com.movieroulette.app.ui.screens.profile.ProfileScreen
import com.movieroulette.app.ui.screens.roulette.RouletteScreen
import com.movieroulette.app.ui.screens.rating.AddRatingScreen
import com.movieroulette.app.ui.screens.settings.SettingsScreen
import com.movieroulette.app.ui.screens.friends.FriendsScreen
import com.movieroulette.app.ui.screens.friends.FollowNotificationsScreen
import com.movieroulette.app.ui.screens.friends.SearchUsersScreen
import com.movieroulette.app.ui.screens.friends.UserProfileScreen
import com.movieroulette.app.ui.screens.friends.ChatScreen
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authRepository = remember { AuthRepository() }
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    var startDestination by remember { mutableStateOf<String?>(null) }
    var isProcessingOAuth by remember { mutableStateOf(false) }
    var shouldNavigateToGroups by remember { mutableStateOf(false) }
    
    // Crear ViewModels compartidos a nivel de navegación
    val groupViewModel: com.movieroulette.app.viewmodel.GroupViewModel = viewModel()
    
    // Manejar OAuth callback
    LaunchedEffect(Unit) {
        if (isProcessingOAuth) return@LaunchedEffect
        
        val intent = activity?.intent
        val uri = intent?.data
        
        if (uri != null && uri.scheme == "com.movieroulette.app" && uri.host == "login") {
            isProcessingOAuth = true
            val fragment = uri.fragment
            if (fragment != null) {
                Log.d("AppNavigation", "Processing OAuth callback")
                val params = fragment.split("&").associate {
                    val parts = it.split("=")
                    if (parts.size == 2) parts[0] to parts[1] else "" to ""
                }
                
                val accessToken = params["access_token"]
                val refreshToken = params["refresh_token"]
                
                Log.d("AppNavigation", "Tokens extracted - accessToken: ${accessToken?.take(20)}..., refreshToken: ${refreshToken?.take(20)}...")
                
                if (accessToken != null && refreshToken != null) {
                    Log.d("AppNavigation", "Calling setSessionFromOAuth...")
                    try {
                        val result = authRepository.setSessionFromOAuth(accessToken, refreshToken)
                        if (result.isSuccess) {
                            Log.d("AppNavigation", "Session established successfully")
                            
                            // Verificar que realmente se guardó la sesión
                            val isLogged = authRepository.isUserLoggedIn()
                            Log.d("AppNavigation", "After setSession, isUserLoggedIn: $isLogged")
                            
                            // Verificar si necesita configurar el username
                            val needsUsername = authRepository.needsUsernameSetup()
                            Log.d("AppNavigation", "Needs username setup: $needsUsername")
                            
                            startDestination = Screen.Login.route
                            
                            // Guardar token FCM después de login exitoso
                            scope.launch {
                                try {
                                    com.movieroulette.app.utils.FCMTokenManager.refreshAndSaveToken()
                                } catch (e: Exception) {
                                    Log.e("AppNavigation", "Error guardando FCM token: ${e.message}")
                                }
                            }
                            
                            if (needsUsername) {
                                // Navegar a SetUsername
                                delay(100)
                                navController.navigate(Screen.SetUsername.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            } else {
                                // Navegar a Groups
                                shouldNavigateToGroups = true
                            }
                        } else {
                            Log.e("AppNavigation", "setSessionFromOAuth failed: ${result.exceptionOrNull()?.message}")
                            startDestination = Screen.Login.route
                        }
                    } catch (e: Exception) {
                        Log.e("AppNavigation", "Exception in setSessionFromOAuth", e)
                        startDestination = Screen.Login.route
                    }
                } else {
                    Log.e("AppNavigation", "Missing tokens! accessToken=$accessToken, refreshToken=$refreshToken")
                    startDestination = Screen.Login.route
                }
            } else {
                checkAuthAndNavigate(authRepository) { startDestination = it }
            }
        } else {
            checkAuthAndNavigate(authRepository) { startDestination = it }
        }
    }
    
    // Navegar a Groups después de autenticación
    LaunchedEffect(shouldNavigateToGroups, startDestination) {
        if (shouldNavigateToGroups && startDestination != null) {
            Log.d("AppNavigation", "Navigating to Groups...")
            delay(100) // Pequeño delay para asegurar que NavHost esté listo
            navController.navigate(Screen.Groups.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
            Log.d("AppNavigation", "Navigation to Groups completed")
            shouldNavigateToGroups = false
        }
    }
    
    if (startDestination != null) {
        NavHost(
            navController = navController,
            startDestination = startDestination!!
        ) {
            // Auth
            composable(Screen.Login.route) {
                LoginScreen(navController = navController)
            }
            
            composable(Screen.Register.route) {
                RegisterScreen(navController = navController)
            }
            
            composable(Screen.SetUsername.route) {
                SetUsernameScreen(navController = navController)
            }
            
            // Groups
            composable(Screen.Groups.route) {
                GroupsScreen(
                    navController = navController,
                    viewModel = groupViewModel
                )
            }
            
            composable(Screen.CreateGroup.route) {
                CreateGroupScreen(navController = navController)
            }
            
            composable(Screen.JoinGroup.route) {
                JoinGroupScreen(navController = navController)
            }
            
            composable(
                route = Screen.GroupDetail.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                GroupDetailScreen(
                    navController = navController,
                    groupId = groupId,
                    groupViewModel = groupViewModel
                )
            }
            
            // Movies
            composable(
                route = Screen.SearchMovie.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                SearchMovieScreenNew(
                    navController = navController,
                    groupId = groupId
                )
            }
            
            composable(
                route = Screen.MovieDetail.route,
                arguments = listOf(navArgument("movieId") { type = NavType.StringType })
            ) { backStackEntry ->
                val movieId = backStackEntry.arguments?.getString("movieId") ?: return@composable
                MovieDetailScreen(
                    navController = navController,
                    movieId = movieId
                )
            }
            
            composable(
                route = Screen.MoviesList.route,
                arguments = listOf(
                    navArgument("groupId") { type = NavType.StringType },
                    navArgument("status") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                val status = backStackEntry.arguments?.getString("status") ?: return@composable
                MoviesListScreen(
                    navController = navController,
                    groupId = groupId,
                    status = status
                )
            }
            
            // Roulette
            composable(
                route = Screen.Roulette.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                RouletteScreen(
                    navController = navController,
                    groupId = groupId
                )
            }
            
            // Rate Movie
            composable(
                route = Screen.RateMovie.route,
                arguments = listOf(
                    navArgument("movieId") { type = NavType.StringType },
                    navArgument("groupId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val movieId = backStackEntry.arguments?.getString("movieId") ?: return@composable
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                RateMovieScreen(
                    navController = navController,
                    movieId = movieId,
                    groupId = groupId
                )
            }
            
            // Movie Ratings
            composable(
                route = Screen.MovieRatings.route,
                arguments = listOf(
                    navArgument("movieId") { type = NavType.StringType },
                    navArgument("groupId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val movieId = backStackEntry.arguments?.getString("movieId") ?: return@composable
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                MovieRatingsScreen(
                    navController = navController,
                    movieId = movieId,
                    groupId = groupId
                )
            }
            
            // Rating
            composable(
                route = Screen.AddRating.route,
                arguments = listOf(navArgument("movieId") { type = NavType.StringType })
            ) { backStackEntry ->
                val movieId = backStackEntry.arguments?.getString("movieId") ?: return@composable
                AddRatingScreen(
                    navController = navController,
                    movieId = movieId
                )
            }
            
            // Profile
            composable(Screen.Profile.route) {
                ProfileScreen(navController = navController)
            }
            
            composable(Screen.EditProfile.route) {
                EditProfileScreen(navController = navController)
            }
            
            composable(Screen.ManageFavorites.route) {
                ManageFavoritesScreen(navController = navController)
            }
            
            // Edit Group
            composable(
                route = Screen.EditGroup.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                EditGroupScreen(
                    navController = navController,
                    groupId = groupId,
                    viewModel = groupViewModel
                )
            }
            
            // Group Members
            composable(
                route = Screen.GroupMembers.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                GroupMembersScreen(
                    navController = navController,
                    groupId = groupId,
                    groupViewModel = groupViewModel
                )
            }
            
            // Settings
            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }
            
            // Friends
            composable(Screen.Friends.route) {
                FriendsScreen(navController = navController)
            }
            
            // Notifications
            composable("notifications") {
                FollowNotificationsScreen(navController = navController)
            }
            
            // Search Users
            composable(Screen.SearchUsers.route) {
                SearchUsersScreen(navController = navController)
            }
            
            // User Profile
            composable(
                route = Screen.UserProfile.route,
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                android.util.Log.d("AppNavigation", "=== Navigating to UserProfile with userId: $userId ===")
                UserProfileScreen(userId = userId, navController = navController)
            }
            
            // Chat
            composable(
                route = Screen.Chat.route,
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                ChatScreen(userId = userId, navController = navController)
            }
        }
    } else {
        SplashScreen()
    }
}

private suspend fun checkAuthAndNavigate(authRepository: AuthRepository, setDestination: (String) -> Unit) {
    delay(1000) // Splash screen delay
    val isLoggedIn = authRepository.isUserLoggedIn()
    
    if (!isLoggedIn) {
        setDestination(Screen.Login.route)
        return
    }
    
    // Guardar token FCM si el usuario ya está logueado
    try {
        com.movieroulette.app.utils.FCMTokenManager.refreshAndSaveToken()
    } catch (e: Exception) {
        Log.e("AppNavigation", "Error guardando FCM token en checkAuth: ${e.message}")
    }
    
    // Verificar si el usuario necesita configurar username
    val needsSetup = authRepository.needsUsernameSetup()
    if (needsSetup) {
        setDestination(Screen.SetUsername.route)
    } else {
        setDestination(Screen.Groups.route)
    }
}
