package com.giftbook.app.ui.screen.add

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giftbook.app.GiftBookApp
import com.giftbook.app.data.db.GiftEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加记录") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.save {
                                onNavigateBack()
                            }
                        },
                        enabled = uiState.name.isNotBlank() && uiState.amount > 0
                    ) {
                        Text("保存")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 姓名
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("姓名") },
                placeholder = { Text("请输入姓名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 金额
            OutlinedTextField(
                value = if (uiState.amount == 0.0) "" else "%.0f".format(uiState.amount),
                onValueChange = { viewModel.updateAmount(it.toDoubleOrNull() ?: 0.0) },
                label = { Text("金额") },
                placeholder = { Text("请输入金额") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                prefix = { Text("¥ ") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 日期
            DatePickerField(
                dateMillis = uiState.dateMillis,
                onDateSelected = { viewModel.updateDate(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 收入/支出切换
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = uiState.direction == "收入",
                    onClick = { viewModel.updateDirection("收入") },
                    label = { Text("收入") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.ArrowCircleDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = uiState.direction == "支出",
                    onClick = { viewModel.updateDirection("支出") },
                    label = { Text("支出") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.ArrowCircleUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 备注
            OutlinedTextField(
                value = uiState.note,
                onValueChange = { viewModel.updateNote(it) },
                label = { Text("备注（事由）") },
                placeholder = { Text("如：结婚、生日、满月…") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 错误信息
            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }

            // 保存按钮（底部安全区）
            Button(
                onClick = {
                    viewModel.save {
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = uiState.name.isNotBlank() && uiState.amount > 0 && !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("保存", fontSize = 16.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    dateMillis: Long,
    onDateSelected: (Long) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA) }
    var showDatePicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = dateFormat.format(Date(dateMillis)),
        onValueChange = {},
        label = { Text("日期") },
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.DateRange, contentDescription = "选择日期")
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                    showDatePicker = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ==================== ViewModel ====================

class AddViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GiftBookApp
    private val repository = app.repository

    private val _uiState = MutableStateFlow(AddUiState())
    val uiState: StateFlow<AddUiState> = _uiState.asStateFlow()

    fun updateName(name: String) { _uiState.update { it.copy(name = name) } }
    fun updateAmount(amount: Double) { _uiState.update { it.copy(amount = amount) } }
    fun updateDate(date: Long) { _uiState.update { it.copy(dateMillis = date) } }
    fun updateDirection(dir: String) { _uiState.update { it.copy(direction = dir) } }
    fun updateNote(note: String) { _uiState.update { it.copy(note = note) } }

    fun save(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "请输入姓名") }
            return
        }
        if (state.amount <= 0) {
            _uiState.update { it.copy(error = "请输入有效金额") }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null) }

        val gift = GiftEntity(
            name = state.name,
            amount = state.amount,
            date = state.dateMillis,
            note = state.note,
            direction = state.direction,
            ownerId = app.authManager.currentUserId ?: ""
        )

        viewModelScope.launch {
            try {
                repository.saveAndSync(gift)
                _uiState.update { it.copy(isSaving = false) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message ?: "保存失败") }
            }
        }
    }
}

data class AddUiState(
    val name: String = "",
    val amount: Double = 0.0,
    val dateMillis: Long = System.currentTimeMillis(),
    val direction: String = "收入",
    val note: String = "",
    val error: String? = null,
    val isSaving: Boolean = false
)
