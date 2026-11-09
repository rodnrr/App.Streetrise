package com.example.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

/**
 * Audio Engine providing realistic call tones and calm, natural voice synthesis
 * using Android's native TextToSpeech and ToneGenerator.
 * Specially designed for Riley Phone Assistant and Scheduled Check-In calls.
 */
class CallAudioPlayer(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var pendingSpeechText: String? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isAudioReady = MutableStateFlow(false)
    val isAudioReady: StateFlow<Boolean> = _isAudioReady.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("CallAudioPlayer", "Error initializing TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val result = engine.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.language = Locale.getDefault()
                }
                // Gentle, calm pacing and soothing pitch for wellness check-ins
                engine.setSpeechRate(0.88f)
                engine.setPitch(1.02f)

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        mainHandler.post { _isSpeaking.value = true }
                    }

                    override fun onDone(utteranceId: String?) {
                        mainHandler.post { _isSpeaking.value = false }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        mainHandler.post { _isSpeaking.value = false }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        mainHandler.post { _isSpeaking.value = false }
                    }
                })

                isTtsInitialized = true
                _isAudioReady.value = true

                pendingSpeechText?.let { pending ->
                    speakText(pending)
                    pendingSpeechText = null
                }
            }
        } else {
            Log.w("CallAudioPlayer", "TextToSpeech init returned status: $status")
        }
    }

    /**
     * Plays a pleasant telephone call connect tone so the user feels like
     * Riley has picked up the line.
     */
    fun playConnectChime() {
        if (_isMuted.value) return
        Thread {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 70)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 140)
                Thread.sleep(180)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
                Thread.sleep(220)
                toneGen.release()
            } catch (e: Exception) {
                Log.w("CallAudioPlayer", "Chime tone skipped: ${e.message}")
            }
        }.start()
    }

    /**
     * Speaks the check-in script aloud with warm and calm tone.
     */
    fun speakText(text: String, pitch: Float = 1.0f, speechRate: Float = 0.88f) {
        if (_isMuted.value) {
            return
        }

        if (!isTtsInitialized || tts == null) {
            pendingSpeechText = text
            return
        }

        try {
            tts?.apply {
                setPitch(pitch)
                setSpeechRate(speechRate)
                val utteranceId = UUID.randomUUID().toString()
                val params = Bundle().apply {
                    putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_VOICE_CALL)
                }
                speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            }
        } catch (e: Exception) {
            Log.e("CallAudioPlayer", "Failed to speak text aloud", e)
            _isSpeaking.value = false
        }
    }

    fun toggleMute() {
        val next = !_isMuted.value
        _isMuted.value = next
        if (next) {
            stopSpeaking()
        }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.w("CallAudioPlayer", "Error stopping TTS: ${e.message}")
        }
        _isSpeaking.value = false
    }

    fun shutdown() {
        try {
            stopSpeaking()
            tts?.shutdown()
            tts = null
            isTtsInitialized = false
        } catch (e: Exception) {
            Log.w("CallAudioPlayer", "Error shutting down TTS: ${e.message}")
        }
    }
}
