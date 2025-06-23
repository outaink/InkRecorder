package com.outaink.inkrecorder

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.outaink.inkrecorder.ui.InitialRecorderScreenUi
import com.outaink.inkrecorder.ui.theme.InkRecorderTheme
import com.outaink.inkrecorder.viewmodel.RecorderUiState
import com.outaink.inkrecorder.viewmodel.RecorderViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: RecorderViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // 权限被授予。可以在这里通知 ViewModel 或直接触发录音（如果这是流程的一部分）
                // viewModel.processIntent(RecorderIntent.StartRecording) // 例如
            } else {
                // 权限被拒绝。向用户解释为什么需要权限。
                // 可以更新 UI 状态显示权限被拒绝的信息
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InkRecorderTheme {
                val recorderState by viewModel.uiState.collectAsState()

                InitialRecorderScreenUi ( // 将 ViewModel 实例或其状态和事件处理器传递给 Composable
                    recorderState = recorderState,
                    onIntent = viewModel::onIntent,
                    requestAudioPermission = {
                        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                )
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun AppPreview() {
    InkRecorderTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            InitialRecorderScreenUi(
                recorderState = RecorderUiState(),
                onIntent = {},
                requestAudioPermission = {}
            )
        }
    }
}


