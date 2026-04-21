package com.example.ridebuddy.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.ridebuddy.data.LocationRepository
import dagger.hilt.android.AndroidEntryPoint
import com.example.ridebuddy.util.RemoteConfigManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: LocationRepository

    @Inject
    lateinit var remoteConfigManager: RemoteConfigManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            if (!remoteConfigManager.isSmsFallbackEnabled.value) return
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            for (message in messages) {
                val messageBody = message.messageBody

                if (messageBody != null && SmsCodec.isRideBuddyMessage(messageBody)) {
                    Log.d("SmsReceiver", "Intercepted Ride Buddy SMS")
                    // Abort broadcast so it doesn't show up in the standard SMS app
                    abortBroadcast()

                    val payload = SmsCodec.decode(messageBody)
                    if (payload != null) {
                        Log.d("SmsReceiver", "Decoded SMS Payload: $payload")
                        CoroutineScope(Dispatchers.IO).launch {
                            repository.updateOfflineRiderFromSms(payload)
                        }
                    } else {
                        Log.e("SmsReceiver", "Failed to decode Ride Buddy SMS payload")
                    }
                }
            }
        }
    }
}
