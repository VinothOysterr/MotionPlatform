package com.example.motionplatform

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupQuest(onBack: () -> Unit) {
    val context = LocalContext.current
    val checkboxPrefs = remember { CheckboxPrefHelper(context) }
    val listPrefHelper = remember { ListPrefHelper(context) }

    val PREF_KEY = "show_names_list"

    val showNames = remember {
        mutableStateListOf<String>().apply {
            val saved = listPrefHelper.getStringList(PREF_KEY)
            if (saved.isEmpty()) {
                addAll(
                    listOf(
                        "Mission to Mars",
                        "International Space Station",
                        "Roller Coaster Adventure",
                        "Journey to Moon"
                    )
                )
            } else {
                addAll(saved)
            }
        }
    }

    var isDialogOpen by remember { mutableStateOf(false) }

    // Automatically save when the list changes
    LaunchedEffect(showNames) {
        snapshotFlow { showNames.toList() }.collect {
            listPrefHelper.saveStringList(PREF_KEY, it)
        }
    }

    Box(
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
                    title = { Text("Content Selection") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF131313),
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    actions = {
                        IconButton(onClick = { isDialogOpen = true }) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = "Edit",
                                tint = Color.White
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items((1..5).toList()) { i ->
                    var expandable by remember { mutableStateOf(false) }

                    // Maintain checkbox states for each card's items
                    val checkStates = remember {
                        mutableStateMapOf<Int, Boolean>().apply {
                            val savedStates = checkboxPrefs.getAllStates()
                            putAll(savedStates)
                        }
                    }

                    val textToShow = if (i > showNames.size) {
                        showNames[(i - 1) % showNames.size]
                    } else {
                        showNames[i - 1]
                    }

                    ElevatedCard(
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxWidth()
                            .clickable { expandable = !expandable }
                            .animateContentSize(),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 8.dp
                        ),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = Color.Transparent, // Background color of the card
                            contentColor = Color.White // Color for content (e.g., text) inside the card
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF00BCD4),
                                            Color(0xFF3F51B5)
                                        ) // Purple to blue
                                    )
                                ) // Apply gradient here
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = textToShow,
                                )

                                if (expandable) {
                                    val start = (i - 1) * 5 + 1
                                    val end = i * 5

                                    Row(
                                        modifier = Modifier
                                            .padding(vertical = 4.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        for (j in start..end) {
                                            val checked = checkStates[j] != false

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Checkbox(
                                                    checked = checked,
                                                    onCheckedChange = { isChecked ->
                                                        checkStates[j] = isChecked
                                                        checkboxPrefs.saveCheckboxState(
                                                            j,
                                                            isChecked
                                                        )
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = Color(0xFFE3FF7A), // Background color when checked
                                                        uncheckedColor = Color(0xFFB0BEC5), // Background color when unchecked
                                                        checkmarkColor = Color.White, // Color of the checkmark
                                                        disabledCheckedColor = Color.Gray, // Background color when checked and disabled
                                                        disabledUncheckedColor = Color.LightGray // Background color when unchecked and disabled
                                                    )
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .padding(vertical = 4.dp)
                                                ) {
                                                    Text(text ="$j")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (isDialogOpen) {
        EditNamesDialog(
            currentNames = showNames,
            onCancel = { isDialogOpen = false },
            onSave = { editedNames ->
                showNames.clear()
                showNames.addAll(editedNames)
                isDialogOpen = false
            }
        )
    }
}
