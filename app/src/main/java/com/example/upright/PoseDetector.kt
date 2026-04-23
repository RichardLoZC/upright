package com.example.upright

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class PoseDetector(context: Context) {
    private var poseLandmarker: PoseLandmarker? = null

    init {
        setupPoseLandmarker(context)
    }

    private fun setupPoseLandmarker(context: Context) {
        val baseOptionsBuilder = BaseOptions.builder()
            .setModelAssetPath("pose_landmarker_full.task")

        val optionsBuilder = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setMinPoseDetectionConfidence(0.5f)
            .setMinPosePresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener(this::returnLivestreamResult)
            .setErrorListener(this::returnLivestreamError)

        try {
            baseOptionsBuilder.setDelegate(Delegate.GPU)
            poseLandmarker = PoseLandmarker.createFromOptions(context, optionsBuilder.setBaseOptions(baseOptionsBuilder.build()).build())
        } catch (e: Exception) {
            Log.w("PoseDetector", "GPU delegate failed, falling back to CPU", e)
            try {
                baseOptionsBuilder.setDelegate(Delegate.CPU)
                poseLandmarker = PoseLandmarker.createFromOptions(context, optionsBuilder.setBaseOptions(baseOptionsBuilder.build()).build())
            } catch (e2: Exception) {
                Log.e("PoseDetector", "CPU delegate also failed", e2)
            }
        }
    }

    private var resultListener: ((PoseDetection) -> Unit)? = null

    fun setListener(listener: (PoseDetection) -> Unit) {
        this.resultListener = listener
    }

    fun detect(bitmap: Bitmap, rotationDegrees: Int) {
        if (poseLandmarker == null) return

        val mpImage = BitmapImageBuilder(bitmap).build()
        val timestamp = SystemClock.uptimeMillis()

        poseLandmarker?.detectAsync(mpImage, timestamp)
    }

    private fun returnLivestreamResult(result: PoseLandmarkerResult, input: MPImage) {
        if (result.landmarks().isEmpty()) {
            resultListener?.invoke(PoseDetection(emptyList(), emptyList()))
            return
        }

        val normalized = result.landmarks()[0].map { lm ->
            Landmark3D(lm.x().toDouble(), lm.y().toDouble(), lm.z().toDouble(), lm.visibility().orElse(0.0f))
        }

        val worldLandmarks = result.worldLandmarks()
        val world = if (worldLandmarks.isNotEmpty()) {
            worldLandmarks[0].map { lm ->
                Landmark3D(lm.x().toDouble(), lm.y().toDouble(), lm.z().toDouble(), lm.visibility().orElse(0.0f))
            }
        } else {
            emptyList()
        }

        resultListener?.invoke(PoseDetection(normalized, world))
    }

    private fun returnLivestreamError(error: RuntimeException) {
        Log.e("PoseDetector", "Detection error", error)
    }

    fun close() {
        poseLandmarker?.close()
    }
}

data class PoseDetection(
    val landmarks2d: List<Landmark3D>,
    val landmarks3d: List<Landmark3D>
)
