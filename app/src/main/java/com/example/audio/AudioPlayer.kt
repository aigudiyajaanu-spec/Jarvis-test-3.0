package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var amplitudeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _playbackAmplitude = MutableStateFlow(0f)
    val playbackAmplitude: StateFlow<Float> = _playbackAmplitude.asStateFlow()

    private val _isPlayingState = MutableStateFlow(false)
    val isPlayingState: StateFlow<Boolean> = _isPlayingState.asStateFlow()

    var onPlaybackFinished: (() -> Unit)? = null

    fun playAudioBase64(base64Audio: String, mimeType: String = "audio/mp3") {
        stop()
        try {
            val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)
            if (audioBytes.isEmpty()) {
                onPlaybackFinished?.invoke()
                return
            }

            if (mimeType.contains("pcm") || mimeType.contains("raw")) {
                playPcm24k(audioBytes)
            } else {
                // Save to cache temp file and play with MediaPlayer
                val tempFile = File(context.cacheDir, "jarvis_response_${System.currentTimeMillis()}.mp3")
                FileOutputStream(tempFile).use { it.write(audioBytes) }

                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_ASSISTANT)
                            .build()
                    )
                    setDataSource(tempFile.absolutePath)
                    prepare()
                    setOnCompletionListener {
                        stop()
                        tempFile.delete()
                        onPlaybackFinished?.invoke()
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e("AudioPlayer", "MediaPlayer error: what=$what, extra=$extra")
                        stop()
                        tempFile.delete()
                        onPlaybackFinished?.invoke()
                        true
                    }
                    start()
                }
                isPlaying = true
                _isPlayingState.value = true
                startAmplitudeSimulation()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Failed to play audio", e)
            stop()
            onPlaybackFinished?.invoke()
        }
    }

    private fun playPcm24k(pcmData: ByteArray) {
        scope.launch(Dispatchers.IO) {
            try {
                val sampleRate = 24000
                val channelConfig = AudioFormat.CHANNEL_OUT_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(audioFormat)
                            .setSampleRate(sampleRate)
                            .setChannelMask(channelConfig)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufferSize.coerceAtLeast(pcmData.size))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.play()
                isPlaying = true
                _isPlayingState.value = true

                var offset = 0
                val chunkSize = 2048
                while (isActive && offset < pcmData.size && isPlaying) {
                    val length = (pcmData.size - offset).coerceAtMost(chunkSize)
                    audioTrack?.write(pcmData, offset, length)

                    // Calculate peak RMS amplitude from chunk
                    var sum = 0L
                    var i = offset
                    while (i < offset + length - 1) {
                        val sample = (pcmData[i + 1].toInt() shl 8) or (pcmData[i].toInt() and 0xFF)
                        sum += sample * sample
                        i += 2
                    }
                    val rms = Math.sqrt((sum / (length / 2).coerceAtLeast(1)).toDouble()).toFloat()
                    val normalized = (rms / 8000f).coerceIn(0.1f, 1.0f)
                    _playbackAmplitude.value = normalized

                    offset += length
                    delay(20)
                }

                audioTrack?.stop()
                audioTrack?.release()
                audioTrack = null
                stop()
                onPlaybackFinished?.invoke()
            } catch (e: Exception) {
                Log.e("AudioPlayer", "PCM audio playback error", e)
                stop()
                onPlaybackFinished?.invoke()
            }
        }
    }

    private fun startAmplitudeSimulation() {
        amplitudeJob?.cancel()
        amplitudeJob = scope.launch {
            while (isActive && isPlaying) {
                // Generate dynamic realistic speech amplitude envelope
                val base = (Math.sin(System.currentTimeMillis() / 80.0) * 0.4 + 0.5).toFloat()
                val jitter = (Math.random() * 0.3).toFloat()
                _playbackAmplitude.value = (base + jitter).coerceIn(0.15f, 0.95f)
                delay(50)
            }
            _playbackAmplitude.value = 0f
        }
    }

    fun stop() {
        isPlaying = false
        _isPlayingState.value = false
        _playbackAmplitude.value = 0f
        amplitudeJob?.cancel()
        amplitudeJob = null

        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error releasing MediaPlayer", e)
        }
        mediaPlayer = null

        try {
            audioTrack?.let {
                it.stop()
                it.release()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error releasing AudioTrack", e)
        }
        audioTrack = null
    }

    fun release() {
        stop()
    }
}
