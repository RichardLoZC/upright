package com.example.postureguard

import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.abs
import kotlin.math.atan2

enum class PostureState {
    GOOD,
    BAD_TILT_LEFT,
    BAD_TILT_RIGHT,
    BAD_SLOUCH, // Shoulders uneven or too low
    NO_PERSON
}

object PostureLogic {
    
    // Thresholds (normalized coordinates 0.0 - 1.0)
    private const val TILT_THRESHOLD = 0.05f // ~5% diff in Y
    private const val SHOULDER_LEVEL_THRESHOLD = 0.04f
    
    fun analyze(result: PoseLandmarkerResult): PostureState {
        if (result.landmarks().isEmpty()) return PostureState.NO_PERSON
        
        val landmarks = result.landmarks()[0]
        if (landmarks.size < 33) return PostureState.NO_PERSON

        val nose = landmarks[0]
        val leftEar = landmarks[7]
        val rightEar = landmarks[8]
        val leftShoulder = landmarks[11]
        val rightShoulder = landmarks[12]

        // 1. Head Tilt (Ear level)
        // If left ear Y is significantly different from right ear Y
        val earDiffY = leftEar.y() - rightEar.y()
        if (earDiffY > TILT_THRESHOLD) return PostureState.BAD_TILT_LEFT // Left ear lower (image coordinates y increases downwards? wait. y=0 is top. so larger y is lower.)
        // Wait: y=0 top. leftEar.y > rightEar.y means left ear is LOWER (down). So head tilted LEFT.
        if (earDiffY < -TILT_THRESHOLD) return PostureState.BAD_TILT_RIGHT

        // 2. Shoulder Level
        val shoulderDiffY = leftShoulder.y() - rightShoulder.y()
        if (abs(shoulderDiffY) > SHOULDER_LEVEL_THRESHOLD) return PostureState.BAD_SLOUCH

        return PostureState.GOOD
    }
}
