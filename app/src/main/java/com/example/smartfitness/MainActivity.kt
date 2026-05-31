package com.example.smartfitness

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.smartfitness.data.AppDatabase
import com.example.smartfitness.data.FitnessRepository
import com.example.smartfitness.presentation.ui.FitnessApp
import com.example.smartfitness.presentation.viewmodel.FitnessViewModel
import com.example.smartfitness.ui.theme.SmartFitnessAnalyzerTheme

class MainActivity : ComponentActivity() {

    // Trình quản lý yêu cầu quyền truy cập (Dùng cho Camera)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Xin quyền Camera ngay khi người dùng mở ứng dụng
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        // Kết nối Dữ liệu (Repository) và Logic (ViewModel)
        val database = AppDatabase.getDatabase(this)
        val repository = FitnessRepository(database.fitnessDao())
        val viewModel = FitnessViewModel(repository)

        // Hiển thị giao diện người dùng
        setContent {
            SmartFitnessAnalyzerTheme {
                FitnessApp(viewModel)
            }
        }
    }
}
