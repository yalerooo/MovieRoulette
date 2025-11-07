package com.movieroulette.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val username: String,
    @SerialName("display_name")
    val displayName: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class Group(
    val id: String,
    val name: String,
    @SerialName("invite_code")
    val inviteCode: String,
    @SerialName("created_by")
    val createdBy: String,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null
)

@Serializable
data class GroupMember(
    val id: String,
    @SerialName("group_id")
    val groupId: String,
    @SerialName("user_id")
    val userId: String,
    val role: String = "member",
    @SerialName("joined_at")
    val joinedAt: String? = null
)

@Serializable
data class GroupMemberWithProfile(
    val id: String,
    @SerialName("group_id")
    val groupId: String,
    @SerialName("user_id")
    val userId: String,
    val role: String,
    val username: String,
    @SerialName("display_name")
    val displayName: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null
)

@Serializable
data class Movie(
    val id: String,
    @SerialName("group_id")
    val groupId: String,
    @SerialName("tmdb_id")
    val tmdbId: Int,
    val title: String,
    @SerialName("original_title")
    val originalTitle: String? = null,
    val overview: String? = null,
    @SerialName("poster_path")
    val posterPath: String? = null,
    @SerialName("backdrop_path")
    val backdropPath: String? = null,
    @SerialName("release_date")
    val releaseDate: String? = null,
    val runtime: Int? = null,
    val genres: String? = null, // JSON string
    @SerialName("added_by")
    val addedBy: String,
    @SerialName("added_at")
    val addedAt: String? = null
)

@Serializable
data class MovieStatus(
    val id: String,
    @SerialName("movie_id")
    val movieId: String,
    @SerialName("group_id")
    val groupId: String,
    val status: String = "pending", // pending, watching, watched, dropped, removed
    @SerialName("started_watching_at")
    val startedWatchingAt: String? = null,
    @SerialName("finished_watching_at")
    val finishedWatchingAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class MovieRating(
    val id: String,
    @SerialName("movie_id")
    val movieId: String,
    @SerialName("user_id")
    val userId: String,
    val rating: Double,
    val comment: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    val username: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null
)

@Serializable
data class Profile(
    val id: String,
    val username: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null
)

@Serializable
data class MovieWithDetails(
    val id: String,
    @SerialName("group_id")
    val groupId: String,
    @SerialName("tmdb_id")
    val tmdbId: Int,
    val title: String,
    @SerialName("original_title")
    val originalTitle: String? = null,
    val overview: String? = null,
    @SerialName("poster_path")
    val posterPath: String? = null,
    @SerialName("backdrop_path")
    val backdropPath: String? = null,
    @SerialName("release_date")
    val releaseDate: String? = null,
    val runtime: Int? = null,
    val genres: String? = null,
    @SerialName("added_by")
    val addedBy: String,
    val status: String = "pending",
    @SerialName("total_ratings")
    val totalRatings: Int = 0,
    @SerialName("average_rating")
    val averageRating: Double? = null
)

// Request/Response models
@Serializable
data class CreateGroupRequest(
    val name: String
)

@Serializable
data class JoinGroupRequest(
    @SerialName("invite_code")
    val inviteCode: String
)

@Serializable
data class AddMovieRequest(
    @SerialName("group_id")
    val groupId: String,
    @SerialName("tmdb_id")
    val tmdbId: Int,
    val title: String,
    @SerialName("original_title")
    val originalTitle: String? = null,
    val overview: String? = null,
    @SerialName("poster_path")
    val posterPath: String? = null,
    @SerialName("backdrop_path")
    val backdropPath: String? = null,
    @SerialName("release_date")
    val releaseDate: String? = null,
    val runtime: Int? = null,
    val genres: String? = null
)

@Serializable
data class UpdateMovieStatusRequest(
    val status: String
)

@Serializable
data class AddRatingRequest(
    @SerialName("movie_id")
    val movieId: String,
    val rating: Double,
    val comment: String? = null
)
