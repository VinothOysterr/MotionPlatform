package com.example.motionplatform

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.motionplatform.ui.theme.MotionPlatformTheme
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.PrintWriter
import java.net.Socket


class MainActivity : ComponentActivity() {
    private var serialPort: UsbSerialPort? = null
    private lateinit var vibrator: Vibrator

    companion object {
        private const val ACTION_USB_PERMISSION = "com.example.minibricks_control.USB_PERMISSION"
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (ACTION_USB_PERMISSION == intent?.action) {
                synchronized(this) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.let { initializeSerial { /* Update handled in state */ } }
                    } else {
                        Log.e("USB", "Permission denied for device $device")
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        // Initialize vibrator service
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        setContent {
            MotionPlatformTheme {
                // State to track connection status
                var isConnected by remember { mutableStateOf(false) }

                // Navigation controller
                val navController = rememberNavController()

                // LaunchedEffect to initialize USB connection on app start
                LaunchedEffect(Unit) {
                    initializeSerial { newStatus ->
                        isConnected = newStatus
                    }
                }
                NavHost(
                    navController = navController,
                    startDestination = "main_screen"
                ) {
                    composable("main_screen") {
                        MainScreen(
                            isConnected = isConnected,
                            onRefresh = {
                                triggerHapticFeedback()
                                Log.d("IP", "${readPrefIp(context = this@MainActivity)}")
                                // Close existing connection if any
                                try {
                                    serialPort?.close()
                                } catch (e: Exception) {
                                    Log.e("USB", "Error closing serial port: ${e.message}")
                                }
                                serialPort = null
                                // Reinitialize USB connection
                                initializeSerial { newStatus ->
                                    isConnected = newStatus
                                }
                            },
                            onSendCommand = { command: String ->
                                triggerHapticFeedback()
                                try {
                                    val jsonObject = JSONObject(command)
                                    val serialCommand = jsonObject.getString("serial")
                                    val tcpCommand = jsonObject.getString("TCP")

                                    sendSerialCommand(serialCommand)
//                                    sendTCPSerialCommand(serialCommand)
                                    sendTCPCommand(tcpCommand)
                                } catch (e: Exception) {
                                    Log.e("onSendCommand", "Invalid JSON format: $command", e)
                                }
                            },
                            onNavigateToPair = {
                                triggerHapticFeedback()
//                                navController.navigate("scan_network")
                                navController.navigate("group_quest")
                            }
                        )
                    }
                    composable ("scan_network") {
                        NetworkScannerScreen(
                            onBack = {
                                triggerHapticFeedback()
                                navController.popBackStack()
                            },
                            onScan = { triggerHapticFeedback() }
                        )
                    }
                    composable ("group_quest") {
                        GroupQuest(
                            onBack = {
                                triggerHapticFeedback()
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            serialPort?.close()
            serialPort = null
        } catch (e: Exception) {
            Log.e("USB", "Error closing serial port on destroy: ${e.message}")
        }
    }

    private fun initializeSerial(onConnectionStatus: (Boolean) -> Unit) {
        val usbManager = getSystemService(USB_SERVICE) as UsbManager
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)

        if (availableDrivers.isNotEmpty()) {
            val driver = availableDrivers[0]
            val device = driver.device

            val permissionIntent = PendingIntent.getBroadcast(
                this, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
            )

            if (!usbManager.hasPermission(device)) {
                usbManager.requestPermission(device, permissionIntent)
                Log.e("USB", "Requesting USB permission")
                onConnectionStatus(false)
                return
            }

            val connection = usbManager.openDevice(device)
            if (connection == null) {
                Log.e("USB", "Failed to open USB connection")
                onConnectionStatus(false)
                return
            }

            serialPort = driver.ports[0]
            try {
                serialPort?.open(connection)
                serialPort?.setParameters(9600, 8, 1, UsbSerialPort.PARITY_NONE)
                Log.d("USB", "Serial port initialized successfully")
                onConnectionStatus(true)
            } catch (e: Exception) {
                Log.e("USB", "Error opening serial port: ${e.message}")
                onConnectionStatus(false)
            }
        } else {
            Log.e("USB", "No USB serial device detected")
            onConnectionStatus(false)
        }
    }

    private fun sendSerialCommand(command: String) {
        try {
            serialPort?.write(command.toByteArray(), 1000)
            Log.d("USB", "Sent command: $command")
        } catch (e: Exception) {
            Log.e("USB", "Error sending command: ${e.message}")
        }
    }

    private fun triggerHapticFeedback() {
        val effect = VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(effect)
    }

    private fun readPrefIp(context: Context): List<String> {
        // Get SharedPreferences instance
        val prefs: SharedPreferences = context.getSharedPreferences("NetworkScannerPrefs", Context.MODE_PRIVATE)

        // Read the stored IPs string and convert it back to a list
        val savedIpsString = prefs.getString("discovered_ips", null)
        return savedIpsString?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    }

    private fun sendTCPCommand(command: String) {
        Log.d("TCP Command", command)
//        val ipList = readPrefIp(this)
        val ipList = listOf("192.168.0.111")
        if (ipList.isEmpty()) {
            Log.e("TCP", "No IP addresses found")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            ipList.forEach { ip ->
                try {
                    val port = 8080
                    Socket(ip, port).use { socket ->
                        val writer = PrintWriter(socket.getOutputStream(), true)
                        writer.println(command)
                        Log.d("TCP", "Sent command '$command' to $ip:$port")
                    }
                } catch (e: Exception) {
                    Log.e("TCP", "Error sending command to $ip: ${e.message}")
                }
            }
        }
    }
    private fun sendTCPSerialCommand(command: String) {
        val ipAdd = "192.168.0.117"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val port = 23554
                Socket(ipAdd, port).use { socket ->
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    writer.println(command)
                    Log.d("Serial", "Sent command '$command' to $ipAdd:$port")
                }
            } catch (e: Exception) {
                Log.e("Serial", "Error sending command to $ipAdd: ${e.message}")
            }
        }
    }
}