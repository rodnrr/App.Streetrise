package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MessageSender
import com.example.data.RealTimeService
import com.example.data.SearchSource
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

/**
 * Prominent Emergency Call Button for Riley, the StreetRise Phone Assistant (463-2-STREET).
 */
@Composable
fun ProminentCallNowButton(
  onCallRiley: () -> Unit,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.15f,
    animationSpec = infiniteRepeatable(
      animation = tween(900, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_scale"
  )

  Surface(
    onClick = onCallRiley,
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.primary,
    contentColor = MaterialTheme.colorScheme.onPrimary,
    shadowElevation = 6.dp,
    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f)),
    modifier = modifier.testTag("prominent_call_now_button")
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Pulsing beacon dot
      Box(
        modifier = Modifier
          .size(12.dp)
          .background(Color(0xFF00FF66), CircleShape)
          .padding(2.dp)
      )

      Icon(
        imageVector = Icons.Default.Call,
        contentDescription = "Call Riley",
        modifier = Modifier.size(20.dp)
      )

      Column {
        Text(
          text = "Call 463-2-STREET",
          fontWeight = FontWeight.ExtraBold,
          fontSize = 13.sp,
          letterSpacing = 0.5.sp
        )
        Text(
          text = "Riley Phone Assistant",
          fontSize = 10.sp,
          color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
          fontWeight = FontWeight.Medium
        )
      }
    }
  }
}

/**
 * Native Google Map with Real-Time Service Markers found via Google Search.
 */
@Composable
fun RealTimeGoogleMapScreen(
  viewModel: RealTimeSearchViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val services by viewModel.searchResults.collectAsStateWithLifecycle()
  val selectedService by viewModel.selectedServiceForDetail.collectAsStateWithLifecycle()
  val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
  val searchNotice by viewModel.searchNotice.collectAsStateWithLifecycle()
  val searchSource by viewModel.searchSource.collectAsStateWithLifecycle()

  // Default center: Tampa Bay / Central Florida
  val tampaLatLng = LatLng(27.9506, -82.4572)
  val cameraPositionState = rememberCameraPositionState {
    position = CameraPosition.fromLatLngZoom(tampaLatLng, 10.5f)
  }

  // Update camera when selected service changes
  LaunchedEffect(selectedService) {
    selectedService?.let { svc ->
      if (svc.latitude != 0.0 && svc.longitude != 0.0) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(
          LatLng(svc.latitude, svc.longitude),
          13.5f
        )
      }
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    // 1. Google Maps View
    GoogleMap(
      modifier = Modifier.fillMaxSize().testTag("google_map_view"),
      cameraPositionState = cameraPositionState,
      properties = MapProperties(isMyLocationEnabled = false),
      uiSettings = MapUiSettings(
        zoomControlsEnabled = false,
        myLocationButtonEnabled = false,
        compassEnabled = true
      )
    ) {
      services.forEach { service ->
        val markerColor = when (service.category) {
          "food" -> BitmapDescriptorFactory.HUE_ORANGE
          "shelter" -> BitmapDescriptorFactory.HUE_AZURE
          "hygiene" -> BitmapDescriptorFactory.HUE_CYAN
          "medical" -> BitmapDescriptorFactory.HUE_ROSE
          else -> BitmapDescriptorFactory.HUE_VIOLET
        }

        Marker(
          state = MarkerState(position = LatLng(service.latitude, service.longitude)),
          title = service.name,
          snippet = "${service.category.uppercase()} • ${service.operatingHours}",
          icon = BitmapDescriptorFactory.defaultMarker(markerColor),
          onClick = {
            viewModel.selectedServiceForDetail.value = service
            false
          }
        )
      }
    }

    // 2. Status Badge Indicator
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
      tonalElevation = 4.dp,
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(top = 10.dp)
        .testTag("map_search_status_badge")
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        if (isSearching) {
          CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
          Text("Searching live services via Google...", fontSize = 11.sp, fontWeight = FontWeight.Medium)
        } else {
          Icon(
            imageVector = if (searchSource == SearchSource.GOOGLE_SEARCH_LIVE) Icons.Default.Star else Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
          )
          Text(
            text = searchNotice ?: "${services.size} services active on map",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }

    // 3. Bottom Service Detail Card Overlay
    selectedService?.let { service ->
      ServiceDetailBottomCard(
        service = service,
        onClose = { viewModel.selectedServiceForDetail.value = null },
        onCall = {
          val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${service.phone}"))
          context.startActivity(dialIntent)
        },
        onNavigate = {
          val geoIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("geo:${service.latitude},${service.longitude}?q=${service.latitude},${service.longitude}(${Uri.encode(service.name)})")
          )
          context.startActivity(geoIntent)
        },
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(16.dp)
          .fillMaxWidth()
      )
    }
  }
}

/**
 * Bottom Card displaying service details with Call & Navigation shortcuts.
 */
@Composable
fun ServiceDetailBottomCard(
  service: RealTimeService,
  onClose: () -> Unit,
  onCall: () -> Unit,
  onNavigate: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
    modifier = modifier.testTag("service_detail_bottom_card")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f)) {
          // Category & Live Badge
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.primaryContainer
            ) {
              Text(
                text = service.category.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
            if (service.isLiveVerified) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFE8F5E9)
              ) {
                Text(
                  text = "VERIFIED",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF1B5E20),
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = service.name,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.outline)
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = service.description,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Address & Hours
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(4.dp))
        Text(service.address, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.DateRange, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.width(4.dp))
        Text("Hours: ${service.operatingHours}", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Action buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = onCall,
          modifier = Modifier.weight(1f).height(38.dp).testTag("service_call_button"),
          contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
          Icon(Icons.Default.Call, null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Call ${service.phone.take(12)}", fontSize = 11.sp)
        }

        OutlinedButton(
          onClick = onNavigate,
          modifier = Modifier.weight(1f).height(38.dp).testTag("service_directions_button"),
          contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
          Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Directions", fontSize = 11.sp)
        }
      }
    }
  }
}

/**
 * Top Search Header with Google Search Tool Grounding & Category Filters.
 */
@Composable
fun RealTimeSearchHeader(
  viewModel: RealTimeSearchViewModel,
  onCallRiley: () -> Unit,
  modifier: Modifier = Modifier
) {
  val query by viewModel.searchQuery.collectAsStateWithLifecycle()
  val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
  val currentMode by viewModel.hubViewMode.collectAsStateWithLifecycle()

  val categories = listOf(
    Pair("shelter", "🏠 Shelters"),
    Pair("food", "🍲 Hot Food"),
    Pair("hygiene", "🚿 Hygiene"),
    Pair("medical", "🩺 Medical"),
    Pair("work_exchange", "🤝 Jobs")
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface)
      .padding(horizontal = 12.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // Row 1: Search text field & Prominent Call Now button
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      OutlinedTextField(
        value = query,
        onValueChange = { viewModel.searchQuery.value = it },
        placeholder = { Text("Search services with Google...", fontSize = 12.sp) },
        leadingIcon = {
          Icon(Icons.Default.Search, "Search", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        },
        trailingIcon = {
          if (query.isNotEmpty()) {
            IconButton(onClick = {
              viewModel.searchQuery.value = ""
              viewModel.executeSearch("", selectedCategory)
            }) {
              Icon(Icons.Default.Clear, "Clear", modifier = Modifier.size(16.dp))
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .testTag("real_time_search_input")
      )

      IconButton(
        onClick = { viewModel.executeSearch() },
        modifier = Modifier
          .size(44.dp)
          .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
          .testTag("trigger_google_search_btn")
      ) {
        Icon(Icons.Default.Search, "Execute Search", tint = MaterialTheme.colorScheme.onPrimaryContainer)
      }

      // Prominent Call Riley Button
      ProminentCallNowButton(onCallRiley = onCallRiley)
    }

    // Row 2: Category chips and view mode toggle
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Horizontal category chips
      Row(
        modifier = Modifier
          .weight(1f)
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        FilterChip(
          selected = selectedCategory == null,
          onClick = { viewModel.setCategoryFilter(null) },
          label = { Text("All", fontSize = 11.sp) },
          modifier = Modifier.height(30.dp)
        )

        categories.forEach { (catKey, label) ->
          FilterChip(
            selected = selectedCategory == catKey,
            onClick = {
              if (selectedCategory == catKey) {
                viewModel.setCategoryFilter(null)
              } else {
                viewModel.setCategoryFilter(catKey)
              }
            },
            label = { Text(label, fontSize = 11.sp) },
            modifier = Modifier.height(30.dp)
          )
        }
      }

      Spacer(modifier = Modifier.width(6.dp))

      // View Mode Toggle (Web Portal vs Map)
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.height(32.dp)
      ) {
        Row(
          modifier = Modifier.padding(2.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(14.dp))
              .background(if (currentMode == HubViewMode.WEB_PORTAL) MaterialTheme.colorScheme.primary else Color.Transparent)
              .clickable { viewModel.switchHubView(HubViewMode.WEB_PORTAL) }
              .padding(horizontal = 8.dp, vertical = 4.dp)
              .testTag("toggle_web_view_mode")
          ) {
            Text(
              "Web",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = if (currentMode == HubViewMode.WEB_PORTAL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(14.dp))
              .background(if (currentMode == HubViewMode.LIVE_SEARCH_MAP) MaterialTheme.colorScheme.primary else Color.Transparent)
              .clickable { viewModel.switchHubView(HubViewMode.LIVE_SEARCH_MAP) }
              .padding(horizontal = 8.dp, vertical = 4.dp)
              .testTag("toggle_map_view_mode")
          ) {
            Text(
              "Live Map",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = if (currentMode == HubViewMode.LIVE_SEARCH_MAP) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}

/**
 * Interactive Sheet simulating Riley, the StreetRise 24/7 Phone Assistant (463-2-STREET).
 * Includes live on-screen speech transcription, soundwave visualizer, and dialer integration.
 */
@Composable
fun RileyPhoneAssistantSheet(
  viewModel: RealTimeSearchViewModel,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val dialogue by viewModel.rileyDialogue.collectAsStateWithLifecycle()
  val callSeconds by viewModel.callDurationSeconds.collectAsStateWithLifecycle()
  val isTranscribing by viewModel.isRileyTranscribing.collectAsStateWithLifecycle()
  val currentSnippet by viewModel.currentTranscribingSnippet.collectAsStateWithLifecycle()

  var inputPrompt by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  // Auto-scroll to latest dialogue
  LaunchedEffect(dialogue.size, currentSnippet) {
    if (dialogue.isNotEmpty()) {
      listState.animateScrollToItem(dialogue.size)
    }
  }

  // Format mm:ss
  val formattedTimer = remember(callSeconds) {
    val mins = callSeconds / 60
    val secs = callSeconds % 60
    String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
  }

  Surface(
    modifier = modifier.fillMaxSize().testTag("riley_phone_assistant_sheet"),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // 1. Top Call Header (Phone call simulation)
      Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .background(Color(0xFF00FF66), CircleShape)
              )
              Text("Connected • $formattedTimer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
              Icon(Icons.Default.Close, contentDescription = "End Call", tint = MaterialTheme.colorScheme.onPrimary)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Avatar & Title
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
            modifier = Modifier.size(64.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text("SR", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimary)
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = "Riley — StreetRise Phone Assistant",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onPrimary
          )

          Text(
            text = "Official Hotline: 463-2-STREET (463-278-7338)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
          )

          Spacer(modifier = Modifier.height(10.dp))

          // Animated Audio Soundwave Visualizer
          SoundWaveVisualizer(isSpeaking = isTranscribing)
        }
      }

      // 2. Transcribed Text Dialogue View
      LazyColumn(
        state = listState,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        item {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "🎙️ Real-time speech-to-text transcript active. What Riley speaks is transcribed on your screen.",
              fontSize = 11.sp,
              textAlign = TextAlign.Center,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(8.dp)
            )
          }
        }

        items(dialogue, key = { it.id }) { msg ->
          DialogueBubble(message = msg)
        }

        // Live streaming transcript chunk
        if (isTranscribing && currentSnippet.isNotBlank()) {
          item {
            Card(
              shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
              modifier = Modifier.fillMaxWidth(0.92f)
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 2.dp)
                  Text("Riley speaking...", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = currentSnippet,
                  fontSize = 13.sp,
                  lineHeight = 18.sp,
                  color = MaterialTheme.colorScheme.onPrimaryContainer
                )
              }
            }
          }
        }
      }

      // 3. Quick Inquiry Chips
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        val quickPrompts = listOf(
          "🛏️ Need a bed tonight",
          "🍲 Hot meals now",
          "🚿 Showers & Hygiene",
          "🩺 Walk-in Clinic",
          "🚨 Crisis Hotline (988)",
          "👤 Speak to live operator"
        )

        quickPrompts.forEach { promptText ->
          SuggestionChip(
            onClick = { viewModel.askRiley(promptText) },
            label = { Text(promptText, fontSize = 11.sp) }
          )
        }
      }

      // 4. Interactive Ask Input & Dial Button Footer
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = inputPrompt,
              onValueChange = { inputPrompt = it },
              placeholder = { Text("Ask Riley anything...", fontSize = 12.sp) },
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("riley_prompt_input")
            )

            IconButton(
              onClick = {
                if (inputPrompt.isNotBlank()) {
                  val text = inputPrompt
                  inputPrompt = ""
                  viewModel.askRiley(text)
                }
              },
              modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .testTag("send_riley_prompt_btn")
            ) {
              Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = MaterialTheme.colorScheme.onPrimary)
            }
          }

          // Bottom Call / Dial Device Action Bar
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:4632787338"))
                context.startActivity(dialIntent)
              },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1.5f).height(44.dp).testTag("dial_official_line_btn")
            ) {
              Icon(Icons.Default.Call, null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Dial 463-2-STREET", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            OutlinedButton(
              onClick = onDismiss,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).height(44.dp).testTag("hang_up_riley_btn")
            ) {
              Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
              Spacer(modifier = Modifier.width(4.dp))
              Text("End Call", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
          }
        }
      }
    }
  }
}

/**
 * Renders an animated multi-bar equalizer to visually indicate Riley's active speech.
 */
@Composable
fun SoundWaveVisualizer(isSpeaking: Boolean) {
  val infiniteTransition = rememberInfiniteTransition(label = "soundwave_transition")
  
  val bar1 by infiniteTransition.animateFloat(
    initialValue = 4f, targetValue = if (isSpeaking) 24f else 8f,
    animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    label = "b1"
  )
  val bar2 by infiniteTransition.animateFloat(
    initialValue = 8f, targetValue = if (isSpeaking) 32f else 10f,
    animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    label = "b2"
  )
  val bar3 by infiniteTransition.animateFloat(
    initialValue = 6f, targetValue = if (isSpeaking) 28f else 6f,
    animationSpec = infiniteRepeatable(tween(300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    label = "b3"
  )
  val bar4 by infiniteTransition.animateFloat(
    initialValue = 10f, targetValue = if (isSpeaking) 34f else 8f,
    animationSpec = infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    label = "b4"
  )
  val bar5 by infiniteTransition.animateFloat(
    initialValue = 5f, targetValue = if (isSpeaking) 22f else 7f,
    animationSpec = infiniteRepeatable(tween(380, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    label = "b5"
  )

  Row(
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.height(36.dp)
  ) {
    listOf(bar1, bar2, bar3, bar4, bar5).forEach { barHeight ->
      Box(
        modifier = Modifier
          .width(4.dp)
          .height(barHeight.dp)
          .background(Color.White, RoundedCornerShape(2.dp))
      )
    }
  }
}

/**
 * Dialogue bubble displaying transcribed messages with timestamps.
 */
@Composable
fun DialogueBubble(message: com.example.data.RileyDialogueMessage) {
  val isRiley = message.sender == MessageSender.RILEY

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (isRiley) Alignment.Start else Alignment.End
  ) {
    Card(
      shape = RoundedCornerShape(
        topStart = if (isRiley) 4.dp else 16.dp,
        topEnd = if (isRiley) 16.dp else 4.dp,
        bottomStart = 16.dp,
        bottomEnd = 16.dp
      ),
      colors = CardDefaults.cardColors(
        containerColor = if (isRiley) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
      ),
      modifier = Modifier.fillMaxWidth(0.88f)
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (isRiley) "Riley (Assistant)" else "You",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isRiley) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
          )
          Text(
            text = message.timestamp,
            fontSize = 10.sp,
            color = (if (isRiley) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer).copy(alpha = 0.7f)
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = message.text,
          fontSize = 13.sp,
          lineHeight = 18.sp,
          color = if (isRiley) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
        )
      }
    }
  }
}
