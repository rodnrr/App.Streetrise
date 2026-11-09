package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MessageSender
import com.example.data.RealTimeSearchService
import com.example.data.RealTimeService
import com.example.data.RileyDialogueMessage
import com.example.data.SearchSource
import com.example.audio.CallAudioPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class HubViewMode {
  WEB_PORTAL,
  LIVE_SEARCH_MAP
}

class RealTimeSearchViewModel(application: Application) : AndroidViewModel(application) {

  private val searchService = RealTimeSearchService(application.applicationContext)
  private val audioPlayer = CallAudioPlayer(application.applicationContext)

  // Audio Controls
  val isAudioMuted: StateFlow<Boolean> = audioPlayer.isMuted
  val isAudioSpeaking: StateFlow<Boolean> = audioPlayer.isSpeaking

  fun toggleAudioMute() {
    audioPlayer.toggleMute()
  }

  // Hub View Mode: Web app vs Native Google Search Map
  val hubViewMode = MutableStateFlow(HubViewMode.WEB_PORTAL)

  // Search state
  val searchQuery = MutableStateFlow("")
  val selectedCategory = MutableStateFlow<String?>(null)

  private val _searchResults = MutableStateFlow<List<RealTimeService>>(emptyList())
  val searchResults: StateFlow<List<RealTimeService>> = _searchResults.asStateFlow()

  private val _isSearching = MutableStateFlow(false)
  val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

  private val _searchSource = MutableStateFlow(SearchSource.VERIFIED_OFFLINE_CACHE)
  val searchSource: StateFlow<SearchSource> = _searchSource.asStateFlow()

  private val _searchNotice = MutableStateFlow<String?>(null)
  val searchNotice: StateFlow<String?> = _searchNotice.asStateFlow()

  val selectedServiceForDetail = MutableStateFlow<RealTimeService?>(null)

  // Riley Phone Assistant State
  val isRileyCalling = MutableStateFlow(false)
  val callDurationSeconds = MutableStateFlow(0)
  val isRileyTranscribing = MutableStateFlow(false)
  val currentTranscribingSnippet = MutableStateFlow("")

  private val _rileyDialogue = MutableStateFlow<List<RileyDialogueMessage>>(emptyList())
  val rileyDialogue: StateFlow<List<RileyDialogueMessage>> = _rileyDialogue.asStateFlow()

  private var timerJob: Job? = null
  private var streamJob: Job? = null

  init {
    // Perform initial seed load so map markers and search have immediate rich Florida data
    executeSearch("", null)
  }

  fun executeSearch(query: String = searchQuery.value, category: String? = selectedCategory.value) {
    viewModelScope.launch {
      _isSearching.value = true
      _searchNotice.value = null
      try {
        val (services, source) = searchService.searchLocalServices(query, category)
        _searchResults.value = services
        _searchSource.value = source

        if (source == SearchSource.GOOGLE_SEARCH_LIVE) {
          _searchNotice.value = "Updated with live Google Search support data"
        } else {
          _searchNotice.value = "Displaying verified local StreetRise support directory"
        }

        // Auto-select first service for preview if map view is active
        if (services.isNotEmpty() && selectedServiceForDetail.value == null) {
          selectedServiceForDetail.value = services.first()
        }
      } catch (e: Exception) {
        _searchNotice.value = "Using local verified directory: ${e.message}"
        val fallback = searchService.queryLocalSeedServices(query, category)
        _searchResults.value = fallback
      } finally {
        _isSearching.value = false
      }
    }
  }

  fun setCategoryFilter(category: String?) {
    selectedCategory.value = category
    executeSearch(searchQuery.value, category)
  }

  fun switchHubView(mode: HubViewMode) {
    hubViewMode.value = mode
  }

  // --- Riley Phone Assistant methods ---

  fun summonRileyAssistant() {
    isRileyCalling.value = true
    startCallTimer()
    audioPlayer.playConnectChime()

    // Add initial greeting if empty
    if (_rileyDialogue.value.isEmpty()) {
      val welcomeText = "Hello! You've reached Riley with StreetRise Support at 463-2-STREET. I'm your automated assistant connecting you to open emergency shelter beds, hot meal schedules, showers, or healthcare. What kind of help do you need right now?"
      streamRileySpeech(welcomeText)
    }
  }

  fun dismissRileyAssistant() {
    isRileyCalling.value = false
    timerJob?.cancel()
    streamJob?.cancel()
    audioPlayer.stopSpeaking()
    isRileyTranscribing.value = false
    currentTranscribingSnippet.value = ""
  }

  private fun startCallTimer() {
    timerJob?.cancel()
    callDurationSeconds.value = 0
    timerJob = viewModelScope.launch {
      while (isRileyCalling.value) {
        delay(1000)
        callDurationSeconds.value += 1
      }
    }
  }

  fun askRiley(userPrompt: String) {
    if (userPrompt.isBlank()) return

    // Append user query to transcript
    val userMsg = RileyDialogueMessage(
      sender = MessageSender.USER,
      text = userPrompt
    )
    _rileyDialogue.value = _rileyDialogue.value + userMsg

    viewModelScope.launch {
      isRileyTranscribing.value = true
      currentTranscribingSnippet.value = "Connecting audio..."

      val response = searchService.consultRileyAssistant(userPrompt, _rileyDialogue.value)
      streamRileySpeech(response)
    }
  }

  private fun streamRileySpeech(fullText: String) {
    streamJob?.cancel()
    // Speak aloud with pleasant natural voice
    audioPlayer.speakText(fullText, pitch = 1.02f, speechRate = 0.90f)

    streamJob = viewModelScope.launch {
      isRileyTranscribing.value = true
      currentTranscribingSnippet.value = ""

      // Simulate live spoken audio transcription chunk-by-chunk synchronized with voice
      val words = fullText.split(" ")
      val builder = StringBuilder()
      for (word in words) {
        builder.append(word).append(" ")
        currentTranscribingSnippet.value = builder.toString()
        delay(42) // Smooth speech cadence
      }

      val rileyMsg = RileyDialogueMessage(
        sender = MessageSender.RILEY,
        text = fullText
      )
      _rileyDialogue.value = _rileyDialogue.value + rileyMsg
      isRileyTranscribing.value = false
      currentTranscribingSnippet.value = ""
    }
  }

  override fun onCleared() {
    super.onCleared()
    audioPlayer.shutdown()
  }
}
