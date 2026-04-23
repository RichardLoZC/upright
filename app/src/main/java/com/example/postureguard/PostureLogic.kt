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
    BAD_TILT,
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
    private const val MIN_VISIBILITY = 0.3f

    // Deviation thresholds for calibrated analysis
    private const val TILT_DEVIATION = 0.03
    private const val SHOULDER_DEVIATION = 0.03
    private const val CVA_DEVIATION_DEG = 10.0
    private const val TRUNK_DEVIATION_DEG = 10.0

    data class CalibrationProfile(
        val earDiffY: Double,
        val shoulderDiffY: Double,
        val cva: Double?,
        val trunkAngle: Double?,
        val boneLengths: BoneLengths? = null,
        val rotationMatrix: RotationMatrix? = null
    )

    fun calibrateFromSamples(
        samples: List<Pair<List<Landmark3D>, List<Landmark3D>?>>
    ): CalibrationProfile? {
        val earDiffs = mutableListOf<Double>()
        val shoulderDiffs = mutableListOf<Double>()
        val cvaValues = mutableListOf<Double>()
        val trunkValues = mutableListOf<Double>()
        val boneSamples = mutableListOf<BoneLengths>()
        val rotationSamples = mutableListOf<RotationMatrix>()

        for ((lm2d, lm3d) in samples) {
            if (lm2d.size < 33) continue
            val le = lm2d[7]; val re = lm2d[8]
            val ls = lm2d[11]; val rs = lm2d[12]
            if (le.visibility < MIN_VISIBILITY || re.visibility < MIN_VISIBILITY) continue
            if (ls.visibility < MIN_VISIBILITY || rs.visibility < MIN_VISIBILITY) continue
            earDiffs.add(le.y - re.y)
            shoulderDiffs.add(abs(ls.y - rs.y))

            if (lm3d != null && lm3d.size >= 33) {
                // Calibrate bone lengths
                BoneLengthOptimizer.calibrate(lm3d)?.let { boneSamples.add(it) }

                // Calibrate rotation matrix from spine vector
                val ls3d = lm3d[11]; val rs3d = lm3d[12]
                val lh3d = lm3d[23]; val rh3d = lm3d[24]
                if (ls3d.visibility >= MIN_VISIBILITY && rs3d.visibility >= MIN_VISIBILITY &&
                    lh3d.visibility >= MIN_VISIBILITY && rh3d.visibility >= MIN_VISIBILITY) {
                    val c7 = midpoint(ls3d, rs3d)
                    val pelvis = midpoint(lh3d, rh3d)
                    AffineNormalizer.fromSpineVector(pelvis, c7)?.let { rotationSamples.add(it) }
                }

                val r3d = analyze3dCalibrated(lm3d, null)
                r3d?.cva?.let { cvaValues.add(it) }
                r3d?.trunkAngle?.let { trunkValues.add(it) }
            }
        }

        if (earDiffs.size < 3) return null
        return CalibrationProfile(
            earDiffY = earDiffs.average(),
            shoulderDiffY = shoulderDiffs.average(),
            cva = cvaValues.takeIf { it.size >= 3 }?.average(),
            trunkAngle = trunkValues.takeIf { it.size >= 3 }?.average(),
            boneLengths = BoneLengthOptimizer.averageCalibration(boneSamples),
            rotationMatrix = AffineNormalizer.averageRotation(rotationSamples)
        )
    }

    data class Analysis3dResult(
        val state: PostureState,
        val cva: Double?,
        val trunkAngle: Double?
    )

    fun analyzeWithDiagnosis(
        landmarks2d: List<Landmark3D>,
        landmarks3d: List<Landmark3D>?,
        fps: Double,
        calibration: CalibrationProfile? = null,
        sensitivityMultiplier: Double = 1.0
    ): PostureDiagnosis {
        val has3d = landmarks3d != null

        // Apply spatial refinement to 3D landmarks
        val refined3d = landmarks3d?.let { raw3d ->
            var refined = raw3d
            // Step 1: Affine normalization (camera tilt compensation)
            calibration?.rotationMatrix?.let { rot ->
                refined = rot.applyAll(refined)
            }
            // Step 2: Bone-length constancy correction
            calibration?.boneLengths?.let { bones ->
                refined = BoneLengthOptimizer.refine(refined, bones)
            }
            refined
        }

        val result3d = refined3d?.let { analyze3dCalibrated(it, calibration, sensitivityMultiplier) }

        // 3D bad → return immediately
        if (result3d != null && result3d.state != PostureState.GOOD) {
            return PostureDiagnosis(
                state = result3d.state,
                cva = result3d.cva,
                trunkAngle = result3d.trunkAngle,
                hasWorldLandmarks = has3d,
                fps = fps
            )
        }

        val result2d = analyze2dCalibrated(landmarks2d, calibration, sensitivityMultiplier)
        if (result2d != null && result2d != PostureState.GOOD) {
            return PostureDiagnosis(
                state = result2d,
                cva = result3d?.cva,
                trunkAngle = result3d?.trunkAngle,
                hasWorldLandmarks = has3d,
                fps = fps
            )
        }

        val good = result3d?.state == PostureState.GOOD || result2d == PostureState.GOOD
        return PostureDiagnosis(
            state = if (good) PostureState.GOOD else PostureState.NO_PERSON,
            cva = result3d?.cva,
            trunkAngle = result3d?.trunkAngle,
            hasWorldLandmarks = has3d,
            fps = fps
        )
    }

    private fun analyze2dCalibrated(
        landmarks: List<Landmark3D>,
        calibration: CalibrationProfile?,
        sensitivityMultiplier: Double = 1.0
    ): PostureState? {
        if (landmarks.size < 33) return null
        val leftEar = landmarks[7]; val rightEar = landmarks[8]
        val leftShoulder = landmarks[11]; val rightShoulder = landmarks[12]
        if (leftEar.visibility < MIN_VISIBILITY || rightEar.visibility < MIN_VISIBILITY) return null
        if (leftShoulder.visibility < MIN_VISIBILITY || rightShoulder.visibility < MIN_VISIBILITY) return null

        val earDiffY = leftEar.y - rightEar.y
        val shoulderDiffY = abs(leftShoulder.y - rightShoulder.y)

        if (calibration != null) {
            val earDeviation = earDiffY - calibration.earDiffY
            if (abs(earDeviation) > TILT_DEVIATION * sensitivityMultiplier) return PostureState.BAD_TILT
            val shoulderDeviation = shoulderDiffY - calibration.shoulderDiffY
            if (shoulderDeviation > SHOULDER_DEVIATION * sensitivityMultiplier) return PostureState.BAD_SLOUCH
        } else {
            if (abs(earDiffY) > TILT_THRESHOLD * sensitivityMultiplier) return PostureState.BAD_TILT
            if (shoulderDiffY > SHOULDER_LEVEL_THRESHOLD * sensitivityMultiplier) return PostureState.BAD_SLOUCH
        }
        return PostureState.GOOD
    }

    private fun analyze3dCalibrated(
        landmarks: List<Landmark3D>,
        calibration: CalibrationProfile?,
        sensitivityMultiplier: Double = 1.0
    ): Analysis3dResult? {
        if (landmarks.size < 33) return null
        val leftShoulder = landmarks[11]; val rightShoulder = landmarks[12]
        val leftHip = landmarks[23]; val rightHip = landmarks[24]
        if (leftShoulder.visibility < MIN_VISIBILITY || rightShoulder.visibility < MIN_VISIBILITY) return null
        if (leftHip.visibility < MIN_VISIBILITY || rightHip.visibility < MIN_VISIBILITY) return null

        val c7 = midpoint(leftShoulder, rightShoulder)
        val pelvis = midpoint(leftHip, rightHip)
        val spineVector = Vec3(c7.x - pelvis.x, c7.y - pelvis.y, c7.z - pelvis.z)
        val vertical = Vec3(0.0, -1.0, 0.0)
        val trunkAngle = angleBetween(spineVector, vertical)

        val leftEar = landmarks[7]; val rightEar = landmarks[8]
        val cva = if (leftEar.visibility >= MIN_VISIBILITY || rightEar.visibility >= MIN_VISIBILITY) {
            val ear = midpointVisible(leftEar, rightEar)
            val earRelY = ear.y - c7.y
            val earRelZ = ear.z - c7.z
            // CVA in sagittal plane (YZ), immune to lateral bending
            Math.toDegrees(atan2(-earRelY, -earRelZ))
        } else null

        val state = if (calibration != null) {
            when {
                calibration.trunkAngle != null && trunkAngle > calibration.trunkAngle + TRUNK_DEVIATION_DEG * sensitivityMultiplier -> PostureState.BAD_HUNCHBACK
                calibration.cva != null && cva != null && cva < calibration.cva - CVA_DEVIATION_DEG * sensitivityMultiplier -> PostureState.BAD_FORWARD_HEAD
                else -> PostureState.GOOD
            }
        } else {
            when {
                trunkAngle > TRUNK_INCLINATION_THRESHOLD_DEG * sensitivityMultiplier -> PostureState.BAD_HUNCHBACK
                cva != null && cva < CVA_THRESHOLD_DEG * sensitivityMultiplier -> PostureState.BAD_FORWARD_HEAD
                else -> PostureState.GOOD
            }
        }
        return Analysis3dResult(state, cva, trunkAngle)
    }

    private fun midpoint(a: Landmark3D, b: Landmark3D): Landmark3D {
        return Landmark3D(
            (a.x + b.x) / 2.0,
            (a.y + b.y) / 2.0,
            (a.z + b.z) / 2.0,
            minOf(a.visibility, b.visibility)
        )
    }

    // Weighted midpoint: use only visible landmarks, fall back to the visible one
    private fun midpointVisible(a: Landmark3D, b: Landmark3D): Landmark3D {
        val aOk = a.visibility >= MIN_VISIBILITY
        val bOk = b.visibility >= MIN_VISIBILITY
        return when {
            aOk && bOk -> midpoint(a, b)
            aOk -> a
            bOk -> b
            else -> midpoint(a, b)
        }
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
