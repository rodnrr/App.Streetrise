package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EmergencyDialAndCheckInViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = EmergencyDialAndCheckInRepository(application.applicationContext)

  // Emergency Dials
  val emergencyDials: StateFlow<List<EmergencyDialContact>> = repository.emergencyDials
  val selectedDialCategory = MutableStateFlow<EmergencyDialCategory?>(null)

  // Check-In Schedule & Logs
  val checkInSchedule: StateFlow<CheckInSchedule> = repository.checkInSchedule
  val checkInLogs: StateFlow<List<CheckInLog>> = repository.checkInLogs

  // Live Weather Advisory for Central Florida
  private val _currentWeather = MutableStateFlow(repository.getCurrentWeatherAdvisory())
  val currentWeather: StateFlow<WeatherAdvisory> = _currentWeather.asStateFlow()

  // Active incoming / simulated Check-In Call state
  val isCheckInCallActive = MutableStateFlow(false)
  val checkInCallSeconds = MutableStateFlow(0)
  val isRileySpeaking = MutableStateFlow(false)
  val currentTranscription = MutableStateFlow("")
  val currentAffirmation = MutableStateFlow("")
  val currentWeatherAdvisory = MutableStateFlow("")

  private var callTimerJob: Job? = null
  private var speechJob: Job? = null

  // Speed Dial Actions
  fun addCustomDial(name: String, phone: String, description: String, organization: String) {
    repository.addCustomDial(name, phone, description, organization)
  }

  fun removeCustomDial(id: String) {
    repository.removeCustomDial(id)
  }

  // Check-In Schedule Actions
  fun updateCheckInSchedule(
    hour: Int,
    minute: Int,
    tone: CheckInTone,
    enabled: Boolean,
    weather: Boolean,
    affirmation: Boolean
  ) {
    repository.updateSchedule(hour, minute, tone, enabled, weather, affirmation)
  }

  fun toggleCheckInEnabled(enabled: Boolean) {
    val current = checkInSchedule.value
    repository.updateSchedule(
      current.timeHour,
      current.timeMinute,
      current.tone,
      enabled,
      current.includeWeatherAdvisory,
      current.includeAffirmation
    )
  }

  // Trigger automated or test check-in call
  fun triggerCheckInCall() {
    val schedule = checkInSchedule.value
    val affirmation = repository.getAffirmationForTone(schedule.tone)
    val weather = repository.getCurrentWeatherAdvisory()

    currentAffirmation.value = affirmation
    currentWeatherAdvisory.value = "${weather.title}: ${weather.recommendation}"
    isCheckInCallActive.value = true
    checkInCallSeconds.value = 0

    // Start timer
    callTimerJob?.cancel()
    callTimerJob = viewModelScope.launch {
      while (isCheckInCallActive.value) {
        delay(1000)
        checkInCallSeconds.value += 1
      }
    }

    // Simulate calm speech delivery with live transcription
    speechJob?.cancel()
    speechJob = viewModelScope.launch {
      isRileySpeaking.value = true
      currentTranscription.value = ""

      val fullSpeech = buildString {
        append("Hello, this is Riley checking in on you from StreetRise. I hope you're in a safe spot right now. ")
        if (schedule.includeAffirmation) {
          append("\n\nHere is your affirmation today: $affirmation ")
        }
        if (schedule.includeWeatherAdvisory) {
          append("\n\nLocal Weather Update: ${weather.condition}. ${weather.recommendation} ")
        }
        append("\n\nAre you doing alright today, or do you need assistance connecting with emergency shelter, hot food, or a caseworker?")
      }

      val words = fullSpeech.split(" ")
      val builder = StringBuilder()
      for (word in words) {
        builder.append(word).append(" ")
        currentTranscription.value = builder.toString()
        delay(38) // Soothing cadence
      }
      isRileySpeaking.value = false
    }
  }

  fun respondToCheckIn(userResponse: String) {
    val schedule = checkInSchedule.value
    val status = if (userResponse.contains("OK", ignoreCase = true)) "Completed - Safe" else "Needs Assistance"

    repository.recordCheckInLog(
      status = status,
      tone = schedule.tone,
      affirmation = currentAffirmation.value,
      weather = currentWeatherAdvisory.value,
      userResponse = userResponse
    )

    dismissCheckInCall()
  }

  fun dismissCheckInCall() {
    isCheckInCallActive.value = false
    callTimerJob?.cancel()
    speechJob?.cancel()
    isRileySpeaking.value = false
    currentTranscription.value = ""
  }

  fun endCheckInCall() = dismissCheckInCall()
}
