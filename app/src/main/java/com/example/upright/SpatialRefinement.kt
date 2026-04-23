package com.example.upright

import kotlin.math.sqrt

// Bone connections used for length-constraint optimization
private val BONE_CONNECTIONS = listOf(
    11 to 12, // shoulders
    11 to 13, 13 to 15, // left arm
    12 to 14, 14 to 16, // right arm
    11 to 23, 12 to 24, // torso sides
    23 to 24, // hips
    0 to 7, 0 to 8, // nose to ears
    7 to 8   // ear to ear
)

data class BoneLengths(
    val lengths: Map<Pair<Int, Int>, Double>
) {
    fun getRatios(): Map<Pair<Int, Int>, Double> {
        val ref = lengths.values.sortedDescending().firstOrNull() ?: return emptyMap()
        return lengths.mapValues { it.value / ref }
    }
}

object BoneLengthOptimizer {

    fun calibrate(landmarks: List<Landmark3D>): BoneLengths? {
        if (landmarks.size < 33) return null
        val lengths = mutableMapOf<Pair<Int, Int>, Double>()
        for ((a, b) in BONE_CONNECTIONS) {
            val la = landmarks[a]; val lb = landmarks[b]
            if (la.visibility < 0.3f || lb.visibility < 0.3f) continue
            lengths[a to b] = euclidean(la, lb)
        }
        return if (lengths.size >= 4) BoneLengths(lengths) else null
    }

    fun averageCalibration(samples: List<BoneLengths>): BoneLengths? {
        if (samples.isEmpty()) return null
        val allKeys = samples.flatMap { it.lengths.keys }.toSet()
        val avg = mutableMapOf<Pair<Int, Int>, Double>()
        for (key in allKeys) {
            val vals = samples.mapNotNull { it.lengths[key] }
            if (vals.size >= 2) avg[key] = vals.average()
        }
        return if (avg.size >= 4) BoneLengths(avg) else null
    }

    // Iterative correction: scale each landmark toward its expected bone ratio
    fun refine(
        landmarks: List<Landmark3D>,
        baseline: BoneLengths,
        iterations: Int = 3,
        weight: Double = 0.3
    ): List<Landmark3D> {
        if (landmarks.size < 33) return landmarks
        val result = landmarks.map { it.copy() }.toMutableList()
        val baselineRatios = baseline.getRatios()

        repeat(iterations) {
            for ((a, b) in BONE_CONNECTIONS) {
                val expectedRatio = baselineRatios[a to b] ?: continue
                val la = result[a]; val lb = result[b]
                if (la.visibility < 0.3f || lb.visibility < 0.3f) continue

                val currentLen = euclidean(la, lb)
                val refLen = baselineRatios.entries.firstOrNull()?.let { (_, _) ->
                    // Use shoulder width as reference (most stable)
                    val sw = baselineRatios[11 to 12]
                    if (sw != null && result[11].visibility >= 0.3f && result[12].visibility >= 0.3f) {
                        euclidean(result[11], result[12]) / sw
                    } else currentLen / expectedRatio
                } ?: continue

                val targetLen = expectedRatio * refLen
                if (currentLen < 1e-6) continue

                val scale = 1.0 - weight * (1.0 - targetLen / currentLen)
                val midX = (la.x + lb.x) / 2.0
                val midY = (la.y + lb.y) / 2.0
                val midZ = (la.z + lb.z) / 2.0

                result[a] = la.copy(
                    x = midX + (la.x - midX) * scale,
                    y = midY + (la.y - midY) * scale,
                    z = midZ + (la.z - midZ) * scale
                )
                result[b] = lb.copy(
                    x = midX + (lb.x - midX) * scale,
                    y = midY + (lb.y - midY) * scale,
                    z = midZ + (lb.z - midZ) * scale
                )
            }
        }
        return result.toList()
    }

    private fun euclidean(a: Landmark3D, b: Landmark3D): Double {
        val dx = a.x - b.x; val dy = a.y - b.y; val dz = a.z - b.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}

// Affine rotation matrix (3x3) for camera tilt compensation
data class RotationMatrix(
    val m: Array<DoubleArray> // 3x3
) {
    fun apply(p: Landmark3D): Landmark3D {
        val x = m[0][0] * p.x + m[0][1] * p.y + m[0][2] * p.z
        val y = m[1][0] * p.x + m[1][1] * p.y + m[1][2] * p.z
        val z = m[2][0] * p.x + m[2][1] * p.y + m[2][2] * p.z
        return Landmark3D(x, y, z, p.visibility)
    }

    fun applyAll(landmarks: List<Landmark3D>): List<Landmark3D> {
        return landmarks.map { apply(it) }
    }
}

object AffineNormalizer {

    // Compute rotation from the raw spine vector to true vertical (0, -1, 0)
    fun fromSpineVector(
        pelvis: Landmark3D,
        c7: Landmark3D
    ): RotationMatrix? {
        val dx = c7.x - pelvis.x
        val dy = c7.y - pelvis.y
        val dz = c7.z - pelvis.z
        val len = sqrt(dx * dx + dy * dy + dz * dz)
        if (len < 1e-6) return null

        // Spine direction (normalized)
        val sx = dx / len; val sy = dy / len; val sz = dz / len

        // Target: straight up (0, -1, 0) — MediaPipe Y goes down
        val tx = 0.0; val ty = -1.0; val tz = 0.0

        // Rotation axis = spine × target
        val rx = sy * tz - sz * ty
        val ry = sz * tx - sx * tz
        val rz = sx * ty - sy * tx
        val rLen = sqrt(rx * rx + ry * ry + rz * rz)

        // If nearly parallel, no rotation needed
        if (rLen < 1e-6) {
            val dot = sx * tx + sy * ty + sz * tz
            return if (dot > 0) {
                // Aligned with target, identity
                identity()
            } else {
                // Anti-aligned, 180° rotation around X
                RotationMatrix(arrayOf(
                    doubleArrayOf(1.0, 0.0, 0.0),
                    doubleArrayOf(0.0, -1.0, 0.0),
                    doubleArrayOf(0.0, 0.0, -1.0)
                ))
            }
        }

        // Rodrigues' rotation formula → 3x3 matrix
        val ux = rx / rLen; val uy = ry / rLen; val uz = rz / rLen
        val cosA = sx * tx + sy * ty + sz * tz
        val sinA = rLen // because target is unit length
        val oneMinusCos = 1.0 - cosA

        return RotationMatrix(arrayOf(
            doubleArrayOf(
                cosA + ux * ux * oneMinusCos,
                ux * uy * oneMinusCos - uz * sinA,
                ux * uz * oneMinusCos + uy * sinA
            ),
            doubleArrayOf(
                uy * ux * oneMinusCos + uz * sinA,
                cosA + uy * uy * oneMinusCos,
                uy * uz * oneMinusCos - ux * sinA
            ),
            doubleArrayOf(
                uz * ux * oneMinusCos - uy * sinA,
                uz * uy * oneMinusCos + ux * sinA,
                cosA + uz * uz * oneMinusCos
            )
        ))
    }

    fun averageRotation(matrices: List<RotationMatrix>): RotationMatrix? {
        if (matrices.isEmpty()) return null
        // Average each element (sufficient for small rotations)
        val n = matrices.size.toDouble()
        val avg = Array(3) { i -> DoubleArray(3) { j ->
            matrices.sumOf { it.m[i][j] } / n
        }}
        // Re-orthogonalize via Gram-Schmidt
        // Row 0
        val r0 = avg[0]
        val r0Len = sqrt(r0[0]*r0[0] + r0[1]*r0[1] + r0[2]*r0[2])
        if (r0Len < 1e-6) return null
        avg[0] = doubleArrayOf(r0[0]/r0Len, r0[1]/r0Len, r0[2]/r0Len)
        // Row 1 = row1 - proj(row1, row0)
        val dot10 = avg[1][0]*avg[0][0] + avg[1][1]*avg[0][1] + avg[1][2]*avg[0][2]
        val r1 = doubleArrayOf(avg[1][0] - dot10*avg[0][0], avg[1][1] - dot10*avg[0][1], avg[1][2] - dot10*avg[0][2])
        val r1Len = sqrt(r1[0]*r1[0] + r1[1]*r1[1] + r1[2]*r1[2])
        if (r1Len < 1e-6) return null
        avg[1] = doubleArrayOf(r1[0]/r1Len, r1[1]/r1Len, r1[2]/r1Len)
        // Row 2 = row0 × row1
        avg[2] = doubleArrayOf(
            avg[0][1]*avg[1][2] - avg[0][2]*avg[1][1],
            avg[0][2]*avg[1][0] - avg[0][0]*avg[1][2],
            avg[0][0]*avg[1][1] - avg[0][1]*avg[1][0]
        )
        return RotationMatrix(avg)
    }

    fun identity(): RotationMatrix = RotationMatrix(arrayOf(
        doubleArrayOf(1.0, 0.0, 0.0),
        doubleArrayOf(0.0, 1.0, 0.0),
        doubleArrayOf(0.0, 0.0, 1.0)
    ))
}
