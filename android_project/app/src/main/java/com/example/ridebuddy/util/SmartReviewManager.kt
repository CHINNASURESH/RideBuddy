package com.example.ridebuddy.util

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartReviewManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val reviewManager = ReviewManagerFactory.create(context)

    companion object {
        private const val PREFS_NAME = "RideBuddyReviewPrefs"
        private const val KEY_COMPLETED_RIDES = "completed_rides_count"
        private const val KEY_LAST_REVIEW_PROMPT_TIME = "last_review_prompt_time"

        // Settings for trigger logic
        private const val MIN_RIDES_BEFORE_PROMPT = 3
        private const val COOLDOWN_DAYS = 14
        private const val MILLIS_IN_DAY = 24 * 60 * 60 * 1000L
    }

    /**
     * Call this when a user successfully completes a ride.
     * It increments the ride count and checks if we should prompt for a review.
     *
     * @param activity The current Activity context required by the Play Review API.
     */
    fun onRideCompleted(activity: Activity) {
        val currentRides = prefs.getInt(KEY_COMPLETED_RIDES, 0) + 1
        prefs.edit().putInt(KEY_COMPLETED_RIDES, currentRides).apply()

        if (shouldPromptForReview(currentRides)) {
            requestAndLaunchReviewFlow(activity)
        }
    }

    private fun shouldPromptForReview(currentRides: Int): Boolean {
        if (currentRides < MIN_RIDES_BEFORE_PROMPT) {
            return false
        }

        val lastPromptTime = prefs.getLong(KEY_LAST_REVIEW_PROMPT_TIME, 0)
        val currentTime = System.currentTimeMillis()
        val daysSinceLastPrompt = (currentTime - lastPromptTime) / MILLIS_IN_DAY

        // Only prompt if it's the first time (lastPromptTime == 0) or enough time has passed
        return lastPromptTime == 0L || daysSinceLastPrompt >= COOLDOWN_DAYS
    }

    private fun requestAndLaunchReviewFlow(activity: Activity) {
        val request = reviewManager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // We got the ReviewInfo object
                val reviewInfo = task.result
                val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener { _ ->
                    // The flow has finished. The API does not indicate whether the user
                    // reviewed or not, or even whether the review dialog was shown.
                    // Thus, no matter the result, we update the last prompt time.
                    prefs.edit().putLong(KEY_LAST_REVIEW_PROMPT_TIME, System.currentTimeMillis()).apply()
                }
            } else {
                // There was some problem, log or handle the error.
                // We do not update the prompt time here so we can try again later.
            }
        }
    }
}
