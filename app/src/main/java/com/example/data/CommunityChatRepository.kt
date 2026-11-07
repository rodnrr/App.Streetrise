package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.SupabaseManager
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class CommunityChatRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val messageDao = database.communityMessageDao()
    private val prefs: SharedPreferences = context.getSharedPreferences("streetrise_chat_prefs", Context.MODE_PRIVATE)

    private val _isAlwaysOn = MutableStateFlow(prefs.getBoolean("always_on_chat_enabled", true))
    val isAlwaysOn: StateFlow<Boolean> = _isAlwaysOn.asStateFlow()

    private val _userProfileName = MutableStateFlow(prefs.getString("user_display_name", "Community Member #${(100..999).random()}") ?: "Neighbor")
    val userProfileName: StateFlow<String> = _userProfileName.asStateFlow()

    private val _userRole = MutableStateFlow(prefs.getString("user_display_role", "Community Member") ?: "Community Member")
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialMessagesIfEmpty()
            fetchRemoteMessagesIfAvailable()
        }
    }

    fun getAllMessages(): Flow<List<CommunityMessage>> {
        return messageDao.getAllMessages()
    }

    suspend fun sendMessage(
        content: String,
        category: String = "General",
        locationTag: String? = null,
        isPinned: Boolean = false
    ): CommunityMessage {
        val message = CommunityMessage(
            id = UUID.randomUUID().toString(),
            senderName = _userProfileName.value,
            senderRole = _userRole.value,
            senderId = getOrCreateDeviceId(),
            content = content.trim(),
            category = category,
            timestamp = System.currentTimeMillis(),
            reactionsCount = 0,
            isPinned = isPinned,
            locationTag = locationTag
        )

        // 1. Immediately store in Room DB
        messageDao.insertMessage(message)

        // 2. Try pushing to remote Supabase if available
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = com.example.SupabaseManager.client
                client.postgrest["community_messages"].insert(message)
            } catch (e: Throwable) {
                Log.d("ChatRepo", "Remote push skipped or table pending: ${e.message}")
            }
        }

        return message
    }

    suspend fun reactToMessage(messageId: String) {
        messageDao.incrementReaction(messageId)
    }

    fun toggleAlwaysOn(enabled: Boolean) {
        _isAlwaysOn.value = enabled
        prefs.edit().putBoolean("always_on_chat_enabled", enabled).apply()
    }

    fun updateProfile(name: String, role: String) {
        _userProfileName.value = name
        _userRole.value = role
        prefs.edit()
            .putString("user_display_name", name)
            .putString("user_display_role", role)
            .apply()
    }

    private fun getOrCreateDeviceId(): String {
        var id = prefs.getString("device_user_id", null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString("device_user_id", id).apply()
        }
        return id
    }

    private suspend fun seedInitialMessagesIfEmpty() {
        if (messageDao.getMessageCount() == 0) {
            val now = System.currentTimeMillis()
            val seed = listOf(
                CommunityMessage(
                    id = "seed-pinned-1",
                    senderName = "St. Anthony's Outreach",
                    senderRole = "Resource Provider",
                    content = "Hot lunch served 10:00 AM - 1:45 PM daily! To-go boxes and hydration packs available at the counter.",
                    category = "Meals & Food",
                    timestamp = now - 3600000 * 4,
                    reactionsCount = 14,
                    isPinned = true,
                    locationTag = "121 Golden Gate Ave"
                ),
                CommunityMessage(
                    id = "seed-msg-2",
                    senderName = "Marcus T.",
                    senderRole = "Community Member",
                    content = "Does anyone know if shower slots are still open at Downtown Center this afternoon?",
                    category = "Hygiene",
                    timestamp = now - 3600000 * 3,
                    reactionsCount = 2,
                    locationTag = "Downtown"
                ),
                CommunityMessage(
                    id = "seed-msg-3",
                    senderName = "Elena (Navigator)",
                    senderRole = "Outreach Worker",
                    content = "Yes Marcus! Just checked: 8 shower vouchers remaining until 3:30 PM. Free soap and fresh towels provided.",
                    category = "Hygiene",
                    timestamp = now - 3600000 * 2,
                    reactionsCount = 8,
                    locationTag = "Downtown Center"
                ),
                CommunityMessage(
                    id = "seed-msg-4",
                    senderName = "Civic Care Team",
                    senderRole = "Resource Provider",
                    content = "Cold weather alert: Clean dry blankets and heavy beanies being handed out at Civic Center Plaza near the fountain.",
                    category = "Shelter & Beds",
                    timestamp = now - 1800000,
                    reactionsCount = 19,
                    locationTag = "Civic Center"
                ),
                CommunityMessage(
                    id = "seed-msg-5",
                    senderName = "Dave K.",
                    senderRole = "Community Member",
                    content = "Can confirm! Picked up a blanket, volunteers are super kind. Thank you everyone!",
                    category = "General",
                    timestamp = now - 600000,
                    reactionsCount = 5,
                    locationTag = "Civic Center"
                )
            )
            messageDao.insertMessages(seed)
        }
    }

    private suspend fun fetchRemoteMessagesIfAvailable() {
        try {
            val client = com.example.SupabaseManager.client
            val remoteList = client.postgrest["community_messages"]
                .select()
                .decodeList<CommunityMessage>()
            if (remoteList.isNotEmpty()) {
                messageDao.insertMessages(remoteList)
            }
        } catch (e: Throwable) {
            Log.d("ChatRepo", "Supabase sync skipped: ${e.message}")
        }
    }
}
