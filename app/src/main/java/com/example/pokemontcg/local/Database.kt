package com.example.pokemontcg.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pokemontcg.local.dao.CardDao
import com.example.pokemontcg.local.dao.SetDao
import com.example.pokemontcg.local.entity.CardEntity
import com.example.pokemontcg.local.entity.SetEntity

@Database(entities = [CardEntity::class, SetEntity::class], version = 8)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun setDao(): SetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "card_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}