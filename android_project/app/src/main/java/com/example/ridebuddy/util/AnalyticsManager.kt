package com.example.ridebuddy.util

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManager @Inject constructor() {
    private val firebaseAnalytics: FirebaseAnalytics = Firebase.analytics

    fun logProUpgradeView() {
        firebaseAnalytics.logEvent("pro_upgrade_view", null)
    }

    fun logProSubscribeTap() {
        firebaseAnalytics.logEvent("pro_subscribe_tap", null)
    }

    fun logProBillingSuccess() {
        firebaseAnalytics.logEvent("pro_billing_success", null)
    }

    fun logProBillingError(errorCode: Int, errorMessage: String) {
        val bundle = Bundle().apply {
            putInt("error_code", errorCode)
            putString("error_message", errorMessage)
        }
        firebaseAnalytics.logEvent("pro_billing_error", bundle)
    }
}
