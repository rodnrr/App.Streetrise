package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "community_messages")
data class CommunityMessage(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "sender_name")
    @SerialName("sender_name")
    val senderName: String,
    @ColumnInfo(name = "sender_role")
    @SerialName("sender_role")
    val senderRole: String = "Community Member",
    @ColumnInfo(name = "sender_id")
    @SerialName("sender_id")
    val senderId: String = "",
    val content: String,
    val category: String = "General",
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "reactions_count")
    @SerialName("reactions_count")
    val reactionsCount: Int = 0,
    @ColumnInfo(name = "is_pinned")
    @SerialName("is_pinned")
    val isPinned: Boolean = false,
    @ColumnInfo(name = "location_tag")
    @SerialName("location_tag")
    val locationTag: String? = null
)
