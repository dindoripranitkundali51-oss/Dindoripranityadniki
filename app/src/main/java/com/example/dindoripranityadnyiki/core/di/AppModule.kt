package com.example.dindoripranityadnyiki.core.di

import com.example.dindoripranityadnyiki.core.util.RemoteConfigManager
import com.example.dindoripranityadnyiki.core.util.PredictiveEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideRemoteConfigManager(): RemoteConfigManager = RemoteConfigManager()

    @Provides
    @Singleton
    fun providePredictiveEngine(): PredictiveEngine = PredictiveEngine()
}
