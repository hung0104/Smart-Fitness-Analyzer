package com.example.smartfitness.domain

import android.graphics.Bitmap
import com.example.smartfitness.data.BodyMetrics
import com.example.smartfitness.data.Gender
import com.example.smartfitness.data.Goal
import com.example.smartfitness.data.UserProfile
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.pow

// --- 1. BỘ TÍNH TOÁN SỨC KHỎE ---
object HealthCalculator {
    fun calculateBMI(weightKg: Float, heightCm: Float): Float {
        val heightM = heightCm / 100f
        return weightKg / heightM.pow(2)
    }

    fun getBMICategory(bmi: Float): String = when {
        bmi < 18.5f -> "Thiếu cân"
        bmi in 18.5f..24.9f -> "Bình thường"
        bmi in 25.0f..29.9f -> "Thừa cân"
        else -> "Béo phì"
    }

    fun calculateTDEE(profile: UserProfile): Float {
        // Công thức BMR (Mifflin-St Jeor)
        val bmr = if (profile.gender == Gender.MALE) {
            (10 * profile.weightKg) + (6.25f * profile.heightCm) - (5 * profile.age) + 5
        } else {
            (10 * profile.weightKg) + (6.25f * profile.heightCm) - (5 * profile.age) - 161
        }
        // Áp dụng hệ số vận động trung bình (1.55)
        return bmr * 1.55f
    }

    fun getCalorieTarget(tdee: Float, goal: Goal): Float = when(goal) {
        Goal.LOSE_FAT -> tdee - 500f
        Goal.BUILD_MUSCLE -> tdee + 300f
        Goal.MAINTAIN -> tdee
    }

    // Trả về Protein, Carbs, Fat tính theo gram
    fun getMacros(calories: Float, goal: Goal): Triple<Int, Int, Int> {
        val (pPct, cPct, fPct) = when(goal) {
            Goal.LOSE_FAT -> Triple(0.40f, 0.35f, 0.25f)
            Goal.BUILD_MUSCLE -> Triple(0.30f, 0.50f, 0.20f)
            Goal.MAINTAIN -> Triple(0.30f, 0.40f, 0.30f)
        }
        val proteinGrams = ((calories * pPct) / 4).toInt()
        val carbsGrams = ((calories * cPct) / 4).toInt()
        val fatGrams = ((calories * fPct) / 9).toInt()
        return Triple(proteinGrams, carbsGrams, fatGrams)
    }
}

// --- 2. BỘ XỬ LÝ HÌNH ẢNH OPENCV ---
object OpenCVProcessor {
    fun processBodyImage(bitmap: Bitmap): BodyMetrics {
        val mat = Mat()
        // Chuyển ảnh chụp (Bitmap) sang định dạng của OpenCV (Mat)
        Utils.bitmapToMat(bitmap, mat)

        // Bước 1 - Tiền xử lý
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.GaussianBlur(mat, mat, Size(5.0, 5.0), 0.0)
        Imgproc.Canny(mat, mat, 50.0, 150.0)

        // Bước 2 - Nhận diện đường viền cơ thể
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(mat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        if (contours.isEmpty()) return fallbackMetrics()

        // Chọn đường viền lớn nhất (chính là cơ thể người)
        val largestContour = contours.maxByOrNull { Imgproc.contourArea(it) } ?: return fallbackMetrics()

        // Bước 3 - Trích xuất số đo
        val boundingRect = Imgproc.boundingRect(largestContour)

        // Ước lượng các vị trí trên cơ thể dựa vào chiều cao của khung hình
        val top20Y = boundingRect.y + (boundingRect.height * 0.20).toInt() // Vai
        val mid50Y = boundingRect.y + (boundingRect.height * 0.50).toInt() // Eo
        val bot40Y = boundingRect.y + (boundingRect.height * 0.80).toInt() // Hông

        val shoulderWidth = getWidthAtY(largestContour, top20Y, 10).toFloat()
        val waistWidth = getWidthAtY(largestContour, mid50Y, 10).toFloat()
        val hipWidth = getWidthAtY(largestContour, bot40Y, 10).toFloat()

        // Chống lỗi chia cho số 0
        if (waistWidth <= 0f || hipWidth <= 0f) return fallbackMetrics()

        val swr = shoulderWidth / waistWidth
        val whr = waistWidth / hipWidth

        // Bước 4 - Phân loại hình dáng cơ thể
        val shape = when {
            swr > 1.4 && whr < 0.85 -> "Dáng Thể Thao (V-Shape)"
            swr in 1.2..1.4 && whr < 0.90 -> "Cân đối"
            whr > 0.95 -> "Dáng Quả Táo (Nguy cơ mỡ bụng cao)"
            swr < 1.1 -> "Dáng Quả Lê"
            else -> "Dáng Chữ Nhật (Trung bình)"
        }

        return BodyMetrics(swr, whr, shape)
    }

    // Hàm đo chiều rộng của cơ thể tại một mốc nhất định
    private fun getWidthAtY(contour: MatOfPoint, targetY: Int, tolerance: Int): Int {
        val points = contour.toList().filter { abs(it.y - targetY) <= tolerance }
        if (points.isEmpty()) return 1
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        return (maxX - minX).toInt()
    }

    // Dữ liệu dự phòng nếu ảnh quá mờ không quét được
    private fun fallbackMetrics() = BodyMetrics(1.2f, 0.85f, "Không rõ (Cần chụp lại)")
}