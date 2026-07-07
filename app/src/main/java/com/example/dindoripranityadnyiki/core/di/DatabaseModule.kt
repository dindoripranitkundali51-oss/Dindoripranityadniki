package com.example.dindoripranityadnyiki.core.di

import android.content.Context
import com.example.dindoripranityadnyiki.core.data.DataStoreManager
import com.example.dindoripranityadnyiki.core.data.DivineDao
import com.example.dindoripranityadnyiki.core.data.DivineDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DivineDatabase {
        return DivineDatabase.getInstance(context)
    }

    @Provides
    fun provideDivineDao(database: DivineDatabase): DivineDao {
        return database.divineDao()
    }

    @Provides
    @Singleton
    fun provideDataStoreManager(@ApplicationContext context: Context): DataStoreManager {
        return DataStoreManager(context)
    }
}
