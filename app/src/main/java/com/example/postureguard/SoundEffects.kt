package com.example.postureguard

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

class SoundEffects(context: Context) {

    private val sampleRate = 22050

    private val chirpBuffer = generateBirdChirp()
    private val cawBuffer = generateCrowCaw()

    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
        .build()

    fun playChirp() {
        playBuffer(chirpBuffer)
    }

    fun playCaw() {
        playBuffer(cawBuffer)
    }

    private fun playBuffer(buffer: ShortArray) {
        try {
            val bufSize = buffer.size * 2
            val track = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(buffer, 0, buffer.size)
            track.play()
        } catch (e: Exception) {
            Log.w("SoundEffects", "Failed to play sound", e)
        }
    }

    private fun generateBirdChirp(): ShortArray {
        val durationSec = 0.35
        val numSamples = (sampleRate * durationSec).toInt()
        val samples = ShortArray(numSamples)

        // 3 rapid chirps with ascending pitch
        val chirpCount = 3
        val chirpLen = numSamples / chirpCount

        for (c in 0 until chirpCount) {
            val start = c * chirpLen
            val baseFreq = 2800.0 + c * 600.0
            for (i in 0 until chirpLen) {
                val idx = start + i
                if (idx >= numSamples) break
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / chirpLen

                // Frequency sweep up
                val freq = baseFreq + progress * 1200.0
                val phase = 2.0 * PI * freq * t + c * 10.0

                // Envelope: quick attack, sustain, quick release
                val env = when {
                    progress < 0.08 -> progress / 0.08
                    progress > 0.85 -> (1.0 - progress) / 0.15
                    else -> 1.0
                }

                // Add slight vibrato
                val vibrato = 1.0 + 0.03 * sin(2.0 * PI * 15.0 * t)
                val sample = env * 0.6 * sin(phase * vibrato)
                samples[idx] = (sample * Short.MAX_VALUE).roundToInt().toShort()
            }
        }
        return samples
    }

    private fun generateCrowCaw(): ShortArray {
        val durationSec = 0.5
        val numSamples = (sampleRate * durationSec).toInt()
        val samples = ShortArray(numSamples)

        // Two harsh caws
        val cawCount = 2
        val cawGap = (sampleRate * 0.06).toInt()
        val cawLen = (numSamples - cawGap * (cawCount - 1)) / cawCount

        for (c in 0 until cawCount) {
            val start = c * (cawLen + cawGap)
            val baseFreq = 480.0 - c * 40.0
            for (i in 0 until cawLen) {
                val idx = start + i
                if (idx >= numSamples) break
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / cawLen

                // Descending frequency
                val freq = baseFreq - progress * 180.0
                val phase = 2.0 * PI * freq * t

                // Envelope
                val env = when {
                    progress < 0.05 -> progress / 0.05
                    progress > 0.7 -> (1.0 - progress) / 0.3
                    else -> 1.0
                }

                // Add harmonics for harshness
                val fundamental = sin(phase)
                val harmonic2 = 0.4 * sin(phase * 2.0)
                val harmonic3 = 0.2 * sin(phase * 3.0 + 0.5)
                // Noise-like roughness
                val roughness = 0.15 * sin(phase * 7.3 + c * 2.0)

                val sample = env * 0.5 * (fundamental + harmonic2 + harmonic3 + roughness)
                samples[idx] = (sample * Short.MAX_VALUE).roundToInt().toShort()
            }
        }
        return samples
    }
}
