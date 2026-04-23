package com.example.postureguard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.postureguard.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HistoryScreen(vm: PostureGuardViewModel) {
    val uiState by vm.uiState.collectAsState()
    val s = stringsFor(uiState.settings.alertLanguage)

    // Refresh every time the screen is shown
    LaunchedEffect(System.currentTimeMillis()) { vm.loadHistoryData() }

    // Show loading while data is being fetched
    if (uiState.weeklySummary.isEmpty() && uiState.todaySessions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceDark),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = PgGreen, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(s.loading, color = TextMuted, fontSize = 14.sp)
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { vm.navigateTo(Screen.MAIN) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = s.back, tint = TextPrimary)
            }
            Text(s.history, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Today's goal + Streak row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Today's goal progress
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceCardLight,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(s.todayGoal, color = TextSecondary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        TodayGoalRing(uiState, s)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(s.goalDesc, color = TextMuted, fontSize = 11.sp)
                    }
                }

                // Streak badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceCardLight,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(s.streak, color = TextSecondary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = s.streak,
                            tint = PgOrange,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${uiState.currentStreak} ${s.streakUnit}",
                            color = PgOrange,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Weekly bar chart
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceCardLight
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(s.weeklyTrend, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    WeeklyBarChart(summary = uiState.weeklySummary, dayNames = s.dayNames)
                }
            }

            // Today's sessions
            if (uiState.todaySessions.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceCardLight
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(s.todayRecords, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        uiState.todaySessions.forEach { session ->
                            val total = session.goodDurationSeconds + session.badDurationSeconds
                            val ratio = if (total > 0) session.goodDurationSeconds.toFloat() / total else 0f
                            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${sdf.format(session.startTime)} - ${sdf.format(session.endTime)}",
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                                Text(
                                    "${formatDuration(session.goodDurationSeconds)} / ${formatDuration(total)}",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (ratio > 0.8f) PgGreen.copy(alpha = 0.15f) else PgRed.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        "${(ratio * 100).toInt()}%",
                                        color = if (ratio > 0.8f) PgGreen else PgRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceCardLight
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.History, contentDescription = s.noRecords, tint = TextMuted, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(s.noRecords, color = TextMuted, fontSize = 14.sp)
                        Text(s.noRecordsHint, color = TextMuted.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TodayGoalRing(uiState: UiState, s: S) {
    // Combine saved sessions + current live session for today's goal
    val savedGood = uiState.todaySessions.sumOf { it.goodDurationSeconds }
    val savedBad = uiState.todaySessions.sumOf { it.badDurationSeconds }
    val good = savedGood + uiState.sessionGoodDuration
    val bad = savedBad + uiState.sessionBadDuration
    val total = good + bad
    val ratio = if (total > 0) good.toFloat() / total else 0f
    val metGoal = ratio >= 0.8f

    val color = if (metGoal) PgGreen else PgRed

    Canvas(modifier = Modifier.size(64.dp)) {
        val strokeWidth = 6.dp.toPx()
        val diameter = size.width - strokeWidth
        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

        drawArc(
            color = Color.White.copy(alpha = 0.1f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(diameter, diameter),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        if (total > 0) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = ratio * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
    if (metGoal && total > 0) {
        Icon(Icons.Default.CheckCircle, contentDescription = s.goalMet, tint = PgGreen, modifier = Modifier.size(18.dp))
    }
    Text(
        if (total > 0) "${(ratio * 100).toInt()}%" else "--",
        color = if (total > 0) color else TextMuted,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun WeeklyBarChart(summary: List<DailySummary>, dayNames: List<String>) {
    val hasData = summary.isNotEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        for (i in 0 until 7) {
            val dayData = summary.getOrNull(i)
            val good = dayData?.goodDurationSeconds ?: 0L
            val bad = dayData?.badDurationSeconds ?: 0L
            val total = good + bad
            val ratio = if (total > 0) good.toFloat() / total else 0f
            val barColor = if (!hasData) PgGray.copy(alpha = 0.2f) else if (ratio >= 0.8f) PgGreen else if (total > 0) PgRed else PgGray.copy(alpha = 0.3f)
            val barFraction = if (total > 0) maxOf(ratio, 0.05f) else 0.05f

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                // Chart area (100.dp) + label area (20.dp) = 120.dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        modifier = Modifier
                            .width(20.dp)
                            .height((100.dp * barFraction).coerceAtLeast(4.dp)),
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                        color = barColor
                    ) {}
                }
                if (total > 0) {
                    Text("${(ratio * 100).toInt()}%", color = barColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Text(dayNames[i], color = TextMuted, fontSize = 12.sp)
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    if (seconds <= 0) return "0s"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return when {
        h > 0 -> "${h}h${m}m"
        m > 0 -> "${m}m${s}s"
        else -> "${s}s"
    }
}
