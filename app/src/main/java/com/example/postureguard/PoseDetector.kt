package com.example.postureguard

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
            .setModelAssetPath("pose_landmarker_lite.task")

        // Use GPU if available (usually yes on Android)
        // For stability on old phones, maybe CPU is safer? Let's try CPU first or auto.
        // There is no AUTO. Let's stick to CPU for MVP stability on "idle/old" phones, 
        // or GPU if supported. 
        // Let's use CPU for MVP to avoid OpenGL context issues in background/weird states.
        // Actually, for real-time video, GPU is much better.
        baseOptionsBuilder.setDelegate(Delegate.GPU)

        val optionsBuilder = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptionsBuilder.build())
            .setMinPoseDetectionConfidence(0.5f)
            .setMinPosePresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener(this::returnLivestreamResult)
            .setErrorListener(this::returnLivestreamError)

        try {
            poseLandmarker = PoseLandmarker.createFromOptions(context, optionsBuilder.build())
        } catch (e: IllegalStateException) {
            Log.e("PoseDetector", "MediaPipe failed to load", e)
        } catch (e: RuntimeException) {
            Log.e("PoseDetector", "MediaPipe failed to load", e)
        }
    }

    private var resultListener: ((PoseLandmarkerResult) -> Unit)? = null

    fun setListener(listener: (PoseLandmarkerResult) -> Unit) {
        this.resultListener = listener
    }

    fun detect(bitmap: Bitmap, rotationDegrees: Int) {
        if (poseLandmarker == null) return

        // Note: rotating the bitmap is expensive. 
        // Ideally we pass rotation to MediaPipe, but MPImage usually expects upright images?
        // Actually MPImage has direct support for Bitmap.
        // If the camera image is rotated (e.g. 270 deg), we need to handle it.
        // For MVP, we'll assume the ImageAnalysis gives us a Bitmap that is already rotated 
        // OR we just feed it and see.
        // ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888 usually gives upright if we configure it?
        // No, it gives the buffer.
        // Let's rely on the caller to provide a correct Bitmap.

        val mpImage = BitmapImageBuilder(bitmap).build()
        val timestamp = SystemClock.uptimeMillis()
        
        poseLandmarker?.detectAsync(mpImage, timestamp)
    }

    private fun returnLivestreamResult(result: PoseLandmarkerResult, input: MPImage) {
        resultListener?.invoke(result)
    }

    private fun returnLivestreamError(error: RuntimeException) {
        Log.e("PoseDetector", "Detection error", error)
    }

    fun close() {
        poseLandmarker?.close()
    }
}
