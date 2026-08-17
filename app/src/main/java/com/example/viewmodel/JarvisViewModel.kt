package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayer
import com.example.audio.SpeechManager
import com.example.data.ChatMessage
import com.example.data.GeminiJarvisService
import com.example.data.JarvisPreferences
import com.example.data.JarvisResponse
import com.example.data.SystemToolsHandler
import com.example.vision.VisionCameraManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class JarvisHudState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}

data class TelemetryLog(
    val timestamp: String,
    val tag: String,
    val message: String,
    val isError: Boolean = false,
    val isTool: Boolean = false
)

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    val preferences = JarvisPreferences(application)
    private val geminiService = GeminiJarvisService()
    val toolsHandler = SystemToolsHandler(application)
    val audioPlayer = AudioPlayer(application)
    val speechManager = SpeechManager(application)
    val visionManager = VisionCameraManager(application)

    private val _hudState = MutableStateFlow(JarvisHudState.IDLE)
    val hudState: StateFlow<JarvisHudState> = _hudState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _currentSubtitle = MutableStateFlow("JARVIS PROTOCOLS STANDBY")
    val currentSubtitle: StateFlow<String> = _currentSubtitle.asStateFlow()

    private val _isVisionMode = MutableStateFlow(preferences.isVisionEnabled.value)
    val isVisionMode: StateFlow<Boolean> = _isVisionMode.asStateFlow()

    private val _telemetryLogs = MutableStateFlow<List<TelemetryLog>>(emptyList())
    val telemetryLogs: StateFlow<List<TelemetryLog>> = _telemetryLogs.asStateFlow()

    private val _keyValidationStatus = MutableStateFlow<String?>(null)
    val keyValidationStatus: StateFlow<String?> = _keyValidationStatus.asStateFlow()

    private val _isValidatingKey = MutableStateFlow(false)
    val isValidatingKey: StateFlow<Boolean> = _isValidatingKey.asStateFlow()

    // Combined amplitude from mic or audio playback for pulsing Arc Reactor & Waveform
    val liveAmplitude: StateFlow<Float> = combine(
        speechManager.micRmsDb,
        audioPlayer.playbackAmplitude,
        _hudState
    ) { micAmp, playAmp, state ->
        when (state) {
            JarvisHudState.LISTENING -> micAmp
            JarvisHudState.SPEAKING -> playAmp
            JarvisHudState.THINKING -> 0.35f
            JarvisHudState.IDLE -> 0.05f
            JarvisHudState.ERROR -> 0.1f
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.05f)

    init {
        addLog("SYSTEM", "Stark Industries Core OS initializing...")
        addLog("AUDIO", "Speech synthesizers and neural audio tracks armed.")
        addLog("DIAG", "Security protocols active.")

        // Forward live speech recognition updates to HUD subtitle
        viewModelScope.launch {
            speechManager.partialTranscript.collect { liveText ->
                if (_hudState.value == JarvisHudState.LISTENING && liveText.isNotBlank()) {
                    _currentSubtitle.value = "\"$liveText\""
                }
            }
        }

        // Speech Manager callbacks
        speechManager.onSpeechRecognized = { recognizedText ->
            if (recognizedText.isNotBlank()) {
                addLog("SPEECH_IN", recognizedText)
                sendUserQuery(recognizedText)
            } else {
                _hudState.value = JarvisHudState.IDLE
                _currentSubtitle.value = "Tap reactor or speak to begin, Sir."
            }
        }

        speechManager.onSpeechError = { errorMsg ->
            addLog("SPEECH_ERR", errorMsg, isError = true)
            _hudState.value = JarvisHudState.IDLE
            _currentSubtitle.value = errorMsg
        }

        speechManager.onTtsFinished = {
            if (_hudState.value == JarvisHudState.SPEAKING) {
                _hudState.value = JarvisHudState.IDLE
                _currentSubtitle.value = "Awaiting command, Sir."
            }
        }

        // Audio Player callbacks
        audioPlayer.onPlaybackFinished = {
            if (_hudState.value == JarvisHudState.SPEAKING) {
                _hudState.value = JarvisHudState.IDLE
                _currentSubtitle.value = "At your service, Sir."
            }
        }
    }

    fun onReactorClick() {
        when (_hudState.value) {
            JarvisHudState.LISTENING -> {
                // User taps while listening -> process captured text or reset to IDLE
                val captured = speechManager.partialTranscript.value.trim()
                speechManager.stopListening()
                if (captured.isNotBlank()) {
                    addLog("SPEECH_IN", captured)
                    sendUserQuery(captured)
                } else {
                    _hudState.value = JarvisHudState.IDLE
                    _currentSubtitle.value = "Listening paused. Tap reactor or type below, Sir."
                }
            }
            JarvisHudState.SPEAKING -> {
                // Interruption / Barge-in
                interrupt()
            }
            JarvisHudState.THINKING -> {
                // Cancel current query
                _hudState.value = JarvisHudState.IDLE
                _currentSubtitle.value = "Protocol halted."
            }
            JarvisHudState.IDLE, JarvisHudState.ERROR -> {
                startVoiceInput()
            }
        }
    }

    fun startVoiceInput() {
        interrupt()
        _hudState.value = JarvisHudState.LISTENING
        _currentSubtitle.value = "Listening..."
        addLog("MIC", "Acoustic audio stream engaged.")
        speechManager.startListening()
    }

    fun interrupt() {
        audioPlayer.stop()
        speechManager.stopSpeaking()
        speechManager.stopListening()
        if (_hudState.value == JarvisHudState.SPEAKING) {
            addLog("AUDIO", "Barge-in detected: playback terminated.")
        }
        _hudState.value = JarvisHudState.IDLE
    }

    fun toggleVision() {
        val newState = !_isVisionMode.value
        _isVisionMode.value = newState
        preferences.setVisionDefault(newState)
        if (newState) {
            addLog("VISION", "HUD Optical Sensor Feed Online.")
        } else {
            addLog("VISION", "HUD Optical Sensor Feed Disengaged.")
            visionManager.unbind()
        }
    }

    fun captureVisionAndQuery(customPrompt: String? = null) {
        if (!_isVisionMode.value) {
            toggleVision()
        }
        _hudState.value = JarvisHudState.THINKING
        _currentSubtitle.value = "Scanning optical viewport..."
        addLog("VISION", "Acquiring high-resolution visual frame...")

        visionManager.captureFrame { base64Frame ->
            val prompt = customPrompt ?: "JARVIS, analyze what you see in the viewport and give a sharp tactical report."
            sendUserQuery(prompt, base64Frame)
        }
    }

    fun sendUserQuery(prompt: String, imageBase64: String? = null) {
        interrupt()
        _hudState.value = JarvisHudState.THINKING
        _currentSubtitle.value = "Analyzing query protocols..."

        val userMessage = ChatMessage(
            role = "user",
            text = prompt,
            imageBase64 = imageBase64
        )
        _messages.value = _messages.value + userMessage
        addLog("QUERY", prompt)

        viewModelScope.launch {
            val apiKey = preferences.apiKey.value
            val voice = preferences.voiceName.value
            val thinking = preferences.thinkingLevel.value

            val response = geminiService.generateJarvisReply(
                apiKey = apiKey,
                history = _messages.value.dropLast(1),
                userPrompt = prompt,
                imageBase64 = imageBase64,
                voiceName = voice,
                thinkingLevel = thinking,
                toolsHandler = toolsHandler
            )

            when (response) {
                is JarvisResponse.AudioAndText -> {
                    val replyText = response.text.ifBlank { "Right away, Sir." }
                    _currentSubtitle.value = replyText

                    val modelMessage = ChatMessage(
                        role = "model",
                        text = replyText,
                        isSpoken = true
                    )
                    _messages.value = _messages.value + modelMessage

                    if (response.toolExecuted != null) {
                        addLog("TOOL_EXEC", "Tool '${response.toolExecuted}' -> ${response.toolResult}", isTool = true)
                    }
                    addLog("JARVIS", replyText)

                    _hudState.value = JarvisHudState.SPEAKING

                    if (!response.audioBase64.isNullOrBlank()) {
                        addLog("AUDIO", "Playing neural audio voice stream (${response.mimeType}).")
                        audioPlayer.playAudioBase64(response.audioBase64, response.mimeType)
                    } else {
                        addLog("TTS", "Rendering spoken response via British cadence engine.")
                        speechManager.speakText(replyText)
                    }
                }
                is JarvisResponse.Error -> {
                    _hudState.value = JarvisHudState.ERROR
                    _currentSubtitle.value = response.message
                    addLog("ERR", response.message, isError = true)
                }
            }
        }
    }

    fun validateAndSaveKey(key: String) {
        _isValidatingKey.value = true
        _keyValidationStatus.value = "Initiating handshake with Gemini neural core..."
        viewModelScope.launch {
            val (success, message) = geminiService.validateApiKey(key.trim())
            _isValidatingKey.value = false
            _keyValidationStatus.value = message
            if (success) {
                preferences.setApiKey(key.trim())
                addLog("AUTH", "New Gemini API key verified and stored securely.")
            } else {
                addLog("AUTH_ERR", "API Key verification failed: $message", isError = true)
            }
        }
    }

    fun setVoice(voice: String) {
        preferences.setVoiceName(voice)
        addLog("VOICE", "Voice profile set to '$voice'.")
    }

    fun setThinking(level: String) {
        preferences.setThinkingLevel(level)
        addLog("CORE", "Neural thinking level adjusted to '$level'.")
    }

    fun addLog(tag: String, message: String, isError: Boolean = false, isTool: Boolean = false) {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        val log = TelemetryLog(
            timestamp = sdf.format(Date()),
            tag = tag,
            message = message,
            isError = isError,
            isTool = isTool
        )
        _telemetryLogs.value = (_telemetryLogs.value + log).takeLast(100)
    }

    fun clearLogs() {
        _telemetryLogs.value = emptyList()
    }

    fun clearHistory() {
        _messages.value = emptyList()
        _currentSubtitle.value = "Memory buffers purged. Ready for instructions, Sir."
        addLog("MEMORY", "Context conversation history cleared.")
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
        speechManager.release()
        visionManager.release()
    }
}
