package com.example.smartfitness.domain

import android.graphics.Bitmap
import com.example.smartfitness.data.*
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.abs

object HealthCalculator {

    fun calculateBMI(weightKg: Float, heightCm: Float): Float {
        val heightM = heightCm / 100
        return weightKg / (heightM * heightM)
    }

    fun getBMICategory(bmi: Float): String = when {
        bmi < 18.5f -> "Thiếu cân"
        bmi in 18.5f..24.9f -> "Bình thường"
        bmi in 25.0f..29.9f -> "Thừa cân"
        else -> "Béo phì"
    }

    fun calculateTDEE(weightKg: Float, heightCm: Float, age: Int, gender: Gender): Float {
        // Công thức BMR (Mifflin-St Jeor)
        val bmr = if (gender == Gender.MALE) {
            (10 * weightKg) + (6.25f * heightCm) - (5 * age) + 5
        } else {
            (10 * weightKg) + (6.25f * heightCm) - (5 * age) - 161
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
        val protein = (calories * pPct / 4).toInt()
        val carbs = (calories * cPct / 4).toInt()
        val fat = (calories * fPct / 9).toInt()
        return Triple(protein, carbs, fat)
    }
}

object OpenCVProcessor {

    fun processBodyImage(bitmap: Bitmap): BodyMetrics {
        val mat = Mat()
        // Chuyển Bitmap thành Mat
        Utils.bitmapToMat(bitmap, mat)

        // 1. Chuyển sang ảnh xám và làm mờ để giảm nhiễu
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.GaussianBlur(mat, mat, Size(5.0, 5.0), 0.0)

        // 2. Nhận diện các đường biên (Canny Edge)
        Imgproc.Canny(mat, mat, 50.0, 150.0)

        // 3. Tìm đường bao (Contours)
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(mat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        if (contours.isEmpty()) return fallbackMetrics()

        // 4. Tìm đường bao lớn nhất (giả định là cơ thể người)
        val largestContour = contours.maxByOrNull { Imgproc.contourArea(it) } ?: return fallbackMetrics()

        // 5. Tính toán các điểm mốc (Vai, Eo, Hông)
        // Đây là thuật toán mô phỏng đơn giản hóa để minh họa OpenCV
        val rect = Imgproc.boundingRect(largestContour)
        val height = rect.height
        val width = rect.width

        val shoulderY = rect.y + (height * 0.2).toInt()
        val waistY = rect.y + (height * 0.5).toInt()
        val hipY = rect.y + (height * 0.7).toInt()

        val shoulderWidth = getWidthAtY(largestContour, shoulderY, 10)
        val waistWidth = getWidthAtY(largestContour, waistY, 10)
        val hipWidth = getWidthAtY(largestContour, hipY, 10)

        // 6. Tính toán các tỷ lệ
        val swr = if (waistWidth > 0) shoulderWidth.toFloat() / waistWidth else 1.0f
        val whr = if (hipWidth > 0) waistWidth.toFloat() / hipWidth else 1.0f

        // Xác định dáng người (Dựa trên tỷ lệ đơn giản)
        val bodyShape = when {
            whr > 0.9f -> "Dáng quả táo (O)"
            whr < 0.75f -> "Dáng đồng hồ cát (X)"
            swr > 1.2f -> "Dáng tam giác ngược (V)"
            else -> "Dáng hình chữ nhật (H)"
        }

        return BodyMetrics(swr, whr, bodyShape)
    }

    private fun getWidthAtY(contour: MatOfPoint, targetY: Int, tolerance: Int): Int {
        val points = contour.toList().filter { abs(it.y - targetY) <= tolerance }
        if (points.isEmpty()) return 0
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        return (maxX - minX).toInt()
    }

    private fun fallbackMetrics() = BodyMetrics(1.0f, 0.8f, "Đang phân tích...")
}
