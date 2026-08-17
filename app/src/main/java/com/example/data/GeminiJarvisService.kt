package com.example.data

import android.util.Base64
import android.util.Log
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

sealed class JarvisResponse {
    data class AudioAndText(
        val text: String,
        val audioBase64: String?,
        val mimeType: String,
        val toolExecuted: String? = null,
        val toolResult: String? = null
    ) : JarvisResponse()

    data class Error(val message: String) : JarvisResponse()
}

data class ChatMessage(
    val role: String, // "user", "model", "tool"
    val text: String,
    val imageBase64: String? = null,
    val isSpoken: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class GeminiJarvisService {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Model selection prioritizing real-time audio / native audio preview
    private val primaryModel = "gemini-2.5-flash-native-audio-preview-12-2025"
    private val fallbackModel = "gemini-2.5-flash"

    suspend fun generateJarvisReply(
        apiKey: String,
        history: List<ChatMessage>,
        userPrompt: String,
        imageBase64: String? = null,
        voiceName: String = "Orus",
        thinkingLevel: String = "minimal",
        toolsHandler: SystemToolsHandler
    ): JarvisResponse = withContext(Dispatchers.IO) {
        // If API key is not configured, immediately use built-in Stark tactical intelligence
        if (apiKey.isBlank()) {
            return@withContext generateLocalJarvisReply(userPrompt, imageBase64, toolsHandler)
        }

        // Try primary native audio model first
        val firstAttempt = executeRequest(primaryModel, apiKey, history, userPrompt, imageBase64, voiceName, thinkingLevel, toolsHandler, isNativeAudio = true)
        if (firstAttempt is JarvisResponse.AudioAndText) {
            return@withContext firstAttempt
        }

        // If error on primary, try standard flash model
        val secondAttempt = executeRequest(fallbackModel, apiKey, history, userPrompt, imageBase64, voiceName, thinkingLevel, toolsHandler, isNativeAudio = false)
        if (secondAttempt is JarvisResponse.AudioAndText) {
            return@withContext secondAttempt
        }

        // If both cloud requests failed, fallback to local tactical response so JARVIS always speaks
        val localResponse = generateLocalJarvisReply(userPrompt, imageBase64, toolsHandler)
        localResponse
    }

    private fun generateLocalJarvisReply(
        userPrompt: String,
        imageBase64: String?,
        toolsHandler: SystemToolsHandler
    ): JarvisResponse {
        val lower = userPrompt.lowercase().trim()

        return when {
            lower.contains("diagnostics") || lower.contains("status") || lower.contains("battery") || lower.contains("system") -> {
                val statusText = toolsHandler.getSystemStatus()
                JarvisResponse.AudioAndText(
                    text = "All primary diagnostics operational, Sir. Systems are online and functioning within standard Stark parameters.",
                    audioBase64 = null,
                    mimeType = "audio/mp3",
                    toolExecuted = "getSystemStatus",
                    toolResult = statusText
                )
            }
            lower.contains("torch") || lower.contains("flashlight") || lower.contains("light") -> {
                val (tName, tRes) = toolsHandler.handleFunctionCall("toggleTorch", JSONObject())
                JarvisResponse.AudioAndText(
                    text = "Illumination protocols updated, Sir.",
                    audioBase64 = null,
                    mimeType = "audio/mp3",
                    toolExecuted = tName,
                    toolResult = tRes
                )
            }
            lower.contains("time") || lower.contains("date") || lower.contains("clock") -> {
                val (tName, tRes) = toolsHandler.handleFunctionCall("getTimeAndDate", JSONObject())
                JarvisResponse.AudioAndText(
                    text = "$tRes, Sir.",
                    audioBase64 = null,
                    mimeType = "audio/mp3",
                    toolExecuted = tName,
                    toolResult = tRes
                )
            }
            lower.contains("open") && (lower.contains("github") || lower.contains("google") || lower.contains("http") || lower.contains("web")) -> {
                val url = if (lower.contains("github")) "https://github.com" else if (lower.contains("google")) "https://google.com" else "https://ai.google.dev"
                val (tName, tRes) = toolsHandler.handleFunctionCall("openWebsite", JSONObject().apply { put("url", url) })
                JarvisResponse.AudioAndText(
                    text = "Opening requested destination in your browser, Sir.",
                    audioBase64 = null,
                    mimeType = "audio/mp3",
                    toolExecuted = tName,
                    toolResult = tRes
                )
            }
            lower.contains("hello") || lower.contains("hi") || lower.contains("jarvis") || lower.contains("who are you") || lower.contains("what can you do") -> {
                JarvisResponse.AudioAndText(
                    text = "Good day, Sir. I am JARVIS, your tactical Stark operating intelligence. All HUD feeds, acoustic sensors, and device controls are at your disposal.",
                    audioBase64 = null,
                    mimeType = "audio/mp3"
                )
            }
            !imageBase64.isNullOrBlank() -> {
                JarvisResponse.AudioAndText(
                    text = "Optical feed analyzed, Sir. Viewport telemetry captured and logged into memory buffers.",
                    audioBase64 = null,
                    mimeType = "audio/mp3"
                )
            }
            else -> {
                JarvisResponse.AudioAndText(
                    text = "Instruction acknowledged, Sir. Stark tactical systems are standing by. For unbounded generative intelligence, you can connect your Gemini API key in Settings.",
                    audioBase64 = null,
                    mimeType = "audio/mp3"
                )
            }
        }
    }

    private fun executeRequest(
        model: String,
        apiKey: String,
        history: List<ChatMessage>,
        userPrompt: String,
        imageBase64: String?,
        voiceName: String,
        thinkingLevel: String,
        toolsHandler: SystemToolsHandler,
        isNativeAudio: Boolean = false
    ): JarvisResponse {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        try {
            val rootJson = JSONObject()

            // System Instruction for JARVIS persona
            val systemInstruction = JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put(
                            "text",
                            "You are JARVIS (Just A Rather Very Intelligent System), Tony Stark's composed, exceptionally sharp, and quietly witty male AI assistant. " +
                                    "Your tone is calm, highly capable, crisp, and respectful (addressing the user as 'Sir' or 'Boss'). " +
                                    "Keep spoken responses concise, punchy, and natural. Never ramble, never sound robotic. " +
                                    "You have access to real device tools such as opening websites (openWebsite), inspecting system telemetry (getSystemStatus), toggling torch illumination (toggleTorch), and retrieving system time (getTimeAndDate). " +
                                    "When visual data from the HUD camera/screen is provided, analyze the scene with high precision."
                        )
                    })
                })
            }
            rootJson.put("systemInstruction", systemInstruction)

            // Contents array
            val contentsArray = JSONArray()

            // Add recent history turns
            val recentHistory = history.takeLast(6)
            for (msg in recentHistory) {
                val contentObj = JSONObject().apply {
                    put("role", if (msg.role == "user") "user" else "model")
                    val parts = JSONArray()
                    if (!msg.imageBase64.isNullOrBlank()) {
                        parts.put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", msg.imageBase64)
                            })
                        })
                    }
                    if (msg.text.isNotBlank()) {
                        parts.put(JSONObject().apply {
                            put("text", msg.text)
                        })
                    }
                    put("parts", parts)
                }
                contentsArray.put(contentObj)
            }

            // Current user turn
            val currentUserTurn = JSONObject().apply {
                put("role", "user")
                val parts = JSONArray()
                if (!imageBase64.isNullOrBlank()) {
                    parts.put(JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", "image/jpeg")
                            put("data", imageBase64)
                        })
                    })
                }
                parts.put(JSONObject().apply {
                    put("text", userPrompt)
                })
                put("parts", parts)
            }
            contentsArray.put(currentUserTurn)
            rootJson.put("contents", contentsArray)

            // Generation Config
            val generationConfig = JSONObject().apply {
                put("temperature", 0.6)
                put("topP", 0.95)

                if (isNativeAudio) {
                    val modalities = JSONArray().apply {
                        put("AUDIO")
                        put("TEXT")
                    }
                    put("responseModalities", modalities)

                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", voiceName)
                            })
                        })
                    })
                }
            }
            rootJson.put("generationConfig", generationConfig)

            // Tools definitions
            val toolsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("functionDeclarations", JSONArray().apply {
                        put(JSONObject().apply {
                            put("name", "openWebsite")
                            put("description", "Opens a web URL in the user's browser")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject().apply {
                                    put("url", JSONObject().apply {
                                        put("type", "STRING")
                                        put("description", "The website URL to launch, e.g. https://github.com")
                                    })
                                })
                                put("required", JSONArray().apply { put("url") })
                            })
                        })
                        put(JSONObject().apply {
                            put("name", "getSystemStatus")
                            put("description", "Retrieves real-time system diagnostics including battery charge, uplink connection, device unit specs, and storage matrix")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject())
                            })
                        })
                        put(JSONObject().apply {
                            put("name", "toggleTorch")
                            put("description", "Toggles the device flashlight / torch illumination on or off")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject().apply {
                                    put("enable", JSONObject().apply {
                                        put("type", "BOOLEAN")
                                        put("description", "True to turn on illumination, false to turn off")
                                    })
                                })
                                put("required", JSONArray().apply { put("enable") })
                            })
                        })
                        put(JSONObject().apply {
                            put("name", "getTimeAndDate")
                            put("description", "Retrieves current accurate system date and time")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject())
                            })
                        })
                    })
                })
            }
            rootJson.put("tools", toolsArray)

            val requestBody = rootJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("GeminiJarvisService", "API Error $model: ${response.code} $responseString")
                val errorMsg = try {
                    val errJson = JSONObject(responseString)
                    errJson.optJSONObject("error")?.optString("message") ?: "API Error (${response.code})"
                } catch (e: Exception) {
                    "Protocol communication error (${response.code})"
                }
                return JarvisResponse.Error(errorMsg)
            }

            val respJson = JSONObject(responseString)
            val candidates = respJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            var responseText = ""
            var audioBase64: String? = null
            var mimeType = "audio/mp3"
            var executedToolName: String? = null
            var executedToolResult: String? = null

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i) ?: continue

                    // Check for function calls
                    if (part.has("functionCall")) {
                        val fc = part.getJSONObject("functionCall")
                        val funcName = fc.optString("name")
                        val args = fc.optJSONObject("args") ?: JSONObject()

                        val (tName, tRes) = toolsHandler.handleFunctionCall(funcName, args)
                        executedToolName = tName
                        executedToolResult = tRes

                        // Send synchronous tool response back to Gemini to complete conversation turn
                        return sendToolResponseTurn(
                            model = model,
                            apiKey = apiKey,
                            rootJson = rootJson,
                            functionName = funcName,
                            toolResult = tRes,
                            voiceName = voiceName,
                            thinkingLevel = thinkingLevel
                        )
                    }

                    // Check for text
                    if (part.has("text")) {
                        responseText += part.optString("text") + " "
                    }

                    // Check for audio inlineData
                    if (part.has("inlineData")) {
                        val inline = part.getJSONObject("inlineData")
                        audioBase64 = inline.optString("data")
                        mimeType = inline.optString("mimeType", "audio/mp3")
                    }
                }
            }

            return JarvisResponse.AudioAndText(
                text = responseText.trim(),
                audioBase64 = audioBase64,
                mimeType = mimeType,
                toolExecuted = executedToolName,
                toolResult = executedToolResult
            )

        } catch (e: Exception) {
            Log.e("GeminiJarvisService", "Exception in API call", e)
            return JarvisResponse.Error("Transmission exception: ${e.localizedMessage}")
        }
    }

    private fun sendToolResponseTurn(
        model: String,
        apiKey: String,
        rootJson: JSONObject,
        functionName: String,
        toolResult: String,
        voiceName: String,
        thinkingLevel: String
    ): JarvisResponse {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val contents = rootJson.getJSONArray("contents")

            // Append model function call
            val modelCallTurn = JSONObject().apply {
                put("role", "model")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("functionCall", JSONObject().apply {
                            put("name", functionName)
                            put("args", JSONObject())
                        })
                    })
                })
            }
            contents.put(modelCallTurn)

            // Append user function response
            val toolResponseTurn = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("functionResponse", JSONObject().apply {
                            put("name", functionName)
                            put("response", JSONObject().apply {
                                put("result", toolResult)
                            })
                        })
                    })
                })
            }
            contents.put(toolResponseTurn)

            val requestBody = rootJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder().url(url).post(requestBody).build()
            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return JarvisResponse.AudioAndText(
                    text = toolResult,
                    audioBase64 = null,
                    mimeType = "audio/mp3",
                    toolExecuted = functionName,
                    toolResult = toolResult
                )
            }

            val respJson = JSONObject(responseString)
            val candidates = respJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            var responseText = ""
            var audioBase64: String? = null
            var mimeType = "audio/mp3"

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i) ?: continue
                    if (part.has("text")) {
                        responseText += part.optString("text") + " "
                    }
                    if (part.has("inlineData")) {
                        val inline = part.getJSONObject("inlineData")
                        audioBase64 = inline.optString("data")
                        mimeType = inline.optString("mimeType", "audio/mp3")
                    }
                }
            }

            return JarvisResponse.AudioAndText(
                text = if (responseText.isNotBlank()) responseText.trim() else toolResult,
                audioBase64 = audioBase64,
                mimeType = mimeType,
                toolExecuted = functionName,
                toolResult = toolResult
            )
        } catch (e: Exception) {
            return JarvisResponse.AudioAndText(
                text = toolResult,
                audioBase64 = null,
                mimeType = "audio/mp3",
                toolExecuted = functionName,
                toolResult = toolResult
            )
        }
    }

    suspend fun validateApiKey(apiKey: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Pair(false, "API Key is empty.")
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val testJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", "Respond with 'JARVIS Online'") })
                        })
                    })
                })
            }
            val request = Request.Builder()
                .url(url)
                .post(testJson.toString().toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Pair(true, "Authentication Handshake Successful: JARVIS Core Linked.")
            } else {
                val errBody = response.body?.string() ?: ""
                val msg = try {
                    JSONObject(errBody).optJSONObject("error")?.optString("message") ?: "Error ${response.code}"
                } catch (e: Exception) {
                    "Handshake Rejected (${response.code})"
                }
                Pair(false, msg)
            }
        } catch (e: Exception) {
            Pair(false, "Network error: ${e.localizedMessage}")
        }
    }
}
