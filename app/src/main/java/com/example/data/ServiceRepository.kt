package com.example.data

import com.example.SupabaseManager
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ServiceRepository(private val cachedServiceDao: CachedServiceDao) {
    val allServices: Flow<List<CachedService>> = cachedServiceDao.getAllServices()
    val allProviderProfiles: Flow<List<CachedProviderProfile>> = cachedServiceDao.getAllProviderProfiles()

    // Combined flow that joins services with their profiles
    val servicesWithProfiles: Flow<List<Pair<CachedService, CachedProviderProfile?>>> =
        allServices.combine(allProviderProfiles) { services, profiles ->
            services.map { service ->
                val profile = profiles.find { it.id == service.providerId }
                Pair(service, profile)
            }
        }

    suspend fun syncData() {
        try {
            val dbServices = SupabaseManager.client.postgrest["support_services"]
                .select()
                .decodeList<DBSupportService>()
            
            val dbProfiles = SupabaseManager.client.postgrest["profiles"]
                .select()
                .decodeList<ProviderProfile>()

            val cachedServices = dbServices.mapNotNull {
                if (it.id != null) {
                    CachedService(
                        id = it.id,
                        providerId = it.providerId,
                        name = it.name,
                        description = it.description,
                        status = it.status,
                        category = it.category,
                        latitude = it.latitude,
                        longitude = it.longitude
                    )
                } else null
            }

            val cachedProfiles = dbProfiles.map {
                CachedProviderProfile(
                    id = it.id,
                    email = it.email,
                    companyName = it.companyName,
                    description = it.description,
                    phone = it.phone,
                    website = it.website
                )
            }

            cachedServiceDao.updateServices(cachedServices)
            cachedServiceDao.updateProviderProfiles(cachedProfiles)
        } catch (e: Exception) {
            // Handle error, optionally log it. Offline data will still be exposed from DB.
        }
    }
}
