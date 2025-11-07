package com.movieroulette.app.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.movieroulette.app.data.repository.AuthRepository
import com.movieroulette.app.ui.screens.auth.LoginScreen
import com.movieroulette.app.ui.screens.auth.RegisterScreen
import com.movieroulette.app.ui.screens.auth.SplashScreen
import com.movieroulette.app.ui.screens.groups.GroupsScreen
import com.movieroulette.app.ui.screens.groups.CreateGroupScreen
import com.movieroulette.app.ui.screens.groups.JoinGroupScreen
import com.movieroulette.app.ui.screens.groups.GroupDetailScreen
import com.movieroulette.app.ui.screens.groups.EditGroupScreen
import com.movieroulette.app.ui.screens.groups.GroupMembersScreen
import com.movieroulette.app.ui.screens.movies.SearchMovieScreen
import com.movieroulette.app.ui.screens.movies.MovieDetailScreen
import com.movieroulette.app.ui.screens.movies.MoviesListScreen
import com.movieroulette.app.ui.screens.movies.RateMovieScreen
import com.movieroulette.app.ui.screens.movies.MovieRatingsScreen
import com.movieroulette.app.ui.screens.profile.EditProfileScreen
import com.movieroulette.app.ui.screens.roulette.RouletteScreen
import com.movieroulette.app.ui.screens.rating.AddRatingScreen
import com.movieroulette.app.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.delay

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authRepository = remember { AuthRepository() }
    var startDestination by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        delay(1000) // Splash screen delay
        val isLoggedIn = authRepository.isUserLoggedIn()
        startDestination = if (isLoggedIn) {
            Screen.Groups.route
        } else {
            Screen.Login.route
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
            
            // Groups
            composable(Screen.Groups.route) {
                GroupsScreen(navController = navController)
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
                    groupId = groupId
                )
            }
            
            // Movies
            composable(
                route = Screen.SearchMovie.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                SearchMovieScreen(
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
            composable(Screen.EditProfile.route) {
                EditProfileScreen(navController = navController)
            }
            
            // Edit Group
            composable(
                route = Screen.EditGroup.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                EditGroupScreen(
                    navController = navController,
                    groupId = groupId
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
                    groupId = groupId
                )
            }
            
            // Settings
            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }
        }
    } else {
        SplashScreen()
    }
}
