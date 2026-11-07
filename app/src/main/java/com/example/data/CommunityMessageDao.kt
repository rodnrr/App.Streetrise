package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CommunityMessageDao {
    @Query("SELECT * FROM community_messages ORDER BY is_pinned DESC, timestamp ASC")
    fun getAllMessages(): Flow<List<CommunityMessage>>

    @Query("SELECT * FROM community_messages WHERE category = :category ORDER BY is_pinned DESC, timestamp ASC")
    fun getMessagesByCategory(category: String): Flow<List<CommunityMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: CommunityMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<CommunityMessage>)

    @Query("UPDATE community_messages SET reactions_count = reactions_count + 1 WHERE id = :messageId")
    suspend fun incrementReaction(messageId: String)

    @Query("SELECT COUNT(*) FROM community_messages")
    suspend fun getMessageCount(): Int

    @Query("DELETE FROM community_messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)
}
