package com.example.upright

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upright.ui.theme.*

@Composable
fun OnboardingScreen(vm: UpRightViewModel) {
    val uiState by vm.uiState.collectAsState()
    val s = stringsFor(uiState.settings.alertLanguage)
    var page by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A237E), Color(0xFF0D1B2A))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                },
                label = "onboarding"
            ) { targetPage ->
                when (targetPage) {
                    0 -> OnboardingPage(
                        icon = Icons.Default.Shield,
                        iconColor = PgGreen,
                        title = "UpRight",
                        subtitle = s.onbTitle1,
                        description = s.onbDesc1
                    )
                    1 -> OnboardingPage(
                        icon = Icons.Default.PhoneAndroid,
                        iconColor = PgBlue,
                        title = s.onbTitle2,
                        subtitle = s.onbSubtitle2,
                        description = s.onbDesc2
                    )
                    2 -> OnboardingPage(
                        icon = Icons.Default.Tune,
                        iconColor = PgBlue,
                        title = s.onbTitle3,
                        subtitle = s.onbSubtitle3,
                        description = s.onbDesc3
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { i ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (i == page) 8.dp else 6.dp)
                            .background(
                                color = if (i == page) PgGreen else PgGray.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                if (page < 2) {
                    Button(
                        onClick = { page++ },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PgGreen),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = s.next, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(s.next, fontSize = 16.sp)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            onClick = { vm.completeOnboarding() },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PgGreen),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = s.getStarted, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(s.getStarted, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = { vm.completeOnboarding() }) {
                            Text(s.skipCalib, color = TextMuted, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    description: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = title,
            modifier = Modifier.size(72.dp),
            tint = iconColor
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            title,
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            subtitle,
            color = TextSecondary,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            description,
            color = TextMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}
