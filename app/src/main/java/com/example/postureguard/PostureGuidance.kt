package com.example.postureguard

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.postureguard.ui.theme.PgOrange

@Composable
fun PostureGuidanceArrow(state: PostureState, landmarks: List<Landmark3D>?) {
    val infiniteTransition = rememberInfiniteTransition(label = "guidancePulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "guidancePulseAlpha"
    )

    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "guidanceBounce"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2
        val arrowLen = 80f
        val bounceOffset = bounce * 15f
        val color = PgOrange.copy(alpha = pulse)

        when (state) {
            PostureState.BAD_TILT_LEFT -> {
                // Arrow pointing right: head should move right
                drawArrow(
                    start = Offset(cx - arrowLen + bounceOffset, cy),
                    end = Offset(cx + arrowLen + bounceOffset, cy),
                    color = color
                )
            }
            PostureState.BAD_TILT_RIGHT -> {
                // Arrow pointing left
                drawArrow(
                    start = Offset(cx + arrowLen - bounceOffset, cy),
                    end = Offset(cx - arrowLen - bounceOffset, cy),
                    color = color
                )
            }
            PostureState.BAD_FORWARD_HEAD -> {
                // Arrow pointing up (back): pull head back
                drawArrow(
                    start = Offset(cx, cy + arrowLen - bounceOffset),
                    end = Offset(cx, cy - arrowLen - bounceOffset),
                    color = color
                )
            }
            PostureState.BAD_HUNCHBACK -> {
                // Arrow pointing up from chest area: straighten up
                drawArrow(
                    start = Offset(cx, cy + arrowLen + 60 - bounceOffset),
                    end = Offset(cx, cy + 60 - arrowLen - bounceOffset),
                    color = color
                )
            }
            PostureState.BAD_SLOUCH -> {
                // Two arrows: level shoulders
                val y = cy + 40f
                drawArrow(
                    start = Offset(cx - 40f, y - bounceOffset * 0.5f),
                    end = Offset(cx - 40f, y - 60f - bounceOffset * 0.5f),
                    color = color
                )
                drawArrow(
                    start = Offset(cx + 40f, y + bounceOffset * 0.5f),
                    end = Offset(cx + 40f, y - 60f + bounceOffset * 0.5f),
                    color = color
                )
            }
            else -> {}
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrow(
    start: Offset,
    end: Offset,
    color: Color
) {
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = 6f
    )
    // Arrowhead
    val arrowSize = 20f
    val angle = kotlin.math.atan2(
        (end.y - start.y).toDouble(),
        (end.x - start.x).toDouble()
    )
    val path = Path()
    path.moveTo(end.x, end.y)
    path.lineTo(
        (end.x - arrowSize * kotlin.math.cos(angle - 0.5)).toFloat(),
        (end.y - arrowSize * kotlin.math.sin(angle - 0.5)).toFloat()
    )
    path.lineTo(
        (end.x - arrowSize * kotlin.math.cos(angle + 0.5)).toFloat(),
        (end.y - arrowSize * kotlin.math.sin(angle + 0.5)).toFloat()
    )
    path.close()
    drawPath(path, color = color)
}
