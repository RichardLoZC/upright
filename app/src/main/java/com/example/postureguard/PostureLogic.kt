package com.example.postureguard

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

data class Landmark3D(
    val x: Double,
    val y: Double,
    val z: Double,
    val visibility: Float
)

enum class PostureState {
    GOOD,
    BAD_TILT_LEFT,
    BAD_TILT_RIGHT,
    BAD_SLOUCH,
    BAD_FORWARD_HEAD,
    BAD_HUNCHBACK,
    NO_PERSON
}

object PostureLogic {

    // --- 2D thresholds (front camera, normalized coords 0-1) ---
    private const val TILT_THRESHOLD = 0.05f
    private const val SHOULDER_LEVEL_THRESHOLD = 0.04f

    // --- 3D thresholds (World Landmarks, meters) ---
    // CVA: angle between ear-C7 vector and horizontal in sagittal plane
    // Clinical threshold: <50° = forward head. We use 48° to be slightly conservative.
    private const val CVA_THRESHOLD_DEG = 48.0

    // Trunk Inclination: angle between spine vector (hip→shoulder) and vertical
    // >20° forward lean = hunchback/slouch
    private const val TRUNK_INCLINATION_THRESHOLD_DEG = 20.0

    // Minimum landmark visibility to trust a measurement
    private const val MIN_VISIBILITY = 0.5f

    /**
     * Analyze posture from 2D normalized landmarks (front camera).
     * Detects: head tilt, shoulder asymmetry.
     */
    fun analyze2d(
        landmarks: List<Landmark3D>
    ): PostureState? {
        if (landmarks.size < 33) return null

        val leftEar = landmarks[7]
        val rightEar = landmarks[8]
        val leftShoulder = landmarks[11]
        val rightShoulder = landmarks[12]

        // Check visibility
        if (leftEar.visibility < MIN_VISIBILITY || rightEar.visibility < MIN_VISIBILITY) return null
        if (leftShoulder.visibility < MIN_VISIBILITY || rightShoulder.visibility < MIN_VISIBILITY) return null

        // Head tilt (ear Y difference)
        val earDiffY = leftEar.y - rightEar.y
        if (earDiffY > TILT_THRESHOLD) return PostureState.BAD_TILT_LEFT
        if (earDiffY < -TILT_THRESHOLD) return PostureState.BAD_TILT_RIGHT

        // Shoulder level
        val shoulderDiffY = abs(leftShoulder.y - rightShoulder.y)
        if (shoulderDiffY > SHOULDER_LEVEL_THRESHOLD) return PostureState.BAD_SLOUCH

        return PostureState.GOOD
    }

    /**
     * Analyze posture from 3D World Landmarks.
     * Uses clinical biomechanical metrics:
     * - CVA (Craniovertebral Angle) for forward head detection
     * - Trunk Inclination Angle for hunchback detection
     *
     * World Landmarks coordinate system:
     * - Origin: midpoint of hips
     * - X: lateral width (positive = right)
     * - Y: vertical (negative = up)
     * - Z: depth (negative = toward camera)
     */
    data class Analysis3dResult(
        val state: PostureState,
        val cva: Double?,
        val trunkAngle: Double?
    )

    fun analyze3d(
        landmarks: List<Landmark3D>
    ): Analysis3dResult? {
        if (landmarks.size < 33) return null

        val leftShoulder = landmarks[11]
        val rightShoulder = landmarks[12]
        val leftHip = landmarks[23]
        val rightHip = landmarks[24]

        if (leftShoulder.visibility < MIN_VISIBILITY || rightShoulder.visibility < MIN_VISIBILITY) return null
        if (leftHip.visibility < MIN_VISIBILITY || rightHip.visibility < MIN_VISIBILITY) return null

        val c7 = midpoint(leftShoulder, rightShoulder)
        val pelvis = midpoint(leftHip, rightHip)

        val spineVector = Vec3(c7.x - pelvis.x, c7.y - pelvis.y, c7.z - pelvis.z)
        val vertical = Vec3(0.0, -1.0, 0.0)
        val trunkAngle = angleBetween(spineVector, vertical)

        val leftEar = landmarks[7]
        val rightEar = landmarks[8]
        val ear = if (leftEar.visibility >= rightEar.visibility) leftEar else rightEar

        val cva = if (ear.visibility >= MIN_VISIBILITY) {
            val earRelY = ear.y - c7.y
            val earRelZ = ear.z - c7.z
            Math.toDegrees(atan2(-earRelY, -earRelZ))
        } else null

        val state = when {
            trunkAngle > TRUNK_INCLINATION_THRESHOLD_DEG -> PostureState.BAD_HUNCHBACK
            cva != null && cva < CVA_THRESHOLD_DEG -> PostureState.BAD_FORWARD_HEAD
            else -> PostureState.GOOD
        }

        return Analysis3dResult(state, cva, trunkAngle)
    }

    fun analyzeWithDiagnosis(
        landmarks2d: List<Landmark3D>,
        landmarks3d: List<Landmark3D>?,
        fps: Double
    ): PostureDiagnosis {
        val result3d = landmarks3d?.let { analyze3d(it) }

        // 3D bad → return immediately
        if (result3d != null && result3d.state != PostureState.GOOD) {
            return PostureDiagnosis(
                state = result3d.state,
                cva = result3d.cva,
                trunkAngle = result3d.trunkAngle,
                hasWorldLandmarks = true,
                fps = fps
            )
        }

        val result2d = analyze2d(landmarks2d)
        if (result2d != null && result2d != PostureState.GOOD) {
            return PostureDiagnosis(
                state = result2d,
                cva = result3d?.cva,
                trunkAngle = result3d?.trunkAngle,
                hasWorldLandmarks = result3d != null,
                fps = fps
            )
        }

        val good = result3d?.state == PostureState.GOOD || result2d == PostureState.GOOD
        return PostureDiagnosis(
            state = if (good) PostureState.GOOD else PostureState.NO_PERSON,
            cva = result3d?.cva,
            trunkAngle = result3d?.trunkAngle,
            hasWorldLandmarks = result3d != null,
            fps = fps
        )
    }

    private fun midpoint(a: Landmark3D, b: Landmark3D): Landmark3D {
        return Landmark3D(
            (a.x + b.x) / 2.0,
            (a.y + b.y) / 2.0,
            (a.z + b.z) / 2.0,
            minOf(a.visibility, b.visibility)
        )
    }

    private data class Vec3(val x: Double, val y: Double, val z: Double) {
        fun dot(other: Vec3): Double = x * other.x + y * other.y + z * other.z
        fun magnitude(): Double = sqrt(x * x + y * y + z * z)
    }

    private fun angleBetween(a: Vec3, b: Vec3): Double {
        val magA = a.magnitude()
        val magB = b.magnitude()
        if (magA < 1e-6 || magB < 1e-6) return 0.0
        val cosAngle = (a.dot(b) / (magA * magB)).coerceIn(-1.0, 1.0)
        return Math.toDegrees(kotlin.math.acos(cosAngle))
    }
}

class PostureStateMachine(
    private val badConfirmFrames: Int = 3,
    private val goodConfirmFrames: Int = 5
) {
    private var currentState = PostureState.NO_PERSON
    private var pendingState: PostureState? = null
    private var consecutiveFrames = 0

    fun update(rawState: PostureState): PostureState {
        if (rawState == pendingState) {
            consecutiveFrames++
        } else {
            pendingState = rawState
            consecutiveFrames = 1
        }

        val isCurrentlyOk = currentState == PostureState.GOOD || currentState == PostureState.NO_PERSON
        val threshold = when {
            isCurrentlyOk && rawState != PostureState.GOOD && rawState != PostureState.NO_PERSON -> badConfirmFrames
            !isCurrentlyOk && (rawState == PostureState.GOOD || rawState == PostureState.NO_PERSON) -> goodConfirmFrames
            else -> 1
        }

        if (consecutiveFrames >= threshold) {
            currentState = rawState
            pendingState = rawState
            consecutiveFrames = 0
        }

        return currentState
    }
}
