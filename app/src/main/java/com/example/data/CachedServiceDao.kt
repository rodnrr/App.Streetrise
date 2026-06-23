package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedServiceDao {
    @Query("SELECT * FROM cached_services")
    fun getAllServices(): Flow<List<CachedService>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServices(services: List<CachedService>)

    @Query("DELETE FROM cached_services")
    suspend fun clearAllServices()

    @Transaction
    suspend fun updateServices(services: List<CachedService>) {
        clearAllServices()
        insertServices(services)
    }

    @Query("SELECT * FROM cached_provider_profiles")
    fun getAllProviderProfiles(): Flow<List<CachedProviderProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProviderProfiles(profiles: List<CachedProviderProfile>)

    @Query("DELETE FROM cached_provider_profiles")
    suspend fun clearAllProviderProfiles()

    @Transaction
    suspend fun updateProviderProfiles(profiles: List<CachedProviderProfile>) {
        clearAllProviderProfiles()
        insertProviderProfiles(profiles)
    }
}
