package com.outaink.inkrecorder.ui.mic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.outaink.inkrecorder.ui.DrawMode
import com.outaink.inkrecorder.ui.Waveform
import com.outaink.inkrecorder.ui.WaveformStyle
import com.outaink.inkrecorder.ui.theme.InkRecorderTheme

/**
 * Formats elapsed time in milliseconds to MM:SS format
 */
private fun formatElapsedTime(elapsedTimeMs: Long): String {
    val totalSeconds = elapsedTimeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitialMicScreenUi(
    uiState: MicUiState,
    onUiAction: (MicAction) -> Unit = {},
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.fillMaxWidth(),
                title = { },
                navigationIcon = {
                    Icon(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(vertical = 20.dp),
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier.padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(32.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Waveform(
                            modifier = Modifier
                                .fillMaxSize()
                                .align(Alignment.Center)
                                .padding(16.dp),
                            data = uiState.waveformData,
                            style = WaveformStyle(
                                drawMode = DrawMode.Bars,
                                waveColor = if (uiState.isRecording)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                progressColor = if (uiState.isRecording)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                animationIntensity = if (uiState.isRecording) 1.3f else 1.0f
                            )
                        )
                    }
                }

                // Error message or recording status
                if (uiState.error != null) {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        text = uiState.error
                    )
                } else {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        text = if (uiState.isRecording) "Recording..." else "Ready to record"
                    )
                }
                
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.isRecording) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    text = formatElapsedTime(uiState.elapsedTimeMs)
                )

                FloatingActionButton(
                    modifier = Modifier
                        .width(80.dp)
                        .height(80.dp)
                        .padding(top = 16.dp),
                    shape = FloatingActionButtonDefaults.largeShape,
                    containerColor = if (uiState.isRecording) 
                        MaterialTheme.colorScheme.errorContainer 
                    else 
                        MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (uiState.isRecording)
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = { onUiAction(MicAction.ClickMicButton) }
                ) {
                    Icon(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        imageVector = if (uiState.isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = if (uiState.isRecording) "Stop Recording" else "Start Recording"
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun InitialMicScreenUiPreview() {
    InkRecorderTheme {
        InitialMicScreenUi(
            uiState = MicUiState()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InitialMicScreenUiRecordingPreview() {
    InkRecorderTheme {
        InitialMicScreenUi(
            uiState = MicUiState(
                isRecording = true,
                elapsedTimeMs = 65000L // 1 minute 5 seconds
            )
        )
    }
}
