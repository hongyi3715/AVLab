package com.lq.avlab.ui.activity



import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.lq.video.camera.CameraController
import com.lq.video.play.PlayController
import com.lq.video.view.CameraPlayView
import com.lq.video.view.CameraPreview
import java.net.Inet4Address
import java.net.NetworkInterface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayScreen(
) {
    val viewModel: RecordViewModel = hiltViewModel()
    val context = LocalContext.current
    val controller = remember { PlayController(context) }

    LaunchedEffect(Unit) {
        printAllIPs()
        viewModel.startAudioPlay()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPlayView(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            controller = controller,
            pipeline = viewModel.playPipeline
        )
    }
}


fun printAllIPs() {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            val addresses = networkInterface.inetAddresses

            println("接口: ${networkInterface.name} (${if (networkInterface.isUp) "UP" else "DOWN"})")

            while (addresses.hasMoreElements()) {
                val addr = addresses.nextElement()
                if (addr is Inet4Address) {
                    println("  IPv4: ${addr.hostAddress}")
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
