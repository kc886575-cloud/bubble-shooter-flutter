package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sin

enum class SoundEffect {
    SHOOT,
    POP,
    BOUNCE,
    COMBO,
    WIN,
    LOSE,
    CLANK,
    TIER_UP
}

class SoundManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)
    var isSoundEnabled: Boolean = true
    var isHapticsEnabled: Boolean = true

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val sampleRate = 22050
    private val audioTrackMap = ConcurrentHashMap<SoundEffect, AudioTrack>()

    init {
        scope.launch {
            try {
                initAudioTrack(SoundEffect.SHOOT, generateChirp(350.0, 850.0, 70, 0.5))
                initAudioTrack(SoundEffect.POP, generatePop(700.0, 200.0, 55, 0.7))
                initAudioTrack(SoundEffect.BOUNCE, generateTone(520.0, 40, 0.4))
                initAudioTrack(SoundEffect.COMBO, generateArpeggio(listOf(523.25, 659.25, 783.99, 1046.5), 60, 0.6))
                initAudioTrack(SoundEffect.WIN, generateArpeggio(listOf(523.25, 659.25, 783.99, 1046.5, 1318.5), 90, 0.7))
                initAudioTrack(SoundEffect.LOSE, generateChirp(500.0, 150.0, 300, 0.6))
                initAudioTrack(SoundEffect.CLANK, generateChirp(180.0, 80.0, 160, 0.8))
                initAudioTrack(SoundEffect.TIER_UP, generateArpeggio(listOf(440.0, 554.37, 659.25, 880.0), 50, 0.7))
            } catch (_: Exception) {
                // Ignore audio init errors
            }
        }
    }

    private fun initAudioTrack(effect: SoundEffect, buffer: ShortArray) {
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            audioTrackMap[effect] = track
        } catch (_: Exception) {}
    }

    fun play(effect: SoundEffect) {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                val track = audioTrackMap[effect]
                if (track != null && track.state == AudioTrack.STATE_INITIALIZED) {
                    track.stop()
                    track.reloadStaticData()
                    track.play()
                }
            } catch (_: Exception) {
                // Ignore transient audio play errors
            }
        }
    }

    fun vibrate(durationMs: Long = 25) {
        if (!isHapticsEnabled || vibrator == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        durationMs.coerceAtLeast(10),
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    private fun generateTone(frequency: Double, durationMs: Int, volume: Double): ShortArray {
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = 1.0 - (i.toDouble() / numSamples)
            val sample = sin(2.0 * Math.PI * frequency * t) * envelope * volume
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generatePop(startFreq: Double, endFreq: Double, durationMs: Int, volume: Double): ShortArray {
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val currentFreq = startFreq + (endFreq - startFreq) * progress
            val envelope = (1.0 - progress) * (1.0 - progress)
            val t = i.toDouble() / sampleRate
            val sample = sin(2.0 * Math.PI * currentFreq * t) * envelope * volume
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateChirp(startFreq: Double, endFreq: Double, durationMs: Int, volume: Double): ShortArray {
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val currentFreq = startFreq + (endFreq - startFreq) * progress
            val envelope = sin(progress * Math.PI)
            val t = i.toDouble() / sampleRate
            val sample = sin(2.0 * Math.PI * currentFreq * t) * envelope * volume
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateArpeggio(notes: List<Double>, noteDurationMs: Int, volume: Double): ShortArray {
        val totalSamples = ((sampleRate * noteDurationMs) / 1000) * notes.size
        val buffer = ShortArray(totalSamples)
        val noteSamples = (sampleRate * noteDurationMs) / 1000

        for (n in notes.indices) {
            val freq = notes[n]
            for (i in 0 until noteSamples) {
                val idx = n * noteSamples + i
                val progress = i.toDouble() / noteSamples
                val envelope = (1.0 - progress)
                val t = i.toDouble() / sampleRate
                val sample = sin(2.0 * Math.PI * freq * t) * envelope * volume
                buffer[idx] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        return buffer
    }
}
