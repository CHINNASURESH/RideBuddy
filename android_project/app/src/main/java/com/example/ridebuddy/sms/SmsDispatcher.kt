package com.example.ridebuddy.sms

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsDispatcher @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Hardcoded phone numbers for other riders for Phase 6.
    // In a real app, these would come from the group settings in Firestore.
    private val groupPhoneNumbers = listOf(
        "+15551234567",
        "+15557654321"
    )

    fun dispatch(userId: String, lat: Double, lon: Double, heading: Float, status: String?) {
        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            if (smsManager == null) {
                Log.e("SmsDispatcher", "SmsManager is null")
                return
            }

            val message = SmsCodec.encode(userId, lat, lon, heading, status)
            Log.d("SmsDispatcher", "Dispatching SMS: $message")

            for (phoneNumber in groupPhoneNumbers) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            }
        } catch (e: Exception) {
            Log.e("SmsDispatcher", "Error sending SMS: \${e.message}", e)
        }
    }
}
