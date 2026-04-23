package com.example.postureguard

import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.postureguard.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: PostureGuardViewModel) {
    val uiState by vm.uiState.collectAsState()
    val settings = uiState.settings
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color(0xFF1A1A2E),
            title = { Text("清除校准", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("确定要清除校准数据吗？清除后将使用默认阈值进行检测。", color = TextSecondary, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = {
                    vm.resetCalibration()
                    showResetDialog = false
                    Toast.makeText(context, "校准已清除", Toast.LENGTH_SHORT).show()
                }) { Text("清除", color = PgRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("取消", color = TextMuted) }
            }
        )
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
                Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = TextPrimary)
            }
            Text("设置", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Alert interval
            SettingsSection("提醒间隔") {
                ChipRow(
                    options = listOf(5 to "5秒", 10 to "10秒", 30 to "30秒", 60 to "60秒"),
                    selected = settings.alertIntervalSeconds,
                    onSelect = { vm.updateAlertInterval(it) }
                )
            }

            // Sound
            SettingsSection("语音提醒") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("语音播报提醒", color = TextSecondary, fontSize = 14.sp)
                    Switch(
                        checked = settings.soundEnabled,
                        onCheckedChange = { vm.updateSoundEnabled(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = PgGreen)
                    )
                }
            }

            // Vibration
            SettingsSection("震动反馈") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("姿态异常时震动提醒", color = TextSecondary, fontSize = 14.sp)
                    Switch(
                        checked = settings.vibrationEnabled,
                        onCheckedChange = { vm.updateVibrationEnabled(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = PgGreen)
                    )
                }
            }

            // Sensitivity
            SettingsSection("检测灵敏度") {
                ChipRow(
                    options = listOf(SensitivityLevel.LOW to "低", SensitivityLevel.MEDIUM to "中", SensitivityLevel.HIGH to "高"),
                    selected = settings.sensitivityLevel,
                    onSelect = { vm.updateSensitivity(it) }
                )
                Text(
                    when (settings.sensitivityLevel) {
                        SensitivityLevel.LOW -> "较低的灵敏度，减少误报"
                        SensitivityLevel.MEDIUM -> "默认灵敏度，平衡准确性和体验"
                        SensitivityLevel.HIGH -> "较高的灵敏度，更及时提醒"
                    },
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            // Alert language
            SettingsSection("提醒语言") {
                ChipRow(
                    options = listOf(AlertLanguage.ZH to "中文", AlertLanguage.EN to "English"),
                    selected = settings.alertLanguage,
                    onSelect = { vm.updateLanguage(it) }
                )
            }

            // Auto resume
            SettingsSection("暂停后自动恢复") {
                ChipRow(
                    options = listOf(5 to "5分钟", 10 to "10分钟", 15 to "15分钟", 20 to "20分钟"),
                    selected = settings.autoResumeMinutes,
                    onSelect = { vm.updateAutoResumeMinutes(it) }
                )
            }

            // Calibration management
            SettingsSection("校准管理") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("校准状态", color = TextSecondary, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (uiState.calibration != null) PgGreen.copy(alpha = 0.15f) else PgGray.copy(alpha = 0.15f)
                        ) {
                            Text(
                                if (uiState.calibration != null) "已校准" else "未校准",
                                color = if (uiState.calibration != null) PgGreen else PgGray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            vm.navigateTo(Screen.MAIN)
                            vm.startCalibration()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PgBlue,
                            containerColor = PgBlue.copy(alpha = 0.1f)
                        )
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "重新校准", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("重新校准", fontSize = 13.sp)
                    }
                    if (uiState.calibration != null) {
                        OutlinedButton(
                            onClick = { showResetDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PgRed,
                                containerColor = PgRed.copy(alpha = 0.1f)
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "清除校准", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("清除校准", fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCardLight
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun <T> ChipRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) PgGreen.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                modifier = Modifier.weight(1f)
            ) {
                TextButton(
                    onClick = { onSelect(value) },
                    modifier = Modifier.padding(vertical = 2.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (isSelected) PgGreen else TextMuted
                    )
                ) {
                    Text(label, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}
