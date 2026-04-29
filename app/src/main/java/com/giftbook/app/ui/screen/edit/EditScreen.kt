package com.giftbook.app.ui.screen.edit

import android.app.Application
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    giftId: String,
    onNavigateBack: () -> Unit,
    viewModel: EditViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(giftId) {
        viewModel.loadGift(giftId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑记录") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.save { onNavigateBack() }
                        },
                        enabled = uiState.name.isNotBlank() && uiState.amount > 0
                    ) {
                        Text("保存")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
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
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 金额
                OutlinedTextField(
                    value = if (uiState.amount == 0.0) "" else "%.0f".format(uiState.amount),
                    onValueChange = { viewModel.updateAmount(it.toDoubleOrNull() ?: 0.0) },
                    label = { Text("金额") },
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

                // 收入/支出
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = uiState.direction == "收入",
                        onClick = { viewModel.updateDirection("收入") },
                        label = { Text("收入") },
                        leadingIcon = {
                            Icon(Icons.Default.ArrowCircleDown, contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = uiState.direction == "支出",
                        onClick = { viewModel.updateDirection("支出") },
                        label = { Text("支出") },
                        leadingIcon = {
                            Icon(Icons.Default.ArrowCircleUp, contentDescription = null,
                                tint = MaterialTheme.colorScheme.error)
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
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 错误信息
                uiState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 删除按钮
                OutlinedButton(
                    onClick = { viewModel.delete { onNavigateBack() } },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("删除此记录")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
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
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ==================== ViewModel ====================

class EditViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GiftBookApp
    private val repository = app.repository

    private val _uiState = MutableStateFlow(EditUiState())
    val uiState: StateFlow<EditUiState> = _uiState.asStateFlow()

    private var originalGift: GiftEntity? = null

    fun loadGift(giftId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getAllGifts(app.authManager.currentUserId ?: "").collect { gifts ->
                val gift = gifts.find { it.id == giftId }
                if (gift != null) {
                    originalGift = gift
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            name = gift.name,
                            amount = gift.amount,
                            dateMillis = gift.date,
                            direction = gift.direction,
                            note = gift.note
                        )
                    }
                }
            }
        }
    }

    fun updateName(name: String) { _uiState.update { it.copy(name = name) } }
    fun updateAmount(amount: Double) { _uiState.update { it.copy(amount = amount) } }
    fun updateDate(date: Long) { _uiState.update { it.copy(dateMillis = date) } }
    fun updateDirection(dir: String) { _uiState.update { it.copy(direction = dir) } }
    fun updateNote(note: String) { _uiState.update { it.copy(note = note) } }

    fun save(onSuccess: () -> Unit) {
        val state = _uiState.value
        val original = originalGift ?: return

        val updated = original.copy(
            name = state.name,
            amount = state.amount,
            date = state.dateMillis,
            direction = state.direction,
            note = state.note,
            updatedAt = System.currentTimeMillis(),
            syncStatus = "pending"
        )

        viewModelScope.launch {
            try {
                repository.saveAndSync(updated)
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "保存失败") }
            }
        }
    }

    fun delete(onSuccess: () -> Unit) {
        val original = originalGift ?: return
        viewModelScope.launch {
            try {
                repository.deleteAndSync(original)
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "删除失败") }
            }
        }
    }
}

data class EditUiState(
    val isLoading: Boolean = true,
    val name: String = "",
    val amount: Double = 0.0,
    val dateMillis: Long = System.currentTimeMillis(),
    val direction: String = "收入",
    val note: String = "",
    val error: String? = null
)
