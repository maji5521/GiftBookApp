package com.giftbook.app.ui.screen.scan

import android.Manifest
import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giftbook.app.GiftBookApp
import com.giftbook.app.data.db.GiftEntity
import com.giftbook.app.ocr.OcrHelper
import com.giftbook.app.ocr.OcrResult
import com.google.accompanist.permissions.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScanScreen(
    onNavigateBack: () -> Unit,
    viewModel: ScanViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("扫描账本") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        when {
            !cameraPermission.status.isGranted -> {
                // 无权限提示
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("需要相机权限才能扫描账本")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                        Text("授予权限")
                    }
                }
            }

            uiState.ocrResults.isNotEmpty() -> {
                // 识别结果展示
                OcrResultScreen(
                    results = uiState.ocrResults,
                    isImporting = uiState.isImporting,
                    onCorrectResult = { index, corrected ->
                        viewModel.correctResult(index, corrected)
                    },
                    onImport = { viewModel.importResults { onNavigateBack() } },
                    onRetake = { viewModel.reset() }
                )
            }

            else -> {
                // 相机预览 + 拍照
                CameraScreen(
                    isProcessing = uiState.isProcessing,
                    onPhotoCaptured = { bitmap -> viewModel.processImage(bitmap) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
        }
    }
}

/**
 * 相机预览 + 拍照按钮
 */
@Composable
private fun CameraScreen(
    isProcessing: Boolean,
    onPhotoCaptured: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetRotation(android.view.Surface.ROTATION_0)
            .build()
    }

    // 是否触发拍照的状态
    var captureRequested by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        // CameraX 预览
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, cameraExecutor)

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // 半透明取景框
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = Color.Transparent,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .padding(48.dp)
                    .aspectRatio(0.7f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "将账本放在此区域",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // 拍照按钮
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            IconButton(
                onClick = { captureRequested = true },
                modifier = Modifier.size(80.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = Color.White,
                            border = null
                        ) {}
                    }
                }
            }
        }

        // 处理中遮罩
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("识别中…", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }

    // 触发拍照
    if (captureRequested) {
        CapturePhoto(
            imageCapture = imageCapture,
            onPhotoCaptured = { bitmap ->
                captureRequested = false
                onPhotoCaptured(bitmap)
            },
            onError = { captureRequested = false }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

/**
 * 执行拍照的 Composable（通过 LaunchedEffect 触发）
 */
@Composable
private fun CapturePhoto(
    imageCapture: ImageCapture,
    onPhotoCaptured: (Bitmap) -> Unit,
    onError: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(imageCapture) {
        val executor = Executors.newSingleThreadExecutor()

        imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = imageProxyToBitmap(image)
                image.close()
                executor.shutdown()
                if (bitmap != null) {
                    onPhotoCaptured(bitmap)
                } else {
                    onError()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                executor.shutdown()
                onError()
            }
        })
    }

    Box {}
}

/**
 * 将 ImageProxy 转换为 Bitmap
 */
private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    try {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

        // 旋转到正确方向
        val matrix = Matrix()
        matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

// ==================== OCR 结果展示 ====================

@Composable
private fun OcrResultScreen(
    results: List<OcrResult>,
    isImporting: Boolean,
    onCorrectResult: (Int, OcrResult) -> Unit,
    onImport: () -> Unit,
    onRetake: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "识别结果",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Text(
            text = "请核对识别内容，点击可校正",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(results) { index, result ->
                OcrResultCard(
                    index = index,
                    result = result,
                    onCorrect = { corrected -> onCorrectResult(index, corrected) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier.weight(1f)
            ) {
                Text("重新拍照")
            }
            Button(
                onClick = onImport,
                modifier = Modifier.weight(1f),
                enabled = !isImporting
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("确认导入 (${results.size}条)")
                }
            }
        }
    }
}

@Composable
private fun OcrResultCard(
    index: Int,
    result: OcrResult,
    onCorrect: (OcrResult) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { showEditDialog = true }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.name.ifBlank { "（未识别）" },
                    fontWeight = FontWeight.Bold
                )
                Row {
                    Text(
                        text = "¥%.0f".format(result.amount),
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (result.date.isNotBlank()) {
                        Text(
                            text = "  |  ${result.date}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
                if (result.note.isNotBlank()) {
                    Text(
                        text = result.note,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Default.Edit,
                contentDescription = "校正",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showEditDialog) {
        CorrectDialog(
            result = result,
            onDismiss = { showEditDialog = false },
            onConfirm = { corrected ->
                onCorrect(corrected)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun CorrectDialog(
    result: OcrResult,
    onDismiss: () -> Unit,
    onConfirm: (OcrResult) -> Unit
) {
    var name by remember { mutableStateOf(result.name) }
    var amount by remember { mutableStateOf(if (result.amount == 0.0) "" else "%.0f".format(result.amount)) }
    var date by remember { mutableStateOf(result.date) }
    var note by remember { mutableStateOf(result.note) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("校正识别结果") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("姓名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("金额") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("日期") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    OcrResult(
                        name = name,
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        date = date,
                        note = note
                    )
                )
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// ==================== ViewModel ====================

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GiftBookApp
    private val repository = app.repository
    private val ocrHelper = OcrHelper()

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    /**
     * 处理拍照后的图片 - 执行 OCR 识别
     */
    fun processImage(bitmap: Bitmap) {
        _uiState.update { it.copy(isProcessing = true) }

        viewModelScope.launch(Dispatchers.IO) {
            ocrHelper.recognizeText(bitmap) { results, error ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        ocrResults = results,
                        error = error
                    )
                }
            }
        }
    }

    /**
     * 校正单条识别结果
     */
    fun correctResult(index: Int, corrected: OcrResult) {
        val current = _uiState.value.ocrResults.toMutableList()
        if (index in current.indices) {
            current[index] = corrected
            _uiState.update { it.copy(ocrResults = current) }
        }
    }

    /**
     * 批量导入识别结果到数据库
     */
    fun importResults(onSuccess: () -> Unit) {
        _uiState.update { it.copy(isImporting = true) }

        val ownerId = app.authManager.currentUserId ?: ""
        val entities = _uiState.value.ocrResults.map { result ->
            GiftEntity(
                name = result.name,
                amount = result.amount,
                date = parseDate(result.date),
                note = result.note,
                direction = "收入",
                ownerId = ownerId,
                syncStatus = "pending"
            )
        }

        viewModelScope.launch {
            try {
                repository.insertGifts(entities)
                _uiState.update { it.copy(isImporting = false) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isImporting = false, error = e.message) }
            }
        }
    }

    fun reset() {
        _uiState.update { ScanUiState() }
    }

    private fun parseDate(dateStr: String): Long {
        if (dateStr.isBlank()) return System.currentTimeMillis()
        val cleaned = dateStr.replace("年", "-").replace("月", "-").replace("日", "")
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
            sdf.parse(cleaned)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    override fun onCleared() {
        super.onCleared()
        ocrHelper.close()
    }
}

data class ScanUiState(
    val isProcessing: Boolean = false,
    val isImporting: Boolean = false,
    val ocrResults: List<OcrResult> = emptyList(),
    val error: String? = null
)
