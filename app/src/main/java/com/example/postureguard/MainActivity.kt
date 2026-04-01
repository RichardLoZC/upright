package com.example.postureguard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.postureguard.ui.theme.PostureGuardTheme
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var poseDetector: PoseDetector
    private lateinit var tts: TextToSpeech
    private var isTtsReady = false
    private var lastAlertTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        poseDetector = PoseDetector(this)
        tts = TextToSpeech(this, this)

        setContent {
            PostureGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PostureGuardApp(
                        poseDetector = poseDetector,
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
        // Cooldown 5 seconds
        if (now - lastAlertTime < 5000) return

        if (isTtsReady) {
            val message = when (state) {
                PostureState.BAD_TILT_LEFT -> "头向左歪了"
                PostureState.BAD_TILT_RIGHT -> "头向右歪了"
                PostureState.BAD_SLOUCH -> "坐直一点"
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
        CameraScreen(poseDetector, onPostureDetected)
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("需要相机权限来监控坐姿")
        }
    }
}

@Composable
fun CameraScreen(
    poseDetector: PoseDetector,
    onPostureDetected: (PostureState) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var currentPosture by remember { mutableStateOf(PostureState.NO_PERSON) }
    var isEcoMode by remember { mutableStateOf(false) } // Black screen mode

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isEcoMode) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        
                        val preview = Preview.Builder().build()
                        preview.setSurfaceProvider(previewView.surfaceProvider)

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                            .build()

                        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                            processImage(imageProxy, poseDetector) { state ->
                                currentPosture = state
                                onPostureDetected(state)
                            }
                        }

                        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (exc: Exception) {
                            Log.e("Camera", "Use case binding failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            )
        } else {
            // Eco Mode (Black Screen)
            Box(modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)) {
                Text(
                    "省电模式运行中\n点击退出",
                    color = Color.DarkGray,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // Overlay UI
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "状态: $currentPosture",
                color = if (currentPosture == PostureState.GOOD) Color.Green else Color.Red,
                fontSize = 24.sp,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f))
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(onClick = { isEcoMode = !isEcoMode }) {
                Text(if (isEcoMode) "显示画面" else "省电模式 (黑屏)")
            }
        }
    }
}

fun processImage(
    imageProxy: ImageProxy,
    detector: PoseDetector,
    onResult: (PostureState) -> Unit
) {
    val bitmap = imageProxy.toBitmap()
    
    // Rotate bitmap if needed (front camera is usually 270)
    // toBitmap() respects rotation info in ImageProxy? 
    // Usually yes. But let's check rotationDegrees.
    val rotation = imageProxy.imageInfo.rotationDegrees
    val matrix = Matrix()
    matrix.postRotate(rotation.toFloat())
    // Mirror for front camera
    matrix.postScale(-1f, 1f) 

    val rotatedBitmap = Bitmap.createBitmap(
        bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
    )

    detector.setListener { result ->
        val state = PostureLogic.analyze(result)
        onResult(state)
    }
    
    detector.detect(rotatedBitmap, 0) // Already rotated
    
    imageProxy.close()
}
