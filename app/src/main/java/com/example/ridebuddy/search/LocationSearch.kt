package com.example.ridebuddy.search

import android.location.Address

interface LocationSearch {
    suspend fun search(query: String): List<Address>
}
