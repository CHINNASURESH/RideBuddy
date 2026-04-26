package com.example.ridebuddy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSetupScreen(
    onGroupSelected: (String) -> Unit
) {
    var groupCodeInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Group Ride Setup",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = {
                val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
                val randomCode = (1..6)
                    .map { kotlin.random.Random.nextInt(0, charPool.size) }
                    .map(charPool::get)
                    .joinToString("")
                onGroupSelected(randomCode)
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        ) {
            Text("Create New Group (Generate Code)")
        }

        Text(
            text = "OR",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = groupCodeInput,
            onValueChange = { groupCodeInput = it },
            label = { Text("Enter Group Code") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            singleLine = true
        )

        Button(
            onClick = {
                if (groupCodeInput.isNotBlank()) {
                    onGroupSelected(groupCodeInput)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = groupCodeInput.isNotBlank()
        ) {
            Text("Join Group")
        }
    }
}
