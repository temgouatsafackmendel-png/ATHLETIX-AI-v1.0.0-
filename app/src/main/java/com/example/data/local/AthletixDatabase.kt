package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        ExerciseEntity::class,
        WorkoutProgramEntity::class,
        WorkoutSessionEntity::class,
        ExerciseSetEntity::class,
        FoodItemEntity::class,
        MealLogEntity::class,
        WaterLogEntity::class,
        SleepLogEntity::class,
        BodyMeasurementEntity::class,
        HabitEntity::class,
        ReminderSettingEntity::class,
        AiMessageEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AthletixDatabase : RoomDatabase() {
    abstract fun athletixDao(): AthletixDao

    companion object {
        @Volatile
        private var INSTANCE: AthletixDatabase? = null

        fun getInstance(context: Context): AthletixDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AthletixDatabase::class.java,
                    "athletix_ai_platform.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
