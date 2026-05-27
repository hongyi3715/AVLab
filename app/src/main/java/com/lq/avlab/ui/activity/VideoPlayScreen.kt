package com.lq.avlab.ui.activity



import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import java.net.Inet4Address
import java.net.NetworkInterface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayScreen(
) {
    val viewModel: RecordViewModel = hiltViewModel()
    val context = LocalContext.current

    printAllIPs()
    LaunchedEffect(Unit) {
        viewModel.initReceiver()
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
