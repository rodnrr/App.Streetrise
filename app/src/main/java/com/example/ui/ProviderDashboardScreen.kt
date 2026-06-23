package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.SupabaseManager
import com.example.data.DBCoordinationRequest
import com.example.data.DBSupportService
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

data class SupportService(val id: String, val name: String, val description: String, val status: String)
data class CoordinationRequest(val id: String, val clientName: String, val serviceName: String, val status: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDashboardScreen(onSignOut: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var isCloudConnected by remember { mutableStateOf(false) }
    var isOperatingOnDb by remember { mutableStateOf(false) }
    
    // Support Service Creation Form state
    var newServiceName by remember { mutableStateOf("") }
    var newServiceDesc by remember { mutableStateOf("") }
    
    // UI state data (will start with mocks but pull from real Supabase when possible)
    val mockServices = listOf(
        SupportService("1", "Emergency Shelter bed", "Overnight bed check-in", "Active"),
        SupportService("2", "Food Kitchen Pantry", "Hot meals 12 PM - 2 PM", "Active")
    )
    val mockRequests = listOf(
        CoordinationRequest("101", "Alex Smith", "Emergency Shelter bed", "Pending"),
        CoordinationRequest("102", "Maria Garcia", "Food Kitchen Pantry", "Approved")
    )
    
    var services by remember { mutableStateOf(mockServices) }
    var requests by remember { mutableStateOf(mockRequests) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val currentUser = remember { SupabaseManager.client.auth.currentUserOrNull() }
    val userId = currentUser?.id

    // Pull from Supabase when screens or active user changes
    fun refreshData() {
        if (userId == null) return
        isLoading = true
        scope.launch {
            try {
                // Fetch services
                val dbServices = SupabaseManager.client.postgrest["support_services"]
                    .select()
                    .decodeList<DBSupportService>()
                
                services = dbServices.map { db ->
                    SupportService(
                        id = db.id ?: "",
                        name = db.name,
                        description = db.description,
                        status = db.status
                    )
                }
                
                // Fetch requests
                val dbRequests = SupabaseManager.client.postgrest["coordination_requests"]
                    .select()
                    .decodeList<DBCoordinationRequest>()
                
                requests = dbRequests.map { db ->
                    CoordinationRequest(
                        id = db.id ?: "",
                        clientName = db.clientName,
                        serviceName = db.serviceName,
                        status = db.status
                    )
                }
                
                isCloudConnected = true
            } catch (e: Exception) {
                android.util.Log.e("ProviderDashboard", "Database pull error, keeping local cache: ${e.message}")
                isCloudConnected = false
            } finally {
                isLoading = false
            }
        }
    }

    // Refresh initially
    LaunchedEffect(userId) {
        refreshData()
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false
                newServiceName = ""
                newServiceDesc = ""
            },
            title = { Text("Add Support Service") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newServiceName,
                        onValueChange = { newServiceName = it },
                        label = { Text("Service Name") },
                        modifier = Modifier.fillMaxWidth().testTag("add_service_name_input")
                    )
                    OutlinedTextField(
                        value = newServiceDesc,
                        onValueChange = { newServiceDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth().testTag("add_service_desc_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newServiceName.isNotBlank()) {
                            val name = newServiceName
                            val desc = newServiceDesc
                            showAddDialog = false
                            newServiceName = ""
                            newServiceDesc = ""
                            
                            scope.launch {
                                isOperatingOnDb = true
                                var operationSucceeded = false
                                if (userId != null) {
                                    try {
                                        val newDbVal = DBSupportService(
                                            providerId = userId,
                                            name = name,
                                            description = desc,
                                            status = "Active"
                                        )
                                        SupabaseManager.client.postgrest["support_services"].insert(newDbVal)
                                        operationSucceeded = true
                                    } catch (e: Exception) {
                                        android.util.Log.e("ProviderDashboard", "Supabase insert failed: ${e.message}")
                                    }
                                }
                                
                                // Local fallback or success reload
                                if (operationSucceeded) {
                                    refreshData()
                                } else {
                                    // Fallback to updating local mock list
                                    val newId = (services.size + 1).toString()
                                    services = services + SupportService(
                                        id = newId,
                                        name = name,
                                        description = desc,
                                        status = "Active"
                                    )
                                }
                                isOperatingOnDb = false
                            }
                        }
                    },
                    enabled = newServiceName.isNotBlank() && !isOperatingOnDb,
                    modifier = Modifier.testTag("add_service_confirm_button")
                ) {
                    Text(if (isOperatingOnDb) "Saving..." else "Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showAddDialog = false
                        newServiceName = ""
                        newServiceDesc = ""
                    },
                    modifier = Modifier.testTag("add_service_cancel_button")
                ) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("add_service_dialog")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Provider Space")
                        if (isCloudConnected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Cloud Connected",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Offline Mode / Mock Mode",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("provider_sign_out_button")
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out")
                    }
                },
                modifier = Modifier.testTag("provider_top_app_bar")
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("provider_add_service_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Service")
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("My Services") },
                    modifier = Modifier.testTag("provider_tab_my_services")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Requests") },
                    modifier = Modifier.testTag("provider_tab_requests")
                )
            }
            
            if (selectedTab == 0) {
                if (isLoading) {
                    ServicesSkeletonList()
                } else {
                    ServicesList(services)
                }
            } else {
                if (isLoading) {
                    RequestsSkeletonList()
                } else {
                    RequestsList(
                        requests = requests,
                    onApprove = { req ->
                        scope.launch {
                            isOperatingOnDb = true
                            var opSucceeded = false
                            try {
                                SupabaseManager.client.postgrest["coordination_requests"].update(
                                    mapOf("status" to "Approved")
                                ) {
                                    filter {
                                        eq("id", req.id)
                                    }
                                }
                                opSucceeded = true
                            } catch (e: Exception) {
                                android.util.Log.e("ProviderDashboard", "Supabase update failed: ${e.message}")
                            }
                            
                            if (opSucceeded) {
                                refreshData()
                            } else {
                                requests = requests.map { if (it.id == req.id) it.copy(status = "Approved") else it }
                            }
                            isOperatingOnDb = false
                        }
                    },
                    onDecline = { req ->
                        scope.launch {
                            isOperatingOnDb = true
                            var opSucceeded = false
                            try {
                                SupabaseManager.client.postgrest["coordination_requests"].update(
                                    mapOf("status" to "Declined")
                                ) {
                                    filter {
                                        eq("id", req.id)
                                    }
                                }
                                opSucceeded = true
                            } catch (e: Exception) {
                                android.util.Log.e("ProviderDashboard", "Supabase update failed: ${e.message}")
                            }
                            
                            if (opSucceeded) {
                                refreshData()
                            } else {
                                requests = requests.map { if (it.id == req.id) it.copy(status = "Declined") else it }
                            }
                            isOperatingOnDb = false
                        }
                    }
                )
            }
        }
    }
}
}

@Composable
fun ServicesList(services: List<SupportService>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("provider_services_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(services) { service ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("provider_service_card_${service.id}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = service.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = service.description, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    SuggestionChip(
                        onClick = { },
                        label = { Text(service.status) },
                        modifier = Modifier.testTag("provider_service_status_chip_${service.id}")
                    )
                }
            }
        }
    }
}

@Composable
fun RequestsList(
    requests: List<CoordinationRequest>,
    onApprove: (CoordinationRequest) -> Unit,
    onDecline: (CoordinationRequest) -> Unit
) {
    if (requests.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No incoming requests.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("provider_requests_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(requests) { request ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("provider_request_card_${request.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = request.clientName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "Requested: ${request.serviceName}", style = MaterialTheme.typography.bodyMedium)
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = request.status,
                                style = MaterialTheme.typography.labelLarge,
                                color = when(request.status) {
                                    "Approved" -> Color(0xFF4CAF50)
                                    "Declined" -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.secondary
                                },
                                modifier = Modifier.testTag("provider_request_status_text_${request.id}")
                            )
                            
                            if (request.status == "Pending") {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(
                                        onClick = { onDecline(request) },
                                        modifier = Modifier.testTag("provider_request_decline_${request.id}")
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Decline", tint = MaterialTheme.colorScheme.error)
                                    }
                                    IconButton(
                                        onClick = { onApprove(request) },
                                        modifier = Modifier.testTag("provider_request_approve_${request.id}")
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Approve", tint = Color(0xFF4CAF50))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServicesSkeletonList() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize().alpha(alpha).testTag("provider_services_skeleton_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(4) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(modifier = Modifier.width(140.dp).height(18.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), shape = MaterialTheme.shapes.small))
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(14.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small))
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth(0.8f).height(14.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small))
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.width(64.dp).height(24.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), shape = MaterialTheme.shapes.small))
                }
            }
        }
    }
}

@Composable
fun RequestsSkeletonList() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_skeleton_requests")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize().alpha(alpha).testTag("provider_requests_skeleton_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(3) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(modifier = Modifier.width(120.dp).height(18.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), shape = MaterialTheme.shapes.small))
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth(0.9f).height(14.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.width(60.dp).height(18.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small))
                            Box(modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small))
                        }
                    }
                }
            }
        }
    }
}
