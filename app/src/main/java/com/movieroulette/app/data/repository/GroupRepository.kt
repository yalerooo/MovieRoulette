package com.movieroulette.app.data.repository

import com.movieroulette.app.data.model.Group
import com.movieroulette.app.data.model.GroupMemberWithProfile
import com.movieroulette.app.data.model.UserProfile
import com.movieroulette.app.data.remote.SupabaseConfig
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class GroupRepository {
    
    private val supabase = SupabaseConfig.client
    
    suspend fun getUserProfile(): Result<UserProfile> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: return@withContext Result.failure(Exception("User not logged in"))
                
                val profile = supabase.from("profiles")
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }
                    .decodeSingle<UserProfile>()
                
                Result.success(profile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun createGroup(name: String): Result<Group> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: return@withContext Result.failure(Exception("User not logged in"))
                
                // Generate unique invite code
                val inviteCode = generateInviteCode()
                
                // Create group
                val group = supabase.from("groups")
                    .insert(
                        mapOf(
                            "name" to name,
                            "invite_code" to inviteCode,
                            "created_by" to userId
                        )
                    ) {
                        select()
                    }
                    .decodeSingle<Group>()
                
                // Add creator as admin member
                supabase.from("group_members")
                    .insert(
                        mapOf(
                            "group_id" to group.id,
                            "user_id" to userId,
                            "role" to "admin"
                        )
                    )
                
                Result.success(group)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun joinGroupByCode(inviteCode: String): Result<Group> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: return@withContext Result.failure(Exception("User not logged in"))

                val normalizedCode = inviteCode.trim().uppercase()

                val params = buildJsonObject {
                    put("p_invite_code", normalizedCode)
                }

                val group = supabase.postgrest.rpc("join_group_by_code", params)
                    .decodeSingle<Group>()

                Result.success(group)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun getUserGroups(): Result<List<Group>> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: return@withContext Result.failure(Exception("User not logged in"))
                
                val groups = supabase.from("groups")
                    .select(
                        columns = Columns.raw("""
                            *,
                            group_members!inner(user_id)
                        """.trimIndent())
                    ) {
                        filter {
                            eq("group_members.user_id", userId)
                        }
                    }
                    .decodeList<Group>()
                
                Result.success(groups)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun getGroupMembers(groupId: String): Result<List<GroupMemberWithProfile>> {
        return withContext(Dispatchers.IO) {
            try {
                val members = supabase.from("group_members_with_profiles")
                    .select {
                        filter {
                            eq("group_id", groupId)
                        }
                    }
                    .decodeList<GroupMemberWithProfile>()
                
                Result.success(members)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun leaveGroup(groupId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: return@withContext Result.failure(Exception("User not logged in"))
                
                supabase.from("group_members")
                    .delete {
                        filter {
                            eq("group_id", groupId)
                            eq("user_id", userId)
                        }
                    }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun updateGroupImage(groupId: String, imageUrl: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                supabase.from("groups")
                    .update(
                        mapOf("image_url" to imageUrl)
                    ) {
                        filter {
                            eq("id", groupId)
                        }
                    }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun updateUserAvatar(avatarUrl: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: return@withContext Result.failure(Exception("User not logged in"))
                
                supabase.from("profiles")
                    .update(
                        mapOf("avatar_url" to avatarUrl)
                    ) {
                        filter {
                            eq("id", userId)
                        }
                    }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun removeMemberFromGroup(groupId: String, userId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val params = buildJsonObject {
                    put("p_group_id", groupId)
                    put("p_target_user", userId)
                }

                supabase.postgrest.rpc("remove_group_member", params)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8)
            .map { chars.random() }
            .joinToString("")
    }
}
