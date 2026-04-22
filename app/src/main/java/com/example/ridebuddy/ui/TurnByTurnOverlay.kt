package com.example.ridebuddy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh


import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.ridebuddy.routing.TurnInstruction

@Composable
fun TurnByTurnOverlay(
    currentInstruction: TurnInstruction?,
    distanceToNext: Double,
    modifier: Modifier = Modifier
) {
    if (currentInstruction == null) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = getIconForCommand(currentInstruction.command),
                contentDescription = "Turn direction",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Column {
                Text(
                    text = formatDistance(distanceToNext),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = currentInstruction.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

private fun formatDistance(distanceInMeters: Double): String {
    return if (distanceInMeters < 1000) {
        "${distanceInMeters.toInt()} m"
    } else {
        String.format("%.1f km", distanceInMeters / 1000.0)
    }
}

private fun getIconForCommand(command: Int): ImageVector {
    // See btools.router.VoiceHint
    return when (command) {
        1 -> Icons.Default.KeyboardArrowUp // C
        2 -> Icons.Default.ArrowBack // TL
        3 -> Icons.Default.KeyboardArrowLeft // TSLL
        4 -> Icons.Default.ArrowBack // TSHL
        5 -> Icons.Default.ArrowForward // TR
        6 -> Icons.Default.KeyboardArrowRight // TSLR
        7 -> Icons.Default.ArrowForward // TSHR
        8 -> Icons.Default.KeyboardArrowLeft // KL
        9 -> Icons.Default.KeyboardArrowRight // KR
        10 -> Icons.Default.Refresh // TLU
        11 -> Icons.Default.Refresh // TRU
        13 -> Icons.Default.Refresh // RNDB
        14 -> Icons.Default.Refresh // RNLB
        15 -> Icons.Default.Refresh // TU
        17 -> Icons.Default.KeyboardArrowLeft // EL
        18 -> Icons.Default.KeyboardArrowRight // ER
        else -> Icons.Default.KeyboardArrowUp
    }
}
