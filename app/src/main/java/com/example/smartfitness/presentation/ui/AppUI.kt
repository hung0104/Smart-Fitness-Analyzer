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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.example.smartfitness.data.Gender
import com.example.smartfitness.data.Goal
import com.example.smartfitness.data.Exercise
import com.example.smartfitness.data.FitnessRecord
import com.example.smartfitness.data.BodyMetrics
import com.example.smartfitness.data.UserEntity
import com.example.smartfitness.domain.HealthCalculator
import com.example.smartfitness.presentation.viewmodel.FitnessViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FitnessApp(viewModel: FitnessViewModel) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "dashboard") {
        composable("dashboard") { DashboardScreen(navController, viewModel) }
        composable("user_selection") { UserSelectionScreen(navController, viewModel) }
        composable("input") { InputScreen(navController, viewModel) }
        composable("camera") { CameraScreen(navController, viewModel) }
        composable("results") { ResultsScreen(navController, viewModel) }
        composable("history") { HistoryScreen(navController, viewModel) }
    }
}

@Composable
fun DashboardScreen(navController: NavController, viewModel: FitnessViewModel) {
    val user by viewModel.currentUser.collectAsState()
    val latestRecord by viewModel.latestRecord.collectAsState()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Column(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Hệ thống Smart Fitness", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        if (user == null) {
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Chào mừng bạn!", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("Vui lòng chọn hoặc tạo một thành viên để xem dữ liệu Dashboard phân tích.", textAlign = TextAlign.Center, color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { navController.navigate("user_selection") }) {
                        Text("Bắt đầu ngay")
                    }
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(50.dp).background(MaterialTheme.colorScheme.primary, shape = CircleShape), contentAlignment = Alignment.Center) {
                        Text(user!!.name.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user!!.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("Tuổi: ${user!!.age} | Giới tính: ${if(user!!.gender == Gender.MALE) "Nam" else "Nữ"}", color = Color.Gray)
                    }
                    OutlinedButton(onClick = { navController.navigate("user_selection") }) {
                        Text("Đổi người")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Chỉ số phân tích gần nhất", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (latestRecord == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Thành viên mới. Chưa có dữ liệu quét hình thể.", color = Color.Gray)
                    }
                } else {
                    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                        Text("📅 Ngày phân tích: ${dateFormat.format(Date(latestRecord!!.date))}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        Text("👉 Hình dáng (Body Shape):", style = MaterialTheme.typography.titleMedium)
                        Text(latestRecord!!.bodyShape, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                        Spacer(Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Cân nặng mới nhất", color = Color.Gray)
                                Text("${latestRecord!!.weight} kg", style = MaterialTheme.typography.titleLarge)
                            }
                            Column {
                                Text("BMI mới nhất", color = Color.Gray)
                                Text("%.1f".format(latestRecord!!.bmi), style = MaterialTheme.typography.titleLarge)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Trạng thái cơ thể: ${HealthCalculator.getBMICategory(latestRecord!!.bmi)}", style = MaterialTheme.typography.bodyLarge)

                        Spacer(Modifier.height(16.dp))
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                        RecommendedExercisesSection(viewModel)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { navController.navigate("history") },
                    modifier = Modifier.weight(1f).height(50.dp).padding(end = 4.dp)
                ) {
                    Text("Xem lịch sử")
                }
                Button(
                    onClick = { navController.navigate("camera") },
                    modifier = Modifier.weight(1f).height(50.dp).padding(start = 4.dp)
                ) {
                    Text("Phân tích mới")
                }
            }
        }
    }
}

@Composable
fun RecommendedExercisesSection(viewModel: FitnessViewModel) {
    val exercises by viewModel.recommendedExercises.collectAsState()

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("Bài tập gợi ý từ chuyên gia", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        exercises.forEach { exercise ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = exercise.imageResId),
                        contentDescription = exercise.name,
                        modifier = Modifier.size(85.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(exercise.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        Text("💪 Nhóm cơ: ${exercise.primaryMuscles}", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                        Spacer(Modifier.height(4.dp))
                        Text(exercise.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun ResultsScreen(navController: NavController, viewModel: FitnessViewModel) {
    val user = viewModel.currentUser.collectAsState().value ?: return
    val metrics = viewModel.bodyMetrics.collectAsState().value
    val image = viewModel.capturedImage.collectAsState().value

    if (metrics == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Hệ thống OpenCV đang quét cơ thể của ${user.name}...")
            }
        }
        return
    }

    // GỌI LẠI HÀM TÍNH TOÁN DINH DƯỠNG (LỊCH ĂN)
    val bmi = HealthCalculator.calculateBMI(user.weightKg, user.heightCm)
    val tdee = HealthCalculator.calculateTDEE(user.weightKg, user.heightCm, user.age, user.gender)
    val targetCals = HealthCalculator.getCalorieTarget(tdee, user.goal)
    val macros = HealthCalculator.getMacros(targetCals, user.goal)



    val breakfastCals = targetCals * 0.25f
    val lunchCals = targetCals * 0.35f
    val dinnerCals = targetCals * 0.30f
    val snackCals = targetCals * 0.10f

    Column(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Kết quả phân tích", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = {
                navController.navigate("dashboard") {
                    popUpTo("dashboard") { inclusive = true }
                }
            }) {
                Text("Về Dashboard")
            }
        }

        // 1. KHỐI PHÂN TÍCH HÌNH THỂ
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Phân tích hình thể bằng AI", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                image?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.height(150.dp).align(Alignment.CenterHorizontally)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("👉 Dáng người quét được: ${metrics.bodyShape}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                Text("👉 Chỉ số BMI: %.1f (${HealthCalculator.getBMICategory(bmi)})".format(bmi))
                Text("👉 Tỷ lệ Vai/Eo (SWR): %.2f".format(metrics.swr))
                Text("👉 Tỷ lệ Eo/Hông (WHR): %.2f".format(metrics.whr))
            }
        }

        // 2. KHỐI LỊCH ĂN (ĐÃ ĐƯỢC KHÔI PHỤC HOÀN TOÀN)
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Thực đơn & Dinh dưỡng cá nhân", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))

                // GIẢI PHÁP MỚI: Dùng ${...toInt()} cực kỳ an toàn, không bao giờ bị văng app
                Text("Mục tiêu: ${targetCals.toInt()} kcal/ngày", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                Text("Protein: ${macros.first}g | Carb: ${macros.second}g | Fat: ${macros.third}g", color = Color.Gray)

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Text("Gợi ý phân bổ bữa ăn:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))

                Text("☀️ Bữa sáng (25%): ~${breakfastCals.toInt()} kcal", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                Text("Yến mạch, trứng luộc, sữa hạt hoặc phở/bún ít nước béo.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))

                Text("🌤️ Bữa trưa (35%): ~${lunchCals.toInt()} kcal", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                Text("Cơm gạo lứt, ức gà/cá biển, nhiều rau xanh luộc.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))

                Text("🌙 Bữa tối (30%): ~${dinnerCals.toInt()} kcal", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                Text("Khoai lang, thịt bò nạc, salad dầu giấm.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))

                Text("🍎 Bữa phụ (10%): ~${snackCals.toInt()} kcal", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                Text("Trái cây (táo, chuối), sữa chua không đường hoặc Whey.", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // 3. KHỐI LỊCH TẬP CÓ HÌNH ẢNH
        RecommendedExercisesSection(viewModel)
    }
}

@Composable
fun UserSelectionScreen(navController: NavController, viewModel: FitnessViewModel) {
    val users by viewModel.usersList.collectAsState()

    Column(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Danh sách thành viên", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            TextButton(onClick = { navController.popBackStack() }) { Text("Đóng") }
        }
        Spacer(Modifier.height(16.dp))

        if (users.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("Chưa có thành viên nào.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(users) { user ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .clickable {
                                viewModel.selectUser(user)
                                navController.navigate("dashboard") {
                                    popUpTo("dashboard") { inclusive = true }
                                }
                            }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(user.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
                            Text("Tuổi: ${user.age} | Cao: ${user.heightCm}cm | Nặng: ${user.weightKg}kg", color = Color.Gray)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { navController.navigate("input") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ Tạo thành viên mới")
        }
    }
}

@Composable
fun InputScreen(navController: NavController, viewModel: FitnessViewModel) {
    var name by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(Gender.MALE) }
    var goal by remember { mutableStateOf(Goal.LOSE_FAT) }
    var showError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Tạo tài khoản mới", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Tên thành viên") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = height,
            onValueChange = { height = it },
            label = { Text("Chiều cao (cm)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it },
            label = { Text("Cân nặng (kg)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = age,
            onValueChange = { age = it },
            label = { Text("Tuổi") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { gender = Gender.MALE }) {
                RadioButton(selected = gender == Gender.MALE, onClick = { gender = Gender.MALE })
                Text("Nam")
            }
            Spacer(Modifier.width(32.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { gender = Gender.FEMALE }) {
                RadioButton(selected = gender == Gender.FEMALE, onClick = { gender = Gender.FEMALE })
                Text("Nữ")
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Mục tiêu tập luyện:", fontWeight = FontWeight.Medium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
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
            Text("Vui lòng điền đầy đủ số liệu hợp lệ.", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                if (viewModel.createNewUser(name, height, weight, age, gender, goal)) {
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                } else {
                    showError = true
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Xác nhận tạo thành viên")
        }
    }
}

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
            Text(if (isCapturing) "AI đang phân tích..." else "Chụp ảnh phân tích")
        }
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
        ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    val matrix = Matrix().apply { postRotate(image.imageInfo.rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

@Composable
fun HistoryScreen(navController: NavController, viewModel: FitnessViewModel) {
    val user = viewModel.currentUser.collectAsState().value

    if (user == null) {
        navController.popBackStack()
        return
    }

    val historyList by viewModel.getUserHistory(user.userId).collectAsState(initial = emptyList())
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Column(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Lịch sử của ${user.name}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Button(onClick = { navController.popBackStack() }) { Text("Quay lại") }
        }
        Spacer(Modifier.height(16.dp))

        if (historyList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Chưa có lịch sử phân tích nào.", color = Color.Gray)
            }
        } else {
            LazyColumn {
                items(historyList) { record ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Ngày đo: ${dateFormat.format(Date(record.date))}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Text("👉 Dáng người: ${record.bodyShape}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                            Text("Cân nặng: ${record.weight} kg | BMI: %.1f".format(record.bmi))
                            Text("Vai/Eo: %.2f | Eo/Hông: %.2f".format(record.swr, record.whr))
                        }
                    }
                }
            }
        }
    }
}
