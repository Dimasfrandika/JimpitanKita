package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.exp
import kotlin.math.sin

/**
 * Helper to trigger realistic sound and haptic alert when JimpitanKita notifications arrive.
 * Synthesizes the authentic "Kentongan Jimpitan Ronda Siskamling" dual-tone percussion chime
 * (Thok... TONG!) that perfectly matches Indonesian RT/RW neighborhood night patrol.
 */
object NotificationSoundHelper {

    /**
     * Synthesizes PCM audio buffer replicating traditional Indonesian bamboo/wooden Kentongan.
     * Strike 1: "Thok" (520 Hz tight wood block)
     * Strike 2: "TONG" (780 Hz hollow chamber resonance with 1560 Hz harmonic)
     */
    private fun generateKentonganPcm(): ShortArray {
        val sampleRate = 22050
        val totalSamples = (sampleRate * 0.28).toInt()
        val buffer = ShortArray(totalSamples)

        val strike1Duration = (sampleRate * 0.07).toInt()
        val gap = (sampleRate * 0.04).toInt()
        val strike2Start = strike1Duration + gap
        val strike2Duration = totalSamples - strike2Start

        // Strike 1: Tight wood knock
        for (i in 0 until strike1Duration) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 60.0)
            val sample = (sin(2.0 * Math.PI * 520.0 * t) * 0.75 +
                          sin(2.0 * Math.PI * 1040.0 * t) * 0.25) * env
            buffer[i] = (sample * 28000.0).toInt().coerceIn(-32767, 32767).toShort()
        }

        // Strike 2: Resonant hollow bamboo / kentongan chamber
        for (i in 0 until strike2Duration) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 22.0)
            val sample = (sin(2.0 * Math.PI * 780.0 * t) * 0.70 +
                          sin(2.0 * Math.PI * 1560.0 * t) * 0.30) * env
            buffer[strike2Start + i] = (sample * 30000.0).toInt().coerceIn(-32767, 32767).toShort()
        }

        return buffer
    }

    /**
     * Plays the signature Kentongan Jimpitan chime accompanied by rhythmic dual-pulse vibration.
     */
    fun playNotificationAlert(context: Context) {
        try {
            val pcm = generateKentonganPcm()
            val sampleRate = 22050
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val audioFormat = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val track = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcm, 0, pcm.size)
            track.play()

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
            }, 600)
        } catch (_: Exception) {
            // Fallback to standard system ringtone
            try {
                val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context, notificationUri)
                ringtone?.play()
            } catch (_: Exception) {}
        }

        // Dual-beat haptic feedback matching the kentongan cadence
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 70, 50, 110), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
        } catch (_: Exception) {}
    }
}
