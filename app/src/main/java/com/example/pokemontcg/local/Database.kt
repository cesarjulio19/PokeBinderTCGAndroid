package com.example.pokemontcg.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pokemontcg.local.dao.CardDao
import com.example.pokemontcg.local.dao.PersonDao
import com.example.pokemontcg.local.dao.SetDao
import com.example.pokemontcg.local.dao.UserDao
import com.example.pokemontcg.local.entity.CardEntity
import com.example.pokemontcg.local.entity.SetEntity
import com.example.pokemontcg.local.entity.UserEntity
import com.example.pokemontcg.local.entity.PersonEntity

@Database(entities = [CardEntity::class, SetEntity::class, UserEntity::class, PersonEntity::class], version = 14)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun setDao(): SetDao
    abstract fun personDao(): PersonDao
    abstract fun userDao(): UserDao

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