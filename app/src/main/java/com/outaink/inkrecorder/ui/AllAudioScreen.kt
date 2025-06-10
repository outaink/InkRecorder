package com.outaink.inkrecorder.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.outaink.inkrecorder.R
import com.outaink.inkrecorder.ui.theme.InkRecorderTheme

@Composable
fun AllAudioScreen() {
    Scaffold(
        topBar = {
            AllAudioTopBar()
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = "你没有任何录音"
            )

            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                onClick = {}
            ) {
                Icon(
                    imageVector = Icons.Rounded.Circle,
                    contentDescription = "Add Audio",
                )
            }
        }


    }
}

@Composable
fun AllAudioTopBar(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.padding(16.dp),
            imageVector = Icons.Rounded.Menu,
            contentDescription = "All Audio",
        )
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.all_audio_files),
        )
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = "Search"
        )
        Icon(
            modifier = Modifier.padding(16.dp),
            imageVector = Icons.AutoMirrored.Rounded.Sort,
            contentDescription = "Sort",
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AllAudioTopBarPreview() {
    InkRecorderTheme {
        AllAudioTopBar()
    }
}


@Preview(showBackground = true)
@Composable
fun AllAudioScreenPreview() {
    InkRecorderTheme {
        AllAudioScreen()
    }
}