package com.example.motionplatform

import android.content.Context
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.motionplatform.ui.theme.MotionPlatformTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import androidx.core.content.edit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScannerScreen(onBack: () -> Unit, onScan: () -> Unit) {
    val context = LocalContext.current
    var ipList by remember { mutableStateOf(listOf<String>()) }
    var isScanning by remember { mutableStateOf(false) }
    var deviceIp by remember { mutableStateOf("Unknown") }

    // Get SharedPreferences instance
    val prefs: SharedPreferences = context.getSharedPreferences("NetworkScannerPrefs", Context.MODE_PRIVATE)

    // Load saved IPs when the composable is first created
    LaunchedEffect(Unit) {
        val savedIps = prefs.getString("discovered_ips", null)?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
        ipList = savedIps
    }

    // Get the device's IP address
    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val dhcpInfo = wifiManager.dhcpInfo
    deviceIp = formatIpAddress(dhcpInfo.ipAddress)

    Box (
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF131313), Color(0xFF363434))
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Scanning Screen") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription =  "Back",
                                tint =  Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF131313),
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White,
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Device IP: $deviceIp",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = Color.White
                )

                Button(
                    onClick = {
                        onScan()
                        isScanning = true
                        CoroutineScope(Dispatchers.IO).launch {
                            val discoveredIps = scanNetwork(context)
                            withContext(Dispatchers.Main) {
                                ipList = discoveredIps
                                // Save to SharedPreferences
                                prefs.edit {
                                    putString("discovered_ips", discoveredIps.joinToString(","))
                                }
                                isScanning = false
                            }
                        }
                    },
                    enabled = !isScanning
                ) {
                    Text(if (isScanning) "Scanning..." else "Scan Network")
                }

                if (isScanning) {
                    CircularProgressIndicator()
                }

                LazyColumn {
                    items(ipList) { ip ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .padding(8.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = ip, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

// The rest of the functions (scanNetwork, getSubnet, formatIpAddress, and Preview) remain unchanged

suspend fun scanNetwork(context: Context): List<String> {
    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val dhcpInfo = wifiManager.dhcpInfo
    val ipAddress = dhcpInfo.ipAddress
    val subnet = getSubnet(ipAddress)
    val deviceIp = formatIpAddress(ipAddress)
    val firstIp = "$subnet.1" // The first IP in the series (e.g., 192.168.0.1)

    val discoveredIps = mutableListOf<String>()
    val jobs = mutableListOf<Job>()

    for (i in 2..254) {
        val ipToCheck = "$subnet.$i"
        if (ipToCheck == deviceIp) continue

        jobs.add(CoroutineScope(Dispatchers.IO).launch {
            try {
                val inetAddress = InetAddress.getByName(ipToCheck)
                if (inetAddress.isReachable(1000)) {
                    discoveredIps.add(ipToCheck)
                }
            } catch (_: Exception) {
                // Ignore unreachable IPs
            }
        })
    }

    jobs.joinAll()
    println("Discovered IPs: $discoveredIps")
    return discoveredIps
}

fun getSubnet(ipAddress: Int): String {
    val ipBytes = byteArrayOf(
        (ipAddress and 0xFF).toByte(),
        ((ipAddress shr 8) and 0xFF).toByte(),
        ((ipAddress shr 16) and 0xFF).toByte(),
        ((ipAddress shr 24) and 0xFF).toByte()
    )
    return "${ipBytes[0].toInt() and 0xFF}.${ipBytes[1].toInt() and 0xFF}.${ipBytes[2].toInt() and 0xFF}"
}

fun formatIpAddress(ipAddress: Int): String {
    return "${ipAddress and 0xFF}.${(ipAddress shr 8) and 0xFF}.${(ipAddress shr 16) and 0xFF}.${(ipAddress shr 24) and 0xFF}"
}

@Preview(
    showBackground = true,
    showSystemUi = false,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES or android.content.res.Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun NetworkScannerScreenPreview() {
    MotionPlatformTheme { NetworkScannerScreen(onBack = {}, onScan = {}) }
}