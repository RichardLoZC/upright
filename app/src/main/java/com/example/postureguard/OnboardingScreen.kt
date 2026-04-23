package com.example.postureguard

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
import com.example.postureguard.ui.theme.*

@Composable
fun OnboardingScreen(vm: PostureGuardViewModel) {
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
                        title = "PostureGuard",
                        subtitle = "实时坐姿监测助手",
                        description = "利用手机前置摄像头和 AI 技术，\n实时监测您的坐姿状态，\n帮助您养成健康的工作习惯。"
                    )
                    1 -> OnboardingPage(
                        icon = Icons.Default.PhoneAndroid,
                        iconColor = PgBlue,
                        title = "手机摆放建议",
                        subtitle = "获得最佳检测效果",
                        description = "• 将手机放在正前方，与视线等高\n• 前置摄像头朝向自己\n• 保持 0.5-1.5 米的距离\n• 确保光线充足"
                    )
                    2 -> OnboardingPage(
                        icon = Icons.Default.Tune,
                        iconColor = PgBlue,
                        title = "个性化校准",
                        subtitle = "记录您的标准坐姿",
                        description = "校准会记录您正确坐姿的姿态数据，\n后续检测会以此为基准。\n建议在坐姿端正时进行校准。"
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Page indicator dots
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

            // Buttons
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
                        Icon(Icons.Default.ArrowForward, contentDescription = "下一步", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("下一步", fontSize = 16.sp)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            onClick = { vm.completeOnboarding() },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PgGreen),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "开始使用", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("开始使用", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = { vm.completeOnboarding() }) {
                            Text("跳过，稍后再校准", color = TextMuted, fontSize = 14.sp)
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
