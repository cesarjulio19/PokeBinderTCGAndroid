package com.example.pokemontcg.di

import android.content.Context
import androidx.room.Room
import com.example.pokemontcg.local.AppDatabase
import com.example.pokemontcg.local.dao.CardDao
import com.example.pokemontcg.local.dao.SetDao
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
    fun provideDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "my_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideCardDao(database: AppDatabase): CardDao {
        return database.cardDao()
    }

    @Provides
    fun provideSetDao(database: AppDatabase): SetDao {
        return database.setDao()
    }
}