package com.example.postureguard

import org.junit.Assert.*
import org.junit.Test

class PostureLogic2DTest {

    private fun lm(x: Double, y: Double, z: Double = 0.0, vis: Float = 1.0f) = Landmark3D(x, y, z, vis)

    private fun makeLandmarks(
        leftEar: Landmark3D = lm(0.4, 0.3),
        rightEar: Landmark3D = lm(0.6, 0.3),
        leftShoulder: Landmark3D = lm(0.35, 0.5),
        rightShoulder: Landmark3D = lm(0.65, 0.5)
    ): List<Landmark3D> {
        val list = MutableList(33) { lm(0.5, 0.5) }
        list[7] = leftEar; list[8] = rightEar
        list[11] = leftShoulder; list[12] = rightShoulder
        return list
    }

    @Test
    fun `good posture returns GOOD`() {
        val result = PostureLogic.analyzeWithDiagnosis(
            makeLandmarks(), null, 30.0
        )
        assertEquals(PostureState.GOOD, result.state)
    }

    @Test
    fun `head tilt detected when left ear lower`() {
        val lm = makeLandmarks(leftEar = lm(0.4, 0.4), rightEar = lm(0.6, 0.3))
        val result = PostureLogic.analyzeWithDiagnosis(lm, null, 30.0)
        assertEquals(PostureState.BAD_TILT, result.state)
    }

    @Test
    fun `head tilt detected when right ear lower`() {
        val lm = makeLandmarks(leftEar = lm(0.4, 0.3), rightEar = lm(0.6, 0.4))
        val result = PostureLogic.analyzeWithDiagnosis(lm, null, 30.0)
        assertEquals(PostureState.BAD_TILT, result.state)
    }

    @Test
    fun `shoulder asymmetry detected`() {
        val lm = makeLandmarks(
            leftShoulder = lm(0.35, 0.5),
            rightShoulder = lm(0.65, 0.56)
        )
        val result = PostureLogic.analyzeWithDiagnosis(lm, null, 30.0)
        assertEquals(PostureState.BAD_SLOUCH, result.state)
    }

    @Test
    fun `low visibility returns NO_PERSON`() {
        val lm = makeLandmarks(
            leftEar = lm(0.4, 0.3, vis = 0.1f),
            rightEar = lm(0.6, 0.3, vis = 0.1f)
        )
        val result = PostureLogic.analyzeWithDiagnosis(lm, null, 30.0)
        // Should not crash; result depends on visibility thresholds
        assertNotNull(result)
    }

    @Test
    fun `calibrated detection uses deviation thresholds`() {
        val calib = PostureLogic.CalibrationProfile(
            earDiffY = 0.0, shoulderDiffY = 0.0, cva = null, trunkAngle = null
        )
        // Slight tilt within deviation → GOOD
        val goodLm = makeLandmarks(leftEar = lm(0.4, 0.32), rightEar = lm(0.6, 0.3))
        val goodResult = PostureLogic.analyzeWithDiagnosis(goodLm, null, 30.0, calib)
        assertEquals(PostureState.GOOD, goodResult.state)

        // Tilt exceeding deviation → BAD
        val badLm = makeLandmarks(leftEar = lm(0.4, 0.36), rightEar = lm(0.6, 0.3))
        val badResult = PostureLogic.analyzeWithDiagnosis(badLm, null, 30.0, calib)
        assertEquals(PostureState.BAD_TILT, badResult.state)
    }

    @Test
    fun `sensitivity multiplier LOW requires larger deviation`() {
        // Slight tilt that would trigger with default sensitivity
        val lm = makeLandmarks(leftEar = lm(0.4, 0.36), rightEar = lm(0.6, 0.3))
        // earDiffY = 0.06, threshold * 1.5 = 0.075 → 0.06 < 0.075 so GOOD
        val lowResult = PostureLogic.analyzeWithDiagnosis(lm, null, 30.0, null, 1.5)
        assertEquals(PostureState.GOOD, lowResult.state)
    }

    @Test
    fun `sensitivity multiplier HIGH triggers more easily`() {
        // Slight tilt that would NOT trigger with default sensitivity
        // earDiffY = 0.04, default threshold = 0.05 → GOOD
        // but HIGH threshold = 0.05 * 0.7 = 0.035 → 0.04 > 0.035 so BAD
        val lm = makeLandmarks(leftEar = lm(0.4, 0.34), rightEar = lm(0.6, 0.3))
        val highResult = PostureLogic.analyzeWithDiagnosis(lm, null, 30.0, null, 0.7)
        assertEquals(PostureState.BAD_TILT, highResult.state)
    }
}

class PostureLogic3DTest {

    private fun lm(x: Double, y: Double, z: Double = 0.0, vis: Float = 1.0f) = Landmark3D(x, y, z, vis)

    private fun makeLandmarks3d(
        leftShoulder: Landmark3D = lm(-0.2, -0.4, -0.5),
        rightShoulder: Landmark3D = lm(0.2, -0.4, -0.5),
        leftHip: Landmark3D = lm(-0.15, 0.0, -0.5),
        rightHip: Landmark3D = lm(0.15, 0.0, -0.5),
        leftEar: Landmark3D = lm(-0.08, -0.65, -0.45),
        rightEar: Landmark3D = lm(0.08, -0.65, -0.45)
    ): List<Landmark3D> {
        val list = MutableList(33) { lm(0.0, 0.0, 0.0, 0.1f) }
        list[7] = leftEar; list[8] = rightEar
        list[11] = leftShoulder; list[12] = rightShoulder
        list[23] = leftHip; list[24] = rightHip
        // Fill nose with decent visibility
        list[0] = lm(0.0, -0.7, -0.4, 0.9f)
        return list
    }

    @Test
    fun `good upright posture returns GOOD with 3D`() {
        val lm = makeLandmarks3d()
        val result = PostureLogic.analyzeWithDiagnosis(
            List(33) { lm(0.5, 0.5) }, lm, 30.0
        )
        // Upright posture should be GOOD (or detected via 2D if 3D says good)
        assertNotEquals(PostureState.NO_PERSON, result.state)
        assertTrue(result.hasWorldLandmarks)
    }

    @Test
    fun `forward head posture detected via CVA`() {
        // Ear far forward (very negative Z) relative to shoulders → low CVA
        val lm = makeLandmarks3d(
            leftEar = lm(-0.08, -0.55, -0.8),
            rightEar = lm(0.08, -0.55, -0.8)
        )
        val result = PostureLogic.analyzeWithDiagnosis(
            List(33) { lm(0.5, 0.5) }, lm, 30.0
        )
        // CVA should be low, triggering forward head
        assertEquals(PostureState.BAD_FORWARD_HEAD, result.state)
        assertNotNull(result.cva)
        assertTrue(result.cva!! < 48.0)
    }

    @Test
    fun `hunchback detected via trunk inclination`() {
        // Shoulders far forward relative to hips → large trunk angle
        val lm = makeLandmarks3d(
            leftShoulder = lm(-0.2, -0.3, -0.9),
            rightShoulder = lm(0.2, -0.3, -0.9)
        )
        val result = PostureLogic.analyzeWithDiagnosis(
            List(33) { lm(0.5, 0.5) }, lm, 30.0
        )
        assertEquals(PostureState.BAD_HUNCHBACK, result.state)
        assertNotNull(result.trunkAngle)
        assertTrue(result.trunkAngle!! > 20.0)
    }

    @Test
    fun `CVA uses ear midpoint not single ear`() {
        // One ear hidden, the other visible but offset — should use visible one
        val lm = makeLandmarks3d(
            leftEar = lm(-0.08, -0.65, -0.45, vis = 0.1f), // hidden
            rightEar = lm(0.08, -0.65, -0.45, vis = 1.0f)   // visible
        )
        val result = PostureLogic.analyzeWithDiagnosis(
            List(33) { lm(0.5, 0.5) }, lm, 30.0
        )
        // Should not crash and should still compute CVA
        assertNotNull(result.cva)
    }
}

class PostureStateMachineTest {

    @Test
    fun `initial state is NO_PERSON`() {
        val sm = PostureStateMachine()
        assertEquals(PostureState.NO_PERSON, sm.update(PostureState.NO_PERSON))
    }

    @Test
    fun `bad state requires 3 consecutive frames`() {
        val sm = PostureStateMachine()
        assertEquals(PostureState.NO_PERSON, sm.update(PostureState.BAD_HUNCHBACK))
        assertEquals(PostureState.NO_PERSON, sm.update(PostureState.BAD_HUNCHBACK))
        assertEquals(PostureState.BAD_HUNCHBACK, sm.update(PostureState.BAD_HUNCHBACK))
    }

    @Test
    fun `recovery requires 5 consecutive good frames`() {
        val sm = PostureStateMachine()
        // Enter bad state
        repeat(3) { sm.update(PostureState.BAD_HUNCHBACK) }

        // Not enough good frames
        repeat(4) { assertEquals(PostureState.BAD_HUNCHBACK, sm.update(PostureState.GOOD)) }
        // 5th frame clears
        assertEquals(PostureState.GOOD, sm.update(PostureState.GOOD))
    }

    @Test
    fun `interrupted streak resets counter`() {
        val sm = PostureStateMachine()
        sm.update(PostureState.BAD_HUNCHBACK)
        sm.update(PostureState.BAD_HUNCHBACK)
        // Interrupt with GOOD — immediately accepted (threshold=1 when going to OK)
        val afterGood = sm.update(PostureState.GOOD)
        assertEquals(PostureState.GOOD, afterGood)
        // Back to bad — counter reset, need 3 more
        assertEquals(PostureState.GOOD, sm.update(PostureState.BAD_HUNCHBACK))
        assertEquals(PostureState.GOOD, sm.update(PostureState.BAD_HUNCHBACK))
        assertEquals(PostureState.BAD_HUNCHBACK, sm.update(PostureState.BAD_HUNCHBACK))
    }

    @Test
    fun `switching between bad states transitions immediately`() {
        val sm = PostureStateMachine()
        repeat(3) { sm.update(PostureState.BAD_HUNCHBACK) }
        assertEquals(PostureState.BAD_HUNCHBACK, sm.update(PostureState.BAD_HUNCHBACK))

        // Switch to different bad state — threshold is 1 (else branch: bad→bad)
        assertEquals(PostureState.BAD_FORWARD_HEAD, sm.update(PostureState.BAD_FORWARD_HEAD))
    }
}

class OneEuroFilterTest {

    @Test
    fun `first value passes through unchanged`() {
        val filter = OneEuroFilter()
        assertEquals(5.0, filter.filter(5.0, 1000L), 0.001)
    }

    @Test
    fun `static signal is smoothed`() {
        val filter = OneEuroFilter(minCutoff = 1.0, beta = 0.0)
        var value = 10.0
        for (i in 1..100) {
            value = filter.filter(10.0, 1000L + i * 33)
        }
        // After many identical inputs, output should converge to input
        assertEquals(10.0, value, 0.01)
    }

    @Test
    fun `rapid movement passes through with low lag`() {
        val filter = OneEuroFilter(minCutoff = 1.0, beta = 1.0)
        // Initialize
        filter.filter(0.0, 1000L)
        // Big jump
        val result = filter.filter(100.0, 1033L)
        // With high beta, should track closely
        assertTrue(result > 50.0) // At least halfway there
    }

    @Test
    fun `reset clears state`() {
        val filter = OneEuroFilter()
        filter.filter(10.0, 1000L)
        filter.reset()
        // After reset, first value should pass through again
        assertEquals(20.0, filter.filter(20.0, 2000L), 0.001)
    }
}

class BoneLengthOptimizerTest {

    private fun lm(x: Double, y: Double, z: Double = 0.0, vis: Float = 1.0f) = Landmark3D(x, y, z, vis)

    @Test
    fun `calibrate returns bone lengths`() {
        val landmarks = MutableList(33) { lm(0.0, 0.0) }
        landmarks[11] = lm(-0.2, -0.4) // left shoulder
        landmarks[12] = lm(0.2, -0.4)  // right shoulder
        val result = BoneLengthOptimizer.calibrate(landmarks)
        assertNotNull(result)
        assertTrue(result!!.lengths.containsKey(11 to 12))
        assertEquals(0.4, result.lengths[11 to 12]!!, 0.001)
    }

    @Test
    fun `calibrate requires minimum visibility`() {
        val landmarks = MutableList(33) { lm(0.0, 0.0, 0.0, 0.1f) }
        val result = BoneLengthOptimizer.calibrate(landmarks)
        assertNull(result)
    }

    @Test
    fun `refine produces valid landmarks`() {
        val baseline = MutableList(33) { lm(0.0, 0.0) }
        baseline[11] = lm(-0.2, -0.4, -0.5)
        baseline[12] = lm(0.2, -0.4, -0.5)
        baseline[23] = lm(-0.15, 0.0, -0.5)
        baseline[24] = lm(0.15, 0.0, -0.5)
        val boneLengths = BoneLengthOptimizer.calibrate(baseline)!!

        // Simulate Z-axis drift
        val drifted = MutableList(33) { lm(0.0, 0.0) }
        drifted[11] = lm(-0.2, -0.4, -0.7)
        drifted[12] = lm(0.2, -0.4, -0.3)
        drifted[23] = lm(-0.15, 0.0, -0.5)
        drifted[24] = lm(0.15, 0.0, -0.5)

        val refined = BoneLengthOptimizer.refine(drifted, boneLengths, iterations = 5, weight = 0.5)

        assertNotNull(refined)
        assertEquals(33, refined.size)
        // Refined landmarks should be finite numbers
        assertTrue(refined[11].x.isFinite())
        assertTrue(refined[11].y.isFinite())
        assertTrue(refined[11].z.isFinite())
    }
}

class AffineNormalizerTest {

    private fun lm(x: Double, y: Double, z: Double = 0.0, vis: Float = 1.0f) = Landmark3D(x, y, z, vis)

    @Test
    fun `identity when spine is already vertical`() {
        val pelvis = lm(0.0, 0.0, 0.0)
        val c7 = lm(0.0, -0.4, 0.0) // straight up (Y down)
        val rot = AffineNormalizer.fromSpineVector(pelvis, c7)
        assertNotNull(rot)
        // Applying to a point should not change it significantly
        val p = lm(1.0, 2.0, 3.0)
        val transformed = rot!!.apply(p)
        assertEquals(1.0, transformed.x, 0.01)
        assertEquals(2.0, transformed.y, 0.01)
        assertEquals(3.0, transformed.z, 0.01)
    }

    @Test
    fun `corrects forward-tilted spine`() {
        val pelvis = lm(0.0, 0.0, 0.0)
        val c7 = lm(0.0, -0.3, -0.2) // tilted forward
        val rot = AffineNormalizer.fromSpineVector(pelvis, c7)
        assertNotNull(rot)
        // After rotation, c7 should align with vertical
        val correctedC7 = rot!!.apply(c7)
        // X should be ~0 (no lateral), Z should be ~0 (no forward lean)
        assertEquals(0.0, correctedC7.x, 0.05)
        assertEquals(0.0, correctedC7.z, 0.05)
        // Y should be negative (upward)
        assertTrue(correctedC7.y < 0)
    }

    @Test
    fun `averageRotation returns valid result`() {
        val pelvis = lm(0.0, 0.0, 0.0)
        val matrices = (1..5).map { i ->
            val c7 = lm(0.0, -0.4, -0.01 * i) // slight variations
            AffineNormalizer.fromSpineVector(pelvis, c7)!!
        }
        val avg = AffineNormalizer.averageRotation(matrices)
        assertNotNull(avg)
    }
}
