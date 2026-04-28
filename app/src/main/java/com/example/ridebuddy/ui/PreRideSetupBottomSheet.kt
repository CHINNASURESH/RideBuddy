package com.example.ridebuddy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreRideSetupBottomSheet(
    distanceMeters: Int,
    etaSeconds: Int,
    selectedVehicle: String,
    onVehicleSelected: (String) -> Unit,
    onStartRide: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Route Setup",
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = androidx.compose.material.icons.Icons.Default.Clear, contentDescription = "Close")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Vehicle Selector
            Text(
                text = "Vehicle Profile",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            VehicleSelector(
                selectedVehicle = selectedVehicle,
                onVehicleSelected = onVehicleSelected
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ETA and Distance
            Text(
                text = "Route Details",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Distance", style = MaterialTheme.typography.bodyMedium)
                    val km = distanceMeters / 1000.0
                    Text(text = String.format("%.1f km", km), style = MaterialTheme.typography.titleLarge)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "ETA", style = MaterialTheme.typography.bodyMedium)
                    val minutes = (etaSeconds / 60) % 60
                    val hours = etaSeconds / 3600
                    val timeStr = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
                    Text(text = timeStr, style = MaterialTheme.typography.titleLarge)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onStartRide,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Navigation")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun VehicleSelector(
    selectedVehicle: String,
    onVehicleSelected: (String) -> Unit
) {
    val vehicles = listOf("Bike", "Car", "Bus")
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        vehicles.forEach { vehicle ->
            FilterChip(
                selected = selectedVehicle == vehicle,
                onClick = { onVehicleSelected(vehicle) },
                label = { Text(vehicle) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
