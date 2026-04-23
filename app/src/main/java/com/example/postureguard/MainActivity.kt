package com.example.postureguard

import android.content.Context
import android.content.res.Configuration
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Surface
import android.view.WindowManager
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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
    val uiState by vm.uiState.collectAsState()
    val context = LocalContext.current

    // Calibration result toast
    LaunchedEffect(uiState.calibrationSuccess) {
        when (uiState.calibrationSuccess) {
            true -> Toast.makeText(context, "校准成功", Toast.LENGTH_SHORT).show()
            false -> Toast.makeText(context, "校准失败，请确保坐在摄像头前方", Toast.LENGTH_LONG).show()
            null -> {}
        }
        if (uiState.calibrationSuccess != null) vm.consumeCalibrationResult()
    }

    when (uiState.currentScreen) {
        Screen.ONBOARDING -> OnboardingScreen(vm)
        Screen.MAIN -> MainWithPermission(vm)
        Screen.SETTINGS -> SettingsScreen(vm)
        Screen.HISTORY -> HistoryScreen(vm)
    }
}

@Composable
private fun MainWithPermission(vm: PostureGuardViewModel) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
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
                contentDescription = "应用图标",
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
            Text("实时坐姿监测", color = TextSecondary, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("需要相机权限来分析您的坐姿", color = TextMuted, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRequest,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PgGreen),
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "授权相机", modifier = Modifier.size(20.dp))
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
    val config = LocalConfiguration.current
    val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraBound by remember { mutableStateOf(false) }
    var imageAnalysisRef by remember { mutableStateOf<ImageAnalysis?>(null) }

    // Compute current display rotation, recomputed when orientation changes
    val displayRotation = remember(config.orientation) {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        wm.defaultDisplay.rotation
    }

    // Update camera target rotation when device rotates
    LaunchedEffect(displayRotation) {
        imageAnalysisRef?.targetRotation = displayRotation
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> vm.startForegroundMonitor()
                Lifecycle.Event.ON_PAUSE -> {
                    vm.stopForegroundMonitor()
                    vm.saveCurrentSession()
                }
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

                            val preview = Preview.Builder()
                                .setTargetRotation(displayRotation)
                                .build()
                            preview.setSurfaceProvider(surfaceProvider)

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                .setTargetRotation(displayRotation)
                                .build()
                            imageAnalysisRef = imageAnalysis

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

        // Overlay layer (pause takes priority over eco)
        if (uiState.isPaused) {
            PauseOverlay(remaining = uiState.pauseRemainingSeconds)
        } else if (uiState.isEcoMode) {
            EcoModeOverlay()
        }

        // Skeleton overlay
        val skeleton = uiState.skeletonLandmarks
        if (uiState.showDebug && skeleton != null) {
            SkeletonOverlay(landmarks = skeleton)
        }

        // Posture guidance arrows
        if (uiState.currentPosture != PostureState.GOOD &&
            uiState.currentPosture != PostureState.NO_PERSON &&
            !uiState.isPaused
        ) {
            PostureGuidanceArrow(state = uiState.currentPosture)
        }

        // Top bar
        Box(modifier = Modifier.align(Alignment.TopStart)) {
            TopBar(uiState = uiState, vm = vm, isLandscape = isLandscape)
        }

        // Bottom panel
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            BottomPanel(vm = vm, uiState = uiState, isLandscape = isLandscape)
        }

        // NO_PERSON pause suggestion
        if (uiState.showPauseSuggestion) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 200.dp)
                    .padding(horizontal = 16.dp),
                action = {
                    TextButton(onClick = {
                        vm.togglePause()
                        vm.dismissPauseSuggestion()
                    }) { Text("暂停", color = PgGreen) }
                },
                dismissAction = {
                    TextButton(onClick = { vm.dismissPauseSuggestion() }) {
                        Text("忽略", color = TextMuted)
                    }
                },
                containerColor = SurfaceCard
            ) {
                Text("长时间未检测到人，是否暂停监测？", color = TextPrimary, fontSize = 14.sp)
            }
        }
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
                contentDescription = "省电模式",
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
private fun PauseOverlay(remaining: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xDD0D1B2A)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Pause,
                contentDescription = "已暂停",
                modifier = Modifier.size(48.dp),
                tint = PgOrange
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("已暂停", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            val min = remaining / 60
            val sec = remaining % 60
            Text(
                "${min}分${sec}秒后自动恢复",
                color = PgOrange,
                fontSize = 16.sp
            )
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
private fun TopBar(uiState: UiState, vm: PostureGuardViewModel, isLandscape: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xDD000000), Color.Transparent)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = if (isLandscape) 6.dp else 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { vm.navigateTo(Screen.SETTINGS) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "设置",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Default.Shield,
                contentDescription = "PostureGuard",
                tint = PgGreen,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("PostureGuard", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // History button
            IconButton(
                onClick = { vm.navigateTo(Screen.HISTORY) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = "历史记录",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            PostureStatusBadge(state = uiState.currentPosture)
        }
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
        color = color.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = "坐姿状态", tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun BottomPanel(vm: PostureGuardViewModel, uiState: UiState, isLandscape: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0xDD000000))
                )
            )
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = if (isLandscape) 4.dp else 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLandscape) {
            // Compact landscape layout: ring + text side by side
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                PostureRing(state = uiState.currentPosture, ringSize = 48.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    PostureStateText(state = uiState.currentPosture, compact = true)
                    if (uiState.showDebug && uiState.diagnosis != null) {
                        DebugInfoBadge(diagnosis = uiState.diagnosis)
                    }
                }
                if (uiState.sessionGoodDuration > 0 || uiState.sessionBadDuration > 0) {
                    Spacer(modifier = Modifier.width(12.dp))
                    SessionStatsRow(uiState = uiState, compact = true)
                }
            }
        } else {
            // Portrait layout (original)
            PostureRing(state = uiState.currentPosture)
            Spacer(modifier = Modifier.height(8.dp))
            PostureStateText(state = uiState.currentPosture)
            Spacer(modifier = Modifier.height(4.dp))
            if (uiState.showDebug && uiState.diagnosis != null) {
                DebugInfoBadge(diagnosis = uiState.diagnosis)
                Spacer(modifier = Modifier.height(4.dp))
            }
            if (uiState.sessionGoodDuration > 0 || uiState.sessionBadDuration > 0) {
                SessionStatsRow(uiState = uiState)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Calibration indicator
        if (uiState.isCalibrating) {
            CalibrationProgressBadge(countdown = uiState.calibCountdown)
        } else if (uiState.calibration != null) {
            CalibratedBadge()
        }

        Spacer(modifier = Modifier.height(if (isLandscape) 2.dp else 8.dp))

        // Control buttons
        ControlButtons(vm = vm, uiState = uiState)
    }
}

@Composable
private fun PostureRing(state: PostureState, ringSize: Dp = 80.dp) {
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

    Canvas(modifier = Modifier.size(ringSize)) {
        val strokeWidth = 6.dp.toPx()
        val diameter = min(size.width, size.height) - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)

        drawArc(
            color = Color.White.copy(alpha = 0.1f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(diameter, diameter),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

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
private fun PostureStateText(state: PostureState, compact: Boolean = false) {
    val text = when (state) {
        PostureState.GOOD -> "坐姿良好"
        PostureState.BAD_TILT -> "头部侧歪"
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
    Text(text, color = color, fontSize = if (compact) 14.sp else 18.sp, fontWeight = FontWeight.Bold)
    if (!compact && state == PostureState.NO_PERSON) {
        Text("请坐在摄像头前方", color = TextMuted, fontSize = 13.sp)
    }
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
            Icon(Icons.Default.Check, contentDescription = "已校准", tint = PgGreen, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("已校准", color = PgGreen, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SessionStatsRow(uiState: UiState, compact: Boolean = false) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 16.dp),
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
        Icon(icon, contentDescription = "统计", tint = color, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text(label, color = color, fontSize = 11.sp)
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

@Composable
private fun ControlButtons(vm: PostureGuardViewModel, uiState: UiState) {
    var showCalibDialog by remember { mutableStateOf(false) }

    if (showCalibDialog) {
        CalibrationInfoDialog(
            onDismiss = { showCalibDialog = false },
            onStart = {
                showCalibDialog = false
                vm.startCalibration()
            }
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ControlButton(
            icon = if (uiState.isEcoMode) Icons.Default.Visibility else Icons.Default.PowerSettingsNew,
            label = if (uiState.isEcoMode) "画面" else "省电",
            onClick = vm::toggleEcoMode,
            modifier = Modifier.weight(1f)
        )
        ControlButton(
            icon = if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
            label = if (uiState.isPaused) "继续" else "暂停",
            onClick = vm::togglePause,
            color = if (uiState.isPaused) PgOrange else TextSecondary,
            modifier = Modifier.weight(1f)
        )
        ControlButton(
            icon = Icons.Default.Tune,
            label = if (uiState.isCalibrating) "校准中" else "校准",
            onClick = {
                if (uiState.isCalibrating) return@ControlButton
                showCalibDialog = true
            },
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
        Icon(icon, contentDescription = label, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 11.sp)
    }
}

@Composable
private fun CalibrationInfoDialog(
    onDismiss: () -> Unit,
    onStart: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color(0xFF1A1A2E),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = "坐姿校准", tint = PgBlue, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("坐姿校准", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    "校准会记录你当前的坐姿作为「标准姿势」，后续检测会以此为基准判断偏差。",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "第一步", tint = PgGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("先调整到正确坐姿", color = TextSecondary, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = "第二步", tint = PgBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("保持 3 秒完成采集", color = TextSecondary, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Save, contentDescription = "第三步", tint = PgOrange, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("校准结果自动保存，下次无需重新校准", color = TextSecondary, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onStart) {
                Text("开始校准", color = PgGreen, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextMuted)
            }
        }
    )
}

private val SKELETON_CONNECTIONS = listOf(
    0 to 7, 0 to 8,
    11 to 12,
    11 to 13, 13 to 15,
    12 to 14, 14 to 16,
    11 to 23, 12 to 24,
    23 to 24
)
