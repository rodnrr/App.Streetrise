@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*

enum class CallingSubTab {
  HOTLINE_AND_DIALS,
  AUTOMATED_CHECK_IN
}

/**
 * Main Calling Feature Screen dedicated to:
 * 1. Riley 463-2-STREET Hotline
 * 2. Programmable Emergency Dials (Providers, Resources, Custom)
 * 3. Automated Check-In System (Scheduling, Tone preferences, Affirmations, Weather Advisories, and Logs)
 */
@Composable
fun CallingTabScreen(
  dialViewModel: EmergencyDialAndCheckInViewModel,
  onOpenRileyAssistant: () -> Unit,
  modifier: Modifier = Modifier
) {
  var activeSubTab by remember { mutableStateOf(CallingSubTab.HOTLINE_AND_DIALS) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    Column(
      modifier = Modifier.fillMaxSize()
    ) {
      // Top Calling Hero Banner
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "StreetRise Hotline & Care Desk",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "24/7 Riley Phone Assistant • Speed Dials • Automated Wellness Check-In",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
              )
            }
          }

          // Main Call Riley 463-2-STREET Hero Button
          HeroCallRileyButton(
            onClick = onOpenRileyAssistant,
            modifier = Modifier.fillMaxWidth()
          )

          // Sub-Tab Switcher (Speed Dials vs Automated Check-In)
          PrimaryTabRow(
            selectedTabIndex = activeSubTab.ordinal,
            modifier = Modifier.fillMaxWidth()
          ) {
            Tab(
              selected = activeSubTab == CallingSubTab.HOTLINE_AND_DIALS,
              onClick = { activeSubTab = CallingSubTab.HOTLINE_AND_DIALS },
              text = { Text("Emergency Speed Dials", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
              icon = { Icon(Icons.Default.Call, null, modifier = Modifier.size(16.dp)) },
              modifier = Modifier.testTag("tab_speed_dials")
            )
            Tab(
              selected = activeSubTab == CallingSubTab.AUTOMATED_CHECK_IN,
              onClick = { activeSubTab = CallingSubTab.AUTOMATED_CHECK_IN },
              text = { Text("Automated Check-In", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
              icon = { Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp)) },
              modifier = Modifier.testTag("tab_automated_checkin")
            )
          }
        }
      }

      // Content based on sub-tab
      when (activeSubTab) {
        CallingSubTab.HOTLINE_AND_DIALS -> {
          EmergencySpeedDialsView(
            viewModel = dialViewModel,
            modifier = Modifier.fillMaxSize()
          )
        }
        CallingSubTab.AUTOMATED_CHECK_IN -> {
          AutomatedCheckInView(
            viewModel = dialViewModel,
            modifier = Modifier.fillMaxSize()
          )
        }
      }
    }
  }
}

/**
 * Big Prominent Call Riley Hero Banner.
 */
@Composable
fun HeroCallRileyButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "beacon")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.95f,
    targetValue = 1.25f,
    animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    label = "pulse"
  )

  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.primary,
    contentColor = MaterialTheme.colorScheme.onPrimary,
    shadowElevation = 6.dp,
    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)),
    modifier = modifier.testTag("hero_call_riley_button")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.weight(1f)
      ) {
        // Pulsing phone icon circle
        Box(contentAlignment = Alignment.Center) {
          Box(
            modifier = Modifier
              .size(46.dp * pulseScale)
              .clip(CircleShape)
              .background(Color(0xFF00FF66).copy(alpha = 0.25f))
          )
          Surface(
            shape = CircleShape,
            color = Color.White,
            modifier = Modifier.size(44.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(Icons.Default.Call, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
          }
        }

        Column {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "CALL 463-2-STREET",
              fontWeight = FontWeight.ExtraBold,
              fontSize = 17.sp,
              color = Color.White
            )
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFF00C853)
            ) {
              Text(
                text = "LIVE",
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
              )
            }
          }
          Text(
            text = "Summon Riley Phone Assistant with live transcribed speech",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.85f)
          )
        }
      }

      Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(24.dp))
    }
  }
}

/**
 * Section for Programmable Emergency Dials (Providers, Resources, Custom).
 */
@Composable
fun EmergencySpeedDialsView(
  viewModel: EmergencyDialAndCheckInViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val dials by viewModel.emergencyDials.collectAsStateWithLifecycle()
  val selectedCat by viewModel.selectedDialCategory.collectAsStateWithLifecycle()

  var showAddDialog by remember { mutableStateOf(false) }

  val filteredDials = remember(dials, selectedCat) {
    if (selectedCat == null) dials else dials.filter { it.category == selectedCat }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    // Actions header: Filter chips + "➕ Add Custom Dial"
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier
          .weight(1f)
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        FilterChip(
          selected = selectedCat == null,
          onClick = { viewModel.selectedDialCategory.value = null },
          label = { Text("All", fontSize = 11.sp) }
        )
        FilterChip(
          selected = selectedCat == EmergencyDialCategory.PROVIDER,
          onClick = { viewModel.selectedDialCategory.value = EmergencyDialCategory.PROVIDER },
          label = { Text("🏢 Providers", fontSize = 11.sp) }
        )
        FilterChip(
          selected = selectedCat == EmergencyDialCategory.RESOURCE,
          onClick = { viewModel.selectedDialCategory.value = EmergencyDialCategory.RESOURCE },
          label = { Text("🆘 Resources", fontSize = 11.sp) }
        )
        FilterChip(
          selected = selectedCat == EmergencyDialCategory.CUSTOM,
          onClick = { viewModel.selectedDialCategory.value = EmergencyDialCategory.CUSTOM },
          label = { Text("⭐ Custom", fontSize = 11.sp) }
        )
      }

      Button(
        onClick = { showAddDialog = true },
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("btn_add_custom_dial")
      ) {
        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Program Dial", fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }
    }

    // List of speed dials
    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      contentPadding = PaddingValues(vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(filteredDials, key = { it.id }) { dial ->
        EmergencyDialCard(
          contact = dial,
          onDial = {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${dial.phone}"))
            context.startActivity(intent)
          },
          onDelete = { viewModel.removeCustomDial(dial.id) }
        )
      }
    }
  }

  if (showAddDialog) {
    AddCustomDialDialog(
      onDismiss = { showAddDialog = false },
      onSave = { name, phone, desc, org ->
        viewModel.addCustomDial(name, phone, desc, org)
        showAddDialog = false
      }
    )
  }
}

@Composable
fun EmergencyDialCard(
  contact: EmergencyDialContact,
  onDial: () -> Unit,
  onDelete: () -> Unit
) {
  val catColor = when (contact.category) {
    EmergencyDialCategory.PROVIDER -> Color(0xFFE3F2FD)
    EmergencyDialCategory.RESOURCE -> Color(0xFFFFEBEE)
    EmergencyDialCategory.CUSTOM -> Color(0xFFF3E5F5)
  }

  val catLabel = when (contact.category) {
    EmergencyDialCategory.PROVIDER -> "SHELTER / PROVIDER"
    EmergencyDialCategory.RESOURCE -> "CRISIS / RESOURCE"
    EmergencyDialCategory.CUSTOM -> "PROGRAMMED CONTACT"
  }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("dial_card_${contact.id}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = catColor
          ) {
            Text(
              text = catLabel,
              fontSize = 9.sp,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          if (contact.organization.isNotBlank()) {
            Text(
              text = contact.organization,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.outline,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = contact.name,
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          color = MaterialTheme.colorScheme.onSurface
        )

        Text(
          text = contact.description,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = "Number: ${contact.phone}",
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (contact.isCustom) {
          IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
          ) {
            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
          }
        }

        Button(
          onClick = onDial,
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("dial_btn_${contact.id}")
        ) {
          Icon(Icons.Default.Call, null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Dial", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

/**
 * Dialog to add a custom emergency dial contact.
 */
@Composable
fun AddCustomDialDialog(
  onDismiss: () -> Unit,
  onSave: (name: String, phone: String, desc: String, org: String) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var desc by remember { mutableStateOf("") }
  var org by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Program Emergency Speed Dial", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Save a direct one-tap emergency contact (e.g. Caseworker, Family member, Sponsor, or Clinic).", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Contact Name *") },
          placeholder = { Text("e.g. Sarah Caseworker") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_custom_dial_name")
        )

        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Phone Number *") },
          placeholder = { Text("e.g. 8135550192") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_custom_dial_phone")
        )

        OutlinedTextField(
          value = org,
          onValueChange = { org = it },
          label = { Text("Organization / Relationship") },
          placeholder = { Text("e.g. Street Outreach Team") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = desc,
          onValueChange = { desc = it },
          label = { Text("Notes / Availability") },
          placeholder = { Text("e.g. Direct cell line, available 9-5") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank() && phone.isNotBlank()) {
            onSave(name, phone, desc, org)
          }
        },
        enabled = name.isNotBlank() && phone.isNotBlank(),
        modifier = Modifier.testTag("save_custom_dial_btn")
      ) {
        Text("Save Contact")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

/**
 * Automated Wellness Check-In UI Component:
 * Allows user to schedule call times, choose tone preferences, and view upcoming check-in logs.
 */
@Composable
fun AutomatedCheckInView(
  viewModel: EmergencyDialAndCheckInViewModel,
  modifier: Modifier = Modifier
) {
  val schedule by viewModel.checkInSchedule.collectAsStateWithLifecycle()
  val logs by viewModel.checkInLogs.collectAsStateWithLifecycle()
  val weather by viewModel.currentWeather.collectAsStateWithLifecycle()

  var selectedHour by remember(schedule.timeHour) { mutableIntStateOf(schedule.timeHour) }
  var selectedMinute by remember(schedule.timeMinute) { mutableIntStateOf(schedule.timeMinute) }
  var selectedTone by remember(schedule.tone) { mutableStateOf(schedule.tone) }
  var includeWeather by remember(schedule.includeWeatherAdvisory) { mutableStateOf(schedule.includeWeatherAdvisory) }
  var includeAffirmation by remember(schedule.includeAffirmation) { mutableStateOf(schedule.includeAffirmation) }

  // Format 12-hour AM/PM
  val formattedScheduledTime = remember(schedule.timeHour, schedule.timeMinute) {
    val h = if (schedule.timeHour == 0) 12 else if (schedule.timeHour > 12) schedule.timeHour - 12 else schedule.timeHour
    val amPm = if (schedule.timeHour >= 12) "PM" else "AM"
    String.format(java.util.Locale.US, "%d:%02d %s", h, schedule.timeMinute, amPm)
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 1. Status Banner Card
    Card(
      colors = CardDefaults.cardColors(
        containerColor = if (schedule.isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
      ),
      shape = RoundedCornerShape(18.dp),
      modifier = Modifier.fillMaxWidth().testTag("checkin_status_card")
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.primary)
            Text(
              text = if (schedule.isEnabled) "Automated Check-In: Active" else "Automated Check-In: Paused",
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          Switch(
            checked = schedule.isEnabled,
            onCheckedChange = { viewModel.toggleCheckInEnabled(it) },
            modifier = Modifier.testTag("checkin_master_switch")
          )
        }

        Text(
          text = if (schedule.isEnabled) {
            "✓ Riley will call you daily at $formattedScheduledTime using a ${schedule.tone.displayName} voice to verify your safety, recite an affirmation, and warn of weather advisories."
          } else {
            "Check-in calls are currently paused. Enable above to resume scheduled morning or evening check-ins."
          },
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Immediate Test Call Button
        Button(
          onClick = { viewModel.triggerCheckInCall() },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("test_checkin_call_btn")
        ) {
          Icon(Icons.Default.Call, null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Test Automated Check-In Call Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      }
    }

    // 2. Schedule Time Picker Selector
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("1. Scheduled Call Time", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Choose when Riley should place your automated check-in call:", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)

        // Preset quick times
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf(
            Triple(8, 0, "8:00 AM (Morning)"),
            Triple(12, 30, "12:30 PM (Midday)"),
            Triple(17, 30, "5:30 PM (Evening)"),
            Triple(20, 0, "8:00 PM (Night)")
          ).forEach { (h, m, label) ->
            FilterChip(
              selected = selectedHour == h && selectedMinute == m,
              onClick = {
                selectedHour = h
                selectedMinute = m
                viewModel.updateCheckInSchedule(h, m, selectedTone, schedule.isEnabled, includeWeather, includeAffirmation)
              },
              label = { Text(label, fontSize = 11.sp) }
            )
          }
        }
      }
    }

    // 3. Tone Preferences Selector
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("2. Choose Riley's Voice Tone", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Select the voice demeanor that brings you the most comfort:", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)

        CheckInTone.values().forEach { tone ->
          val isSelected = selectedTone == tone
          Surface(
            onClick = {
              selectedTone = tone
              viewModel.updateCheckInSchedule(selectedHour, selectedMinute, tone, schedule.isEnabled, includeWeather, includeAffirmation)
            },
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
            modifier = Modifier.fillMaxWidth().testTag("tone_option_${tone.name}")
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(tone.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(tone.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              RadioButton(
                selected = isSelected,
                onClick = {
                  selectedTone = tone
                  viewModel.updateCheckInSchedule(selectedHour, selectedMinute, tone, schedule.isEnabled, includeWeather, includeAffirmation)
                }
              )
            }
          }
        }
      }
    }

    // 4. Affirmation & Weather Options
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("3. Content & Advisory Preferences", fontWeight = FontWeight.Bold, fontSize = 14.sp)

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Positive Daily Affirmation", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("Riley recites an encouraging reflection for your day", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
          }
          Switch(
            checked = includeAffirmation,
            onCheckedChange = {
              includeAffirmation = it
              viewModel.updateCheckInSchedule(selectedHour, selectedMinute, selectedTone, schedule.isEnabled, includeWeather, it)
            }
          )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Severe Weather & Advisory Alert", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("Riley informs of storm, rain, freeze, or heat advisories in effect", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
          }
          Switch(
            checked = includeWeather,
            onCheckedChange = {
              includeWeather = it
              viewModel.updateCheckInSchedule(selectedHour, selectedMinute, selectedTone, schedule.isEnabled, it, includeAffirmation)
            }
          )
        }

        // Live Florida Weather Preview
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Color(0xFFFFF3E0),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.Warning, null, tint = Color(0xFFE65100), modifier = Modifier.size(18.dp))
            Column {
              Text(weather.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFE65100))
              Text(weather.recommendation, fontSize = 11.sp, color = Color(0xFF5D4037))
            }
          }
        }
      }
    }

    // 5. Upcoming & Past Check-In Logs
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
      modifier = Modifier.fillMaxWidth().testTag("checkin_logs_card")
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Upcoming & Recent Check-In Logs", fontWeight = FontWeight.Bold, fontSize = 14.sp)
          Text("${logs.size} recorded", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        }

        // Next Upcoming Schedule Log Item
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .size(10.dp)
                .background(Color(0xFF00C853), CircleShape)
            )
            Column {
              Text(
                text = "Next Scheduled Call: Today at $formattedScheduledTime",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
              Text(
                text = "Tone: ${schedule.tone.displayName} • Affirmation & Weather Enabled",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
              )
            }
          }
        }

        // Historical logs
        logs.forEach { log ->
          CheckInLogItemRow(log = log)
        }
      }
    }

    Spacer(modifier = Modifier.height(30.dp))
  }
}

@Composable
fun CheckInLogItemRow(log: CheckInLog) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(log.formattedDate, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = if (log.status.contains("Safe")) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
        ) {
          Text(
            text = log.status,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (log.status.contains("Safe")) Color(0xFF2E7D32) else Color(0xFFC62828),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      if (log.userResponse != null) {
        Text("Response: \"${log.userResponse}\"", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
      }

      Text("Affirmation: \"${log.affirmationText}\"", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
      Text("Weather Note: ${log.weatherAdvisoryText}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
  }
}

/**
 * Full-screen Simulated Incoming Check-In Call Screen.
 */
@Composable
fun CheckInCallOverlay(
  viewModel: EmergencyDialAndCheckInViewModel,
  modifier: Modifier = Modifier
) {
  val transcription by viewModel.currentTranscription.collectAsStateWithLifecycle()
  val isSpeaking by viewModel.isRileySpeaking.collectAsStateWithLifecycle()
  val seconds by viewModel.checkInCallSeconds.collectAsStateWithLifecycle()

  val formattedTimer = remember(seconds) {
    val m = seconds / 60
    val s = seconds % 60
    String.format(java.util.Locale.US, "%02d:%02d", m, s)
  }

  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = modifier.fillMaxSize().testTag("checkin_call_dialog")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // 1. Top Call Header
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(80.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text("SR", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          "Riley — Wellness Check-In",
          fontWeight = FontWeight.Bold,
          fontSize = 20.sp,
          color = MaterialTheme.colorScheme.onBackground
        )

        Text(
          "Automated Care Call • $formattedTimer",
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Soundwave equalizer
        SoundWaveVisualizer(isSpeaking = isSpeaking)
      }

      // 2. Transcribed Spoken Text
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(vertical = 16.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.Info, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Riley Speaking (Live Transcription)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
          }

          Text(
            text = transcription.ifEmpty { "Connecting calm check-in audio stream..." },
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // 3. User Response Action Buttons
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          "How are you feeling today?",
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )

        Button(
          onClick = { viewModel.respondToCheckIn("I'm OK & Safe") },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("checkin_response_ok")
        ) {
          Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("I'm OK & Safe", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
          onClick = { viewModel.respondToCheckIn("Need Food or Shelter Info") },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().height(44.dp).testTag("checkin_response_needs")
        ) {
          Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Need Food or Shelter Info")
        }

        TextButton(
          onClick = { viewModel.dismissCheckInCall() },
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("End Call", color = MaterialTheme.colorScheme.error)
        }
      }
    }
  }
}
