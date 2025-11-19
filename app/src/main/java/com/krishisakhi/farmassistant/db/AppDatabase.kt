package com.krishisakhi.farmassistant.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.krishisakhi.farmassistant.dao.FarmerProfileDao
import com.krishisakhi.farmassistant.data.FarmerProfile

@Database(entities = [FarmerProfile::class], version = 1, exportSchema = false)
@TypeConverters(androidx.room.TypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun farmerProfileDao(): FarmerProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "farm_assistant_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}