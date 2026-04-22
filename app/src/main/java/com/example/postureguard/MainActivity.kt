package com.example.postureguard

import android.Manifest
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.postureguard.ui.theme.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.util.concurrent.Executors
import kotlin.math.min

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            PostureGuardTheme {
                PostureGuardApp()
            }
        }
    }
}

@Composable
fun PostureGuardApp(vm: PostureGuardViewModel = viewModel()) {
    var hasCameraPermission by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    if (hasCameraPermission) {
        CameraScreen(vm)
    } else {
        PermissionScreen(onRequest = { launcher.launch(Manifest.permission.CAMERA) })
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
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
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.VerifiedUser,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = PgGreen
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "PostureGuard",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "实时坐姿监测",
                color = TextSecondary,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "需要相机权限来分析您的坐姿",
                color = TextMuted,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRequest,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PgGreen),
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("授权相机", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun CameraScreen(vm: PostureGuardViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by vm.uiState.collectAsState()

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraBound by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> vm.startForegroundMonitor()
                Lifecycle.Event.ON_PAUSE -> vm.stopForegroundMonitor()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(Unit) {
        onDispose {
            try { cameraProviderFuture.get().unbindAll() } catch (_: Exception) {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    post {
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            if (cameraBound) return@post

                            val preview = Preview.Builder().build()
                            preview.setSurfaceProvider(surfaceProvider)

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                .build()

                            imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                vm.processFrame(imageProxy)
                            }

                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_FRONT_CAMERA,
                                preview,
                                imageAnalysis
                            )
                            cameraBound = true
                        } catch (exc: Exception) {
                            android.util.Log.e("Camera", "Use case binding failed", exc)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Eco mode overlay
        if (uiState.isEcoMode) {
            EcoModeOverlay()
        }

        // Skeleton overlay
        if (uiState.showDebug && uiState.skeletonLandmarks != null) {
            SkeletonOverlay(landmarks = uiState.skeletonLandmarks!!)
        }

        // Top bar
        TopBar(uiState = uiState)

        // Bottom panel
        BottomPanel(vm = vm, uiState = uiState)
    }
}

@Composable
private fun EcoModeOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1B2A)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val infiniteTransition = rememberInfiniteTransition(label = "eco")
            val pulse by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "ecoPulse"
            )
            Icon(
                Icons.Default.PowerSettingsNew,
                contentDescription = null,
                modifier = Modifier.size(48.dp * pulse),
                tint = PgGreen
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("省电模式", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("检测仍在后台运行", color = TextMuted, fontSize = 14.sp)
        }
    }
}

@Composable
private fun SkeletonOverlay(landmarks: List<Landmark3D>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        for ((a, b) in SKELETON_CONNECTIONS) {
            val la = landmarks.getOrNull(a) ?: continue
            val lb = landmarks.getOrNull(b) ?: continue
            if (la.visibility < 0.5f || lb.visibility < 0.5f) continue
            drawLine(
                color = PgGreen.copy(alpha = 0.4f),
                start = Offset(la.x.toFloat() * size.width, la.y.toFloat() * size.height),
                end = Offset(lb.x.toFloat() * size.width, lb.y.toFloat() * size.height),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }
        for (point in landmarks) {
            if (point.visibility < 0.5f) continue
            drawCircle(
                color = PgGreenLight.copy(alpha = 0.7f),
                radius = 5f,
                center = Offset(point.x.toFloat() * size.width, point.y.toFloat() * size.height)
            )
        }
    }
}

@Composable
private fun TopBar(uiState: UiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xDD000000), Color.Transparent)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                tint = PgGreen,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("PostureGuard", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        // Status badge
        PostureStatusBadge(state = uiState.currentPosture)
    }
}

@Composable
private fun PostureStatusBadge(state: PostureState) {
    val color by animateColorAsState(
        targetValue = when (state) {
            PostureState.GOOD -> PgGreen
            PostureState.NO_PERSON -> PgGray
            else -> PgRed
        },
        animationSpec = tween(400),
        label = "statusColor"
    )
    val label = when (state) {
        PostureState.GOOD -> "良好"
        PostureState.NO_PERSON -> "无人"
        else -> "异常"
    }
    val icon: ImageVector = when (state) {
        PostureState.GOOD -> Icons.Default.CheckCircle
        PostureState.NO_PERSON -> Icons.Default.PersonOff
        else -> Icons.Default.Warning
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.2f),
        border = null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun BottomPanel(vm: PostureGuardViewModel, uiState: UiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0xDD000000))
                )
            )
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Posture indicator ring
        PostureRing(state = uiState.currentPosture)
        Spacer(modifier = Modifier.height(8.dp))

        // State text
        PostureStateText(state = uiState.currentPosture)
        Spacer(modifier = Modifier.height(4.dp))

        // Debug info
        if (uiState.showDebug && uiState.diagnosis != null) {
            DebugInfoBadge(diagnosis = uiState.diagnosis!!)
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Calibration indicator
        if (uiState.isCalibrating) {
            CalibrationProgressBadge(countdown = uiState.calibCountdown)
        } else if (uiState.calibration != null) {
            CalibratedBadge()
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Session stats
        if (uiState.sessionGoodDuration > 0 || uiState.sessionBadDuration > 0) {
            SessionStatsRow(uiState = uiState)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Control buttons
        ControlButtons(vm = vm, uiState = uiState)
    }
}

@Composable
private fun PostureRing(state: PostureState) {
    val color by animateColorAsState(
        targetValue = when (state) {
            PostureState.GOOD -> PgGreen
            PostureState.NO_PERSON -> PgGray
            else -> PgRed
        },
        animationSpec = tween(500),
        label = "ringColor"
    )

    val isBad = state != PostureState.GOOD && state != PostureState.NO_PERSON

    val infiniteTransition = rememberInfiniteTransition(label = "ringPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = if (isBad) 0.4f else 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Canvas(modifier = Modifier.size(80.dp)) {
        val strokeWidth = 6.dp.toPx()
        val diameter = min(size.width, size.height) - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)

        // Background ring
        drawArc(
            color = Color.White.copy(alpha = 0.1f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(diameter, diameter),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Pulse ring (bad posture)
        if (pulseAlpha > 0f) {
            drawArc(
                color = color.copy(alpha = pulseAlpha),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth + 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Active arc
        val sweep = if (state == PostureState.NO_PERSON) 60f else 360f
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = Size(diameter, diameter),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun PostureStateText(state: PostureState) {
    val text = when (state) {
        PostureState.GOOD -> "坐姿良好"
        PostureState.BAD_TILT_LEFT -> "头部向左歪斜"
        PostureState.BAD_TILT_RIGHT -> "头部向右歪斜"
        PostureState.BAD_SLOUCH -> "肩膀不平"
        PostureState.BAD_FORWARD_HEAD -> "头部前倾"
        PostureState.BAD_HUNCHBACK -> "驼背"
        PostureState.NO_PERSON -> "未检测到人"
    }
    val color by animateColorAsState(
        targetValue = when (state) {
            PostureState.GOOD -> PgGreen
            PostureState.NO_PERSON -> PgGray
            else -> PgRed
        },
        animationSpec = tween(400),
        label = "stateTextColor"
    )
    Text(text, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun DebugInfoBadge(diagnosis: PostureDiagnosis) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SurfaceCardLight
    ) {
        Text(
            buildString {
                append("FPS ${String.format("%.1f", diagnosis.fps)}")
                append(" · 3D ${if (diagnosis.hasWorldLandmarks) "ON" else "OFF"}")
                if (diagnosis.cva != null) append(" · CVA ${String.format("%.1f", diagnosis.cva)}°")
                if (diagnosis.trunkAngle != null) append(" · Trunk ${String.format("%.1f", diagnosis.trunkAngle)}°")
            },
            color = TextMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun CalibrationProgressBadge(countdown: Int) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PgBlue.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (countdown > 0) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = PgBlue
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text("校准中... $countdown", color = PgBlue, fontSize = 13.sp)
        }
    }
}

@Composable
private fun CalibratedBadge() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PgGreen.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = PgGreen, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("已校准", color = PgGreen, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SessionStatsRow(uiState: UiState) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatChip(
            icon = Icons.Default.AccessTime,
            label = formatDuration(uiState.sessionGoodDuration),
            color = PgGreen
        )
        StatChip(
            icon = Icons.Default.Timer,
            label = formatDuration(uiState.sessionBadDuration),
            color = PgRed
        )
        if (uiState.sessionGoodDuration + uiState.sessionBadDuration > 0) {
            val total = uiState.sessionGoodDuration + uiState.sessionBadDuration
            val ratio = uiState.sessionGoodDuration.toFloat() / total
            StatChip(
                icon = Icons.Default.TrendingUp,
                label = "${(ratio * 100).toInt()}%",
                color = if (ratio > 0.8f) PgGreen else if (ratio > 0.5f) PgOrange else PgRed
            )
        }
    }
}

@Composable
private fun StatChip(icon: ImageVector, label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text(label, color = color, fontSize = 11.sp)
    }
}

private fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}m${s}s" else "${s}s"
}

@Composable
private fun ControlButtons(vm: PostureGuardViewModel, uiState: UiState) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ControlButton(
            icon = if (uiState.isEcoMode) Icons.Default.Visibility else Icons.Default.PowerSettingsNew,
            label = if (uiState.isEcoMode) "显示画面" else "省电",
            onClick = vm::toggleEcoMode,
            modifier = Modifier.weight(1f)
        )
        ControlButton(
            icon = Icons.Default.Tune,
            label = if (uiState.isCalibrating) "校准中" else "校准",
            onClick = vm::startCalibration,
            color = if (uiState.calibration != null) PgBlue else TextPrimary,
            modifier = Modifier.weight(1f)
        )
        ControlButton(
            icon = if (uiState.showDebug) Icons.Default.VisibilityOff else Icons.Default.BugReport,
            label = if (uiState.showDebug) "隐藏" else "调试",
            onClick = vm::toggleDebug,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = TextSecondary
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = color,
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        border = null,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 12.sp)
    }
}

private val SKELETON_CONNECTIONS = listOf(
    0 to 7, 0 to 8,
    11 to 12,
    11 to 13, 13 to 15,
    12 to 14, 14 to 16,
    11 to 23, 12 to 24,
    23 to 24
)
