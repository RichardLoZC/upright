package com.example.postureguard

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.core.content.getSystemService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

data class UiState(
    val currentPosture: PostureState = PostureState.NO_PERSON,
    val diagnosis: PostureDiagnosis? = null,
    val skeletonLandmarks: List<Landmark3D>? = null,
    val isEcoMode: Boolean = false,
    val showDebug: Boolean = true,
    val isCalibrating: Boolean = false,
    val calibCountdown: Int = 0,
    val calibration: PostureLogic.CalibrationProfile? = null,
    val sessionGoodDuration: Long = 0,
    val sessionBadDuration: Long = 0,
    val sessionStartTime: Long = System.currentTimeMillis()
)

class PostureGuardViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val poseDetector = PoseDetector(application)
    private val smoother = LandmarkSmoother(minCutoff = 1.0, beta = 0.007)
    private val frameProcessor = FrameProcessor(poseDetector, smoother)
    private val stateMachine = PostureStateMachine()

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var lastAlertTime = 0L

    private val calibSamples = mutableListOf<Pair<List<Landmark3D>, List<Landmark3D>?>>()
    private val ecoModeFlag = java.util.concurrent.atomic.AtomicBoolean(false)
    private val ecoFrameSkip = java.util.concurrent.atomic.AtomicInteger(0)
    private val calibrationStore = CalibrationStore(application)
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (application.getSystemService(VibratorManager::class.java))?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Vibrator::class.java)
    }

    private var lastStatsUpdateTime = System.currentTimeMillis()
    private var lastPostureForStats = PostureState.NO_PERSON
    private var lastUiUpdateTime = 0L
    private val uiUpdateIntervalMs = 33L // ~30fps UI updates max

    private val forwardHeadMessages = listOf(
        "头太靠前了，收下巴",
        "注意头部前倾",
        "把头收回来一些",
        "下巴微收，头部后移"
    )
    private val hunchbackMessages = listOf(
        "驼背了，挺直背部",
        "挺胸，展开肩膀",
        "背挺直一些",
        "肩膀向后打开"
    )
    private val tiltLeftMessages = listOf("头向左歪了", "头部偏左了")
    private val tiltRightMessages = listOf("头向右歪了", "头部偏右了")
    private val slouchMessages = listOf("肩膀不平，坐直一点", "左右肩膀不平衡")

    val ecoEnabled get() = ecoModeFlag
    val ecoSkipCounter get() = ecoFrameSkip

    init {
        tts = TextToSpeech(application, this)
        viewModelScope.launch {
            calibrationStore.load()?.let { saved ->
                _uiState.value = _uiState.value.copy(calibration = saved)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.CHINA) ?: TextToSpeech.ERROR
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Chinese language not supported")
            } else {
                isTtsReady = true
            }
        } else {
            Log.e("TTS", "Initialization failed")
        }
    }

    fun startCalibration() {
        if (_uiState.value.isCalibrating) return
        calibSamples.clear()
        _uiState.value = _uiState.value.copy(isCalibrating = true, calibCountdown = 3)

        viewModelScope.launch {
            repeat(3) {
                _uiState.value = _uiState.value.copy(calibCountdown = 3 - it)
                kotlinx.coroutines.delay(1000)
            }
            val profile = PostureLogic.calibrateFromSamples(calibSamples.toList())
            if (profile != null) {
                stateMachine.update(PostureState.GOOD)
                _uiState.value = _uiState.value.copy(calibration = profile)
                calibrationStore.save(profile)
            }
            calibSamples.clear()
            _uiState.value = _uiState.value.copy(isCalibrating = false, calibCountdown = 0)
        }
    }

    fun toggleEcoMode() {
        val newVal = !_uiState.value.isEcoMode
        ecoModeFlag.set(newVal)
        _uiState.value = _uiState.value.copy(isEcoMode = newVal)
    }

    fun toggleDebug() {
        _uiState.value = _uiState.value.copy(showDebug = !_uiState.value.showDebug)
    }

    fun processFrame(imageProxy: ImageProxy) {
        if (ecoModeFlag.get()) {
            val skip = ecoFrameSkip.incrementAndGet()
            if (skip % 4 != 0) {
                imageProxy.close()
                return
            }
        }

        val calibration = _uiState.value.calibration
        val isCalibrating = _uiState.value.isCalibrating

        frameProcessor.processImage(imageProxy, calibration) { diag, landmarks, raw2d, raw3d ->
            if (isCalibrating && landmarks != null) {
                calibSamples.add(Pair(raw2d, raw3d))
            }
            val debounced = stateMachine.update(diag.state)

            vibrateOnStateChange(debounced)
            speakAlert(debounced)

            val now = SystemClock.uptimeMillis()
            val stateChanged = debounced != _uiState.value.currentPosture
            if (stateChanged || now - lastUiUpdateTime >= uiUpdateIntervalMs) {
                updateSessionStats(debounced)
                _uiState.value = _uiState.value.copy(
                    currentPosture = debounced,
                    diagnosis = diag.copy(state = debounced),
                    skeletonLandmarks = landmarks
                )
                lastUiUpdateTime = now
            }
        }
    }

    private fun updateSessionStats(newState: PostureState) {
        val now = System.currentTimeMillis()
        val elapsed = (now - lastStatsUpdateTime) / 1000
        if (elapsed > 0) {
            val isGood = lastPostureForStats == PostureState.GOOD
            val isBad = lastPostureForStats != PostureState.GOOD && lastPostureForStats != PostureState.NO_PERSON

            if (isGood) {
                _uiState.value = _uiState.value.copy(
                    sessionGoodDuration = _uiState.value.sessionGoodDuration + elapsed
                )
            } else if (isBad) {
                _uiState.value = _uiState.value.copy(
                    sessionBadDuration = _uiState.value.sessionBadDuration + elapsed
                )
            }
            lastStatsUpdateTime = now
            lastPostureForStats = newState
        }
    }

    private var lastVibratedState = PostureState.NO_PERSON

    private fun vibrateOnStateChange(state: PostureState) {
        if (state == lastVibratedState) return
        lastVibratedState = state

        val isBad = state != PostureState.GOOD && state != PostureState.NO_PERSON
        if (isBad) {
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(150, 100))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(150)
                }
            }
        }
    }

    private fun speakAlert(state: PostureState) {
        val now = System.currentTimeMillis()
        if (now - lastAlertTime < 5000) return

        if (isTtsReady) {
            val message = when (state) {
                PostureState.BAD_TILT_LEFT -> tiltLeftMessages.random()
                PostureState.BAD_TILT_RIGHT -> tiltRightMessages.random()
                PostureState.BAD_SLOUCH -> slouchMessages.random()
                PostureState.BAD_FORWARD_HEAD -> forwardHeadMessages.random()
                PostureState.BAD_HUNCHBACK -> hunchbackMessages.random()
                else -> null
            }
            if (message != null) {
                tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
                lastAlertTime = now
            }
        }
    }

    fun stopTts() {
        tts?.stop()
    }

    fun startForegroundMonitor() {
        val app = getApplication<Application>()
        val intent = Intent(app, PostureMonitorService::class.java).apply {
            action = PostureMonitorService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            app.startForegroundService(intent)
        } else {
            app.startService(intent)
        }
    }

    fun stopForegroundMonitor() {
        val app = getApplication<Application>()
        val intent = Intent(app, PostureMonitorService::class.java).apply {
            action = PostureMonitorService.ACTION_STOP
        }
        app.stopService(intent)
    }

    override fun onCleared() {
        super.onCleared()
        poseDetector.close()
        tts?.stop()
        tts?.shutdown()
    }
}

class FrameProcessor(
    private val detector: PoseDetector,
    private val smoother: LandmarkSmoother
) {
    private var frameCount = 0
    private var fpsTimestamp = SystemClock.uptimeMillis()
    private var currentFps = 0.0
    private var resultToken = 0

    fun processImage(
        imageProxy: ImageProxy,
        calibration: PostureLogic.CalibrationProfile?,
        onResult: (PostureDiagnosis, List<Landmark3D>?, List<Landmark3D>, List<Landmark3D>?) -> Unit
    ) {
        val bitmap = imageProxy.toBitmap()

        val rotation = imageProxy.imageInfo.rotationDegrees
        val matrix = Matrix()
        matrix.postRotate(rotation.toFloat())
        matrix.postScale(-1f, 1f)

        val rotatedBitmap = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
        bitmap.recycle()

        val timestamp = SystemClock.uptimeMillis()

        frameCount++
        val elapsed = timestamp - fpsTimestamp
        if (elapsed >= 1000) {
            currentFps = frameCount * 1000.0 / elapsed
            frameCount = 0
            fpsTimestamp = timestamp
        }

        val myToken = ++resultToken
        val fps = currentFps

        detector.setListener { detection ->
            if (myToken != resultToken) return@setListener

            if (detection.landmarks2d.isEmpty()) {
                onResult(PostureDiagnosis(PostureState.NO_PERSON, null, null, false, fps), null, emptyList(), null)
                return@setListener
            }

            val smoothed2d = smoother.smooth(detection.landmarks2d, timestamp)
            val smoothed3d = if (detection.landmarks3d.isNotEmpty()) detection.landmarks3d else null

            val diag = PostureLogic.analyzeWithDiagnosis(smoothed2d, smoothed3d, fps, calibration)
            onResult(diag, smoothed2d, detection.landmarks2d, smoothed3d)
        }

        detector.detect(rotatedBitmap, 0)
        rotatedBitmap.recycle()
        imageProxy.close()
    }
}
