package com.example.pitjarusproject.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Suppress("SpellCheckingInspection")
@Database(entities = [Toko::class], version = 1, exportSchema = false)
abstract class TokoDatabase : RoomDatabase() {
    abstract val tokoDao: TokoDao

    companion object {
        @Volatile
        private var INSTANCE: TokoDatabase? = null

        fun getInstance(context: Context): TokoDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        TokoDatabase::class.java,
                        "toko_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}