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
data class MovieViewer(
    val id: String,
    @SerialName("movie_id")
    val movieId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("marked_at")
    val markedAt: String? = null,
    @SerialName("marked_by")
    val markedBy: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class MovieViewerWithProfile(
    val id: String,
    @SerialName("movie_id")
    val movieId: String,
    @SerialName("user_id")
    val userId: String,
    val username: String,
    @SerialName("display_name")
    val displayName: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("marked_at")
    val markedAt: String? = null
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
    @SerialName("added_by_username")
    val addedByUsername: String? = null,
    val status: String = "pending",
    @SerialName("total_ratings")
    val totalRatings: Int = 0,
    @SerialName("average_rating")
    val averageRating: Double? = null
)

@Serializable
data class RatingPrompt(
    val id: String,
    @SerialName("group_id")
    val groupId: String,
    @SerialName("movie_id")
    val movieId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("marked_by_user_id")
    val markedByUserId: String,
    @SerialName("prompt_shown")
    val promptShown: Boolean = false,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class DismissedRatingPrompt(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("movie_id")
    val movieId: String,
    @SerialName("created_at")
    val createdAt: String? = null
)

data class TMDBGenresResponse(
    val genres: List<TMDBGenre>
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

// Friends and social models
@Serializable
data class Friendship(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("friend_id") val friendId: String,
    val status: String, // pending, accepted, rejected
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class Follow(
    val id: String,
    @SerialName("follower_id") val followerId: String,
    @SerialName("following_id") val followingId: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class ChatMessage(
    val id: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    @SerialName("encrypted_content_sender") val encryptedContentSender: String,
    @SerialName("encrypted_key_sender") val encryptedKeySender: String,
    @SerialName("iv_sender") val ivSender: String,
    @SerialName("encrypted_content_receiver") val encryptedContentReceiver: String,
    @SerialName("encrypted_key_receiver") val encryptedKeyReceiver: String,
    @SerialName("iv_receiver") val ivReceiver: String,
    val read: Boolean = false,
    val status: String? = null, // sending, sent, read - nullable para compatibilidad
    @SerialName("message_type") val messageType: String = "text", // text, image, movie
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("movie_tmdb_id") val movieTmdbId: Int? = null,
    @SerialName("movie_poster_url") val moviePosterUrl: String? = null,
    @SerialName("created_at") val createdAt: String
)

// Estados de mensaje estilo WhatsApp
enum class MessageStatus {
    SENDING,  // Reloj - enviando
    SENT,     // Círculo vacío - llegó al servidor
    READ      // Círculo relleno - visto por el receptor
}

// Tipos de mensaje
enum class MessageType {
    TEXT,
    IMAGE,
    MOVIE
}

// Modelo local para mostrar mensajes descifrados
data class DecryptedChatMessage(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val message: String,
    val read: Boolean = false,
    val status: MessageStatus = MessageStatus.SENT,
    val messageType: MessageType = MessageType.TEXT,
    val imageUrl: String? = null,
    val movieTmdbId: Int? = null,
    val moviePosterUrl: String? = null,
    val createdAt: String,
    val isMine: Boolean = false
)

@Serializable
data class UserPublicKey(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("public_key") val publicKey: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

data class FriendWithProfile(
    val friendship: Friendship,
    val profile: UserProfile
)

data class FollowWithProfile(
    val follow: Follow,
    val profile: UserProfile,
    val isFollowingBack: Boolean = false
)
