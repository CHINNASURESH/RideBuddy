package com.example.ridebuddy.di

import com.example.ridebuddy.routing.RoutingStateManager
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
    fun provideRoutingStateManager(): RoutingStateManager {
        return RoutingStateManager()
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