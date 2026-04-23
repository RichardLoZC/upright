package com.example.upright

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
import com.example.upright.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: UpRightViewModel) {
    val uiState by vm.uiState.collectAsState()
    val settings = uiState.settings
    val s = stringsFor(settings.alertLanguage)
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color(0xFF1A1A2E),
            title = { Text(s.clearCalibration, color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text(s.clearCalibConfirm, color = TextSecondary, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = {
                    vm.resetCalibration()
                    showResetDialog = false
                    Toast.makeText(context, s.calibCleared, Toast.LENGTH_SHORT).show()
                }) { Text(s.clearCalibration, color = PgRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text(s.cancel, color = TextMuted) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
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
            Text(s.settings, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SettingsSection(s.alertInterval) {
                ChipRow(
                    options = listOf(5 to s.seconds[0], 10 to s.seconds[1], 30 to s.seconds[2], 60 to s.seconds[3]),
                    selected = settings.alertIntervalSeconds,
                    onSelect = { vm.updateAlertInterval(it) }
                )
                Text(s.alertIntervalDesc, color = TextMuted, fontSize = 12.sp)
            }

            SettingsSection(s.soundSection) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(s.soundLabel, color = TextSecondary, fontSize = 14.sp)
                        Text(s.soundDesc, color = TextMuted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = settings.soundEnabled,
                        onCheckedChange = { vm.updateSoundEnabled(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = PgGreen)
                    )
                }
            }

            SettingsSection(s.vibration) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(s.vibrationDesc, color = TextSecondary, fontSize = 14.sp)
                    Switch(
                        checked = settings.vibrationEnabled,
                        onCheckedChange = { vm.updateVibrationEnabled(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = PgGreen)
                    )
                }
            }

            SettingsSection(s.sensitivity) {
                ChipRow(
                    options = listOf(SensitivityLevel.LOW to s.sensLow, SensitivityLevel.MEDIUM to s.sensMedium, SensitivityLevel.HIGH to s.sensHigh),
                    selected = settings.sensitivityLevel,
                    onSelect = { vm.updateSensitivity(it) }
                )
                Text(
                    when (settings.sensitivityLevel) {
                        SensitivityLevel.LOW -> s.sensLowDesc
                        SensitivityLevel.MEDIUM -> s.sensMediumDesc
                        SensitivityLevel.HIGH -> s.sensHighDesc
                    },
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            SettingsSection(s.language) {
                ChipRow(
                    options = listOf(AlertLanguage.ZH to "中文", AlertLanguage.EN to "English"),
                    selected = settings.alertLanguage,
                    onSelect = { vm.updateLanguage(it) }
                )
            }

            SettingsSection(s.autoResume) {
                ChipRow(
                    options = listOf(5 to s.minutes[0], 10 to s.minutes[1], 15 to s.minutes[2], 20 to s.minutes[3]),
                    selected = settings.autoResumeMinutes,
                    onSelect = { vm.updateAutoResumeMinutes(it) }
                )
            }

            SettingsSection(s.ecoModeSection) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(s.ecoModeSection, color = TextSecondary, fontSize = 14.sp)
                        Text(s.ecoModeDesc2, color = TextMuted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = uiState.isEcoMode,
                        onCheckedChange = { vm.toggleEcoMode() },
                        colors = SwitchDefaults.colors(checkedTrackColor = PgGreen)
                    )
                }
            }

            SettingsSection(s.calibManagement) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.calibStatus, color = TextSecondary, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (uiState.calibration != null) PgGreen.copy(alpha = 0.15f) else PgGray.copy(alpha = 0.15f)
                        ) {
                            Text(
                                if (uiState.calibration != null) s.calibrated else s.notCalibrated,
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
                        Icon(Icons.Default.Tune, contentDescription = s.recalibrate, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(s.recalibrate, fontSize = 13.sp)
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
                            Icon(Icons.Default.Delete, contentDescription = s.clearCalibration, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(s.clearCalibration, fontSize = 13.sp)
                        }
                    }
                }
            }

            SettingsSection(s.about) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(s.version, color = TextSecondary, fontSize = 14.sp)
                    Text("1.0.0", color = TextMuted, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(s.aboutDesc, color = TextMuted, fontSize = 12.sp)
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
