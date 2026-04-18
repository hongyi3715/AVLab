package com.lq.avlab.ui.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.lq.avlab.ui.theme.AVLabTheme


@Composable
fun Greeting(viewModel: AudioViewModel = hiltViewModel()) {
    Column(modifier = Modifier.fillMaxSize().padding(40.dp)){
        Button(onClick = {
            viewModel.toggleRecord()
        }) { Text("开始录制音频" )}

        Text(text = "${viewModel.recordState}")
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AVLabTheme {
        VideoScreen()
    }
}
