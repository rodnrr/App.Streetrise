package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CommunityMessage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * Movable Floating Notification Bubble with Magnetic Edge Snapping
 * Can be freely dragged across the screen so it never blocks page content.
 * Magnetically springs to the nearest screen edge on release.
 * Tapping opens the full Community Chat sheet.
 */
@Composable
fun AlwaysOnChatFloatingWidget(
    viewModel: CommunityChatViewModel,
    modifier: Modifier = Modifier
) {
    MovableFloatingChatBubble(viewModel = viewModel, modifier = modifier)
}

@Composable
fun MovableFloatingChatBubble(
    viewModel: CommunityChatViewModel,
    modifier: Modifier = Modifier
) {
    val isAlwaysOn by viewModel.isAlwaysOn.collectAsState()
    val isChatOpen by viewModel.isChatSheetOpen.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()

    // Pulsing animation for live status beacon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    if (isAlwaysOn && !isChatOpen) {
        BoxWithConstraints(
            modifier = modifier.fillMaxSize()
        ) {
            val density = LocalDensity.current
            val coroutineScope = rememberCoroutineScope()

            val bubbleSizeDp = 58.dp
            val marginDp = 14.dp
            val bubbleSizePx = with(density) { bubbleSizeDp.toPx() }
            val marginPx = with(density) { marginDp.toPx() }
            val screenWidthPx = with(density) { maxWidth.toPx() }
            val screenHeightPx = with(density) { maxHeight.toPx() }

            // Safe vertical boundaries: stay below top bar & above bottom navigation bar
            val minY = with(density) { 68.dp.toPx() }
            val maxY = (screenHeightPx - bubbleSizePx - with(density) { 90.dp.toPx() }).coerceAtLeast(minY)

            // Left and Right snap anchor positions
            val snapLeftX = marginPx
            val snapRightX = (screenWidthPx - bubbleSizePx - marginPx).coerceAtLeast(snapLeftX)

            // State for dragging and spring snapping
            val offsetX = remember { Animatable(snapRightX) }
            val offsetY = remember { Animatable((screenHeightPx * 0.62f).coerceIn(minY, maxY)) }

            // Track drag distance to differentiate single tap vs drag
            var totalDragDistance by remember { mutableFloatStateOf(0f) }

            // Update bounds if screen dimensions rotate or resize
            LaunchedEffect(screenWidthPx, screenHeightPx) {
                if (offsetX.value > screenWidthPx / 2f) {
                    offsetX.snapTo(snapRightX)
                } else {
                    offsetX.snapTo(snapLeftX)
                }
                offsetY.snapTo(offsetY.value.coerceIn(minY, maxY))
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                    .size(bubbleSizeDp)
                    .shadow(elevation = 10.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        ),
                        shape = CircleShape
                    )
                    .pointerInput(screenWidthPx, screenHeightPx) {
                        detectDragGestures(
                            onDragStart = {
                                totalDragDistance = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalDragDistance += dragAmount.getDistance()
                                coroutineScope.launch {
                                    val newX = (offsetX.value + dragAmount.x).coerceIn(0f, screenWidthPx - bubbleSizePx)
                                    val newY = (offsetY.value + dragAmount.y).coerceIn(minY, maxY)
                                    offsetX.snapTo(newX)
                                    offsetY.snapTo(newY)
                                }
                            },
                            onDragEnd = {
                                coroutineScope.launch {
                                    if (totalDragDistance < 12f) {
                                        // Tap detected: open the full Community Chat sheet
                                        viewModel.openChat()
                                    } else {
                                        // Magnetic snap to nearest horizontal edge
                                        val targetX = if (offsetX.value + (bubbleSizePx / 2f) < screenWidthPx / 2f) {
                                            snapLeftX
                                        } else {
                                            snapRightX
                                        }
                                        offsetX.animateTo(
                                            targetValue = targetX,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    val targetX = if (offsetX.value + (bubbleSizePx / 2f) < screenWidthPx / 2f) {
                                        snapLeftX
                                    } else {
                                        snapRightX
                                    }
                                    offsetX.animateTo(
                                        targetValue = targetX,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                }
                            }
                        )
                    }
                    .testTag("always_on_chat_widget"),
                contentAlignment = Alignment.Center
            ) {
                // Pulsing outer aura ring
                Box(
                    modifier = Modifier
                        .size(46.dp * pulseScale)
                        .clip(CircleShape)
                        .background(Color(0xFF00C853).copy(alpha = 0.18f))
                )

                // Central Chat Icon
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Community Chat",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )

                // Live status glowing beacon at bottom right
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 6.dp, end = 6.dp)
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(1.5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00C853))
                )

                // Unread Count Badge at top right
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                    ) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ) {
                            Text(
                                text = if (unreadCount > 99) "99+" else "$unreadCount",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full Community Chat Modal Sheet / View
 * Allows everyone to read and post community updates, filter categories,
 * react to helpful tips, and toggle the Always-On floating chat feature.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityChatSheet(
    viewModel: CommunityChatViewModel,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    val isAlwaysOn by viewModel.isAlwaysOn.collectAsState()
    val messages by viewModel.filteredMessages.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val userName by viewModel.userProfileName.collectAsState()
    val userRole by viewModel.userRole.collectAsState()

    var messageText by remember { mutableStateOf("") }
    var inputCategory by remember { mutableStateOf("General") }
    var showProfileDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val categories = listOf("All", "General", "Meals & Food", "Shelter & Beds", "Hygiene", "Urgent")
    val quickPrompts = listOf(
        "🍲 Hot meals serving now",
        "🛏️ Shelter beds open tonight",
        "🧼 Showers available today",
        "⚡ Safe place to charge phone",
        "🧣 Extra warm supplies available"
    )

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("community_chat_sheet"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Header with Title & Always-On Chat Toggle
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Chat",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "StreetRise Community Chat",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Real-time mutual support & live resource updates",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { onClose?.invoke() ?: viewModel.closeChat() },
                            modifier = Modifier.testTag("chat_sheet_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Chat",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Prominent Always-On Toggle Control Row
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAlwaysOn) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isAlwaysOn) Icons.Default.CheckCircle else Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = if (isAlwaysOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Always-On Chat Float",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isAlwaysOn) "Active: floating bubble is visible on every screen"
                                        else "Turn on to keep chat bubble accessible everywhere",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = isAlwaysOn,
                                onCheckedChange = { viewModel.toggleAlwaysOn(it) },
                                modifier = Modifier.testTag("toggle_always_on_chat")
                            )
                        }
                    }
                }
            }

            // 2. User Identity Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Posting as:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SuggestionChip(
                        onClick = { showProfileDialog = true },
                        label = {
                            Text(
                                text = "$userName ($userRole)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        },
                        icon = {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        )
                    )
                }

                TextButton(
                    onClick = { showProfileDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Change Name", fontSize = 11.sp)
                }
            }

            // 3. Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectCategory(cat) },
                        label = { Text(cat, fontSize = 12.sp) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        modifier = Modifier.testTag("chat_filter_$cat")
                    )
                }
            }

            // 4. Message Stream List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No messages in this category yet. Be the first to post!",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    CommunityMessageCard(
                        message = msg,
                        onReact = { viewModel.reactToMessage(msg.id) }
                    )
                }
            }

            // 5. Quick Community Prompt Suggestions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickPrompts.forEach { prompt ->
                    AssistChip(
                        onClick = {
                            messageText = prompt.substringAfter(" ")
                            if (prompt.contains("Food")) inputCategory = "Meals & Food"
                            else if (prompt.contains("Shelter")) inputCategory = "Shelter & Beds"
                            else if (prompt.contains("Shower")) inputCategory = "Hygiene"
                        },
                        label = { Text(prompt, fontSize = 11.sp) }
                    )
                }
            }

            // 6. Composer Input Bar
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Chat with everyone... (e.g., questions, tips, open beds)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input_field"),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 3,
                            trailingIcon = {
                                if (messageText.isNotEmpty()) {
                                    IconButton(onClick = { messageText = "" }) {
                                        Icon(Icons.Default.Clear, "Clear")
                                    }
                                }
                            }
                        )

                        FilledIconButton(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    viewModel.sendMessage(
                                        content = messageText,
                                        category = inputCategory
                                    )
                                    messageText = ""
                                }
                            },
                            enabled = messageText.isNotBlank(),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("chat_send_button")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send Message")
                        }
                    }
                }
            }
        }
    }

    // Profile customizer dialog
    if (showProfileDialog) {
        var tempName by remember { mutableStateOf(userName) }
        var tempRole by remember { mutableStateOf(userRole) }

        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Customize Chat Handle", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Choose how you appear to others in the StreetRise public chat channel:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Display Name / Alias") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("Your Role in the Community:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    listOf("Community Member", "Outreach Worker", "Resource Provider", "Anonymous Neighbor").forEach { role ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tempRole = role }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = tempRole == role,
                                onClick = { tempRole = role }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(role, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProfile(
                            if (tempName.isBlank()) "Neighbor" else tempName.trim(),
                            tempRole
                        )
                        showProfileDialog = false
                    }
                ) {
                    Text("Save Handle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Single Message Card displaying sender identity, role badge, category, and reactions.
 */
@Composable
fun CommunityMessageCard(
    message: CommunityMessage,
    onReact: () -> Unit,
    modifier: Modifier = Modifier
) {
    val roleColor = when (message.senderRole) {
        "Resource Provider" -> MaterialTheme.colorScheme.primary
        "Outreach Worker" -> Color(0xFFE65100) // Deep Amber
        "Anonymous Neighbor" -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.secondary
    }

    val roleBgColor = when (message.senderRole) {
        "Resource Provider" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        "Outreach Worker" -> Color(0xFFFFE0B2)
        "Anonymous Neighbor" -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    }

    val timeFormatted = remember(message.timestamp) {
        val diff = System.currentTimeMillis() - message.timestamp
        when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            else -> SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(message.timestamp))
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (message.isPinned) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = if (message.isPinned) {
            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary)
        } else null,
        modifier = modifier
            .fillMaxWidth()
            .testTag("chat_message_${message.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Pinned indicator
            if (message.isPinned) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Pinned",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "PINNED COMMUNITY ANNOUNCEMENT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // Top sender info & timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = message.senderName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        color = roleBgColor,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = message.senderRole,
                            color = roleColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = timeFormatted,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Message Body
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message.content,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 19.sp
            )

            // Bottom metadata & reactions
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category chip
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "#${message.category}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (message.locationTag != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = "📍 ${message.locationTag}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Reaction Button
                FilledTonalButton(
                    onClick = onReact,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("reaction_button_${message.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Helpful",
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (message.reactionsCount > 0) "${message.reactionsCount}" else "Helpful",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
