package com.example.smartfitness.presentation.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.example.smartfitness.data.Gender
import com.example.smartfitness.data.Goal
import com.example.smartfitness.domain.HealthCalculator
import com.example.smartfitness.presentation.viewmodel.FitnessViewModel

// Màn hình điều hướng chính
@Composable
fun FitnessApp(viewModel: FitnessViewModel) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "input") {
        composable("input") { InputScreen(navController, viewModel) }
        composable("camera") { CameraScreen(navController, viewModel) }
        composable("results") { ResultsScreen(viewModel) }
    }
}

// --- MÀN HÌNH 1: NHẬP THÔNG TIN ---
@Composable
fun InputScreen(navController: NavController, viewModel: FitnessViewModel) {
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(Gender.MALE) }
    var goal by remember { mutableStateOf(Goal.LOSE_FAT) }
    var showError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(16.dp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Smart Fitness Analyzer", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("Chiều cao (cm)") })
        OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Cân nặng (kg)") })
        OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Tuổi") })

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = gender == Gender.MALE, onClick = { gender = Gender.MALE })
            Text("Nam", modifier = Modifier.padding(end = 16.dp))
            RadioButton(selected = gender == Gender.FEMALE, onClick = { gender = Gender.FEMALE })
            Text("Nữ")
        }

        Spacer(Modifier.height(16.dp))
        Text("Mục tiêu tập luyện:")
        Row {
            Goal.entries.forEach { g ->
                Button(
                    onClick = { goal = g },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (goal == g) MaterialTheme.colorScheme.primary else Color.Gray
                    ),
                    modifier = Modifier.padding(4.dp)
                ) {
                    val goalName = when(g) {
                        Goal.LOSE_FAT -> "Giảm mỡ"
                        Goal.BUILD_MUSCLE -> "Tăng cơ"
                        Goal.MAINTAIN -> "Giữ dáng"
                    }
                    Text(goalName)
                }
            }
        }

        if (showError) {
            Text("Vui lòng nhập số hợp lệ vào các ô trống.", color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                if (viewModel.saveUserProfile(height, weight, age, gender, goal)) {
                    navController.navigate("camera")
                } else {
                    showError = true
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Tiếp tục (Mở Camera)")
        }
    }
}

// --- MÀN HÌNH 2: CAMERA QUÉT CƠ THỂ ---
@Composable
fun CameraScreen(navController: NavController, viewModel: FitnessViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraController = remember { LifecycleCameraController(context) }
    var isCapturing by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    controller = cameraController
                    cameraController.bindToLifecycle(lifecycleOwner)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Vẽ khung viền hướng dẫn người dùng đứng vào vị trí
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawOval(
                color = Color.Yellow,
                alpha = 0.5f,
                style = Stroke(width = 5f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.6f, size.height * 0.8f),
                topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.1f)
            )
        }

        Button(
            enabled = !isCapturing,
            onClick = {
                isCapturing = true
                cameraController.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val bitmap = imageProxyToBitmap(image)
                            viewModel.processCapturedImage(bitmap)
                            image.close()
                            navController.navigate("results")
                        }
                        override fun onError(exception: ImageCaptureException) {
                            isCapturing = false
                        }
                    }
                )
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)
        ) {
            Text(if (isCapturing) "Đang xử lý..." else "Chụp ảnh phân tích")
        }
    }
}

// Hàm hỗ trợ xoay ảnh cho đúng chiều
private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
    val matrix = Matrix().apply { postRotate(image.imageInfo.rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

// --- MÀN HÌNH 3: HIỂN THỊ KẾT QUẢ ---
@Composable
fun ResultsScreen(viewModel: FitnessViewModel) {
    val profile = viewModel.userProfile.collectAsState().value ?: return
    val metrics = viewModel.bodyMetrics.collectAsState().value
    val image = viewModel.capturedImage.collectAsState().value
    val scrollState = rememberScrollState()

    // Hiển thị vòng xoay tải dữ liệu nếu OpenCV đang tính toán
    if (metrics == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Trí tuệ nhân tạo đang phân tích cơ thể bạn...")
            }
        }
        return
    }

    val bmi = HealthCalculator.calculateBMI(profile.weightKg, profile.heightCm)
    val tdee = HealthCalculator.calculateTDEE(profile)
    val targetCals = HealthCalculator.getCalorieTarget(tdee, profile.goal)
    val macros = HealthCalculator.getMacros(targetCals, profile.goal)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState)) {
        Text("Báo cáo sức khỏe của bạn", style = MaterialTheme.typography.headlineMedium)

        // THẺ 1: PHÂN TÍCH CƠ THỂ
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Phân tích hình thể", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                image?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.height(150.dp).align(Alignment.CenterHorizontally)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("👉 Dáng người: ${metrics.bodyShape}")
                Text("👉 Chỉ số BMI: %.1f (${HealthCalculator.getBMICategory(bmi)})".format(bmi))
                Text("👉 Tỷ lệ Vai/Eo (SWR): %.2f".format(metrics.swr))
                Text("👉 Tỷ lệ Eo/Hông (WHR): %.2f".format(metrics.whr))
            }
        }

        // THẺ 2: DINH DƯỠNG
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Kế hoạch dinh dưỡng", style = MaterialTheme.typography.titleLarge)
                Text("Mục tiêu Calo: %.0f kcal/ngày".format(targetCals), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                Text("Đạm (Protein): ${macros.first}g | Tinh bột (Carb): ${macros.second}g | Béo (Fat): ${macros.third}g")
                Spacer(Modifier.height(8.dp))
                Text("Thực đơn mẫu:", style = MaterialTheme.typography.titleMedium)
                when(profile.goal) {
                    Goal.LOSE_FAT -> {
                        Text("• Sáng: Yến mạch + 2 quả trứng luộc (~400 kcal)")
                        Text("• Trưa: Ức gà nướng + Gạo lứt + Salad (~600 kcal)")
                        Text("• Tối: Cá hấp + Rau củ luộc (~450 kcal)")
                    }
                    else -> {
                        Text("• Sáng: Bánh mì nguyên cám + Bơ đậu phộng + Sữa")
                        Text("• Trưa: Cơm trắng + Thịt bò xào hành tây + Canh rau")
                        Text("• Tối: Đùi gà luộc + Khoai lang + Salad sốt mè")
                    }
                }
            }
        }

        // THẺ 3: LUYỆN TẬP
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Lịch tập luyện tuần", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                when(profile.goal) {
                    Goal.BUILD_MUSCLE -> {
                        Text("T2: Ngực & Tay sau (VD: Hít đất 3x15)")
                        Text("T3: Lưng & Tay trước")
                        Text("T4: Chân (VD: Squat 4x12)")
                        Text("T5: Vai")
                        Text("T6: Cả cơ thể")
                        Text("T7 & CN: Nghỉ ngơi")
                    }
                    Goal.LOSE_FAT -> {
                        Text("T2 & T5: Cardio cường độ cao (HIIT 20 phút)")
                        Text("T3 & T6: Tập sức mạnh toàn thân")
                        Text("T4 & CN: Nghỉ ngơi")
                        Text("T7: Đi bộ nhẹ nhàng / Đạp xe")
                    }
                    Goal.MAINTAIN -> {
                        Text("T2: Tập thân trên")
                        Text("T3: Tập thân dưới")
                        Text("T4: Chạy bộ / Cardio")
                        Text("T5 & CN: Nghỉ ngơi")
                        Text("T6: Toàn thân")
                        Text("T7: Yoga / Kéo giãn cơ")
                    }
                }
            }
        }
    }
}