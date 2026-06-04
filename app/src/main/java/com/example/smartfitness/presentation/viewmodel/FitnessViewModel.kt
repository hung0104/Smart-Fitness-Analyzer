package com.example.smartfitness.presentation.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartfitness.data.*
import com.example.smartfitness.domain.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FitnessViewModel(private val repository: FitnessRepository) : ViewModel() {

    val usersList = repository.allUsers.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser

    private val _latestRecord = MutableStateFlow<FitnessRecord?>(null)
    val latestRecord: StateFlow<FitnessRecord?> = _latestRecord

    // THIẾT KẾ MỚI: Luồng quản lý danh sách bài tập được đề xuất
    private val _recommendedExercises = MutableStateFlow<List<Exercise>>(emptyList())
    val recommendedExercises: StateFlow<List<Exercise>> = _recommendedExercises

    private val _bodyMetrics = MutableStateFlow<BodyMetrics?>(null)
    val bodyMetrics: StateFlow<BodyMetrics?> = _bodyMetrics

    private val _capturedImage = MutableStateFlow<Bitmap?>(null)
    val capturedImage: StateFlow<Bitmap?> = _capturedImage

    private var historyObservationJob: Job? = null

    // CẬP NHẬT: Tự động nạp bài tập khi chọn thành viên
    fun selectUser(user: UserEntity) {
        _currentUser.value = user
        _recommendedExercises.value = repository.getExercisesByGoal(user.goal)
        observeLatestRecord(user.userId)
    }

    // CẬP NHẬT: Tự động nạp bài tập khi tạo thành viên mới
    fun createNewUser(name: String, height: String, weight: String, age: String, gender: Gender, goal: Goal): Boolean {
        return try {
            val h = height.toFloat()
            val w = weight.toFloat()
            val a = age.toInt()
            if (name.isBlank() || h <= 0 || w <= 0 || a <= 0) return false

            viewModelScope.launch {
                val newUser = UserEntity(name = name, heightCm = h, weightKg = w, age = a, gender = gender, goal = goal)
                val newId = repository.saveUser(newUser)
                val savedUser = newUser.copy(userId = newId.toInt())
                _currentUser.value = savedUser
                _recommendedExercises.value = repository.getExercisesByGoal(savedUser.goal)
                observeLatestRecord(savedUser.userId)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun observeLatestRecord(userId: Int) {
        historyObservationJob?.cancel()
        historyObservationJob = viewModelScope.launch {
            repository.getHistoryForUser(userId).collect { list ->
                _latestRecord.value = list.firstOrNull()
            }
        }
    }

    fun getUserHistory(userId: Int): Flow<List<FitnessRecord>> {
        return repository.getHistoryForUser(userId)
    }

    fun processCapturedImage(bitmap: Bitmap) {
        _capturedImage.value = bitmap
        _bodyMetrics.value = null

        viewModelScope.launch {
            val metrics = OpenCVProcessor.processBodyImage(bitmap)
            _bodyMetrics.value = metrics

            _currentUser.value?.let { user ->
                val bmi = HealthCalculator.calculateBMI(user.weightKg, user.heightCm)
                repository.saveRecord(
                    FitnessRecord(
                        userId = user.userId,
                        bmi = bmi,
                        bodyShape = metrics.bodyShape,
                        weight = user.weightKg,
                        swr = metrics.swr,
                        whr = metrics.whr
                    )
                )
            }
        }
    }
}