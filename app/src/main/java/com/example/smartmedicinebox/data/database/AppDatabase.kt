package com.example.smartmedicinebox.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.smartmedicinebox.data.dao.MedicationRecordDao
import com.example.smartmedicinebox.data.dao.MedicineDao
import com.example.smartmedicinebox.data.model.Medicine
import com.example.smartmedicinebox.data.model.MedicationRecord
import com.example.smartmedicinebox.data.model.MedicationStatus

class Converters {
    @TypeConverter
    fun fromStatus(status: MedicationStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): MedicationStatus = MedicationStatus.valueOf(value)
}

@Database(
    entities = [Medicine::class, MedicationRecord::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun medicineDao(): MedicineDao
    abstract fun medicationRecordDao(): MedicationRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_medicine_box_database"
                ).addMigrations(MIGRATION_1_2).build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE medicines SET compartment = 1")
            }
        }
    }
}
