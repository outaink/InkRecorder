package com.outaink.inkrecorder.ui

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
import com.outaink.inkrecorder.ui.mic.InitialMicScreenUi
import com.outaink.inkrecorder.ui.mic.MicAction
import com.outaink.inkrecorder.ui.mic.MicUiState
import com.outaink.inkrecorder.ui.mic.MicViewModel
import com.outaink.inkrecorder.ui.theme.InkRecorderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MicViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            val action = if (isGranted) {
                PermissionAction.UserGrants
            } else {
                PermissionAction.UserDenies(
                    shouldShowRatinale = shouldShowRequestPermissionRationale(
                        Manifest.permission.RECORD_AUDIO
                    )
                )
            }
            viewModel.dispatch(action)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InkRecorderTheme {
                val micUiState by viewModel.uiState.collectAsState()

                InitialMicScreenUi(
                    uiState = micUiState,
                    onUiAction = { action: MicAction ->
                        viewModel.dispatch(action)
                    },
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
            InitialMicScreenUi(
                uiState = MicUiState.Initial(),
                onUiAction = {},
                requestAudioPermission = {}
            )
        }
    }
}


