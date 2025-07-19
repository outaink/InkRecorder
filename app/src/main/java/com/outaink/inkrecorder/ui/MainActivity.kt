package com.outaink.inkrecorder.ui

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.outaink.inkrecorder.ui.mic.InitialMicScreenUi
import com.outaink.inkrecorder.ui.mic.MicAction
import com.outaink.inkrecorder.ui.mic.MicUiState
import com.outaink.inkrecorder.ui.mic.MicViewModel
import com.outaink.inkrecorder.ui.mic.UiEvent
import com.outaink.inkrecorder.ui.theme.InkRecorderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        const val TAG = "MainActivity"
    }

    private val viewModel: MicViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            Log.d("MainActivity", "isGranted: $isGranted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InkRecorderTheme {
                val micState: MicUiState by viewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.uiEvents.collect { event: UiEvent ->
                        when (event) {
                            is UiEvent.ShowSnackbar -> {
                                Log.d(TAG, "UiEvent.ShowSnackbar: ${event.message}")
                            }

                            is UiEvent.RequestPermission -> {
                                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    }
                }

                InitialMicScreenUi(
                    uiState = micState,
                    onUiAction = { action: MicAction ->
                        viewModel.dispatch(action)
                    },
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
                uiState = MicUiState(),
                onUiAction = {},
            )
        }
    }
}


