app/
└── src/
└── main/
└── java/
└── com/
└── yourcompany/
└── androidmic/
    ├── MicApplication.kt          // (1) Hilt 应用入口
    │
    ├── di/                        // (2) Dagger Hilt 依赖注入模块
    │   ├── AppModule.kt
    │   ├── AudioModule.kt
    │   └── NetworkModule.kt
    │
    ├── data/                      // (3) 数据层 (MVI 的 Model)
    │   ├── audio/
    │   │   ├── AudioRecorder.kt       // 音频录制器接口
    │   │   └── AudioRecorderImpl.kt   // 具体实现
    │   ├── network/
    │   │   ├── AudioStreamer.kt       // 音频流发送器接口
    │   │   └── AudioStreamerImpl.kt   // 具体实现 (UDP/TCP)
    │   └── discovery/
    │       ├── ServiceDiscovery.kt    // 服务发现接口
    │       └── ServiceDiscoveryImpl.kt// 具体实现 (NSD)
    │
    ├── service/                   // (4) 后台服务
    │   └── AudioStreamService.kt    // 前台服务，用于持续录音和传输
    │
    ├── ui/                        // (5) UI 层 (MVI 的 View)
    │   ├── main/                    // 主屏幕功能模块
    │   │   ├── MainContract.kt      // MVI 合约 (State, Intent, Effect)
    │   │   ├── MainViewModel.kt     // ViewModel (处理 Intent，更新 State)
    │   │   └── MainScreen.kt        // Composable UI
    │   ├── components/              // 可复用的 Composable 组件
    │   │   ├── AudioVisualizer.kt
    │   │   └── StatusIndicator.kt
    │   └── theme/                   // Compose 主题
    │       ├── Color.kt
    │       ├── Theme.kt
    │       └── Type.kt
    │
    ├── util/                      // (6) 工具类
    │   ├── Constants.kt
    │   └── PermissionHandler.kt
    │
    └── MainActivity.kt            // 应用主 Activity
