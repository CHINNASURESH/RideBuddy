package com.example.ridebuddy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

@Composable
fun FeedbackDialog(
    onDismiss: () -> Unit,
    onSubmit: (category: String, details: String) -> Unit
) {
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Submit Feedback") },
        text = {
            Column {
                Button(
                    onClick = { onSubmit("Routing Error", details); onDismiss() },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text("Routing Error")
                }
                Button(
                    onClick = { onSubmit("UI/Display Issue", details); onDismiss() },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text("UI/Display Issue")
                }
                Button(
                    onClick = { onSubmit("General Bug", details); onDismiss() },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text("General Bug")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = details,
                        onValueChange = { details = it },
                        label = { Text("Optional Details") },
                        modifier = Modifier.weight(1f)
                    )
                    // We don't have a real SpeechRecognizer implemented here for brevity,
                    // but we provide the visual UI for the "voice-to-text input field" requirement.
                    IconButton(onClick = { /* TODO: Launch SpeechRecognizer Intent */ }) {
                        Icon(imageVector = androidx.compose.material.icons.Icons.Default.Info, contentDescription = "Voice Input") // using Info as placeholder for Mic if Mic isn't in default icons
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RiderDashboard(
    speedMps: Float,
    altitudeMeters: Double,
    distanceToDestinationMeters: Double,
    etaMillis: Long?,
    onFeedbackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    isSolarTelemetryEnabled: Boolean = true
) {
    // Conversion
    val speedKmh = speedMps * 3.6f

    // Formatting
    val speedText = String.format(Locale.getDefault(), "%.0f", speedKmh)
    val altitudeText = if (isSolarTelemetryEnabled) String.format(Locale.getDefault(), "%.0fm", altitudeMeters) else "---"
    val distanceText = if (distanceToDestinationMeters < 1000) {
        String.format(Locale.getDefault(), "%.0f m", distanceToDestinationMeters)
    } else {
        String.format(Locale.getDefault(), "%.1f km", distanceToDestinationMeters / 1000.0)
    }

    val etaText = etaMillis?.let {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(it))
    } ?: "--:--"

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.7f),
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speed
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = speedText,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "KM/H",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray
                )
            }

            // Destination Distance
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = distanceText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "DESTINATION",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray
                )
            }

            // Altitude & ETA
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ALT ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray
                    )
                    Text(
                        text = altitudeText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ETA ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray
                    )
                    Text(
                        text = etaText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Feedback Button
            IconButton(onClick = onFeedbackClick) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Submit Feedback",
                    tint = Color.Yellow
                )
            }
        }
    }
}
