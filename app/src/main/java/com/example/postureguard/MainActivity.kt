package com.example.postureguard

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.postureguard.ui.theme.PostureGuardTheme
import android.os.SystemClock
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var poseDetector: PoseDetector
    private lateinit var smoother: LandmarkSmoother
    private lateinit var tts: TextToSpeech
    private var isTtsReady = false
    private var lastAlertTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        poseDetector = PoseDetector(this)
        smoother = LandmarkSmoother(minCutoff = 1.0, beta = 0.007)
        tts = TextToSpeech(this, this)

        setContent {
            PostureGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PostureGuardApp(
                        poseDetector = poseDetector,
                        smoother = smoother,
                        onPostureDetected = { state -> handlePostureState(state) }
                    )
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.CHINESE)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Chinese language not supported")
            } else {
                isTtsReady = true
            }
        } else {
            Log.e("TTS", "Initialization failed")
        }
    }

    private fun handlePostureState(state: PostureState) {
        val now = System.currentTimeMillis()
        if (now - lastAlertTime < 5000) return

        if (isTtsReady) {
            val message = when (state) {
                PostureState.BAD_TILT_LEFT -> "头向左歪了"
                PostureState.BAD_TILT_RIGHT -> "头向右歪了"
                PostureState.BAD_SLOUCH -> "肩膀不平，坐直一点"
                PostureState.BAD_FORWARD_HEAD -> "头太靠前了，收下巴"
                PostureState.BAD_HUNCHBACK -> "驼背了，挺直背部"
                else -> null
            }

            if (message != null) {
                tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
                lastAlertTime = now
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        poseDetector.close()
        if (isTtsReady) {
            tts.stop()
            tts.shutdown()
        }
    }
}

@Composable
fun PostureGuardApp(
    poseDetector: PoseDetector,
    smoother: LandmarkSmoother,
    onPostureDetected: (PostureState) -> Unit
) {
    var hasCameraPermission by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    if (hasCameraPermission) {
        CameraScreen(poseDetector, smoother, onPostureDetected)
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("需要相机权限来监控坐姿", fontSize = 18.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                    Text("授权")
                }
            }
        }
    }
}

@Composable
fun CameraScreen(
    poseDetector: PoseDetector,
    smoother: LandmarkSmoother,
    onPostureDetected: (PostureState) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentPosture by remember { mutableStateOf(PostureState.NO_PERSON) }
    var isEcoMode by remember { mutableStateOf(false) }
    var showDebug by remember { mutableStateOf(true) }
    var diagnosis by remember { mutableStateOf<PostureDiagnosis?>(null) }

    // Bind camera once, not on every recomposition
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraBound by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isEcoMode) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        post {
                            val cameraProvider = cameraProviderFuture.get()
                            if (cameraBound) return@post

                            val preview = Preview.Builder().build()
                            preview.setSurfaceProvider(surfaceProvider)

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                .build()

                            imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                processImage(imageProxy, poseDetector, smoother) { diag ->
                                    currentPosture = diag.state
                                    diagnosis = diag
                                    onPostureDetected(diag.state)
                                }
                            }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_FRONT_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                                cameraBound = true
                            } catch (exc: Exception) {
                                Log.e("Camera", "Use case binding failed", exc)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("省电模式", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("检测仍在后台运行", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }

        // Top bar - app title + status dot
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "PostureGuard",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                val dotColor = when (currentPosture) {
                    PostureState.GOOD -> Color(0xFF4CAF50)
                    PostureState.NO_PERSON -> Color.Gray
                    else -> Color(0xFFF44336)
                }
                Canvas(modifier = Modifier.size(10.dp)) {
                    drawCircle(color = dotColor)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    when (currentPosture) {
                        PostureState.GOOD -> "正常"
                        PostureState.NO_PERSON -> "无人"
                        else -> "异常"
                    },
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }

        // Bottom panel
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status card
            val stateText = when (currentPosture) {
                PostureState.GOOD -> "坐姿良好"
                PostureState.BAD_TILT_LEFT -> "头部向左歪斜"
                PostureState.BAD_TILT_RIGHT -> "头部向右歪斜"
                PostureState.BAD_SLOUCH -> "肩膀不平"
                PostureState.BAD_FORWARD_HEAD -> "头部前倾"
                PostureState.BAD_HUNCHBACK -> "驼背"
                PostureState.NO_PERSON -> "未检测到人"
            }
            val bgColor = when (currentPosture) {
                PostureState.GOOD -> Color(0xE04CAF50)
                PostureState.NO_PERSON -> Color(0xE09E9E9E)
                else -> Color(0xE0F44336)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text(stateText, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            // Debug info
            if (showDebug && diagnosis != null) {
                val d = diagnosis!!
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        buildString {
                            append("FPS ${String.format("%.1f", d.fps)}")
                            append(" · 3D ${if (d.hasWorldLandmarks) "ON" else "OFF"}")
                            if (d.cva != null) append(" · CVA ${String.format("%.1f", d.cva)}°")
                            if (d.trunkAngle != null) append(" · Trunk ${String.format("%.1f", d.trunkAngle)}°")
                        },
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { isEcoMode = !isEcoMode },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text(if (isEcoMode) "显示画面" else "省电模式", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = { showDebug = !showDebug },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text(if (showDebug) "隐藏调试" else "调试", fontSize = 13.sp)
                }
            }
        }
    }
}

private var frameCount = 0
private var fpsTimestamp = SystemClock.uptimeMillis()
private var currentFps = 0.0

fun processImage(
    imageProxy: ImageProxy,
    detector: PoseDetector,
    smoother: LandmarkSmoother,
    onResult: (PostureDiagnosis) -> Unit
) {
    val bitmap = imageProxy.toBitmap()

    val rotation = imageProxy.imageInfo.rotationDegrees
    val matrix = Matrix()
    matrix.postRotate(rotation.toFloat())
    matrix.postScale(-1f, 1f)

    val rotatedBitmap = Bitmap.createBitmap(
        bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
    )

    val timestamp = SystemClock.uptimeMillis()

    frameCount++
    val elapsed = timestamp - fpsTimestamp
    if (elapsed >= 1000) {
        currentFps = frameCount * 1000.0 / elapsed
        frameCount = 0
        fpsTimestamp = timestamp
    }

    detector.setListener { detection ->
        if (detection.landmarks2d.isEmpty()) {
            onResult(PostureDiagnosis(PostureState.NO_PERSON, null, null, false, currentFps))
            return@setListener
        }

        val smoothed2d = smoother.smooth(detection.landmarks2d, timestamp)
        val smoothed3d = if (detection.landmarks3d.isNotEmpty()) detection.landmarks3d else null

        val diag = PostureLogic.analyzeWithDiagnosis(smoothed2d, smoothed3d, currentFps)
        onResult(diag)
    }

    detector.detect(rotatedBitmap, 0)
    imageProxy.close()
}
