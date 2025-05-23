package com.example.motionplatform

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motionplatform.ui.theme.MotionPlatformTheme
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isConnected: Boolean,
    onRefresh: () -> Unit,
    onSendCommand: (String) -> Unit,
    onNavigateToPair: () -> Unit
) {
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
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("MotionPlatform") },
                    actions = {
                        IconButton(onClick = onRefresh) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh USB Connection",
                                tint = if (isConnected) Color.Green else Color.Red
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF131313), // MaterialTheme.colorScheme.surface,
                        titleContentColor = Color.White,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { innerPadding ->
            Greeting(
                modifier = Modifier.padding(innerPadding),
                onSendCommand = onSendCommand,
                onNavigateToPair = onNavigateToPair
            )
        }
    }
}

@Composable
fun Greeting(
    modifier: Modifier = Modifier,
    onSendCommand: (String) -> Unit,
    onNavigateToPair: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(50.dp, Alignment.CenterVertically)
    ) {
        val actions = mapOf(
            "Start" to "u",
            "Stop" to "y",
            "Pause" to "<00",
            "Resume" to ">00",
            "Loop" to "\""
        )

        val tcpActions = mapOf(
            "Start" to "start",
            "Stop" to "stop",
            "Pause" to "pause",
            "Resume" to "resume",
            "Loop" to "loop"
        )

        // Column for Start and Stop buttons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.mp_logo),
                contentDescription = "Play Icon",
                modifier = Modifier.size(120.dp),
            )
            ActionButton(
                icon = "▶ ",
                text = "Start",
                onClick = {
                    val jsonCommand = JSONObject()
                    jsonCommand.put("serial", actions["Start"])
                    jsonCommand.put("TCP", tcpActions["Start"])

                    Log.d("serial/TCP", jsonCommand.toString())
                    onSendCommand(jsonCommand.toString())
                }
            )
            ActionButton(
                icon = "◼  ",
                text = "Stop",
                onClick = {
                    val jsonCommand = JSONObject()
                    jsonCommand.put("serial", actions["Stop"])
                    jsonCommand.put("TCP", tcpActions["Stop"])

                    Log.d("serial/TCP", jsonCommand.toString())
                    onSendCommand(jsonCommand.toString())
                }
            )
        }

        // Define button pairs for each row
        val buttonRows = listOf(
            "Pause" to "Resume",
            "Loop" to "Pair"
        )

        // Generate rows dynamically
        buttonRows.forEach { (first, second) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth() // Row takes full width of parent Column
            ) {
                ActionButton(
                    text = first,
                    icon = "",
                    onClick = {
                        val jsonCommand = JSONObject()
                        jsonCommand.put("serial", actions[first])
                        jsonCommand.put("TCP", tcpActions[first])

                        Log.d("serial/TCP", jsonCommand.toString())
                        onSendCommand(jsonCommand.toString())
                    },
                    modifier = Modifier.weight(1f) // Each button takes equal space
                )
                ActionButton(
                    text = second,
                    icon = "",
                    onClick = if (second == "Pair") {
                        { onNavigateToPair()  }
                    } else {
                        {
                            val jsonCommand = JSONObject()
                            jsonCommand.put("serial", actions[second])
                            jsonCommand.put("TCP", tcpActions[second])

                            Log.d("serial/TCP", jsonCommand.toString())
                            onSendCommand(jsonCommand.toString())
                        }
                    },
                    modifier = Modifier.weight(1f) // Each button takes equal space
                )
            }
        }

        Text(
            text = "Note: MotionPlatform from DBProductions",
            color = Color.White
        )
    }
}

// Reusable button composable
@Composable
private fun ActionButton(
    text: String,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier
            .height(80.dp)
            .clip(CircleShape)
            .border(1.dp, Color.White, CircleShape),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = Color.Transparent
        ),
        elevation = ButtonDefaults.elevatedButtonElevation(),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE3FF7A), // #C7E051
                            Color(0xFF94AD1F)  // #94AD1F
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Row (
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = icon,
                    fontSize = 48.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxHeight()
                )
                Text(
                    text = text,
                    fontSize = 34.sp,
                    color = Color.Black,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = false,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES or android.content.res.Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun MainScreenPreview() {
    MotionPlatformTheme {
        MainScreen(
            isConnected = true,
            onRefresh = { },
            onSendCommand = { },
            onNavigateToPair = { }
        )
    }
}