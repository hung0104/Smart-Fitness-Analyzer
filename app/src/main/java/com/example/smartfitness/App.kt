package com.example.smartfitness

import android.app.Application
import android.util.Log
import com.example.smartfitness.data.AppDatabase
import org.opencv.android.OpenCVLoader

class App : Application() {
    lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()

        // Gọi trực tiếp hàm getDatabase an toàn mà bạn đã tự tay thiết kế
        database = AppDatabase.getDatabase(this)

        // Khởi tạo "bộ não" phân tích hình ảnh
        if (OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Nạp thư viện OpenCV thành công!")
        } else {
            Log.e("OpenCV", "Không thể nạp thư viện OpenCV!")
        }
    }
}