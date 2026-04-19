package com.example.ridebuddy.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Iteratively unwraps the Context to find the underlying Activity.
 * Useful in Jetpack Compose and AppCompat environments where the base context is often wrapped.
 */
fun Context.getActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}
