package com.outaink.inkrecorder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.outaink.inkrecorder.ui.InitialRecorderScreenUi
import com.outaink.inkrecorder.ui.theme.InkRecorderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun App() {
    InkRecorderTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            InitialRecorderScreenUi(modifier = Modifier.padding())
        }
    }
}


