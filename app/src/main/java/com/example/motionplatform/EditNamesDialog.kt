package com.example.motionplatform

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EditNamesDialog(
    currentNames: List<String>,
    onCancel: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val editedNames = remember {
        currentNames.map { mutableStateOf(it) }
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Edit Names") },
        text = {
            Column {
                editedNames.forEachIndexed { index, nameState ->
                    OutlinedTextField(
                        value = nameState.value,
                        onValueChange = { newValue -> nameState.value = newValue },
                        label = { Text("Name ${index + 1}") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(editedNames.map { it.value })
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}