package com.example.smartfitness.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- 1. CÁC MÔ HÌNH DỮ LIỆU (MODELS) ---

enum class Goal { BUILD_MUSCLE, LOSE_FAT, MAINTAIN }
enum class Gender { MALE, FEMALE }

data class UserProfile(
    val heightCm: Float,
    val weightKg: Float,
    val age: Int,
    val gender: Gender,
    val goal: Goal
)

data class BodyMetrics(
    val swr: Float,
    val whr: Float,
    val bodyShape: String
)

// --- 2. CƠ SỞ DỮ LIỆU ROOM (ROOM DATABASE) ---

@Entity(tableName = "fitness_history")
data class FitnessRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long = System.currentTimeMillis(),
    val bmi: Float,
    val bodyShape: String,
    val weight: Float
)

@Dao
interface FitnessDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: FitnessRecord)

    @Query("SELECT * FROM fitness_history ORDER BY date DESC")
    fun getAllRecords(): Flow<List<FitnessRecord>>
}

@Database(entities = [FitnessRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fitnessDao(): FitnessDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fitness_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// --- 3. KHO LƯU TRỮ (REPOSITORY) ---
// Dùng để làm cầu nối giữa Database và Giao diện
class FitnessRepository(private val dao: FitnessDao) {
    val history: Flow<List<FitnessRecord>> = dao.getAllRecords()

    suspend fun saveRecord(record: FitnessRecord) {
        dao.insertRecord(record)
    }
}