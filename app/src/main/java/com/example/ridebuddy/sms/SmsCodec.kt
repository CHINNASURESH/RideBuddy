package com.example.ridebuddy.sms

import android.util.Base64
import kotlin.math.roundToInt

/**
 * Encodes and decodes rider location and status data into a highly compressed format suitable for SMS.
 * Format: "RB#[base64_payload]"
 * Payload format: "userId|lat|lon|heading|status"
 */
object SmsCodec {
    private const val PREFIX = "RB#"

    fun isRideBuddyMessage(message: String): Boolean {
        return message.startsWith(PREFIX)
    }

    fun encode(userId: String, lat: Double, lon: Double, heading: Float, status: String?): String {
        // Round coordinates to 5 decimal places (~1.1m precision)
        val rLat = (lat * 100000).roundToInt() / 100000.0
        val rLon = (lon * 100000).roundToInt() / 100000.0
        val rHeading = heading.roundToInt()

        val statusPart = status ?: ""
        val payload = "$userId|$rLat|$rLon|$rHeading|$statusPart"

        val base64Payload = Base64.encodeToString(payload.toByteArray(), Base64.NO_WRAP)
        return "$PREFIX$base64Payload"
    }

    fun decode(message: String): SmsPayload? {
        if (!isRideBuddyMessage(message)) return null

        try {
            val base64Payload = message.substring(PREFIX.length)
            val payload = String(Base64.decode(base64Payload, Base64.NO_WRAP))
            val parts = payload.split("|")

            if (parts.size >= 4) {
                val userId = parts[0]
                val lat = parts[1].toDouble()
                val lon = parts[2].toDouble()
                val heading = parts[3].toFloat()
                val status = if (parts.size > 4 && parts[4].isNotEmpty()) parts[4] else null

                return SmsPayload(userId, lat, lon, heading, status)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }
}

data class SmsPayload(
    val userId: String,
    val lat: Double,
    val lon: Double,
    val heading: Float,
    val status: String?
)
