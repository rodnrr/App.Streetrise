package com.example.ui

import android.app.Application
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.data.UserAccount
import com.example.data.UserAccountRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserAccountViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserAccountRepository(application.applicationContext)

    val account: StateFlow<UserAccount> = repository.account
    val isAccountSheetOpen = MutableStateFlow(false)
    val isSyncing = MutableStateFlow(false)
    val syncFeedback = MutableStateFlow<String?>(null)

    fun openAccountSheet() {
        isAccountSheetOpen.value = true
    }

    fun closeAccountSheet() {
        isAccountSheetOpen.value = false
    }

    fun signIn(email: String, name: String) {
        repository.signIn(email, name)
        syncUserData(0, 0, 0)
    }

    fun signOut() {
        repository.signOut(keepLocalData = true)
    }

    fun syncUserData(favoritesCount: Int, logsCount: Int, dialsCount: Int) {
        viewModelScope.launch {
            isSyncing.value = true
            syncFeedback.value = "Backing up favorites and call logs to cloud..."
            delay(900)
            repository.updateSyncMetrics(favoritesCount, logsCount, dialsCount)
            isSyncing.value = false
            syncFeedback.value = "All data preserved & synchronized!"
            delay(2500)
            syncFeedback.value = null
        }
    }
}

/**
 * Modern modal dialog for Optional User Login and Data Preservation / Cloud Sync.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAccountSheet(
    viewModel: UserAccountViewModel,
    favoritesCount: Int,
    callLogsCount: Int,
    dialsCount: Int,
    modifier: Modifier = Modifier
) {
    val account by viewModel.account.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val feedback by viewModel.syncFeedback.collectAsStateWithLifecycle()

    var inputEmail by remember(account.email) { mutableStateOf(account.email) }
    var inputName by remember(account.displayName) { mutableStateOf(if (account.isSignedIn) account.displayName else "") }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "syncSpin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngle"
    )

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("user_account_sheet"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Sheet Top Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = if (account.isSignedIn) "StreetRise Account" else "Account & Sync (Optional)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (account.isSignedIn) "Cloud Backup Active" else "Use anonymously or sign in to preserve data",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.closeAccountSheet() },
                        modifier = Modifier.testTag("close_account_sheet")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            // Body Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!account.isSignedIn) {
                    // --- GUEST / LOGGED OUT VIEW ---
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary)
                                Text("Optional Data Preservation", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Text(
                                "StreetRise is 100% free and fully functional without an account. Signing in is strictly optional to backup your favorites and preserve your check-in history across devices.",
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Feature benefits list
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Account Benefits (When Signed In):", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Favorite, null, tint = Color(0xFFE91E63), modifier = Modifier.size(18.dp))
                                Column {
                                    Text("Preserve Favorites & Notes", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text("Never lose bookmarked shelters, meals, or caseworkers", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Call, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                                Column {
                                    Text("Preserve Check-In Logs & Custom Dials", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text("Keep emergency dials and daily wellness responses safely backed up", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("Multi-Device Access", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text("Access your saved data seamlessly from any browser or phone", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }

                    // Sign In Form
                    Text("Sign In or Register (Optional)", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Your Name or Nickname") },
                        placeholder = { Text("e.g. Jordan M.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("account_input_name")
                    )

                    OutlinedTextField(
                        value = inputEmail,
                        onValueChange = { inputEmail = it },
                        label = { Text("Email Address") },
                        placeholder = { Text("e.g. user@example.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("account_input_email")
                    )

                    // Quick Demo Credentials chips for testing
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Or tap a quick demo account:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SuggestionChip(
                                onClick = {
                                    inputName = "Community Member"
                                    inputEmail = "member@streetrise.org"
                                },
                                label = { Text("member@streetrise.org", fontSize = 11.sp) }
                            )
                            SuggestionChip(
                                onClick = {
                                    inputName = "Alex Rivera"
                                    inputEmail = "alex.r@streetrise.org"
                                },
                                label = { Text("alex.r@streetrise.org", fontSize = 11.sp) }
                            )
                        }
                    }

                    // Action Button
                    Button(
                        onClick = {
                            viewModel.signIn(
                                email = inputEmail.ifBlank { "member@streetrise.org" },
                                name = inputName.ifBlank { "Community Member" }
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("account_sign_in_button")
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign In & Enable Cloud Preservation", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.closeAccountSheet() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Continue in Guest Mode (Local Only)")
                    }
                } else {
                    // --- SIGNED IN USER VIEW ---
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = account.displayName.take(1).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 20.sp
                                            )
                                        }
                                    }

                                    Column {
                                        Text(account.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(account.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF2E7D32)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(modifier = Modifier.size(6.dp).background(Color(0xFF00FF66), CircleShape))
                                        Text("Synced", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))

                            val lastSyncStr = remember(account.lastSyncTimestamp) {
                                if (account.lastSyncTimestamp > 0) {
                                    SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(account.lastSyncTimestamp))
                                } else {
                                    "Pending initial sync"
                                }
                            }
                            Text("Last backup: $lastSyncStr", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
                        }
                    }

                    // Sync metrics counters
                    Text("Preserved in Cloud:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Favorite, null, tint = Color(0xFFE91E63), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$favoritesCount", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                Text("Favorites", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.DateRange, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$callLogsCount", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                Text("Check-In Logs", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Call, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$dialsCount", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                Text("Speed Dials", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    // Sync Now Button
                    Button(
                        onClick = {
                            viewModel.syncUserData(favoritesCount, callLogsCount, dialsCount)
                        },
                        enabled = !isSyncing,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("account_sync_now_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(if (isSyncing) spinAngle else 0f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isSyncing) "Syncing to Cloud..." else "Sync & Backup Now", fontWeight = FontWeight.Bold)
                    }

                    if (feedback != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = feedback ?: "",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sign out button
                    OutlinedButton(
                        onClick = { showSignOutConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("account_sign_out_button")
                    ) {
                        Icon(Icons.Default.ExitToApp, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out (Keep Local Data)")
                    }
                }
            }
        }
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Sign Out of Account?") },
            text = { Text("Your saved favorites, check-in history, and emergency dials will remain safely stored on this device in Guest Mode.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.signOut()
                        showSignOutConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Clean Optional Account Banner Card shown in Saved and Call screens.
 */
@Composable
fun OptionalAccountSyncBanner(
    userAccount: UserAccount,
    onOpenAccountSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (userAccount.isSignedIn) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (userAccount.isSignedIn) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenAccountSheet() }
            .testTag("optional_account_sync_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (userAccount.isSignedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (userAccount.isSignedIn) Icons.Default.Check else Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = if (userAccount.isSignedIn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = if (userAccount.isSignedIn) "Preserved: ${userAccount.displayName}" else "Cloud Preservation (Optional)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (userAccount.isSignedIn) "All favorites, logs & dials backed up" else "Sign in to backup favorites & check-in logs",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = if (userAccount.isSignedIn) "View Backup" else "Sign In",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}
