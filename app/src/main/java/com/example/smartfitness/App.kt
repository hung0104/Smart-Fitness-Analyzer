package com.example.smartfitness

import android.app.Application
import android.util.Log
import com.example.smartfitness.data.AppDatabase
import org.opencv.android.OpenCVLoader

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Khởi tạo Cơ sở dữ liệu (Room DB) qua Singleton để đảm bảo thống nhất
        AppDatabase.getDatabase(this)

        // 2. Khởi tạo "Bộ não" OpenCV
        if (OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Tải OpenCV thành công!")
        } else {
            Log.e("OpenCV", "Lỗi tải OpenCV!")
        }
    }
}