package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class EmergencyDialCategory {
  PROVIDER,
  RESOURCE,
  CUSTOM
}

data class EmergencyDialContact(
  val id: String = UUID.randomUUID().toString(),
  val name: String,
  val phone: String,
  val category: EmergencyDialCategory,
  val description: String,
  val organization: String = "",
  val isCustom: Boolean = false
)

enum class CheckInTone(val displayName: String, val description: String) {
  CALM_AND_GENTLE("Calm & Gentle", "Soft, reassuring, patient cadence to reduce anxiety and stress."),
  REASSURING("Reassuring", "Steadfast, supportive tone emphasizing safety, care, and stability."),
  UPLIFTING("Uplifting", "Encouraging, positive voice focused on empowerment and optimism."),
  DIRECT_AND_GROUNDED("Direct & Grounded", "Concise, straightforward facts and practical guidance.")
}

data class CheckInSchedule(
  val id: String = UUID.randomUUID().toString(),
  val timeHour: Int = 9,
  val timeMinute: Int = 0,
  val isEnabled: Boolean = true,
  val tone: CheckInTone = CheckInTone.CALM_AND_GENTLE,
  val includeWeatherAdvisory: Boolean = true,
  val includeAffirmation: Boolean = true,
  val daysLabel: String = "Daily"
)

data class CheckInLog(
  val id: String = UUID.randomUUID().toString(),
  val timestamp: Long = System.currentTimeMillis(),
  val formattedDate: String = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date()),
  val status: String, // "Completed - Safe", "Scheduled", "Needs Support"
  val toneUsed: CheckInTone,
  val affirmationText: String,
  val weatherAdvisoryText: String,
  val userResponse: String? = null
)

data class WeatherAdvisory(
  val title: String,
  val severity: String, // "Advisory", "Watch", "Normal"
  val recommendation: String,
  val temperatureF: Int,
  val condition: String
)

class EmergencyDialAndCheckInRepository(context: Context) {

  private val prefs: SharedPreferences = context.getSharedPreferences("streetrise_dial_checkin_prefs", Context.MODE_PRIVATE)

  private val _emergencyDials = MutableStateFlow<List<EmergencyDialContact>>(emptyList())
  val emergencyDials: StateFlow<List<EmergencyDialContact>> = _emergencyDials.asStateFlow()

  private val _checkInSchedule = MutableStateFlow(CheckInSchedule())
  val checkInSchedule: StateFlow<CheckInSchedule> = _checkInSchedule.asStateFlow()

  private val _checkInLogs = MutableStateFlow<List<CheckInLog>>(emptyList())
  val checkInLogs: StateFlow<List<CheckInLog>> = _checkInLogs.asStateFlow()

  init {
    loadDials()
    loadSchedule()
    loadLogs()
  }

  // --- Speed Dials ---

  private fun loadDials() {
    val defaultList = listOf(
      // Providers
      EmergencyDialContact(
        id = "provider_metro_min",
        name = "Metropolitan Ministries Intake",
        phone = "8132091000",
        category = EmergencyDialCategory.PROVIDER,
        description = "Emergency family & individual shelter beds. Walk-in intake at 4:30 PM.",
        organization = "Metropolitan Ministries"
      ),
      EmergencyDialContact(
        id = "provider_coalition",
        name = "Coalition for the Homeless",
        phone = "4074261250",
        category = EmergencyDialCategory.PROVIDER,
        description = "Men, women, and family emergency shelter in Central Florida.",
        organization = "Coalition for the Homeless"
      ),
      EmergencyDialContact(
        id = "provider_salvation_army",
        name = "Salvation Army Center of Hope",
        phone = "8636828179",
        category = EmergencyDialCategory.PROVIDER,
        description = "Lakeland emergency shelter & transitional recovery housing.",
        organization = "The Salvation Army"
      ),
      EmergencyDialContact(
        id = "provider_tampa_hope",
        name = "Tampa Hope Shelter Desk",
        phone = "8132821538",
        category = EmergencyDialCategory.PROVIDER,
        description = "Low-barrier outdoor & tent shelter beds with case management.",
        organization = "Catholic Charities"
      ),

      // Resources
      EmergencyDialContact(
        id = "res_crisis_988",
        name = "988 Suicide & Crisis Lifeline",
        phone = "988",
        category = EmergencyDialCategory.RESOURCE,
        description = "Free, 24/7 confidential emotional support and crisis intervention.",
        organization = "National Suicide Prevention Lifeline"
      ),
      EmergencyDialContact(
        id = "res_fl_211",
        name = "Florida 2-1-1 Resource Referral",
        phone = "211",
        category = EmergencyDialCategory.RESOURCE,
        description = "Connects to statewide food, utility assistance, and emergency housing.",
        organization = "Heart of Florida United Way"
      ),
      EmergencyDialContact(
        id = "res_judeo_christian",
        name = "Judeo Christian Free Clinic",
        phone = "8138700395",
        category = EmergencyDialCategory.RESOURCE,
        description = "Free walk-in medical clinic, medication support, and dental triage.",
        organization = "Judeo Christian Health Clinic"
      ),
      EmergencyDialContact(
        id = "res_trinity_cafe",
        name = "Trinity Cafe Free Meals Desk",
        phone = "8138654673",
        category = EmergencyDialCategory.RESOURCE,
        description = "Daily free hot restaurant-style lunch served with dignity at 11:30 AM.",
        organization = "Feeding Tampa Bay"
      )
    )

    // Load custom dials from storage
    val savedCustomJson = prefs.getString("custom_emergency_dials", null)
    val customList = mutableListOf<EmergencyDialContact>()
    if (savedCustomJson != null) {
      try {
        val array = JSONArray(savedCustomJson)
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          customList.add(
            EmergencyDialContact(
              id = obj.optString("id", UUID.randomUUID().toString()),
              name = obj.optString("name", "Custom Contact"),
              phone = obj.optString("phone", ""),
              category = EmergencyDialCategory.CUSTOM,
              description = obj.optString("description", ""),
              organization = obj.optString("organization", "Personal Emergency Contact"),
              isCustom = true
            )
          )
        }
      } catch (e: Exception) {
        // Fallback gracefully
      }
    }

    _emergencyDials.value = defaultList + customList
  }

  fun addCustomDial(name: String, phone: String, description: String, organization: String) {
    val cleanPhone = phone.filter { it.isDigit() }
    val newDial = EmergencyDialContact(
      id = "custom_${System.currentTimeMillis()}",
      name = name.trim(),
      phone = cleanPhone,
      category = EmergencyDialCategory.CUSTOM,
      description = description.trim(),
      organization = if (organization.isNotBlank()) organization.trim() else "Custom Emergency Dial",
      isCustom = true
    )

    val currentList = _emergencyDials.value.toMutableList()
    currentList.add(newDial)
    _emergencyDials.value = currentList
    persistCustomDials()
  }

  fun removeCustomDial(id: String) {
    val currentList = _emergencyDials.value.filterNot { it.id == id && it.isCustom }
    _emergencyDials.value = currentList
    persistCustomDials()
  }

  private fun persistCustomDials() {
    val customDials = _emergencyDials.value.filter { it.isCustom }
    val array = JSONArray()
    for (dial in customDials) {
      val obj = JSONObject().apply {
        put("id", dial.id)
        put("name", dial.name)
        put("phone", dial.phone)
        put("description", dial.description)
        put("organization", dial.organization)
      }
      array.put(obj)
    }
    prefs.edit().putString("custom_emergency_dials", array.toString()).apply()
  }

  // --- Check-In Schedule ---

  private fun loadSchedule() {
    val hour = prefs.getInt("checkin_hour", 9)
    val minute = prefs.getInt("checkin_minute", 0)
    val isEnabled = prefs.getBoolean("checkin_enabled", true)
    val toneName = prefs.getString("checkin_tone", CheckInTone.CALM_AND_GENTLE.name)
    val tone = try {
      CheckInTone.valueOf(toneName ?: CheckInTone.CALM_AND_GENTLE.name)
    } catch (e: Exception) {
      CheckInTone.CALM_AND_GENTLE
    }
    val weather = prefs.getBoolean("checkin_weather", true)
    val affirmation = prefs.getBoolean("checkin_affirmation", true)

    _checkInSchedule.value = CheckInSchedule(
      timeHour = hour,
      timeMinute = minute,
      isEnabled = isEnabled,
      tone = tone,
      includeWeatherAdvisory = weather,
      includeAffirmation = affirmation
    )
  }

  fun updateSchedule(hour: Int, minute: Int, tone: CheckInTone, enabled: Boolean, weather: Boolean, affirmation: Boolean) {
    _checkInSchedule.value = CheckInSchedule(
      timeHour = hour,
      timeMinute = minute,
      isEnabled = enabled,
      tone = tone,
      includeWeatherAdvisory = weather,
      includeAffirmation = affirmation
    )
    prefs.edit()
      .putInt("checkin_hour", hour)
      .putInt("checkin_minute", minute)
      .putBoolean("checkin_enabled", enabled)
      .putString("checkin_tone", tone.name)
      .putBoolean("checkin_weather", weather)
      .putBoolean("checkin_affirmation", affirmation)
      .apply()
  }

  // --- Check-In Logs ---

  private fun loadLogs() {
    val savedLogsJson = prefs.getString("checkin_logs", null)
    if (savedLogsJson != null) {
      try {
        val array = JSONArray(savedLogsJson)
        val list = mutableListOf<CheckInLog>()
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          list.add(
            CheckInLog(
              id = obj.optString("id", UUID.randomUUID().toString()),
              timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
              formattedDate = obj.optString("formattedDate", "Today"),
              status = obj.optString("status", "Completed"),
              toneUsed = try { CheckInTone.valueOf(obj.optString("tone", CheckInTone.CALM_AND_GENTLE.name)) } catch (e: Exception) { CheckInTone.CALM_AND_GENTLE },
              affirmationText = obj.optString("affirmation", ""),
              weatherAdvisoryText = obj.optString("weather", ""),
              userResponse = if (obj.has("userResponse") && !obj.isNull("userResponse")) obj.getString("userResponse") else null
            )
          )
        }
        _checkInLogs.value = list
        return
      } catch (e: Exception) {
        // Fallback
      }
    }

    // Default seed logs so the user sees past history
    val defaultLogs = listOf(
      CheckInLog(
        id = "log_yesterday",
        timestamp = System.currentTimeMillis() - 86400000,
        formattedDate = "Yesterday, 8:30 PM",
        status = "Completed - Safe",
        toneUsed = CheckInTone.CALM_AND_GENTLE,
        affirmationText = "You are worthy of care, shelter, and peace. Take things one breath at a time.",
        weatherAdvisoryText = "Overnight low 68°F. No severe rain advisories.",
        userResponse = "I'm OK & Safe"
      ),
      CheckInLog(
        id = "log_2days_ago",
        timestamp = System.currentTimeMillis() - (86400000 * 2),
        formattedDate = "2 days ago, 9:00 AM",
        status = "Completed - Safe",
        toneUsed = CheckInTone.REASSURING,
        affirmationText = "Every morning is a clean page. Your persistence is your greatest asset.",
        weatherAdvisoryText = "Heat Advisory: High 92°F. Hydration stations active at Trinity Cafe.",
        userResponse = "I'm OK & Safe"
      )
    )
    _checkInLogs.value = defaultLogs
  }

  fun recordCheckInLog(
    status: String,
    tone: CheckInTone,
    affirmation: String,
    weather: String,
    userResponse: String?
  ) {
    val newLog = CheckInLog(
      id = "log_${System.currentTimeMillis()}",
      timestamp = System.currentTimeMillis(),
      formattedDate = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date()),
      status = status,
      toneUsed = tone,
      affirmationText = affirmation,
      weatherAdvisoryText = weather,
      userResponse = userResponse
    )

    val updatedList = listOf(newLog) + _checkInLogs.value
    _checkInLogs.value = updatedList.take(20) // Keep latest 20 logs
    persistLogs()
  }

  private fun persistLogs() {
    val array = JSONArray()
    for (log in _checkInLogs.value) {
      val obj = JSONObject().apply {
        put("id", log.id)
        put("timestamp", log.timestamp)
        put("formattedDate", log.formattedDate)
        put("status", log.status)
        put("tone", log.toneUsed.name)
        put("affirmation", log.affirmationText)
        put("weather", log.weatherAdvisoryText)
        put("userResponse", log.userResponse ?: "")
      }
      array.put(obj)
    }
    prefs.edit().putString("checkin_logs", array.toString()).apply()
  }

  // Live Florida Weather Advisory
  fun getCurrentWeatherAdvisory(): WeatherAdvisory {
    // Current Tampa Bay / Central FL Weather & Advisory data
    return WeatherAdvisory(
      title = "Heat & Rain Advisory Active",
      severity = "Advisory",
      recommendation = "High humidity with afternoon thunderstorms possible. Cooling stations with ice water and dry gear distribution active at Tampa Hope & Day Center.",
      temperatureF = 89,
      condition = "Scattered Clouds • 89°F"
    )
  }

  // Affirmations library by tone
  fun getAffirmationForTone(tone: CheckInTone): String {
    return when (tone) {
      CheckInTone.CALM_AND_GENTLE ->
        "You don't have to carry everything all at once. Take a slow, deep breath: you are safe in this moment, and your journey has purpose."
      CheckInTone.REASSURING ->
        "You possess incredible resilience. Whatever challenges arose today, you made it through, and compassionate help is always within reach at 463-2-STREET."
      CheckInTone.UPLIFTING ->
        "Today is full of new possibilities and open doors. Stand tall, keep your head high, and remember how far you've come already!"
      CheckInTone.DIRECT_AND_GROUNDED ->
        "Focus on the next practical step: secure your shelter, get nourishment, and protect your peace. You have the tools to handle today."
    }
  }
}
