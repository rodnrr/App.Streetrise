package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.SupabaseManager
import com.example.data.DBSupportService
import com.example.data.ProviderProfile
import io.github.jan.supabase.postgrest.postgrest
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.LocationServices
import androidx.compose.material.icons.filled.LocationOn

data class VerifiedServiceWithProvider(
    val service: DBSupportService,
    val provider: ProviderProfile?
)


@Composable
fun VerifiedServicesListComponent(
    modifier: Modifier = Modifier, 
    serviceViewModel: ServiceViewModel = viewModel(),
    savedResViewModel: com.example.data.SavedResViewModel = viewModel()
) {
    val servicesWithProfiles by serviceViewModel.servicesWithProfiles.collectAsState()
    val savedResources by savedResViewModel.resourcesState.collectAsState()
    var isLoading by remember { mutableStateOf(false) } // Removed loading block as we use reactive local DB
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val fusedLocationClient = remember(context) { LocationServices.getFusedLocationProviderClient(context) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    location?.let {
                        userLocation = LatLng(it.latitude, it.longitude)
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }
    
    val dbServices = remember(servicesWithProfiles) {
        servicesWithProfiles.map { (service, profile) ->
            VerifiedServiceWithProvider(
                service = DBSupportService(
                    id = service.id,
                    providerId = service.providerId,
                    name = service.name,
                    description = service.description,
                    status = service.status,
                    category = service.category,
                    latitude = service.latitude,
                    longitude = service.longitude
                ),
                provider = profile?.let {
                    ProviderProfile(
                        id = it.id,
                        email = it.email,
                        companyName = it.companyName,
                        description = it.description,
                        phone = it.phone,
                        website = it.website
                    )
                }
            )
        }
    }

    // Convert local seed items into VerifiedServiceWithProvider objects
    val seedServices = remember(savedResources) {
        savedResources.map { item ->
            VerifiedServiceWithProvider(
                service = DBSupportService(
                    id = item.id,
                    providerId = item.id + "_org",
                    name = item.name,
                    description = item.description.ifBlank { "No description available." },
                    status = "verified",
                    category = item.category.ifBlank { "general" },
                    latitude = item.lat,
                    longitude = item.lng
                ),
                provider = ProviderProfile(
                    id = item.id + "_org",
                    email = "",
                    companyName = item.org,
                    description = item.hours, // using description for hours
                    phone = item.phone.ifBlank { null },
                    website = item.website.ifBlank { null }
                )
            )
        }
    }

    val services = remember(dbServices, seedServices) { dbServices + seedServices }

    LaunchedEffect(Unit) {
        serviceViewModel.forceSync()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Verified Support Services",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return
        }

        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = errorMessage ?: "Unknown error",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            return
        }

        if (services.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No verified services available right now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return
        }

        var searchQuery by remember { mutableStateOf("") }
        val filteredServices = remember(services, searchQuery, userLocation) {
            val filtered = services.filter {
                it.service.name.contains(searchQuery, ignoreCase = true) ||
                it.service.category?.contains(searchQuery, ignoreCase = true) == true
            }
            if (userLocation != null) {
                filtered.sortedBy { 
                    if (it.service.latitude != null && it.service.longitude != null) {
                        val results = FloatArray(1)
                        Location.distanceBetween(
                            userLocation!!.latitude, userLocation!!.longitude,
                            it.service.latitude, it.service.longitude,
                            results
                        )
                        results[0]
                    } else {
                        Float.MAX_VALUE
                    }
                }
            } else {
                filtered
            }
        }

        // Center map roughly on a default location or the first service
        val defaultLatLng = remember { LatLng(28.5383, -81.3792) } // Orlando, Florida
        val startingLocation = remember(userLocation, filteredServices) {
            userLocation ?: filteredServices.mapNotNull { 
                if (it.service.latitude != null && it.service.longitude != null) 
                    LatLng(it.service.latitude, it.service.longitude) 
                else null 
            }.firstOrNull() ?: defaultLatLng
        }

        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(startingLocation, 10f)
        }

        LaunchedEffect(userLocation, startingLocation) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(userLocation ?: startingLocation, if (userLocation != null) 12f else 10f)
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                // Add user location marker if available
                if (userLocation != null) {
                    Marker(
                        state = com.google.maps.android.compose.rememberMarkerState(
                            position = userLocation!!
                        ),
                        title = "Your Location",
                        icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE)
                    )
                }
                
                // Limit markers to prevent UI thread freezing
                filteredServices.asSequence()
                    .filter { it.service.latitude != null && it.service.longitude != null }
                    .take(50)
                    .forEach { item ->
                        val markerId = item.service.id ?: item.hashCode().toString()
                        androidx.compose.runtime.key(markerId) {
                            Marker(
                                state = com.google.maps.android.compose.rememberMarkerState(
                                    key = markerId,
                                    position = LatLng(item.service.latitude!!, item.service.longitude!!)
                                ),
                                title = item.service.name,
                                snippet = item.service.category ?: "General"
                            )
                        }
                    }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search by service name or type...") },
                leadingIcon = { 
                    Icon(Icons.Default.Search, contentDescription = "Search") 
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            FilledIconButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        try {
                            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                                location?.let {
                                    userLocation = LatLng(it.latitude, it.longitude)
                                }
                            }
                        } catch (e: SecurityException) {
                            e.printStackTrace()
                        }
                    } else {
                        permissionLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    }
                },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Use My Location"
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = filteredServices,
                key = { it.service.id ?: it.hashCode() }
            ) { item ->
                VerifiedServiceCard(item)
            }
        }
    }
}

@Composable
fun VerifiedServiceCard(item: VerifiedServiceWithProvider) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = item.service.category?.uppercase() ?: "GENERAL",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = item.service.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                AssistChip(
                    onClick = { },
                    label = { Text("Verified") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified Badge",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    border = null
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = item.service.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            // Provider Info & Contact
            Text(
                text = "Provider Details",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.provider?.companyName ?: "Anonymous Provider",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            if (!item.provider?.phone.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.provider?.phone!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (!item.provider?.email.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.provider?.email!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
