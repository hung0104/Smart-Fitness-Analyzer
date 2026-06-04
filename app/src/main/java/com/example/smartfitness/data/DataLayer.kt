package com.example.smartfitness.data

import androidx.room.*
import com.example.smartfitness.R
import kotlinx.coroutines.flow.Flow

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

// THIẾT KẾ MỚI: Mô hình cấu trúc dữ liệu Bài tập
data class Exercise(
    val name: String,
    val primaryMuscles: String,
    val description: String,
    val imageResId: Int
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val userId: Int = 0,
    val name: String,
    val heightCm: Float,
    val weightKg: Float,
    val age: Int,
    val gender: Gender,
    val goal: Goal
)

@Entity(tableName = "fitness_history")
data class FitnessRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val date: Long = System.currentTimeMillis(),
    val bmi: Float,
    val bodyShape: String,
    val weight: Float,
    val swr: Float,
    val whr: Float
)

@Dao
interface FitnessDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: FitnessRecord)

    @Query("SELECT * FROM fitness_history WHERE userId = :userId ORDER BY date DESC")
    fun getRecordsForUser(userId: Int): Flow<List<FitnessRecord>>
}

@Database(entities = [UserEntity::class, FitnessRecord::class], version = 3, exportSchema = false)
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
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class FitnessRepository(private val dao: FitnessDao) {
    val allUsers: Flow<List<UserEntity>> = dao.getAllUsers()

    suspend fun saveUser(user: UserEntity): Long {
        return dao.insertUser(user)
    }

    fun getHistoryForUser(userId: Int): Flow<List<FitnessRecord>> {
        return dao.getRecordsForUser(userId)
    }

    suspend fun saveRecord(record: FitnessRecord) {
        dao.insertRecord(record)
    }

    // THIẾT KẾ MỚI: Hàm cung cấp danh sách bài tập mẫu cục bộ theo Mục tiêu (Goal)
    // HÀM MỚI: Đã thay thế toàn bộ defaultImg bằng ảnh thực tế từ thư mục drawable
    fun getExercisesByGoal(goal: Goal): List<Exercise> {
        return when (goal) {
            Goal.BUILD_MUSCLE -> listOf(
                Exercise(
                    name = "Bench Press",
                    primaryMuscles = "Chest | Triceps | Front Delts",
                    description = "Nằm trên ghế phẳng, đẩy thanh tạ đòn thẳng lên và hạ xuống từ từ chạm nhẹ cơ ngực.",
                    imageResId = R.drawable.ex_bench_press // Tên file ảnh: ex_bench_press.png
                ),
                Exercise(
                    name = "Squat",
                    primaryMuscles = "Quads | Hamstrings | Glutes",
                    description = "Gánh tạ trên vai, hạ thấp hông vuông góc với sàn rồi dùng lực đùi đẩy người đứng thẳng dậy.",
                    imageResId = R.drawable.ex_squat // Tên file ảnh: ex_squat.png
                ),
                Exercise(
                    name = "Barbell Row",
                    primaryMuscles = "Back | Biceps | Core",
                    description = "Cúi người góc 45 độ, giữ thẳng lưng, dùng cơ lưng kéo thanh tạ đòn sát về phía bụng.",
                    imageResId = R.drawable.ex_barbell_row // Tên file ảnh: ex_barbell_row.png
                )
            )
            Goal.LOSE_FAT -> listOf(
                Exercise(
                    name = "Jumping Jacks",
                    primaryMuscles = "Full Body | Cardio",
                    description = "Đứng thẳng, nhảy bật dang rộng hai chân kết hợp vỗ hai tay vào nhau phía trên đầu.",
                    imageResId = R.drawable.ex_jumping_jacks // Tên file ảnh: ex_jumping_jacks.png
                ),
                Exercise(
                    name = "Burpees",
                    primaryMuscles = "Full Body | Chest | Core",
                    description = "Sự kết hợp liên tục giữa tư thế chống đẩy (Push-up) và bật nhảy cao thu người tại chỗ.",
                    imageResId = R.drawable.ex_burpees // Tên file ảnh: ex_burpees.png
                ),
                Exercise(
                    name = "Mountain Climbers",
                    primaryMuscles = "Core | Shoulders | Cardio",
                    description = "Giữ tư thế chống đẩy, liên tục đẩy luân phiên từng đầu gối áp sát về phía ngực.",
                    imageResId = R.drawable.ex_mountain_climbers // Tên file ảnh: ex_mountain_climbers.png
                )
            )
            Goal.MAINTAIN -> listOf(
                Exercise(
                    name = "Push Up",
                    primaryMuscles = "Chest | Shoulders | Triceps",
                    description = "Giữ toàn thân thẳng như một tấm ván, hạ người sát sàn bằng lực tay ngực rồi đẩy lên.",
                    imageResId = R.drawable.ex_push_up // Tên file ảnh: ex_push_up.png
                ),
                Exercise(
                    name = "Plank",
                    primaryMuscles = "Core | Abs | Lower Back",
                    description = "Chống hai khuỷu tay vuông góc xuống sàn, giữ toàn bộ thân người thẳng cố định lâu nhất có thể.",
                    imageResId = R.drawable.ex_plank // Tên file ảnh: ex_plank.png
                ),
                Exercise(
                    name = "Lunges",
                    primaryMuscles = "Quads | Glutes | Hamstrings",
                    description = "Bước một chân rộng lên phía trước, hạ thấp trọng tâm sao cho cả hai đầu gối tạo góc 90 độ.",
                    imageResId = R.drawable.ex_lunges // Tên file ảnh: ex_lunges.png
                )
            )
        }
    }
}