package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.CallAudioPlayer
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EmergencyDialAndCheckInViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = EmergencyDialAndCheckInRepository(application.applicationContext)
  private val audioPlayer = CallAudioPlayer(application.applicationContext)

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
  val isRileySpeaking = audioPlayer.isSpeaking
  val isAudioMuted: StateFlow<Boolean> = audioPlayer.isMuted
  val isAudioReady: StateFlow<Boolean> = audioPlayer.isAudioReady
  val currentTranscription = MutableStateFlow("")
  val currentAffirmation = MutableStateFlow("")
  val currentWeatherAdvisory = MutableStateFlow("")

  private var callTimerJob: Job? = null
  private var speechJob: Job? = null

  fun toggleAudioMute() {
    audioPlayer.toggleMute()
  }

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

    // Play pleasant connect chime
    audioPlayer.playConnectChime()

    // Speak aloud with calm, natural TTS voice tailored to chosen tone
    val (pitch, speechRate) = when (schedule.tone) {
      CheckInTone.CALM_AND_GENTLE -> Pair(1.0f, 0.82f)
      CheckInTone.REASSURING -> Pair(1.04f, 0.88f)
      CheckInTone.UPLIFTING -> Pair(1.08f, 0.94f)
      CheckInTone.DIRECT_AND_GROUNDED -> Pair(0.98f, 0.90f)
    }

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

    // Speak aloud through speaker/earpiece
    audioPlayer.speakText(fullSpeech, pitch = pitch, speechRate = speechRate)

    // Stream on-screen transcript chunk-by-chunk synchronized with speaking
    speechJob?.cancel()
    speechJob = viewModelScope.launch {
      currentTranscription.value = ""
      val words = fullSpeech.split(" ")
      val builder = StringBuilder()
      for (word in words) {
        builder.append(word).append(" ")
        currentTranscription.value = builder.toString()
        delay(42) // Natural synchronized transcription pacing
      }
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
    audioPlayer.stopSpeaking()
    currentTranscription.value = ""
  }

  fun endCheckInCall() = dismissCheckInCall()

  override fun onCleared() {
    super.onCleared()
    audioPlayer.shutdown()
  }
}
