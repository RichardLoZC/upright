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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

enum class Screen { ONBOARDING, MAIN, SETTINGS, HISTORY }

data class UiState(
    val currentScreen: Screen = Screen.MAIN,
    val currentPosture: PostureState = PostureState.NO_PERSON,
    val diagnosis: PostureDiagnosis? = null,
    val skeletonLandmarks: List<Landmark3D>? = null,
    val isEcoMode: Boolean = false,
    val showDebug: Boolean = true,
    val isCalibrating: Boolean = false,
    val calibCountdown: Int = 0,
    val calibration: PostureLogic.CalibrationProfile? = null,
    val calibrationSuccess: Boolean? = null,
    val isPaused: Boolean = false,
    val pauseRemainingSeconds: Int = 0,
    val settings: SettingsProfile = SettingsProfile(),
    val sessionGoodDuration: Long = 0,
    val sessionBadDuration: Long = 0,
    val sessionStartTime: Long = System.currentTimeMillis(),
    val showPauseSuggestion: Boolean = false,
    val weeklySummary: List<DailySummary> = emptyList(),
    val todaySessions: List<SessionEntity> = emptyList(),
    val currentStreak: Int = 0
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
    private val settingsStore = SettingsStore(application)
    private val db by lazy { AppDatabase.getInstance(application) }

    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (application.getSystemService(VibratorManager::class.java))?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Vibrator::class.java)
    }

    private var lastStatsUpdateTime = System.currentTimeMillis()
    private var lastPostureForStats = PostureState.NO_PERSON
    private var lastUiUpdateTime = 0L
    private val uiUpdateIntervalMs = 33L
    private var pauseJob: Job? = null
    private var calibrationJob: Job? = null
    private val noPersonStartMs = java.util.concurrent.atomic.AtomicLong(0)
    private val noPersonActive = java.util.concurrent.atomic.AtomicBoolean(false)

    private val forwardHeadMessagesZh = listOf(
        "头太靠前了，收下巴", "注意头部前倾", "把头收回来一些", "下巴微收，头部后移"
    )
    private val hunchbackMessagesZh = listOf(
        "驼背了，挺直背部", "挺胸，展开肩膀", "背挺直一些", "肩膀向后打开"
    )
    private val tiltLeftMessagesZh = listOf("头向左歪了", "头部偏左了")
    private val tiltRightMessagesZh = listOf("头向右歪了", "头部偏右了")
    private val slouchMessagesZh = listOf("肩膀不平，坐直一点", "左右肩膀不平衡")

    private val forwardHeadMessagesEn = listOf(
        "Head too far forward, tuck your chin", "Watch your head position",
        "Pull your head back", "Tuck chin, move head back"
    )
    private val hunchbackMessagesEn = listOf(
        "Stop slouching, sit up straight", "Chest up, shoulders back",
        "Straighten your back", "Open your shoulders"
    )
    private val tiltLeftMessagesEn = listOf("Head tilting left", "Your head is leaning left")
    private val tiltRightMessagesEn = listOf("Head tilting right", "Your head is leaning right")
    private val slouchMessagesEn = listOf("Uneven shoulders, sit up straight", "Balance your shoulders")

    val ecoEnabled get() = ecoModeFlag
    val ecoSkipCounter get() = ecoFrameSkip

    init {
        initTts(application)
        viewModelScope.launch {
            val settings = settingsStore.load()
            val calibration = calibrationStore.load()
            val screen = if (!settings.onboardingCompleted) Screen.ONBOARDING else Screen.MAIN
            _uiState.value = _uiState.value.copy(
                settings = settings,
                calibration = calibration,
                currentScreen = screen
            )
        }
    }

    private var ttsRetryCount = 0

    private fun initTts(app: Application) {
        tts = TextToSpeech(app, this)
    }

    override fun onInit(status: Int) {
        Log.d("PostureGuard", "TTS onInit: status=$status")
        if (status == TextToSpeech.SUCCESS) {
            val locale = when (_uiState.value.settings.alertLanguage) {
                AlertLanguage.ZH -> Locale.CHINA
                AlertLanguage.EN -> Locale.US
            }
            val result = tts?.setLanguage(locale) ?: TextToSpeech.ERROR
            Log.d("PostureGuard", "TTS setLanguage result=$result")
            isTtsReady = true
        } else if (ttsRetryCount < 3) {
            ttsRetryCount++
            Log.e("PostureGuard", "TTS init failed ($status), retry $ttsRetryCount/3 in 3s...")
            viewModelScope.launch {
                delay(3000)
                tts?.shutdown()
                tts = TextToSpeech(getApplication(), this@PostureGuardViewModel)
            }
        } else {
            Log.e("PostureGuard", "TTS failed after 3 retries, trying default engine")
            viewModelScope.launch {
                delay(2000)
                tts?.shutdown()
                tts = TextToSpeech(getApplication(), this@PostureGuardViewModel, "com.google.android.tts")
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsStore.saveOnboarding(true)
            _uiState.value = _uiState.value.copy(
                currentScreen = Screen.MAIN,
                settings = _uiState.value.settings.copy(onboardingCompleted = true)
            )
        }
    }

    fun navigateTo(screen: Screen) {
        if (screen == Screen.HISTORY) loadHistoryData()
        _uiState.value = _uiState.value.copy(currentScreen = screen)
    }

    fun startCalibration() {
        if (_uiState.value.isCalibrating) return
        if (_uiState.value.currentPosture == PostureState.NO_PERSON) {
            _uiState.value = _uiState.value.copy(calibrationSuccess = false)
            return
        }
        calibSamples.clear()
        _uiState.value = _uiState.value.copy(isCalibrating = true, calibCountdown = 3)

        calibrationJob = viewModelScope.launch {
            repeat(3) {
                _uiState.value = _uiState.value.copy(calibCountdown = 3 - it)
                delay(1000)
                // Abort if person leaves during calibration
                if (_uiState.value.currentPosture == PostureState.NO_PERSON) {
                    _uiState.value = _uiState.value.copy(
                        isCalibrating = false, calibCountdown = 0,
                        calibrationSuccess = false
                    )
                    calibSamples.clear()
                    return@launch
                }
            }
            val profile = PostureLogic.calibrateFromSamples(calibSamples.toList())
            if (profile != null) {
                stateMachine.update(PostureState.GOOD)
                _uiState.value = _uiState.value.copy(
                    calibration = profile,
                    calibrationSuccess = true
                )
                calibrationStore.save(profile)
            } else {
                _uiState.value = _uiState.value.copy(calibrationSuccess = false)
            }
            calibSamples.clear()
            _uiState.value = _uiState.value.copy(isCalibrating = false, calibCountdown = 0)
        }
    }

    fun consumeCalibrationResult() {
        _uiState.value = _uiState.value.copy(calibrationSuccess = null)
    }

    fun resetCalibration() {
        viewModelScope.launch {
            calibrationStore.clear()
            _uiState.value = _uiState.value.copy(calibration = null)
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

    fun togglePause() {
        if (_uiState.value.isPaused) {
            resumeMonitoring()
        } else {
            _uiState.value = _uiState.value.copy(isPaused = true)
            pauseJob = viewModelScope.launch {
                val totalSeconds = _uiState.value.settings.autoResumeMinutes * 60
                for (i in totalSeconds downTo 1) {
                    _uiState.value = _uiState.value.copy(pauseRemainingSeconds = i)
                    delay(1000)
                }
                resumeMonitoring()
            }
        }
    }

    fun resumeMonitoring() {
        pauseJob?.cancel()
        pauseJob = null
        noPersonActive.set(false)
        sessionSaved = false
        lastStatsUpdateTime = System.currentTimeMillis()
        _uiState.value = _uiState.value.copy(isPaused = false, pauseRemainingSeconds = 0, showPauseSuggestion = false)
    }

    fun dismissPauseSuggestion() {
        _uiState.value = _uiState.value.copy(showPauseSuggestion = false)
    }

    fun updateAlertInterval(seconds: Int) {
        viewModelScope.launch {
            settingsStore.saveAlertInterval(seconds)
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings.copy(alertIntervalSeconds = seconds)
            )
        }
    }

    fun updateSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.saveSoundEnabled(enabled)
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings.copy(soundEnabled = enabled)
            )
        }
    }

    fun updateVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.saveVibrationEnabled(enabled)
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings.copy(vibrationEnabled = enabled)
            )
        }
    }

    fun updateSensitivity(level: SensitivityLevel) {
        viewModelScope.launch {
            settingsStore.saveSensitivity(level)
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings.copy(sensitivityLevel = level)
            )
        }
    }

    fun updateLanguage(language: AlertLanguage) {
        viewModelScope.launch {
            settingsStore.saveLanguage(language)
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings.copy(alertLanguage = language)
            )
            if (isTtsReady) {
                val locale = when (language) {
                    AlertLanguage.ZH -> Locale.CHINA
                    AlertLanguage.EN -> Locale.US
                }
                tts?.setLanguage(locale)
            }
        }
    }

    fun updateAutoResumeMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsStore.saveAutoResumeMinutes(minutes)
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings.copy(autoResumeMinutes = minutes)
            )
        }
    }

    fun processFrame(imageProxy: ImageProxy) {
        if (_uiState.value.isPaused) {
            imageProxy.close()
            return
        }

        if (ecoModeFlag.get()) {
            val skip = ecoFrameSkip.incrementAndGet()
            if (skip % 4 != 0) {
                imageProxy.close()
                return
            }
        }

        val calibration = _uiState.value.calibration
        val isCalibrating = _uiState.value.isCalibrating
        val sensitivity = _uiState.value.settings.sensitivityMultiplier

        frameProcessor.processImage(imageProxy, calibration, sensitivity) { diag, landmarks, raw2d, raw3d ->
            if (isCalibrating && landmarks != null) {
                calibSamples.add(Pair(raw2d, raw3d))
            }
            val debounced = stateMachine.update(diag.state)

            if (_uiState.value.settings.vibrationEnabled) {
                vibrateOnStateChange(debounced)
            }
            if (_uiState.value.settings.soundEnabled) {
                speakAlert(debounced)
            }

            // NO_PERSON timeout for pause suggestion
            if (debounced == PostureState.NO_PERSON) {
                if (!noPersonActive.getAndSet(true)) {
                    noPersonStartMs.set(SystemClock.uptimeMillis())
                }
                if (SystemClock.uptimeMillis() - noPersonStartMs.get() > 120_000 && !_uiState.value.showPauseSuggestion) {
                    _uiState.value = _uiState.value.copy(showPauseSuggestion = true)
                }
            } else {
                noPersonActive.set(false)
                if (_uiState.value.showPauseSuggestion) {
                    _uiState.value = _uiState.value.copy(showPauseSuggestion = false)
                }
            }

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

                // Update notification
                if (stateChanged) {
                    updateNotificationState(debounced)
                }
            }
        }
    }

    private fun updateNotificationState(state: PostureState) {
        val stateText = when (state) {
            PostureState.GOOD -> "坐姿良好"
            PostureState.BAD_TILT_LEFT -> "头部向左歪斜"
            PostureState.BAD_TILT_RIGHT -> "头部向右歪斜"
            PostureState.BAD_SLOUCH -> "肩膀不平"
            PostureState.BAD_FORWARD_HEAD -> "头部前倾"
            PostureState.BAD_HUNCHBACK -> "驼背"
            PostureState.NO_PERSON -> "未检测到人"
        }
        val app = getApplication<Application>()
        val intent = Intent(app, PostureMonitorService::class.java).apply {
            action = PostureMonitorService.ACTION_UPDATE
            putExtra(PostureMonitorService.EXTRA_STATE, stateText)
        }
        app.startService(intent)
    }

    private fun updateSessionStats(newState: PostureState) {
        if (_uiState.value.isPaused) return
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
        if (now - lastAlertTime < _uiState.value.settings.alertIntervalSeconds * 1000L) return

        if (isTtsReady) {
            val useEn = _uiState.value.settings.alertLanguage == AlertLanguage.EN
            val message = when (state) {
                PostureState.BAD_TILT_LEFT -> if (useEn) tiltLeftMessagesEn.random() else tiltLeftMessagesZh.random()
                PostureState.BAD_TILT_RIGHT -> if (useEn) tiltRightMessagesEn.random() else tiltRightMessagesZh.random()
                PostureState.BAD_SLOUCH -> if (useEn) slouchMessagesEn.random() else slouchMessagesZh.random()
                PostureState.BAD_FORWARD_HEAD -> if (useEn) forwardHeadMessagesEn.random() else forwardHeadMessagesZh.random()
                PostureState.BAD_HUNCHBACK -> if (useEn) hunchbackMessagesEn.random() else hunchbackMessagesZh.random()
                else -> null
            }
            if (message != null) {
                tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
                lastAlertTime = now
            }
        }
    }

    private var sessionSaved = false

    fun saveCurrentSession() {
        if (sessionSaved) return
        val state = _uiState.value
        if (state.sessionGoodDuration == 0L && state.sessionBadDuration == 0L) return
        sessionSaved = true
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdf.format(Date(state.sessionStartTime))
            db.sessionDao().insert(
                SessionEntity(
                    date = date,
                    startTime = state.sessionStartTime,
                    endTime = System.currentTimeMillis(),
                    goodDurationSeconds = state.sessionGoodDuration,
                    badDurationSeconds = state.sessionBadDuration,
                    calibrationUsed = state.calibration != null
                )
            )
        }
    }

    fun loadHistoryData() {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val cal = Calendar.getInstance()
            val endDate = sdf.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, -6)
            val startDate = sdf.format(cal.time)
            val today = endDate

            val summary = db.sessionDao().getWeeklySummary(startDate, endDate)
            val sessions = db.sessionDao().getSessionsForDate(today)

            // Calculate streak: count consecutive days ending today with >= 80% good
            var streak = 0
            val streakStartCal = Calendar.getInstance()
            streakStartCal.add(Calendar.DAY_OF_YEAR, -60) // Look back max 60 days
            val streakSummaries = db.sessionDao().getDailySummariesRange(
                sdf.format(streakStartCal.time), today
            )
            for (daySummary in streakSummaries.reversed()) {
                val total = daySummary.goodDurationSeconds + daySummary.badDurationSeconds
                if (total == 0L) break
                val ratio = daySummary.goodDurationSeconds.toDouble() / total
                if (ratio >= 0.8) streak++ else break
            }

            _uiState.value = _uiState.value.copy(
                weeklySummary = summary,
                todaySessions = sessions,
                currentStreak = streak
            )
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
        calibrationJob?.cancel()
        pauseJob?.cancel()
        saveCurrentSession()
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
        sensitivityMultiplier: Double = 1.0,
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

            val diag = PostureLogic.analyzeWithDiagnosis(smoothed2d, smoothed3d, fps, calibration, sensitivityMultiplier)
            onResult(diag, smoothed2d, detection.landmarks2d, smoothed3d)
        }

        detector.detect(rotatedBitmap, 0)
        rotatedBitmap.recycle()
        imageProxy.close()
    }
}
