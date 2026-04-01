<img src="https://r2cdn.perplexity.ai/pplx-full-logo-primary-dark%402x.png" style="height:64px;margin-right:32px"/>

# 好，再做一次 prompt

好的！我为你重新整理一套**完整优化的 Vibe Coding Prompts**，针对**中国大陆环境 + 完全离线版 + 局域网同步**的方案。

***

# 儿童坐姿监控 APP - 完整 Vibe Coding Prompts

## 项目概述 Prompt

```
PROJECT OVERVIEW - Kids Posture Monitor App

Create a complete React Native mobile application for monitoring children's sitting posture with the following specifications:

Target Market: Parents in mainland China
Architecture: Offline-first with optional local WiFi sync
Languages: Chinese UI, bilingual code comments

Key Features:
1. Real-time posture detection using MediaPipe Pose (bundled locally)
2. Two-app architecture:
   - Monitor Device App (runs on old/spare phone)
   - Parent Control App (runs on parent's phone)
3. Child-friendly voice alerts with cartoon characters
4. Gamification system (stars, virtual pet, achievements)
5. Local data storage with SQLite
6. Optional WiFi sync between devices (no internet required)
7. Comprehensive statistics and reports

Technical Constraints:
- Must work 100% offline (no Firebase, no Google services)
- MediaPipe model bundled in APK
- All resources must be local or use China-accessible CDN
- Support Android 7.0+ (API 24+)
- Optimized for low-end devices (2018+ phones)
- APK size target: 25-35 MB after optimization

Performance Requirements:
- Run at 5-10 FPS depending on device capability
- CPU usage < 40% on low-end devices
- Memory footprint < 400 MB
- Battery consumption ~350-450 mAh/hour
- No overheating on 2+ hour sessions

Development Stack:
- React Native 0.73+
- TypeScript for type safety
- SQLite for local database
- react-native-vision-camera for camera access
- MediaPipe Pose Lite (bundled)
- react-native-tts for voice alerts (system TTS)
- WebSocket/HTTP for local WiFi sync

Generate the complete project structure and configuration.
```


***

## 阶段 1: 项目初始化

### Prompt 1.1: 创建完整项目结构

```
Create a React Native project structure for the Kids Posture Monitor app.

Project Setup:
- Initialize with React Native CLI (not Expo, to avoid overseas dependencies)
- Project name: "KidsPostureMonitor"
- Enable TypeScript
- Configure for China-based development (use npm Taobao mirror)

Folder Structure:
/KidsPostureMonitor
├── /android                    # Native Android code
├── /ios                        # iOS (optional, focus on Android)
├── /assets                     # Local resources
│   ├── /models
│   │   └── pose_landmarker_lite.task  # MediaPipe model (3.5 MB)
│   ├── /sounds                 # Alert sound effects
│   │   ├── gentle_bell.mp3
│   │   ├── cute_beep.mp3
│   │   ├── success_chime.mp3
│   │   └── cartoon_boing.mp3
│   ├── /images                 # UI assets
│   │   ├── /avatars            # Child avatars
│   │   ├── /pets               # Virtual pet sprites
│   │   └── /badges             # Achievement badges
│   └── /animations             # Lottie animations
├── /src
│   ├── /apps                   # Two separate app entry points
│   │   ├── MonitorApp.tsx      # Device monitoring app
│   │   └── ParentApp.tsx       # Parent control app
│   ├── /screens
│   │   ├── /monitor            # Monitor device screens
│   │   │   ├── CameraScreen.tsx
│   │   │   ├── SetupScreen.tsx
│   │   │   └── StandbyScreen.tsx
│   │   ├── /parent             # Parent app screens
│   │   │   ├── DashboardScreen.tsx
│   │   │   ├── StatisticsScreen.tsx
│   │   │   ├── SettingsScreen.tsx
│   │   │   ├── RewardsScreen.tsx
│   │   │   └── DevicePairingScreen.tsx
│   │   └── /shared             # Shared screens
│   │       └── SplashScreen.tsx
│   ├── /components             # Reusable components
│   │   ├── /charts
│   │   ├── /alerts
│   │   ├── /gamification
│   │   └── /common
│   ├── /services               # Business logic
│   │   ├── PostureDetectionService.ts    # MediaPipe integration
│   │   ├── DatabaseService.ts            # SQLite operations
│   │   ├── AlertService.ts               # Voice/sound alerts
│   │   ├── SyncService.ts                # WiFi local sync
│   │   ├── PerformanceService.ts         # Device optimization
│   │   └── GamificationService.ts        # Rewards logic
│   ├── /utils                  # Helper functions
│   │   ├── postureAnalysis.ts            # Angle calculations
│   │   ├── deviceDetection.ts            # Device capability check
│   │   ├── thermalManager.ts             # Heat management
│   │   └── dataExport.ts                 # CSV export
│   ├── /constants
│   │   ├── thresholds.ts                 # Posture thresholds
│   │   ├── config.ts                     # App configuration
│   │   └── alerts.ts                     # Alert phrases
│   ├── /types                  # TypeScript definitions
│   │   ├── posture.d.ts
│   │   ├── database.d.ts
│   │   └── settings.d.ts
│   ├── /hooks                  # Custom React hooks
│   │   ├── useCamera.ts
│   │   ├── usePoseDetection.ts
│   │   └── useLocalSync.ts
│   └── /navigation             # Navigation setup
│       ├── MonitorNavigator.tsx
│       └── ParentNavigator.tsx
├── /scripts                    # Build scripts
│   ├── downloadMediaPipeModel.js    # Pre-build model fetch
│   └── optimizeAssets.js            # Asset compression
├── package.json
├── tsconfig.json
├── babel.config.js
├── metro.config.js
└── README.md

Package Dependencies:
{
  "dependencies": {
    "react": "18.2.0",
    "react-native": "0.73.0",
    "@react-navigation/native": "^6.1.9",
    "@react-navigation/stack": "^6.3.20",
    "react-native-vision-camera": "^3.6.0",
    "react-native-sqlite-storage": "^6.0.1",
    "react-native-tts": "^4.1.0",
    "react-native-sound": "^0.11.2",
    "react-native-fs": "^2.20.0",
    "react-native-device-info": "^10.11.0",
    "react-native-chart-kit": "^6.12.0",
    "react-native-svg": "^14.1.0",
    "react-native-linear-gradient": "^2.8.3",
    "lottie-react-native": "^6.4.1",
    "react-native-permissions": "^4.0.0",
    "@mediapipe/tasks-vision": "^0.10.8"
  },
  "devDependencies": {
    "@types/react": "^18.2.0",
    "@types/react-native": "^0.73.0",
    "typescript": "^5.3.0",
    "metro-react-native-babel-preset": "^0.77.0"
  }
}

Configuration Notes:
1. Configure npm to use Taobao registry (https://registry.npmmirror.com)
2. Set up ProGuard for release builds (code obfuscation)
3. Enable Hermes JavaScript engine for better performance
4. Configure Android build.gradle for APK splitting by architecture
5. Add pre-build script to download MediaPipe model from GitHub releases

Generate all necessary configuration files with Chinese comments for maintainability.
```


### Prompt 1.2: 数据库 Schema 设计

```
Create a SQLite database schema for local data storage.

File: src/services/DatabaseService.ts

Database Name: posture_monitor.db

Tables:

1. children (子女信息表)
   - id: INTEGER PRIMARY KEY AUTOINCREMENT
   - name: TEXT NOT NULL              -- 姓名
   - age: INTEGER                     -- 年龄
   - avatar: TEXT                     -- 头像文件名
   - height_cm: INTEGER               -- 身高(可选,用于校准)
   - created_at: INTEGER              -- 创建时间戳
   - updated_at: INTEGER

2. sessions (监控会话表)
   - id: INTEGER PRIMARY KEY AUTOINCREMENT
   - child_id: INTEGER                -- 关联子女
   - start_time: INTEGER              -- 开始时间戳
   - end_time: INTEGER                -- 结束时间戳
   - duration_seconds: INTEGER        -- 持续时长(秒)
   - avg_posture_score: REAL          -- 平均坐姿评分(0-100)
   - good_posture_percentage: REAL    -- 良好坐姿占比
   - alert_count: INTEGER             -- 提醒次数
   - device_name: TEXT                -- 监控设备名称

3. posture_logs (姿态详细记录表)
   - id: INTEGER PRIMARY KEY AUTOINCREMENT
   - session_id: INTEGER              -- 关联会话
   - timestamp: INTEGER               -- 记录时间戳
   - posture_status: TEXT             -- 'GOOD' | 'BAD'
   - neck_angle: REAL                 -- 颈部角度
   - spine_angle: REAL                -- 脊柱角度
   - head_forward_distance: REAL      -- 头部前倾距离
   - shoulder_symmetry: REAL          -- 肩膀对称性
   - issues_detected: TEXT            -- JSON数组: ['slouching', 'tilted']

4. alerts (提醒记录表)
   - id: INTEGER PRIMARY KEY AUTOINCREMENT
   - session_id: INTEGER
   - timestamp: INTEGER
   - alert_type: TEXT                 -- 'VOICE' | 'SOUND' | 'VISUAL'
   - alert_message: TEXT              -- 提醒内容

5. settings (设置表)
   - id: INTEGER PRIMARY KEY AUTOINCREMENT
   - child_id: INTEGER
   - detection_sensitivity: TEXT      -- 'LENIENT' | 'MODERATE' | 'STRICT'
   - alert_delay_seconds: INTEGER     -- 提醒延迟(默认10秒)
   - alert_interval_seconds: INTEGER  -- 提醒间隔(默认60秒)
   - voice_character: TEXT            -- 'SUPERHERO' | 'PRINCESS' | 'ROBOT'
   - voice_language: TEXT             -- 'zh-CN' | 'en-US'
   - enable_gamification: INTEGER     -- 是否启用游戏化(0/1)
   - stars_per_30min: INTEGER         -- 每30分钟奖励星星数

6. rewards (奖励记录表)
   - id: INTEGER PRIMARY KEY AUTOINCREMENT
   - child_id: INTEGER
   - earned_at: INTEGER               -- 获得时间
   - reward_type: TEXT                -- 'STAR' | 'BADGE' | 'PET_LEVEL'
   - reward_value: INTEGER            -- 数值(星星数/徽章ID等)
   - description: TEXT                -- 描述

7. achievements (成就表)
   - id: INTEGER PRIMARY KEY AUTOINCREMENT
   - child_id: INTEGER
   - achievement_type: TEXT           -- 'PERFECT_HOUR' | 'WEEK_WARRIOR'
   - unlocked_at: INTEGER             -- 解锁时间
   - is_claimed: INTEGER              -- 是否已领取(0/1)

Implement the following methods:

DatabaseService class:
- initDatabase(): Promise<void>           // 初始化数据库
- addChild(child): Promise<number>        // 添加子女
- startSession(childId): Promise<number>  // 开始监控会话
- endSession(sessionId): Promise<void>    // 结束会话
- logPosture(sessionLog): Promise<void>   // 记录姿态数据
- logAlert(alertData): Promise<void>      // 记录提醒
- getChildStatistics(childId, dateRange): Promise<Statistics>
- exportToCSV(childId, dateRange): Promise<string>  // 导出CSV
- cleanOldData(daysToKeep): Promise<void>  // 清理旧数据(保留90天)

Use react-native-sqlite-storage library.
Add transaction support for batch inserts (performance optimization).
Include database migration system for future schema updates.
Add comprehensive error handling with Chinese error messages.
```


***

## 阶段 2: 核心检测引擎

### Prompt 2.1: MediaPipe 本地化集成

```
Create a MediaPipe Pose detection service that works 100% offline.

File: src/services/PostureDetectionService.ts

Requirements:

1. Load MediaPipe model from local assets (bundled in APK)
   - Model file: assets/models/pose_landmarker_lite.task (3.5 MB)
   - No network requests during initialization
   - Copy model from assets to app's document directory on first run
   - Cache model path for subsequent launches

2. Initialize PoseLandmarker with optimal settings:
   - Running mode: VIDEO
   - Num poses: 1 (single person detection)
   - Min pose detection confidence: 0.5
   - Min tracking confidence: 0.5
   - Delegate: GPU (if available, fallback to CPU)

3. Main detection method: detectPose(frame, timestamp)
   Input: Camera frame as base64 or ImageData
   Output: PoseResult {
     landmarks: Array<{x, y, z, visibility}> (33 points)
     worldLandmarks: Array<{x, y, z}> (3D coordinates)
     timestamp: number
   }

4. Key landmarks mapping (MediaPipe indices):
   const LANDMARKS = {
     NOSE: 0,
     LEFT_EYE: 2,
     RIGHT_EYE: 5,
     LEFT_EAR: 7,
     RIGHT_EAR: 8,
     LEFT_SHOULDER: 11,
     RIGHT_SHOULDER: 12,
     LEFT_ELBOW: 13,
     RIGHT_ELBOW: 14,
     LEFT_WRIST: 15,
     RIGHT_WRIST: 16,
     LEFT_HIP: 23,
     RIGHT_HIP: 24,
     LEFT_KNEE: 25,
     RIGHT_KNEE: 26
   };

5. Error handling:
   - NO_PERSON_DETECTED: Return null, don't alert
   - LOW_VISIBILITY: Skip frame if key points not visible
   - MODEL_LOAD_FAILED: Fallback to simple motion detection
   - PERFORMANCE_DEGRADED: Auto-reduce FPS

6. Performance optimization:
   - Reuse model instance (singleton pattern)
   - Implement frame skipping based on device performance
   - Release resources when app backgrounded
   - Use Worker thread for heavy computation (if available)

Implementation:

class PostureDetectionService {
  private poseLandmarker: PoseLandmarker | null = null;
  private isInitialized: boolean = false;
  private lastDetectionTime: number = 0;
  private skipFrameCount: number = 0;

  async initialize(performanceProfile: 'LOW' | 'MEDIUM' | 'HIGH'): Promise<void>
  async detectPose(imageData: ImageData, timestamp: number): Promise<PoseResult | null>
  isPersonVisible(landmarks: Landmarks): boolean
  release(): void
}

Add detailed Chinese comments explaining MediaPipe concepts.
Include fallback mechanism if MediaPipe fails to load.
Log performance metrics for debugging (FPS, latency).
```


### Prompt 2.2: 坐姿分析算法

```
Create the core posture analysis algorithm based on biomechanics principles.

File: src/utils/postureAnalysis.ts

Implement the following analysis functions:

1. calculateAngle(p1: Point, p2: Point, p3: Point): number
   - Calculate angle in degrees between three points
   - Use vector dot product formula
   - Return value: 0-180 degrees

2. analyzePosture(landmarks: Landmarks): PostureAnalysis
   Main analysis function that evaluates sitting posture.

   Calculate these metrics:

   A. 颈部角度 (Neck Angle):
      - Points: NOSE → SHOULDER_MIDPOINT → HIP_MIDPOINT
      - Good: > 150° (head upright)
      - Bad: < 150° (head drooping/forward)
      - Critical: < 130° (severe slouch)

   B. 脊柱角度 (Spine Angle):
      - Points: SHOULDER_MIDPOINT → HIP_MIDPOINT → KNEE_MIDPOINT (estimated)
      - Good: > 160° (straight back)
      - Bad: < 160° (hunched back)
      - Critical: < 140° (severe hunch)

   C. 头部前倾 (Head Forward Distance):
      - Horizontal distance: NOSE to SHOULDER_MIDPOINT
      - Normalize by shoulder width
      - Good: < 0.1 (normalized units)
      - Bad: > 0.1 (head too far forward)
      - Critical: > 0.2 (turtle neck posture)

   D. 肩膀对称性 (Shoulder Symmetry):
      - Y-coordinate difference: LEFT_SHOULDER vs RIGHT_SHOULDER
      - Normalize by shoulder width
      - Good: < 0.05 (shoulders level)
      - Bad: > 0.05 (tilted to one side)
      - Critical: > 0.1 (significant tilt)

   E. 身体倾斜 (Body Tilt):
      - Angle between shoulder line and horizontal
      - Good: < 5° (straight)
      - Bad: > 5° (leaning)
      - Critical: > 10° (severe lean)

   Return object:
   {
     status: 'GOOD' | 'BAD' | 'CRITICAL',
     overallScore: number (0-100),
     issues: Array<'SLOUCHING' | 'HEAD_FORWARD' | 'TILTED_LEFT' | 'TILTED_RIGHT' | 'HUNCHED'>,
     details: {
       neckAngle: { value: number, status: string, threshold: number },
       spineAngle: { value: number, status: string, threshold: number },
       headForward: { value: number, status: string, threshold: number },
       shoulderSymmetry: { value: number, status: string, threshold: number },
       bodyTilt: { value: number, status: string, threshold: number }
     },
     recommendations: string[] // Chinese suggestions
   }

3. Helper functions:
   - getMidpoint(p1: Point, p2: Point): Point
   - getDistance(p1: Point, p2: Point): number
   - normalizeCoordinates(landmarks: Landmarks): NormalizedLandmarks
   - isKeyPointVisible(landmark: Landmark, minVisibility = 0.5): boolean
   - getShoulderWidth(landmarks: Landmarks): number

4. Scoring algorithm:
   - Weight each metric (neck: 30%, spine: 30%, head: 25%, symmetry: 15%)
   - Calculate weighted average
   - Apply penalties for multiple issues
   - Score range: 0 (worst) to 100 (perfect)

5. Recommendations generation:
   Based on detected issues, suggest corrections in Chinese:
   - SLOUCHING: "请挺直背部，想象头顶有根线在拉着你"
   - HEAD_FORWARD: "头部太靠前了，请将下巴轻轻往后收"
   - TILTED: "身体有点歪斜，请调整坐姿让两肩保持水平"
   - HUNCHED: "背部太弯了，请坐直并放松肩膀"

Use TypeScript with detailed type definitions.
Add extensive comments explaining biomechanics reasoning.
Include unit tests for angle calculation accuracy.
Visualize thresholds with ASCII diagrams in comments.
```


### Prompt 2.3: 设备性能自适应

```
Create a device performance detection and optimization system.

File: src/services/PerformanceService.ts

Features:

1. detectDeviceCapability(): DeviceProfile
   Automatically detect device performance level.
   
   Check:
   - CPU cores count (DeviceInfo.getCpuCores())
   - Total RAM (DeviceInfo.getTotalMemory())
   - Chipset model (DeviceInfo.getChipset())
   - Android API level (DeviceInfo.getApiLevel())
   - GPU availability (check OpenGL ES version)
   
   Classification logic:
   LOW_END:
     - RAM < 4 GB
     - CPU cores < 6
     - API < 26 (Android 8.0)
     - Chipset: Snapdragon 660/710 or lower
   
   MEDIUM_END:
     - RAM 4-8 GB
     - CPU cores 6-8
     - API 26-29
     - Chipset: Snapdragon 845/855/865 range
   
   HIGH_END:
     - RAM > 8 GB
     - CPU cores > 8
     - API 30+
     - Chipset: Snapdragon 888+ or newer

2. getOptimalSettings(deviceProfile: DeviceProfile): PerformanceSettings
   Return recommended settings:
   
   LOW_END: {
     targetFPS: 5,
     cameraResolution: { width: 640, height: 480 },
     skipFrames: 2,              // Analyze every 3rd frame
     useGPU: false,              // CPU only (avoid old GPU issues)
     modelType: 'lite',
     enableBlur: false,          // Disable visual effects
     maxMemoryMB: 300
   }
   
   MEDIUM_END: {
     targetFPS: 8,
     cameraResolution: { width: 1280, height: 720 },
     skipFrames: 1,              // Analyze every 2nd frame
     useGPU: true,
     modelType: 'lite',
     enableBlur: true,
     maxMemoryMB: 400
   }
   
   HIGH_END: {
     targetFPS: 10,
     cameraResolution: { width: 1920, height: 1080 },
     skipFrames: 0,              // Analyze every frame
     useGPU: true,
     modelType: 'full',          // Can use full model if available
     enableBlur: true,
     maxMemoryMB: 500
   }

3. monitorRuntimePerformance()
   Track real-time metrics:
   - Current FPS (measure actual frame processing rate)
   - Detection latency (time per frame)
   - Memory usage (track heap size)
   - CPU usage percentage
   - Battery temperature
   
   Auto-adjust if performance degrades:
   - If FPS drops below 80% of target → reduce resolution
   - If latency > 200ms → increase skipFrames
   - If memory > maxMemoryMB → trigger garbage collection
   - If temperature > 42°C → pause detection temporarily

4. ThermalManager integration:
   - Check battery temperature every 60 seconds
   - Temperature thresholds:
     * < 35°C: Normal, maintain performance
     * 35-38°C: Warm, gentle optimization
     * 38-42°C: Hot, reduce to LOW_END settings
     * > 42°C: Critical, pause detection for 30s + show warning

5. Battery optimization:
   - Monitor battery level every minute
   - If battery < 20% and not charging:
     * Show warning: "电量过低，建议插上电源"
     * Reduce to lowest performance settings
     * Pause detection if battery < 10%

6. Performance logging:
   - Log metrics to SQLite every 5 minutes
   - Include: avgFPS, avgLatency, peakMemory, temperature, cpuUsage
   - Use for debugging and future optimizations

Implementation:

class PerformanceService {
  private currentProfile: DeviceProfile;
  private settings: PerformanceSettings;
  private metrics: PerformanceMetrics;

  async initialize(): Promise<void>
  startMonitoring(): void
  stopMonitoring(): void
  getRecommendedSettings(): PerformanceSettings
  adjustSettings(reason: string): void
  getMetrics(): PerformanceMetrics
}

Add visual feedback in debug mode showing current performance stats.
Include Chinese warnings for thermal/battery issues.
```


***

## 阶段 3: 提醒系统

### Prompt 3.1: 儿童友好的语音提醒

```
Create a child-friendly alert system with cute voice messages.

File: src/services/AlertService.ts

Features:

1. Voice Alert System (using react-native-tts)
   
   Alert Phrases (Chinese, child-friendly):
   
   GENTLE (第一次提醒，温柔):
   - "小朋友，注意一下坐姿哦~ 😊"
   - "宝贝，记得坐直直的哦~"
   - "挺起小胸脯，你最棒！"
   
   REMINDER (持续不良，提醒):
   - "小心驼背会变小乌龟哦！"
   - "抬起头来，让我看看你的笑脸！"
   - "坐姿小超人可不会弯腰哦！"
   
   FIRM (长时间不改，严肃):
   - "注意！姿势不对了，快点改正！"
   - "已经提醒很多次了，要坚持哦！"
   - "为了健康，请马上坐直！"
   
   ENCOURAGEMENT (恢复良好，鼓励):
   - "太棒了！这样的坐姿最帅气！"
   - "做得好！继续保持！⭐"
   - "你真是个好孩子，姿势perfect！"
   
   Character voices (可配置):
   - SUPERHERO: "超人哥哥提醒你：英雄都是挺胸抬头的！"
   - PRINCESS: "公主姐姐说：优雅的坐姿最美丽哦~"
   - ROBOT: "机器人小助手检测到：姿势需要调整，beep beep！"
   - PARENT: "妈妈/爸爸提醒：坐直一点，保护眼睛和脊柱"

2. Progressive Alert Logic:
   
   State machine:
   GOOD_POSTURE → (10s bad) → FIRST_ALERT
   FIRST_ALERT → (still bad 30s) → SECOND_ALERT
   SECOND_ALERT → (still bad 60s) → PERSISTENT_ALERT (notify parent)
   Any state → (posture good) → RESET + ENCOURAGEMENT
   
   Cooldown mechanism:
   - Minimum 60s between same-level alerts (avoid annoyance)
   - Reset cooldown when posture improves
   - Immediate alert if posture becomes CRITICAL

3. Sound Effects (use react-native-sound):
   
   Sound files (bundled in assets/sounds/):
   - gentle_bell.mp3: Soft notification (first alert)
   - cute_beep.mp3: Friendly reminder (second alert)
   - urgent_chime.mp3: Attention needed (persistent bad posture)
   - success_ding.mp3: Reward sound (good posture resumed)
   - star_collect.mp3: Gamification (earned a star)
   
   Play logic:
   - Preload all sounds on app start
   - Play sound before voice alert (gets attention)
   - Adjust volume based on time of day (quieter at night)

4. Visual Alerts (in-app):
   
   Show animated character on monitor device screen:
   - Happy face: Green, smiling, thumbs up (good posture)
   - Sad face: Yellow, worried expression (bad posture)
   - Angry face: Red, strict expression (persistent bad)
   - Celebrating: Confetti animation (milestone achieved)
   
   Use Lottie animations for smooth effects

5. Parent Notification:
   
   When to notify parent app (via WiFi sync):
   - Persistent bad posture (>5 alerts in 10 minutes)
   - Session started/ended
   - Child achieved milestone (earned reward)
   - Device issue (low battery, overheating)
   
   Notification format:
   {
     type: 'ALERT' | 'INFO' | 'ACHIEVEMENT',
     title: string,
     message: string,
     timestamp: number,
     priority: 'LOW' | 'MEDIUM' | 'HIGH'
   }

6. Customization (parent settings):
   
   - Alert delay: 5s / 10s / 30s / 60s
   - Alert intensity: Gentle / Normal / Strict
   - Voice character: Select from 4 options
   - Voice speed: Slow / Normal / Fast
   - Volume: 30% / 50% / 70% / 100%
   - Quiet hours: Auto-mute during specified times

Implementation:

class AlertService {
  private tts: any; // react-native-tts instance
  private sounds: Map<string, Sound>;
  private alertState: AlertState;
  private cooldownTimer: NodeJS.Timeout | null;

  async initialize(settings: AlertSettings): Promise<void>
  checkAndTriggerAlert(postureStatus: PostureStatus): void
  playVoiceAlert(phrase: string, character: string): Promise<void>
  playSound(soundName: string): void
  showVisualAlert(type: AlertType): void
  notifyParent(notification: ParentNotification): void
  reset(): void
}

Add fallback to sound-only if TTS fails.
Support multiple languages (Chinese, English).
Include unit tests for alert state machine.
```


### Prompt 3.2: 游戏化激励系统

```
Create a comprehensive gamification system to motivate children.

File: src/services/GamificationService.ts

Features:

1. Star Earning System:
   
   Rules:
   - Earn 1 star for every 30 minutes of good posture (>80% score)
   - Bonus star for perfect posture (100% score for 30 min)
   - Lose 1 star if session ends early without reason (<15 min)
   
   Display:
   - Large star counter on rewards screen
   - Animated star collection effect
   - Progress bar to next milestone
   - Daily star goal (configurable by parent)

2. Virtual Pet System:
   
   Pet characteristics:
   - Name: Let child choose (or use default names)
   - Type: Cat / Dog / Dragon / Rabbit (choose from 4)
   - Happiness level: 0-100 (based on posture score)
   - Growth stages: Baby (0-50 stars) → Kid (51-150) → Teen (151-300) → Adult (301+)
   
   Pet behavior:
   - Happy (score >80%): Jumping, playing animations
   - Neutral (score 60-80%): Walking, idle animations
   - Sad (score <60%): Sitting, crying animations
   - Sick (persistent bad posture): Bandaged, needs care
   
   Pet evolution:
   - Every 50 stars: Pet grows to next stage
   - New appearances unlock
   - Celebration animation with fireworks
   
   Implementation: Use Lottie animations stored in assets/animations/pets/

3. Achievement Badges:
   
   Badge types (14 total):
   
   Time-based:
   - "第一次监控" - Complete first session
   - "坚持一周" - 7 consecutive days with sessions
   - "月度冠军" - 30 days in a month with sessions
   
   Performance-based:
   - "完美坐姿" - 100% score for 1 hour straight
   - "快速进步" - 50% improvement in 1 week
   - "零提醒" - No alerts for entire session (min 1 hour)
   
   Milestone-based:
   - "新手星星" - Collect 10 stars
   - "星星达人" - Collect 50 stars
   - "星星大师" - Collect 100 stars
   - "传奇收藏家" - Collect 500 stars
   
   Streak-based:
   - "三天连胜" - 3 days streak
   - "一周全勤" - 7 days streak
   - "月度坚持" - 30 days streak
   
   Special:
   - "早起鸟儿" - Session before 8am
   
   Badge display:
   - Grid layout with locked/unlocked states
   - Tap badge to see unlock criteria and progress
   - Celebration animation when unlocked
   - Share badge to parent app

4. Daily Challenge:
   
   Random challenge each day (resets at midnight):
   - "今天目标：保持完美坐姿2小时"
   - "今天目标：零提醒完成作业"
   - "今天目标：比昨天的分数高10分"
   - "今天目标：连续3个30分钟都获得星星"
   
   Challenge rewards:
   - Completion: Bonus 3 stars
   - Display progress bar
   - Notification when close to completion

5. Leaderboard (optional, if multiple children):
   
   Weekly ranking:
   - Rank by total stars collected this week
   - Show top 5 (or all if <5 children)
   - Display with friendly names and avatars
   - Non-competitive design (everyone gets trophy emoji)
   
   Monthly ranking:
   - Similar to weekly
   - Special badge for #1

6. Reward Redemption:
   
   Parent-defined rewards:
   - Parent sets custom rewards in settings
   - Example rewards:
     * 10 stars: "选一个想吃的零食"
     * 30 stars: "周末看一部电影"
     * 50 stars: "去游乐场玩一次"
     * 100 stars: "买一个心仪的玩具"
   
   Redemption flow:
   - Child taps "兑换奖励" button
   - Select reward from list
   - Confirm redemption (deduct stars)
   - Send notification to parent app
   - Parent approves/fulfills in real life
   
   Redemption history:
   - Log all redemptions with timestamp
   - Display on parent app for tracking

Implementation:

class GamificationService {
  async earnStars(childId: number, amount: number, reason: string): Promise<void>
  async checkAndUnlockAchievements(childId: number): Promise<Achievement[]>
  async updatePetHappiness(childId: number, postureScore: number): Promise<void>
  async getDailyChallenge(childId: number): Promise<Challenge>
  async checkChallengeProgress(childId: number, sessionData: SessionData): Promise<void>
  async redeemReward(childId: number, rewardId: number): Promise<boolean>
  async getLeaderboard(timeRange: 'week' | 'month'): Promise<LeaderboardEntry[]>
}

Store all data in SQLite (rewards table).
Use React Context to share gamification state across app.
Add celebration animations for all positive events.
Include sound effects for achievements.
```


***

## 阶段 4: 摄像头与监控界面

### Prompt 4.1: 摄像头组件 (监控设备端)

```
Create the main camera screen for the monitoring device.

File: src/screens/monitor/CameraScreen.tsx

Layout and Features:

1. Full-screen camera preview:
   - Use react-native-vision-camera
   - Front camera by default
   - Resolution based on device performance profile
   - Keep screen awake during session (use react-native-keep-awake)

2. Semi-transparent overlay (top 20% of screen):
   - Current status indicator:
     * Large colored circle: Green (GOOD) / Yellow (BAD) / Red (CRITICAL)
     * Status text: "坐姿良好 ✅" / "注意坐姿 ⚠️" / "严重错误 ❌"
   - Session timer: "已监控: 01:23:45"
   - Current posture score: "当前评分: 85/100"
   - Battery indicator (warn if <20%)
   - Temperature icon (show if >38°C)

3. Bottom control bar (15% of screen):
   - Large "停止监控" button (red, prominent)
   - Small settings icon (requires PIN to access)
   - WiFi sync status icon (green=connected, gray=offline)

4. Animated character overlay (bottom-right corner):
   - Size: 80x80 dp
   - Changes based on posture status:
     * GOOD: Happy jumping animation
     * BAD: Sad shaking head animation
     * CRITICAL: Angry/worried animation
   - Use Lottie animations
   - Tap to hide/show (optional, for non-distraction)

5. Optional: Skeletal overlay on body:
   - Toggle in settings (default OFF)
   - Draw MediaPipe pose landmarks as dots
   - Connect landmarks with lines (skeleton)
   - Color-code by joint: Green (good) / Red (problematic)
   - Semi-transparent to not obstruct view

6. Real-time detection pipeline:
   - Capture frames at target FPS (5-10 based on device)
   - Pass frame to PostureDetectionService
   - Analyze pose with postureAnalysis utility
   - Update UI with results
   - Trigger alerts if needed
   - Log data to database every 5 seconds

7. Background service integration:
   - Run as Android foreground service (persistent notification)
   - Prevent system from killing app
   - Continue detection even if screen dimmed
   - Stop service when user taps "停止监控"

8. Error handling:
   - Camera permission denied: Show friendly message + guide
   - No person detected: Show "请坐在摄像头前" message
   - MediaPipe initialization failed: Fallback to simple motion detection
   - Device overheating: Pause + show warning

9. Performance indicators (debug mode only):
   - Small FPS counter in corner
   - Detection latency (ms)
   - Memory usage
   - Toggle with 5-finger tap

Implementation:

const CameraScreen: React.FC = () => {
  const device = useCameraDevice('front');
  const camera = useRef<Camera>(null);
  const [sessionId, setSessionId] = useState<number | null>(null);
  const [postureStatus, setPostureStatus] = useState<PostureStatus>('GOOD');
  const [sessionDuration, setSessionDuration] = useState<number>(0);
  const [currentScore, setCurrentScore] = useState<number>(100);

  // Frame processor (runs on every frame)
  const frameProcessor = useFrameProcessor((frame) => {
    'worklet';
    // Process frame with PostureDetectionService
    // This runs in a separate thread for performance
  }, []);

  // Start monitoring session
  const startSession = async () => {
    const id = await DatabaseService.startSession(childId);
    setSessionId(id);
    // Start timer, enable detection
  };

  // Stop monitoring session
  const stopSession = async () => {
    if (sessionId) {
      await DatabaseService.endSession(sessionId);
      setSessionId(null);
      // Navigate back or show summary
    }
  };

  return (
    <View style={styles.container}>
      <Camera
        ref={camera}
        device={device}
        isActive={true}
        frameProcessor={frameProcessor}
        style={StyleSheet.absoluteFill}
      />
      
      {/* Status overlay */}
      <View style={styles.topOverlay}>
        <StatusIndicator status={postureStatus} />
        <Timer duration={sessionDuration} />
        <ScoreDisplay score={currentScore} />
      </View>

      {/* Animated character */}
      <AnimatedCharacter status={postureStatus} />

      {/* Control buttons */}
      <View style={styles.bottomBar}>
        <StopButton onPress={stopSession} />
        <SettingsButton onPress={openSettings} />
      </View>
    </View>
  );
};

Use React hooks for state management.
Optimize re-renders (use React.memo for sub-components).
Add haptic feedback for button presses.
Include Chinese labels and messages throughout.
```


### Prompt 4.2: 家长控制面板

```
Create the parent dashboard screen for monitoring and control.

File: src/screens/parent/DashboardScreen.tsx

Layout (scrollable):

1. Header Section:
   - Child avatar (circular, 60dp)
   - Child name and age
   - Edit profile button (pencil icon)
   - Current monitoring status badge:
     * "监控中 🟢" (green, if active session)
     * "未监控 ⚪" (gray, if idle)

2. Today's Summary Card (hero section):
   - Large circular progress indicator:
     * Center: Today's average score (e.g., "85")
     * Ring: Color-coded progress (green >80, yellow 60-80, red <60)
   - Below progress:
     * Total study time today: "3小时15分"
     * Good posture percentage: "82%"
     * Stars earned today: "⭐ x 6"

3. Quick Stats Grid (2x2):
   Card 1: 本周进步
   - Value: "+12%" (with up arrow icon)
   - Subtext: "比上周提高"
   
   Card 2: 提醒次数
   - Value: "18次"
   - Subtext: "今日已提醒"
   
   Card 3: 连续天数
   - Value: "7天"
   - Subtext: "坚持打卡"
   
   Card 4: 总星星数
   - Value: "⭐ 143"
   - Subtext: "累计收集"

4. Weekly Trend Chart:
   - Title: "本周坐姿趋势"
   - Line chart showing daily average scores (7 days)
   - Use react-native-chart-kit
   - X-axis: 周一 to 周日
   - Y-axis: Score 0-100
   - Color gradient: Green (>80) to Red (<60)
   - Highlight today with larger dot

5. Posture Issues Breakdown:
   - Title: "姿势问题分布"
   - Horizontal bar chart or pie chart:
     * 驼背 (Slouching): 45%
     * 头部前倾 (Head forward): 30%
     * 身体歪斜 (Tilted): 15%
     * 良好姿势 (Good): 10%
   - Color-coded bars with icons

6. Recent Sessions List:
   - Title: "最近的监控记录"
   - Show last 5 sessions
   - Each item:
     * Date + Time: "今天 14:30 - 16:00"
     * Duration: "1小时30分"
     * Score: 88 (with color badge)
     * Tap to see detailed session report

7. Quick Action Buttons (bottom):
   - "开始新监控" (requires monitor device nearby)
   - "查看详细统计"
   - "奖励与成就"
   - "设置"

8. Real-time sync indicator:
   - Top-right corner icon
   - Green: Connected to monitor device (same WiFi)
   - Gray: Offline mode (will sync later)
   - Show last sync time on tap

9. Pull-to-refresh:
   - Refresh all statistics from database
   - Show loading animation

Implementation:

const DashboardScreen: React.FC = () => {
  const [childData, setChildData] = useState<Child | null>(null);
  const [todayStats, setTodayStats] = useState<Statistics | null>(null);
  const [weeklyData, setWeeklyData] = useState<DailyScore[]>([]);
  const [recentSessions, setRecentSessions] = useState<Session[]>([]);
  const [syncStatus, setSyncStatus] = useState<'connected' | 'offline'>('offline');

  useEffect(() => {
    loadDashboardData();
    startSyncListener(); // Listen for WiFi sync updates
  }, []);

  const loadDashboardData = async () => {
    const stats = await DatabaseService.getChildStatistics(childId, 'today');
    setTodayStats(stats);
    // Load other data...
  };

  const onRefresh = async () => {
    await loadDashboardData();
  };

  return (
    <ScrollView style={styles.container} refreshControl={<RefreshControl onRefresh={onRefresh} />}>
      <HeaderSection child={childData} status={syncStatus} />
      <TodaySummaryCard stats={todayStats} />
      <QuickStatsGrid stats={todayStats} />
      <WeeklyTrendChart data={weeklyData} />
      <PostureIssuesChart issues={todayStats?.issuesBreakdown} />
      <RecentSessionsList sessions={recentSessions} />
      <QuickActionButtons />
    </ScrollView>
  );
};

Use modern Material Design 3 components.
Add skeleton loading states for async data.
Implement smooth animations for data updates.
Use Chinese labels throughout.
Add haptic feedback for interactions.
```


***

## 阶段 5: 局域网同步 (可选但推荐)

### Prompt 5.1: WiFi 局域网同步服务

```
Create a local WiFi synchronization system for parent-monitor communication.

File: src/services/SyncService.ts

Architecture: Monitor device runs HTTP server, parent device connects as client

Features:

1. Monitor Device (Server Side):
   
   Start HTTP server on device:
   - Use react-native-http-server or similar
   - Listen on random port (3000-9000 range)
   - Broadcast server info via mDNS (Bonjour/Zeroconf)
   - Server name: "PostureMonitor-{deviceName}"
   
   Endpoints:
   
   GET /api/status
   - Return current monitoring status
   - Response: { isActive, childId, sessionId, currentScore, duration }
   
   GET /api/sessions?since={timestamp}
   - Return all sessions since given timestamp
   - Include full session data + posture logs
   
   GET /api/statistics?range={today|week|month}
   - Return aggregated statistics
   
   POST /api/control/start
   - Start new monitoring session
   - Body: { childId }
   
   POST /api/control/stop
   - Stop current session
   
   POST /api/settings/update
   - Update detection settings remotely
   - Body: { sensitivity, alertDelay, voiceCharacter, etc. }
   
   WebSocket endpoint /ws:
   - Real-time updates stream
   - Push posture updates every 5 seconds
   - Push alerts immediately when triggered

2. Parent Device (Client Side):
   
   Device discovery (mDNS):
   - Scan local network for PostureMonitor servers
   - Use react-native-zeroconf
   - Display list of discovered devices with names
   - Auto-connect to paired device
   
   Data fetching:
   - Poll /api/status every 10 seconds (when connected)
   - Fetch full data on demand (user pulls to refresh)
   - Subscribe to WebSocket for real-time updates
   
   Remote control:
   - Send start/stop commands to monitor device
   - Update settings remotely
   - View live monitoring status

3. Pairing mechanism:
   
   Initial pairing flow:
   - Parent app generates 6-digit PIN code
   - Display PIN code on parent screen
   - Monitor device asks for PIN on first connection
   - Parent enters PIN on monitor device
   - Store paired devices in SQLite (device_pairing table)
   - Future connections auto-authenticate
   
   Security:
   - Generate shared secret on pairing
   - All API requests include HMAC signature
   - Reject requests from unpaired devices

4. Sync strategy:
   
   Incremental sync:
   - Track last sync timestamp for each device
   - Only transfer new/updated data
   - Use delta encoding to minimize bandwidth
   
   Conflict resolution:
   - Monitor device is source of truth for session data
   - Parent device is source of truth for settings
   - Use timestamp to resolve conflicts
   
   Offline queue:
   - If parent offline, queue notifications/updates
   - Deliver when connection restored
   - Max queue size: 100 items (drop oldest)

5. Error handling:
   
   - Network disconnected: Show "离线" indicator, retry every 30s
   - Server unreachable: Fallback to pure local mode
   - Timeout: Retry with exponential backoff
   - Data corruption: Re-sync from scratch

6. Bandwidth optimization:
   
   - Compress data with gzip
   - Send posture_logs in batches (not individual records)
   - Only send changed fields in updates
   - Target: <1 MB/hour data transfer for typical session

Implementation:

// Monitor Device
class SyncServerService {
  private server: HTTPServer | null = null;
  private wsServer: WebSocketServer | null = null;

  async startServer(): Promise<void> {
    this.server = new HTTPServer();
    this.server.listen(PORT);
    this.registerEndpoints();
    this.broadcastMDNS();
  }

  private registerEndpoints(): void {
    this.server.get('/api/status', this.handleGetStatus);
    this.server.get('/api/sessions', this.handleGetSessions);
    // ... other endpoints
  }

  private broadcastMDNS(): void {
    Zeroconf.publish(PORT, 'http', 'tcp', 'PostureMonitor');
  }

  async stopServer(): Promise<void> {
    this.server?.close();
    Zeroconf.unpublish();
  }
}

// Parent Device
class SyncClientService {
  private serverAddress: string | null = null;
  private wsConnection: WebSocket | null = null;

  async discoverDevices(): Promise<Device[]> {
    return new Promise((resolve) => {
      Zeroconf.scan('http', 'tcp', 'PostureMonitor');
      Zeroconf.on('resolved', (service) => {
        // Add to list
      });
      setTimeout(() => resolve(devices), 5000);
    });
  }

  async connect(device: Device): Promise<boolean> {
    this.serverAddress = `http://${device.ip}:${device.port}`;
    // Test connection
    const status = await this.fetchStatus();
    return status !== null;
  }

  async fetchStatus(): Promise<Status | null> {
    try {
      const response = await fetch(`${this.serverAddress}/api/status`);
      return await response.json();
    } catch (error) {
      return null;
    }
  }

  subscribeToUpdates(callback: (update: any) => void): void {
    this.wsConnection = new WebSocket(`${this.serverAddress}/ws`);
    this.wsConnection.onmessage = (event) => {
      callback(JSON.parse(event.data));
    };
  }
}

Add connection status indicators in UI.
Include Chinese error messages for network issues.
Test with multiple devices on same WiFi.
Implement graceful degradation if sync fails.
```


***

## 阶段 6: 设置与配置

### Prompt 6.1: 综合设置界面

```
Create a comprehensive settings screen for parents.

File: src/screens/parent/SettingsScreen.tsx

Settings Categories (grouped):

1. 子女信息 (Child Profile):
   - 姓名 (Text input, required)
   - 年龄 (Number picker: 3-18岁)
   - 头像 (Grid of 12 cartoon avatars, tap to select)
   - 身高 (Optional, number input with cm unit, for calibration)
   - 性别 (Optional, 男/女/保密, for voice customization)

2. 检测灵敏度 (Detection Sensitivity):
   - 严格程度 (Slider with 3 levels):
     * 宽松 (Lenient): Thresholds relaxed 20%
     * 适中 (Moderate): Default thresholds
     * 严格 (Strict): Thresholds tightened 20%
   - Show visual guide: Illustrations of what each level considers "good posture"
   - 高级调节 (Advanced, collapsible):
     * 颈部角度阈值: Slider 140-170°
     * 头部前倾阈值: Slider 0.05-0.15
     * 身体歪斜阈值: Slider 3-10°
   - 重置为默认 button

3. 提醒设置 (Alert Behavior):
   - 首次提醒延迟 (Dropdown):
     * 5秒 / 10秒 (默认) / 30秒 / 60秒
   - 提醒间隔 (Dropdown):
     * 每1分钟 (默认) / 每3分钟 / 每5分钟
   - 提醒方式 (Toggle switches):
     * ✅ 语音提醒 (Voice alerts)
     * ✅ 声音提示 (Sound effects)
     * ✅ 视觉动画 (Visual character)
     * ⬜ 振动 (Vibration, disabled by default)
   - 音量 (Slider): 30% / 50% / 70% / 100%
   - 安静时段 (Time range picker):
     * 开始时间: 22:00 (默认)
     * 结束时间: 07:00 (默认)
     * 期间自动静音

4. 语音角色 (Voice Character):
   - 角色选择 (Radio buttons with preview):
     * 🦸 超人哥哥 (Superhero, deep encouraging voice)
     * 👸 公主姐姐 (Princess, gentle caring voice)
     * 🤖 机器人 (Robot, fun mechanical voice)
     * 👨 家长 (Parent, natural parent voice)
   - 试听按钮 (Play sample phrase for each character)
   - 语速 (Slider): 慢速 / 正常 (默认) / 快速

5. 游戏化设置 (Gamification):
   - 启用游戏化 (Master toggle)
   - 星星奖励规则:
     * 每30分钟好姿势奖励: 1-3 stars (number input)
   - 自定义奖励 (List):
     * 10星星: "选一个零食" (editable)
     * 30星星: "看一部电影" (editable)
     * 50星星: "去游乐场" (editable)
     * 100星星: "买心仪玩具" (editable)
     * + 添加自定义奖励 (max 10 items)
   - 每日挑战 (Toggle to enable/disable)

6. 监控时间表 (Monitoring Schedule):
   - 自动开始时间 (Time picker): 默认关闭
   - 自动结束时间 (Time picker): 默认关闭
   - 活跃日期 (Checkboxes):
     * ☑ 周一 ☑ 周二 ☑ 周三 ☑ 周四 ☑ 周五
     * ⬜ 周六 ⬜ 周日
   - 提醒设置时间 (Toggle): 提前5分钟提醒孩子准备

7. 设备与同步 (Device & Sync):
   - 配对设备列表:
     * 显示已配对的监控设备
     * 状态: 在线🟢 / 离线⚪ / 最后同步时间
     * 重命名设备按钮
     * 解除配对按钮 (需确认)
   - + 添加新设备 (打开二维码配对)
   - WiFi同步 (Toggle):
     * 自动同步 (默认开启)
     * 仅在WiFi下同步 (省流量)
   - 数据管理:
     * 本地存储使用: 显示占用空间 (如 "52 MB")
     * 清理旧数据: "保留最近90天" (dropdown: 30/60/90/180天)
     * 立即清理按钮

8. 隐私与数据 (Privacy & Data):
   - 数据存储说明 (Info card):
     * "所有数据仅存储在本地设备"
     * "摄像头画面不会被录制或保存"
     * "WiFi同步仅在局域网内进行"
   - 导出数据 (Button):
     * 格式: CSV文件
     * 内容: 所有会话记录和统计
     * 分享方式: 微信/邮件/保存到文件
   - 删除所有数据 (Button, red, requires confirmation):
     * 二次确认对话框
     * 警告: "此操作不可恢复！"

9. 关于 (About):
   - 应用名称和版本
   - 开发者信息
   - 用户协议 (link)
   - 隐私政策 (link)
   - 问题反馈 (link to email or form)
   - 检查更新 (button)

Implementation:

const SettingsScreen: React.FC = () => {
  const [settings, setSettings] = useState<Settings | null>(null);

  useEffect(() => {
    loadSettings();
  }, []);

  const loadSettings = async () => {
    const data = await DatabaseService.getSettings(childId);
    setSettings(data);
  };

  const updateSetting = async (key: string, value: any) => {
    await DatabaseService.updateSettings(childId, { [key]: value });
    setSettings({ ...settings, [key]: value });
    // Notify monitor device if connected
    SyncService.pushSettingsUpdate({ [key]: value });
  };

  return (
    <ScrollView style={styles.container}>
      <SettingSection title="子女信息">
        <TextInput label="姓名" value={settings?.childName} onChange={(v) => updateSetting('childName', v)} />
        <NumberPicker label="年龄" value={settings?.age} onChange={(v) => updateSetting('age', v)} />
        <AvatarSelector selected={settings?.avatar} onChange={(v) => updateSetting('avatar', v)} />
      </SettingSection>

      <SettingSection title="检测灵敏度">
        <Slider label="严格程度" options={['宽松', '适中', '严格']} value={settings?.sensitivity} onChange={(v) => updateSetting('sensitivity', v)} />
        <Collapsible title="高级调节">
          {/* Advanced threshold sliders */}
        </Collapsible>
      </SettingSection>

      {/* Other sections... */}
    </ScrollView>
  );
};

Use react-native-paper for Material Design components.
Add confirmation dialogs for destructive actions.
Show toast messages on successful saves.
Implement form validation with Chinese error messages.
Add help tooltips (? icons) for complex settings.
```


***

## 阶段 7: 数据可视化与报告

### Prompt 7.1: 统计图表与报告

```
Create comprehensive statistics and reporting features.

File: src/screens/parent/StatisticsScreen.tsx

Layout:

1. Time Range Selector (Tab bar at top):
   - 今天 (Today)
   - 本周 (This Week)
   - 本月 (This Month)
   - 自定义 (Custom, opens date range picker)

2. Key Metrics Cards (grid, 2x2):
   
   Card 1: 总学习时长
   - Large value: "28小时45分"
   - Comparison: "比上周 +3小时" (with up/down arrow)
   - Mini sparkline showing daily trend
   
   Card 2: 平均坐姿评分
   - Large value: "83"
   - Color-coded ring progress indicator
   - Comparison: "比上周 +5分"
   
   Card 3: 收集星星
   - Large value: "⭐ 42"
   - Total stars in selected period
   - Percentage of potential stars: "93%"
   
   Card 4: 提醒次数
   - Large value: "67次"
   - Average per session: "4.8次"
   - Comparison: "比上周 -12次"

3. Posture Score Trend Chart:
   - Title: "坐姿评分趋势"
   - Line chart showing daily average scores
   - Use react-native-chart-kit with gradient fill
   - X-axis: Dates
   - Y-axis: Score 0-100
   - Mark milestones: First time >90, longest streak, etc.
   - Tap data point to see that day's details

4. Time Distribution Chart:
   - Title: "学习时间分布"
   - Stacked bar chart showing hours per day
   - Color segments: 
     * Green: Good posture time
     * Yellow: Moderate posture time
     * Red: Bad posture time
   - Show percentage labels

5. Posture Issues Heatmap:
   - Title: "姿势问题热力图"
   - Grid showing which hours of day have most issues
   - Rows: Days of week
   - Columns: Hours (8am-10pm)
   - Color intensity: More issues = darker red
   - Insight: "下午3-5点最容易驼背"

6. Issue Type Breakdown:
   - Title: "问题类型分析"
   - Pie chart or donut chart:
     * 驼背 (Slouching): 45% - 红色
     * 头部前倾 (Head forward): 30% - 橙色
     * 身体歪斜 (Tilted): 15% - 黄色
     * 弯腰 (Hunched): 10% - 紫色
   - Tap segment to see detailed analysis
   - Recommendations for each issue type

7. Session History Table:
   - Scrollable list of all sessions in period
   - Each row:
     * Date & Time: "2月10日 14:30-16:15"
     * Duration: "1h 45m"
     * Score: 88 (color badge)
     * Stars: ⭐⭐
     * Status icon: ✅ / ⚠️
   - Tap row to see detailed session report

8. Weekly Comparison:
   - Side-by-side comparison of current vs previous week
   - Metrics:
     * Total time
     * Avg score
     * Stars earned
     * Alerts count
   - Show percentage change for each

9. Achievements Progress:
   - List of achievements with progress bars
   - Example: "一周全勤: 5/7天 (71%)"
   - Show locked achievements with unlock criteria

10. Export & Share Section:
    - "生成报告" button:
      * Creates PDF report with all charts
      * Includes written summary in Chinese
      * Personalized insights and suggestions
    - "分享到微信" button:
      * Creates shareable image (infographic style)
      * Highlights key achievements
    - "导出原始数据" button:
      * CSV file with all session data

Detailed Session Report (modal/new screen):
When user taps a session in history:
- Session summary (time, duration, score)
- Timeline chart showing score over time (minute-by-minute)
- Issue timeline: When alerts were triggered
- Photos/screenshots (if feature enabled)
- Detailed recommendations

Implementation:

const StatisticsScreen: React.FC = () => {
  const [timeRange, setTimeRange] = useState<'today' | 'week' | 'month'>('week');
  const [statistics, setStatistics] = useState<Statistics | null>(null);

  useEffect(() => {
    loadStatistics(timeRange);
  }, [timeRange]);

  const loadStatistics = async (range: string) => {
    const stats = await DatabaseService.getChildStatistics(childId, range);
    setStatistics(stats);
  };

  const generateReport = async () => {
    const reportData = await ReportGenerator.generateWeeklyReport(childId);
    const pdfPath = await ReportGenerator.createPDF(reportData);
    shareFile(pdfPath);
  };

  return (
    <ScrollView>
      <TimeRangeSelector value={timeRange} onChange={setTimeRange} />
      <KeyMetricsGrid stats={statistics} />
      <PostureScoreTrendChart data={statistics?.dailyScores} />
      <TimeDistributionChart data={statistics?.timeBreakdown} />
      <PostureIssuesHeatmap data={statistics?.issueHeatmap} />
      <IssueTypeBreakdown data={statistics?.issueTypes} />
      <SessionHistoryTable sessions={statistics?.sessions} />
      <WeeklyComparison current={statistics} previous={previousWeekStats} />
      <AchievementsProgress achievements={achievements} />
      <ExportShareSection onGenerateReport={generateReport} />
    </ScrollView>
  );
};

Use react-native-chart-kit for all charts.
Add loading skeletons while data loads.
Implement smooth animations for chart updates.
Cache chart images for better performance.
Add empty states with friendly messages if no data.
```


***

## 最终优化 Prompt

### Prompt 8.1: APK 体积优化

```
Optimize the APK size to meet target of 25-35 MB.

Tasks:

1. Enable ProGuard in android/app/build.gradle:
   - Remove unused code
   - Obfuscate class names
   - Optimize bytecode

2. Enable APK splitting by architecture:
   - Generate separate APKs for arm64-v8a, armeabi-v7a
   - Each APK ~15-20 MB (users download only their arch)

3. Image optimization:
   - Convert all PNG to WebP format (70-80% smaller)
   - Use SVG for icons (vector graphics)
   - Compress sound files to MP3 128kbps

4. Remove unused resources:
   - Run resource shrinking
   - Remove unused fonts
   - Remove debug symbols in release build

5. Bundle optimization:
   - Enable Hermes bytecode compilation
   - Tree-shake unused JavaScript code
   - Minify JavaScript bundle

Configuration changes in build.gradle:

android {
  buildTypes {
    release {
      minifyEnabled true
      shrinkResources true
      proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
    }
  }

  splits {
    abi {
      enable true
      reset()
      include 'armeabi-v7a', 'arm64-v8a'
      universalApk false
    }
  }
}

Target APK sizes:
- arm64-v8a: 18-22 MB
- armeabi-v7a: 20-24 MB

Verify optimizations don't break functionality.
Test on multiple devices after optimization.
```


### Prompt 8.2: 性能测试与调优

```
Create a performance testing and tuning script.

File: scripts/performanceTest.js

Test scenarios:

1. Stress test:
   - Run detection continuously for 2 hours
   - Monitor: CPU%, memory, temperature, battery%
   - Log metrics every minute
   - Alert if any metric exceeds threshold

2. Memory leak detection:
   - Run 10x start/stop session cycles
   - Check if memory increases each cycle
   - Should remain stable (~350-400 MB)

3. Frame rate consistency:
   - Measure actual FPS vs target
   - Should maintain 90%+ of target FPS
   - Identify frame drops and causes

4. Database performance:
   - Insert 10,000 posture_logs records
   - Measure query time for statistics
   - Should complete in <200ms

5. Battery drain test:
   - Full charge to 20% battery
   - Measure mAh consumed per hour
   - Target: <450 mAh/hour

6. Thermal test:
   - Run in 30°C environment
   - Measure device temperature over time
   - Should not exceed 45°C

Automated test script that runs all scenarios.
Generate HTML report with charts.
Compare results against benchmarks.
Highlight performance regressions.
```


***

## 完整项目启动命令

### 一键初始化 Prompt

```
COMPLETE PROJECT INITIALIZATION

Generate the entire Kids Posture Monitor app with the following command sequence:

1. Create React Native project:
   npx react-native init KidsPostureMonitor --template react-native-template-typescript

2. Install all dependencies:
   npm install --registry=https://registry.npmmirror.com \
     @react-navigation/native @react-navigation/stack \
     react-native-vision-camera react-native-sqlite-storage \
     react-native-tts react-native-sound react-native-fs \
     react-native-device-info react-native-chart-kit \
     react-native-svg lottie-react-native \
     react-native-permissions @mediapipe/tasks-vision

3. Download MediaPipe model:
   curl -o assets/models/pose_landmarker_lite.task \
     https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/latest/pose_landmarker_lite.task

4. Configure Android:
   - Update android/app/build.gradle with optimization settings
   - Add camera permissions to AndroidManifest.xml
   - Enable Hermes engine

5. Generate all source files from previous prompts:
   - Create folder structure
   - Generate TypeScript files with full implementation
   - Add Chinese comments throughout

6. Build and run:
   npx react-native run-android

Expected result:
- Working APK with all features
- Size: ~30-35 MB (before optimization)
- Runs on Android 7.0+ devices
- 100% offline capable
- Optional WiFi sync

Provide this as a single executable script that sets up everything.
Include error handling and progress indicators.
```


***

好了！这套 Prompts 应该能让 Trae AI 的 Vibe Coding 帮你生成一个完整可用的儿童坐姿监控 APP。

**使用建议**:

1. **按阶段执行**: 不要一次性丢所有 prompts,按1→8的顺序逐步生成
2. **测试验证**: 每个阶段生成后立即测试,确保功能正常再继续
3. **迭代优化**: 如果生成的代码有问题,用 "修改上面的代码,改为..." 类型的 prompt 继续迭代
4. **组合使用**: 可以将相关的几个 prompt 合并成一个更大的 prompt

需要我为某个特定功能提供更详细的实现代码吗?

