package com.example.ridebuddy.search

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class GeocoderLocationSearch(private val context: Context) : LocationSearch {
    override suspend fun search(query: String): List<Address> = withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocationName(query, 5, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            continuation.resume(addresses)
                        }
                        override fun onError(errorMessage: String?) {
                            continuation.resume(emptyList())
                        }
                    })
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(query, 5) ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
