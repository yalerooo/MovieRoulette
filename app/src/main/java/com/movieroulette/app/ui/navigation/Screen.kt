package com.movieroulette.app.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Groups : Screen("groups")
    object CreateGroup : Screen("create_group")
    object JoinGroup : Screen("join_group")
    object GroupDetail : Screen("group_detail/{groupId}") {
        fun createRoute(groupId: String) = "group_detail/$groupId"
    }
    object SearchMovie : Screen("search_movie/{groupId}") {
        fun createRoute(groupId: String) = "search_movie/$groupId"
    }
    object MovieDetail : Screen("movie_detail/{movieId}") {
        fun createRoute(movieId: String) = "movie_detail/$movieId"
    }
    object Roulette : Screen("roulette/{groupId}") {
        fun createRoute(groupId: String) = "roulette/$groupId"
    }
    object RateMovie : Screen("rate_movie/{movieId}/{groupId}") {
        fun createRoute(movieId: String, groupId: String) = "rate_movie/$movieId/$groupId"
    }
    object MovieRatings : Screen("movie_ratings/{movieId}/{groupId}") {
        fun createRoute(movieId: String, groupId: String) = "movie_ratings/$movieId/$groupId"
    }
    object AddRating : Screen("add_rating/{movieId}") {
        fun createRoute(movieId: String) = "add_rating/$movieId"
    }
    object MoviesList : Screen("movies_list/{groupId}/{status}") {
        fun createRoute(groupId: String, status: String) = "movies_list/$groupId/$status"
    }
    object EditProfile : Screen("edit_profile")
    object EditGroup : Screen("edit_group/{groupId}") {
        fun createRoute(groupId: String) = "edit_group/$groupId"
    }
    object GroupMembers : Screen("group_members/{groupId}") {
        fun createRoute(groupId: String) = "group_members/$groupId"
    }
    object Settings : Screen("settings")
}
