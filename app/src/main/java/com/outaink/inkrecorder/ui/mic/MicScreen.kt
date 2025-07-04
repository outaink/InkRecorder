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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.outaink.inkrecorder.ui.DrawMode
import com.outaink.inkrecorder.ui.Waveform
import com.outaink.inkrecorder.ui.WaveformData
import com.outaink.inkrecorder.ui.WaveformProcessor.smoothWaveform
import com.outaink.inkrecorder.ui.WaveformStyle
import com.outaink.inkrecorder.ui.theme.InkRecorderTheme
import kotlin.random.Random


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitialMicScreenUi(
    recorderState: RecorderUiState,
    onIntent: (RecorderIntent) -> Unit = {},
    requestAudioPermission: () -> Unit = {}
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.fillMaxWidth(),
                title = {  },
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
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(32.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {

                        // 自动生成的数据
                        val waveformData = remember {
                            WaveformData(
                                amplitudes = smoothWaveform(List(200) { Random.nextFloat() * 2 - 1 }),
                                progress = 0.3f
                            )
                        }

                        Waveform(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center),
                            data = waveformData,
                            style = WaveformStyle(
                                drawMode = DrawMode.Bars,
                                waveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                progressColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Text(
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    text = "00:00:00",
                )
                FloatingActionButton(
                    modifier = Modifier
                        .width(80.dp)
                        .height(80.dp),
                    shape = FloatingActionButtonDefaults.largeShape,
                    onClick = { onIntent(RecorderIntent.StartRecording) }
                ) {
                    Icon(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        imageVector = Icons.Filled.Mic,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        contentDescription = "Start Recording"
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
            recorderState = RecorderUiState()
        )
    }
}
