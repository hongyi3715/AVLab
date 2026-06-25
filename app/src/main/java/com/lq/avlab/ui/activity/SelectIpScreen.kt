package com.lq.avlab.ui.activity

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit


@Composable
fun SelectIpScreen(
    onIpSelected: (String) -> Unit
) {
    var ipText by remember { mutableStateOf("192.168.") }
    var portText by remember { mutableStateOf("1935") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // 保存的历史IP列表
    var savedIps by remember { mutableStateOf(listOf<String>()) }
    
    // 从SharedPreferences加载历史IP
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("video_record", Context.MODE_PRIVATE)
        val saved = prefs.getStringSet("saved_ips", emptySet()) ?: emptySet()
        savedIps = saved.toList()
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 标题
            Text(
                text = "视频录制",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "请输入接收端IP地址",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // IP输入框
            OutlinedTextField(
                value = ipText,
                onValueChange = { 
                    ipText = it
                    errorMessage = null
                },
                label = { Text("IP地址") },
                placeholder = { Text("例如: 192.168.1.100") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = errorMessage != null,
                supportingText = {
                    errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "IP"
                    )
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 连接按钮
            Button(
                onClick = {
                    when {
                        ipText.isBlank() -> {
                            errorMessage = "请输入IP地址"
                        }
                        !isValidIp(ipText.trim()) -> {
                            errorMessage = "IP地址格式不正确"
                        }
                        else -> {
                            val fullHost = if (portText.isNotBlank()) {
                                "${ipText.trim()}:${portText.trim()}"
                            } else {
                                ipText.trim()
                            }

                            val prefs = context.getSharedPreferences("video_record", Context.MODE_PRIVATE)

                            val nextSaved = prefs
                                .getStringSet("saved_ips", emptySet())
                                .orEmpty()
                                .toMutableSet()
                                .apply {
                                    add(fullHost)
                                }

                            prefs.edit {
                                putStringSet("saved_ips", nextSaved)
                            }

                            savedIps = nextSaved.toList()
                            onIpSelected(fullHost)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = ipText.isNotBlank()
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "连接并录制",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            // 历史记录
            if (savedIps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "历史连接",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(savedIps) { savedIp ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val parts = savedIp.split(":")
                                    ipText = parts.getOrElse(0) { savedIp }
                                    if (parts.size > 1) {
                                        portText = parts[1]
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Home,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = savedIp,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                
                                IconButton(
                                    onClick = {
                                        val prefs = context.getSharedPreferences("video_record", Context.MODE_PRIVATE)

                                        val nextSaved = prefs
                                            .getStringSet("saved_ips", emptySet())
                                            .orEmpty()
                                            .toMutableSet()
                                            .apply {
                                                remove(savedIp)
                                            }

                                        prefs.edit()
                                            .putStringSet("saved_ips", nextSaved)
                                            .apply()

                                        savedIps = nextSaved.toList()
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// IP地址验证
private fun isValidIp(ip: String): Boolean {
    val parts = ip.split(".")
    if (parts.size != 4) return false
    return parts.all { part ->
        part.toIntOrNull()?.let { it in 0..255 } ?: false
    }
}
