package com.krishisakhi.farmassistant.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.krishisakhi.farmassistant.Converters
import com.krishisakhi.farmassistant.dao.FarmerProfileDao
import com.krishisakhi.farmassistant.dao.VectorEntryDao
import com.krishisakhi.farmassistant.data.FarmerProfile
import com.krishisakhi.farmassistant.data.VectorEntryEntity

@Database(
    entities = [FarmerProfile::class, VectorEntryEntity::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun farmerProfileDao(): FarmerProfileDao
    abstract fun vectorEntryDao(): VectorEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration 1 -> 2: create vector_entries table
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS vector_entries (" +
                        "id TEXT NOT NULL PRIMARY KEY, " +
                        "farmerId TEXT, " +
                        "vectorBlob BLOB NOT NULL, " +
                        "metadataJson TEXT NOT NULL" +
                        ")"
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_vector_entries_farmerId ON vector_entries(farmerId)")
            }
        }

        // Migration 2 -> 3: add sourceType column, backfill, create index
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE vector_entries ADD COLUMN sourceType TEXT")
                database.execSQL(
                    "UPDATE vector_entries SET sourceType = CASE " +
                        "WHEN metadataJson LIKE '%\"type\":\"farmer_profile\"%' THEN 'farmer_profile' " +
                        "ELSE 'general' END"
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_vector_entries_sourceType ON vector_entries(sourceType)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "farm_assistant_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // Preserve existing data through upgrades
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}