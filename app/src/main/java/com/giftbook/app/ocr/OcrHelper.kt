package com.giftbook.app.ocr

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions

/**
 * OCR 识别助手
 * 使用 ML Kit 中文识别模型，支持手写和印刷体
 */
class OcrHelper {

    private val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    /**
     * 识别图片中的文字
     * @param bitmap 原始位图
     * @param callback 识别结果回调 (识别到的文本块列表, 错误信息)
     */
    fun recognizeText(bitmap: Bitmap, callback: (List<OcrResult>, String?) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val results = mutableListOf<OcrResult>()

                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        val parsed = parseLine(line.text)
                        results.add(parsed)
                    }
                }

                callback(results, null)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "OCR recognition failed", e)
                callback(emptyList(), e.localizedMessage ?: "识别失败")
            }
    }

    /**
     * 关闭识别器释放资源
     */
    fun close() {
        recognizer.close()
    }

    companion object {
        private const val TAG = "OcrHelper"
    }
}

/**
 * OCR 识别结果 - 已结构化的一行数据
 */
data class OcrResult(
    val rawText: String = "",       // 原始识别文本
    val name: String = "",          // 提取的姓名
    val amount: Double = 0.0,       // 提取的金额
    val date: String = "",          // 提取的日期字符串
    val note: String = ""           // 提取的备注
)

/**
 * 解析 OCR 识别的一行文本
 * 从手写账本中提取 姓名、金额、日期、备注
 *
 * 支持常见格式：
 * - "张三 500 2024-01-01 结婚"
 * - "李四 200元 1月1日"
 * - "王五 1000 贺寿"
 */
fun parseLine(text: String): OcrResult {
    val trimmed = text.trim()

    // 金额模式：匹配数字（含小数点），可能带 "元" 字后缀
    val amountPattern = Regex("""(\d+(?:\.\d+)?)""")
    val amountMatch = amountPattern.find(trimmed)

    // 日期模式：匹配常见日期格式
    val datePattern = Regex(
        """(\d{4}[-年]\d{1,2}[-月]\d{1,2}[日]?|\d{1,2}[-月]\d{1,2}[日]?|今天|昨天|前天|\d{1,2}月\d{1,2})"""
    )
    val dateMatch = datePattern.find(trimmed)

    var amount = 0.0
    var dateStr = ""
    var remainingText = trimmed

    // 提取金额
    if (amountMatch != null) {
        amount = amountMatch.groupValues[1].toDoubleOrNull() ?: 0.0
        remainingText = remainingText.replace(amountMatch.value, "").trim()
    }

    // 提取日期
    if (dateMatch != null) {
        dateStr = dateMatch.value
        remainingText = remainingText.replace(dateMatch.value, "").trim()
    }

    // 清理剩余文本
    remainingText = remainingText
        .replace(Regex("[元￥$]"), "")
        .replace(Regex("[，。、；：]"), " ")
        .trim()

    // 剩余部分：第一个词通常是姓名，后面是备注
    val name: String
    val note: String
    val parts = remainingText.split(Regex("\\s+"), limit = 2).filter { it.isNotBlank() }

    if (parts.isNotEmpty()) {
        name = parts[0]
        note = parts.getOrElse(1) { "" }
    } else {
        name = ""
        note = ""
    }

    return OcrResult(
        rawText = text,
        name = name,
        amount = amount,
        date = dateStr,
        note = note
    )
}
