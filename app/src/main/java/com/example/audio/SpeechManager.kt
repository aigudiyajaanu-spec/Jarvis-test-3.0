package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SpeechManager(private val context: Context) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _micRmsDb = MutableStateFlow(0f)
    val micRmsDb: StateFlow<Float> = _micRmsDb.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    var onSpeechRecognized: ((String) -> Unit)? = null
    var onSpeechError: ((String) -> Unit)? = null
    var onTtsFinished: (() -> Unit)? = null

    private var ttsAmplitudeJob: Job? = null
    private var watchdogJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.let {
                        val supported = listOf(Locale.UK, Locale.US, Locale.ENGLISH, Locale.getDefault())
                        for (loc in supported) {
                            val res = it.setLanguage(loc)
                            if (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED) {
                                break
                            }
                        }
                        // Calm JARVIS British cadence: slightly lower pitch, crisp rate
                        it.setPitch(0.88f)
                        it.setSpeechRate(1.02f)
                        it.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                startTtsAmplitudeSimulation()
                            }

                            override fun onDone(utteranceId: String?) {
                                stopTtsAmplitudeSimulation()
                                onTtsFinished?.invoke()
                            }

                            override fun onError(utteranceId: String?) {
                                stopTtsAmplitudeSimulation()
                                onTtsFinished?.invoke()
                            }
                        })
                        isTtsReady = true
                    }
                } else {
                    Log.e("SpeechManager", "TTS initialization failed (status $status)")
                    isTtsReady = false
                }
            }
        } catch (e: Exception) {
            Log.e("SpeechManager", "TTS init exception", e)
        }
    }

    fun startListening() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { startListening() }
            return
        }

        stopListening()
        stopSpeaking()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _isListening.value = false
            _micRmsDb.value = 0f
            onSpeechError?.invoke("Speech recognition is unavailable on this device.")
            return
        }

        try {
            _isListening.value = true
            _partialTranscript.value = ""
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                    }

                    override fun onBeginningOfSpeech() {
                        _isListening.value = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize dB (-2 to 10 typical) to 0.0 .. 1.0 range
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1.0f)
                        _micRmsDb.value = normalized
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                        _micRmsDb.value = 0f
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        _micRmsDb.value = 0f
                        watchdogJob?.cancel()
                        watchdogJob = null
                        
                        // Check if we captured partial speech before the error
                        val currentText = _partialTranscript.value.trim()
                        if (currentText.isNotBlank()) {
                            onSpeechRecognized?.invoke(currentText)
                            return
                        }

                        val errorMessage = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No microphone voice detected. Tap a command chip or type below, Sir."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out. Tap a command chip or speak again, Sir."
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording issue. Please check microphone permissions."
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech network connection issue. You can type in the HUD bar, Sir."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                            else -> "Audio recognition idle. Tap reactor or type a command, Sir."
                        }
                        onSpeechError?.invoke(errorMessage)
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        _micRmsDb.value = 0f
                        watchdogJob?.cancel()
                        watchdogJob = null
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull { it.isNotBlank() }?.trim() ?: _partialTranscript.value.trim()
                        if (text.isNotBlank()) {
                            _partialTranscript.value = text
                            onSpeechRecognized?.invoke(text)
                        } else {
                            onSpeechError?.invoke("No microphone voice detected. Tap a command chip or type below, Sir.")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull { it.isNotBlank() }?.trim() ?: ""
                        if (text.isNotBlank()) {
                            _partialTranscript.value = text
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            }
            speechRecognizer?.startListening(intent)

            // Auto-timeout watchdog: after 9 seconds, gracefully conclude listening on the main thread
            watchdogJob?.cancel()
            watchdogJob = scope.launch {
                delay(9000)
                if (_isListening.value) {
                    val captured = _partialTranscript.value.trim()
                    stopListening()
                    if (captured.isNotBlank()) {
                        onSpeechRecognized?.invoke(captured)
                    } else {
                        onSpeechError?.invoke("Listening timed out. Tap reactor or type a command, Sir.")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SpeechManager", "Failed to start speech recognizer", e)
            _isListening.value = false
            _micRmsDb.value = 0f
            onSpeechError?.invoke("Recognition initialization failed: ${e.message}")
        }
    }

    fun stopListening() {
        watchdogJob?.cancel()
        watchdogJob = null
        _isListening.value = false
        _micRmsDb.value = 0f
        
        if (Looper.myLooper() == Looper.getMainLooper()) {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.e("SpeechManager", "Error stopping SpeechRecognizer", e)
            }
            speechRecognizer = null
        } else {
            mainHandler.post {
                try {
                    speechRecognizer?.stopListening()
                    speechRecognizer?.destroy()
                } catch (e: Exception) {
                    Log.e("SpeechManager", "Error stopping SpeechRecognizer", e)
                }
                speechRecognizer = null
            }
        }
    }

    private var ttsTimeoutJob: Job? = null

    fun speakText(text: String) {
        val cleanText = text.replace(Regex("[*#_`~]"), "").trim()
        if (cleanText.isBlank()) {
            onTtsFinished?.invoke()
            return
        }

        startTtsAmplitudeSimulation()

        // Estimate duration based on ~130 words per minute (approx 2.1 words/sec)
        val wordCount = cleanText.split("\\s+".toRegex()).size
        val estimatedDurationMs = ((wordCount / 2.2f) * 1000L).toLong().coerceIn(2000L, 15000L)

        // Safety fallback timer in case TTS doesn't trigger onDone
        ttsTimeoutJob?.cancel()
        ttsTimeoutJob = scope.launch {
            delay(estimatedDurationMs + 1200L)
            stopTtsAmplitudeSimulation()
            onTtsFinished?.invoke()
        }

        if (tts != null && isTtsReady) {
            val utteranceId = "jarvis_tts_${System.currentTimeMillis()}"
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        } else if (tts == null) {
            initTts()
        }
    }

    fun stopSpeaking() {
        ttsTimeoutJob?.cancel()
        ttsTimeoutJob = null
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error stopping TTS", e)
        }
        stopTtsAmplitudeSimulation()
    }

    private fun startTtsAmplitudeSimulation() {
        ttsAmplitudeJob?.cancel()
        ttsAmplitudeJob = scope.launch {
            while (isActive) {
                val base = (Math.sin(System.currentTimeMillis() / 70.0) * 0.4 + 0.5).toFloat()
                _micRmsDb.value = (base + (Math.random() * 0.2).toFloat()).coerceIn(0.2f, 0.9f)
                delay(50)
            }
        }
    }

    private fun stopTtsAmplitudeSimulation() {
        ttsAmplitudeJob?.cancel()
        ttsAmplitudeJob = null
        _micRmsDb.value = 0f
    }

    fun release() {
        stopListening()
        stopSpeaking()
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error shutting down TTS", e)
        }
        tts = null
    }
}
