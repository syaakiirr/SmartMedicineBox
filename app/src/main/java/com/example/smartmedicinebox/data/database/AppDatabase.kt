package com.example.smartmedicinebox.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
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
    version = 1,
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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
