package com.example.upright

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.math.sqrt

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "calibration")

class CalibrationStore(private val context: Context) {

    companion object {
        val KEY_EAR_DIFF_Y = doublePreferencesKey("ear_diff_y")
        val KEY_SHOULDER_DIFF_Y = doublePreferencesKey("shoulder_diff_y")
        val KEY_CVA = doublePreferencesKey("cva")
        val KEY_TRUNK_ANGLE = doublePreferencesKey("trunk_angle")
        val KEY_HAS_CALIBRATION = floatPreferencesKey("has_calibration")
        val KEY_BONE_COUNT = floatPreferencesKey("bone_count")

        fun boneKey(a: Int, b: Int) = doublePreferencesKey("bone_${a}_${b}")
        fun rotKey(i: Int, j: Int) = doublePreferencesKey("rot_${i}_${j}")
    }

    suspend fun save(profile: PostureLogic.CalibrationProfile) {
        context.dataStore.edit { prefs ->
            prefs[KEY_EAR_DIFF_Y] = profile.earDiffY
            prefs[KEY_SHOULDER_DIFF_Y] = profile.shoulderDiffY
            profile.cva?.let { prefs[KEY_CVA] = it }
            profile.trunkAngle?.let { prefs[KEY_TRUNK_ANGLE] = it }
            prefs[KEY_HAS_CALIBRATION] = 1.0f

            // Save bone lengths
            profile.boneLengths?.let { bones ->
                prefs[KEY_BONE_COUNT] = bones.lengths.size.toFloat()
                for ((pair, len) in bones.lengths) {
                    prefs[boneKey(pair.first, pair.second)] = len
                }
            }

            // Save rotation matrix
            profile.rotationMatrix?.let { rot ->
                for (i in 0..2) {
                    for (j in 0..2) {
                        prefs[rotKey(i, j)] = rot.m[i][j]
                    }
                }
            }
        }
    }

    suspend fun load(): PostureLogic.CalibrationProfile? {
        val prefs = context.dataStore.data.first()
        if (prefs[KEY_HAS_CALIBRATION] != 1.0f) return null

        val earDiffY = prefs[KEY_EAR_DIFF_Y] ?: return null
        val shoulderDiffY = prefs[KEY_SHOULDER_DIFF_Y] ?: return null

        // Load bone lengths
        val boneCount = prefs[KEY_BONE_COUNT]?.toInt() ?: 0
        val boneLengths = if (boneCount > 0) {
            val lengths = mutableMapOf<Pair<Int, Int>, Double>()
            val boneConnections = listOf(
                11 to 12, 11 to 13, 13 to 15, 12 to 14, 14 to 16,
                11 to 23, 12 to 24, 23 to 24, 0 to 7, 0 to 8, 7 to 8
            )
            for ((a, b) in boneConnections) {
                prefs[boneKey(a, b)]?.let { lengths[a to b] = it }
            }
            if (lengths.size >= 4) BoneLengths(lengths) else null
        } else null

        // Load rotation matrix
        val rotationMatrix = try {
            val m = Array(3) { i -> DoubleArray(3) { j -> prefs[rotKey(i, j)] ?: 0.0 } }
            // Validate: check if it's a valid rotation matrix
            val det = m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1]) -
                    m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0]) +
                    m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0])
            if (kotlin.math.abs(det - 1.0) < 0.1) RotationMatrix(m) else null
        } catch (_: Exception) { null }

        return PostureLogic.CalibrationProfile(
            earDiffY = earDiffY,
            shoulderDiffY = shoulderDiffY,
            cva = prefs[KEY_CVA],
            trunkAngle = prefs[KEY_TRUNK_ANGLE],
            boneLengths = boneLengths,
            rotationMatrix = rotationMatrix
        )
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
