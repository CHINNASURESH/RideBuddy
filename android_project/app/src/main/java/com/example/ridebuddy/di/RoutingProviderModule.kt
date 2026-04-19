package com.example.ridebuddy.di

import com.example.ridebuddy.routing.RoutingStateManager
import com.example.ridebuddy.data.LocationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoutingProviderModule {
    @Provides
    @Singleton
    fun provideRoutingStateManager(repository: LocationRepository): RoutingStateManager {
        return RoutingStateManager(repository)
    }

    @Provides
    @Singleton
    fun provideOfflineRoutingEngine(@dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context): com.example.ridebuddy.routing.OfflineRoutingEngine {
        // Provide LocalBRouterEngine, pointing to offline directory
        val offlineDir = context.getExternalFilesDir(null)
        val brouterDir = java.io.File(offlineDir, "offline_data")
        return com.example.ridebuddy.routing.LocalBRouterEngine(context, brouterDir)
    }

}