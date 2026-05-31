package com.example.smartfitness.presentation.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartfitness.data.*
import com.example.smartfitness.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FitnessViewModel(private val repository: FitnessRepository) : ViewModel() {

    // Trạng thái lưu trữ thông tin người dùng
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile

    // Trạng thái lưu trữ kết quả phân tích cơ thể
    private val _bodyMetrics = MutableStateFlow<BodyMetrics?>(null)
    val bodyMetrics: StateFlow<BodyMetrics?> = _bodyMetrics

    // Trạng thái lưu trữ ảnh đã chụp
    private val _capturedImage = MutableStateFlow<Bitmap?>(null)
    val capturedImage: StateFlow<Bitmap?> = _capturedImage

    // Lịch sử đo lấy từ cơ sở dữ liệu
    val history = repository.history

    // Hàm lưu thông tin nhập từ màn hình 1
    fun saveUserProfile(height: String, weight: String, age: String, gender: Gender, goal: Goal): Boolean {
        return try {
            val h = height.toFloat()
            val w = weight.toFloat()
            val a = age.toInt()
            if (h <= 0 || w <= 0 || a <= 0) return false

            _userProfile.value = UserProfile(h, w, a, gender, goal)
            true
        } catch (e: Exception) {
            // Nếu người dùng nhập sai định dạng (ví dụ nhập chữ vào ô số), trả về false
            false
        }
    }

    // Hàm xử lý ảnh sau khi chụp ở màn hình 2
    fun processCapturedImage(bitmap: Bitmap) {
        _capturedImage.value = bitmap

        // Chạy xử lý ảnh nặng trong luồng phụ (background) để không làm đơ ứng dụng
        viewModelScope.launch {
            // Gọi OpenCV để phân tích
            val metrics = OpenCVProcessor.processBodyImage(bitmap)
            _bodyMetrics.value = metrics

            // Lưu kết quả vào cơ sở dữ liệu (Room DB)
            _userProfile.value?.let { profile ->
                val bmi = HealthCalculator.calculateBMI(profile.weightKg, profile.heightCm)
                repository.saveRecord(
                    FitnessRecord(
                        bmi = bmi,
                        bodyShape = metrics.bodyShape,
                        weight = profile.weightKg
                    )
                )
            }
        }
    }
}