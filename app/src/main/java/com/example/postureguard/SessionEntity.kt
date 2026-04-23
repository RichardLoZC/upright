package com.example.postureguard

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val startTime: Long,
    val endTime: Long,
    val goodDurationSeconds: Long,
    val badDurationSeconds: Long,
    val calibrationUsed: Boolean
)

data class DailySummary(
    val date: String,
    val goodDurationSeconds: Long,
    val badDurationSeconds: Long
)
