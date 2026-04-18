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
}
