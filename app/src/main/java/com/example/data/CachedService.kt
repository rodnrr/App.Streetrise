package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_services")
data class CachedService(
    @PrimaryKey val id: String,
    val providerId: String,
    val name: String,
    val description: String,
    val status: String,
    val category: String?,
    val latitude: Double?,
    val longitude: Double?
)

@Entity(tableName = "cached_provider_profiles")
data class CachedProviderProfile(
    @PrimaryKey val id: String,
    val email: String?,
    val companyName: String,
    val description: String?,
    val phone: String?,
    val website: String?
)
