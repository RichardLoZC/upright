package com.example.postureguard

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE date = :date ORDER BY startTime DESC")
    suspend fun getSessionsForDate(date: String): List<SessionEntity>

    @Query("""
        SELECT date, SUM(goodDurationSeconds) as goodDurationSeconds,
               SUM(badDurationSeconds) as badDurationSeconds
        FROM sessions WHERE date >= :startDate AND date <= :endDate
        GROUP BY date ORDER BY date ASC
    """)
    suspend fun getWeeklySummary(startDate: String, endDate: String): List<DailySummary>

    @Query("SELECT COUNT(DISTINCT date) FROM sessions WHERE date <= :untilDate AND date >= :startDate")
    suspend fun getActiveDaysCount(startDate: String, untilDate: String): Int

    @Query("""
        SELECT date, SUM(goodDurationSeconds) as goodDurationSeconds,
               SUM(badDurationSeconds) as badDurationSeconds
        FROM sessions
        WHERE date <= :untilDate AND date >= :startDate
        GROUP BY date ORDER BY date DESC
    """)
    suspend fun getDailySummariesRange(startDate: String, untilDate: String): List<DailySummary>
}
