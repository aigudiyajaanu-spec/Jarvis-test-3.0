package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class JarvisPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("jarvis_config_prefs", Context.MODE_PRIVATE)

    private val _apiKey = MutableStateFlow(getStoredApiKey())
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _voiceName = MutableStateFlow(getStoredVoiceName())
    val voiceName: StateFlow<String> = _voiceName.asStateFlow()

    private val _thinkingLevel = MutableStateFlow(getStoredThinkingLevel())
    val thinkingLevel: StateFlow<String> = _thinkingLevel.asStateFlow()

    private val _isVisionEnabled = MutableStateFlow(getStoredVisionDefault())
    val isVisionEnabled: StateFlow<Boolean> = _isVisionEnabled.asStateFlow()

    private fun getStoredApiKey(): String {
        val customKey = prefs.getString(KEY_API_KEY, "") ?: ""
        if (customKey.isNotBlank()) return customKey
        // Fallback to injected BuildConfig key if valid
        return if (BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" && BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            BuildConfig.GEMINI_API_KEY
        } else {
            ""
        }
    }

    fun setApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key.trim()).apply()
        _apiKey.value = key.trim()
    }

    private fun getStoredVoiceName(): String {
        return prefs.getString(KEY_VOICE_NAME, "Orus") ?: "Orus"
    }

    fun setVoiceName(voice: String) {
        prefs.edit().putString(KEY_VOICE_NAME, voice).apply()
        _voiceName.value = voice
    }

    private fun getStoredThinkingLevel(): String {
        return prefs.getString(KEY_THINKING_LEVEL, "minimal") ?: "minimal"
    }

    fun setThinkingLevel(level: String) {
        prefs.edit().putString(KEY_THINKING_LEVEL, level).apply()
        _thinkingLevel.value = level
    }

    private fun getStoredVisionDefault(): Boolean {
        return prefs.getBoolean(KEY_VISION_DEFAULT, false)
    }

    fun setVisionDefault(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VISION_DEFAULT, enabled).apply()
        _isVisionEnabled.value = enabled
    }

    companion object {
        private const val KEY_API_KEY = "jarvis_gemini_api_key"
        private const val KEY_VOICE_NAME = "jarvis_voice_name"
        private const val KEY_THINKING_LEVEL = "jarvis_thinking_level"
        private const val KEY_VISION_DEFAULT = "jarvis_vision_default"
    }
}
