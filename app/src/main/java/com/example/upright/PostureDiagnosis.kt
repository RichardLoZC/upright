package com.example.upright

data class PostureDiagnosis(
    val state: PostureState,
    val cva: Double?,
    val trunkAngle: Double?,
    val hasWorldLandmarks: Boolean,
    val fps: Double
)
