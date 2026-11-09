package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AthletixDao {

    // --- USERS & AUTH ---
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun observeUser(userId: Long): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Long): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: Long)

    // --- EXERCISES ---
    @Query("SELECT * FROM exercises ORDER BY muscleGroup ASC, nameFr ASC")
    fun observeAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun countExercises(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    // --- WORKOUT PROGRAMS ---
    @Query("SELECT * FROM workout_programs WHERE userId = 0 OR userId = :userId ORDER BY isCustom DESC, id ASC")
    fun observeProgramsForUser(userId: Long): Flow<List<WorkoutProgramEntity>>

    @Query("SELECT COUNT(*) FROM workout_programs WHERE userId = 0")
    suspend fun countGlobalPrograms(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrograms(programs: List<WorkoutProgramEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgram(program: WorkoutProgramEntity): Long

    @Update
    suspend fun updateProgram(program: WorkoutProgramEntity)

    @Query("DELETE FROM workout_programs WHERE id = :programId")
    suspend fun deleteProgram(programId: Long)

    // --- WORKOUT SESSIONS & SETS ---
    @Query("SELECT * FROM workout_sessions WHERE userId = :userId ORDER BY startTimeEpochMs DESC")
    fun observeWorkoutSessions(userId: Long): Flow<List<WorkoutSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSession(session: WorkoutSessionEntity): Long

    @Query("UPDATE workout_sessions SET syncStatus = :status WHERE userId = :userId")
    suspend fun updateAllSessionsSyncStatus(userId: Long, status: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseSets(sets: List<ExerciseSetEntity>)

    @Query("SELECT * FROM exercise_sets WHERE userId = :userId ORDER BY id DESC")
    fun observeAllSetsForUser(userId: Long): Flow<List<ExerciseSetEntity>>

    // --- FOODS & NUTRITION ---
    @Query("SELECT * FROM foods ORDER BY originRegion ASC, name ASC")
    fun observeAllFoods(): Flow<List<FoodItemEntity>>

    @Query("SELECT COUNT(*) FROM foods")
    suspend fun countFoods(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoods(foods: List<FoodItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodItemEntity): Long

    @Query("SELECT * FROM meals WHERE userId = :userId ORDER BY timestampMs DESC")
    fun observeMealsForUser(userId: Long): Flow<List<MealLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealLog(meal: MealLogEntity): Long

    @Query("DELETE FROM meals WHERE id = :mealId")
    suspend fun deleteMealLog(mealId: Long)

    // --- WATER LOGS ---
    @Query("SELECT * FROM water_logs WHERE userId = :userId ORDER BY timestampMs DESC")
    fun observeWaterLogs(userId: Long): Flow<List<WaterLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLog(log: WaterLogEntity): Long

    // --- SLEEP & RECOVERY LOGS ---
    @Query("SELECT * FROM sleep_logs WHERE userId = :userId ORDER BY timestampMs DESC")
    fun observeSleepLogs(userId: Long): Flow<List<SleepLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepLog(log: SleepLogEntity): Long

    // --- BODY MEASUREMENTS ---
    @Query("SELECT * FROM measurements WHERE userId = :userId ORDER BY timestampMs ASC")
    fun observeMeasurements(userId: Long): Flow<List<BodyMeasurementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(measurement: BodyMeasurementEntity): Long

    // --- HABITS ---
    @Query("SELECT * FROM habits WHERE userId = :userId ORDER BY id ASC")
    fun observeHabits(userId: Long): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabit(habitId: Long)

    // --- NOTIFICATIONS / REMINDERS ---
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY id ASC")
    fun observeReminders(userId: Long): Flow<List<ReminderSettingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ReminderSettingEntity>)

    @Update
    suspend fun updateReminder(reminder: ReminderSettingEntity)

    // --- AI CONVERSATIONS ---
    @Query("SELECT * FROM ai_conversations WHERE userId = :userId ORDER BY timestampMs ASC")
    fun observeAiMessages(userId: Long): Flow<List<AiMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiMessage(message: AiMessageEntity): Long

    @Query("DELETE FROM ai_conversations WHERE userId = :userId")
    suspend fun clearAiMessages(userId: Long)

    // --- SYNC QUEUE ---
    @Query("SELECT * FROM sync_queue WHERE userId = :userId ORDER BY timestampMs DESC")
    fun observeSyncQueue(userId: Long): Flow<List<SyncQueueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncQueueItem(item: SyncQueueEntity): Long

    @Query("UPDATE sync_queue SET status = :newStatus WHERE userId = :userId")
    suspend fun updateSyncQueueStatus(userId: Long, newStatus: String)

    // --- CLEANUP FOR USER DELETION ---
    @Query("DELETE FROM workout_sessions WHERE userId = :userId")
    suspend fun deleteSessionsByUser(userId: Long)

    @Query("DELETE FROM exercise_sets WHERE userId = :userId")
    suspend fun deleteSetsByUser(userId: Long)

    @Query("DELETE FROM meals WHERE userId = :userId")
    suspend fun deleteMealsByUser(userId: Long)

    @Query("DELETE FROM water_logs WHERE userId = :userId")
    suspend fun deleteWaterByUser(userId: Long)

    @Query("DELETE FROM sleep_logs WHERE userId = :userId")
    suspend fun deleteSleepByUser(userId: Long)

    @Query("DELETE FROM measurements WHERE userId = :userId")
    suspend fun deleteMeasurementsByUser(userId: Long)

    @Query("DELETE FROM habits WHERE userId = :userId")
    suspend fun deleteHabitsByUser(userId: Long)
}
