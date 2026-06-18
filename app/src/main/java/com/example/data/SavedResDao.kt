package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedResDao {

  @Query("SELECT * FROM saved_resources ORDER BY timestamp DESC")
  fun getAllSaved(): Flow<List<SavedRes>>

  @Query("SELECT * FROM saved_resources WHERE resourceId = :resourceId")
  suspend fun getById(resourceId: String): SavedRes?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(savedRes: SavedRes)

  @Update
  suspend fun update(savedRes: SavedRes)

  @Query("DELETE FROM saved_resources WHERE resourceId = :resourceId")
  suspend fun deleteById(resourceId: String)
}
