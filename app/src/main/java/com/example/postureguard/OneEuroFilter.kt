package com.example.postureguard

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow

/**
 * 1 Euro Filter for smoothing landmark coordinates.
 *
 * Adapts smoothing based on signal velocity:
 * - Low velocity (static pose) → aggressive smoothing (low cutoff)
 * - High velocity (posture change) → minimal smoothing (high cutoff)
 *
 * This eliminates jitter when still while maintaining zero-lag tracking during movement.
 *
 * @param minCutoff Minimum cutoff frequency (lower = more smoothing at rest). Default 1.0 Hz.
 * @param beta Speed coefficient (higher = faster response to movement). Default 0.007.
 * @param dCutoff Cutoff for derivative estimation. Default 1.0 Hz.
 */
class OneEuroFilter(
    private val minCutoff: Double = 1.0,
    private val beta: Double = 0.007,
    private val dCutoff: Double = 1.0
) {
    private var prevValue: Double? = null
    private var prevDValue: Double? = null
    private var prevTimestamp: Long? = null

    fun filter(value: Double, timestamp: Long): Double {
        val prevTs = prevTimestamp
        if (prevTs == null) {
            prevValue = value
            prevTimestamp = timestamp
            return value
        }

        val te = (timestamp - prevTs) / 1000.0 // ms to seconds
        if (te <= 0.0) return prevValue ?: value

        // Estimate derivative (velocity)
        val dValue = (value - (prevValue ?: value)) / te
        val edValue = smoothWithAlpha(dValue, prevDValue, alpha(dCutoff, te))
        prevDValue = edValue

        // Adaptive cutoff: increases with speed
        val cutoff = minCutoff + beta * abs(edValue)

        // Smooth the value
        val result = smoothWithAlpha(value, prevValue, alpha(cutoff, te))
        prevValue = result
        prevTimestamp = timestamp

        return result
    }

    fun reset() {
        prevValue = null
        prevDValue = null
        prevTimestamp = null
    }

    private fun alpha(cutoff: Double, te: Double): Double {
        val tau = 1.0 / (2.0 * Math.PI * cutoff)
        return 1.0 / (1.0 + tau / te)
    }

    private fun smoothWithAlpha(current: Double, previous: Double?, a: Double): Double {
        if (previous == null) return current
        return a * current + (1.0 - a) * previous
    }
}

/**
 * Maintains a set of 1 Euro Filters for all 33 landmarks (x, y, z each).
 */
class LandmarkSmoother(
    private val minCutoff: Double = 1.0,
    private val beta: Double = 0.007
) {
    // 33 landmarks × 3 axes (x, y, z)
    private val filters = Array(33) { Array(3) { OneEuroFilter(minCutoff, beta) } }

    fun smooth(landmarks: List<Landmark3D>, timestamp: Long): List<Landmark3D> {
        return landmarks.mapIndexed { i, lm ->
            Landmark3D(
                x = filters[i][0].filter(lm.x, timestamp),
                y = filters[i][1].filter(lm.y, timestamp),
                z = filters[i][2].filter(lm.z, timestamp),
                visibility = lm.visibility
            )
        }
    }

    fun reset() {
        filters.forEach { axis -> axis.forEach { it.reset() } }
    }
}
