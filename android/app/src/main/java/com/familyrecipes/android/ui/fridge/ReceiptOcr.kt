package com.familyrecipes.android.ui.fridge

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.text.Normalizer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device bilingual receipt OCR.
 *
 * The Chinese model is good at Chinese but can garble Latin words; the Latin model is the
 * reverse. Run both and merge their lines by vertical position: keep Chinese for Chinese
 * lines, use Latin to correct/complete the Latin part of the same visual line.
 */
class ReceiptOcr(private val context: Context) {
    private data class OcrLine(val text: String, val top: Int, val bottom: Int, val left: Int) {
        val height get() = (bottom - top).coerceAtLeast(1)
    }

    private val chinese = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    private val latin = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun read(file: File): String {
        val image = withContext(Dispatchers.IO) { InputImage.fromFilePath(context, Uri.fromFile(file)) }
        val chineseLines = runCatching { process(chinese, image) }.getOrDefault(emptyList())
        val latinLines = runCatching { process(latin, image) }.getOrDefault(emptyList())
        return merge(chineseLines, latinLines)
    }

    private suspend fun process(recognizer: TextRecognizer, image: InputImage): List<OcrLine> =
        suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { result -> if (continuation.isActive) continuation.resume(toLines(result)) }
                .addOnFailureListener { error -> if (continuation.isActive) continuation.resumeWithException(error) }
        }

    private fun toLines(result: Text): List<OcrLine> =
        result.textBlocks.flatMap { it.lines }.mapNotNull { line ->
            val box = line.boundingBox ?: return@mapNotNull null
            val text = clean(line.text)
            if (text.isBlank()) null else OcrLine(text, box.top, box.bottom, box.left)
        }.sortedWith(compareBy<OcrLine>({ it.top }, { it.left }))

    private fun merge(chinese: List<OcrLine>, latin: List<OcrLine>): String {
        if (latin.isEmpty()) return join(chinese.map { it.text })
        val used = mutableSetOf<Int>()
        val merged = mutableListOf<Pair<Int, String>>()
        chinese.forEach { line ->
            val match = latin.indices
                .filter { it !in used }
                .map { index -> index to overlap(line, latin[index]) }
                .filter { (index, overlap) -> overlap > 0.35 * minOf(line.height, latin[index].height) }
                .maxByOrNull { it.second }
                ?.first
            if (match != null) used.add(match)
            val latinText = match?.let { latin.getOrNull(it)?.text }.orEmpty()
            merged.add(line.top to combine(line.text, latinText))
        }
        // Latin-only lines (English item names the Chinese model missed) are kept as-is.
        latin.forEachIndexed { index, line ->
            if (index !in used && line.text.any { it.isLetter() }) merged.add(line.top to line.text)
        }
        return join(merged.sortedWith(compareBy({ it.first }, { it.second.length })).map { it.second })
    }

    private fun overlap(a: OcrLine, b: OcrLine): Int =
        (minOf(a.bottom, b.bottom) - maxOf(a.top, b.top)).coerceAtLeast(0)

    /**
     * Keep the leading Chinese name, then append the Latin OCR of the same line. This fixes
     * cases like 牛奶Mi天 -> 牛奶 Milk while preserving Chinese product names.
     */
    private fun combine(chinese: String, latin: String): String {
        val chineseText = chinese.trim()
        val latinText = latin.trim()
        if (!CJK.containsMatchIn(chineseText)) return latinText.ifBlank { chineseText }
        if (latinText.isBlank()) return chineseText
        val prefix = chineseText.takeWhile { it.isWhitespace() || CJK.matches(it.toString()) }.trim()
        if (prefix.isEmpty()) return chineseText
        val nameEnd = chineseText.indexOfFirst { it.isDigit() }.let { if (it < 0) chineseText.length else it }
        val name = chineseText.substring(0, nameEnd).trim()
        val rest = chineseText.substring(nameEnd).trim()
        val tail = name.removePrefix(prefix).trim()
        // Only touch the part after the Chinese name when it contains Latin garbage.
        if (tail.isEmpty() || !tail.any { it in 'A'..'Z' || it in 'a'..'z' }) return chineseText
        val latinName = Regex("[A-Za-z][A-Za-z'’.-]+").find(latinText)?.value?.trim().orEmpty()
        val cleanTail = latinName
        return listOf(prefix, cleanTail, rest).filter { it.isNotBlank() }.joinToString(" ")
    }

    private fun clean(raw: String): String = Normalizer.normalize(raw, Normalizer.Form.NFKC)
        .replace(Regex("(?<=[\\u3400-\\u9fff])\\s+(?=[\\u3400-\\u9fff])"), "")
        .replace(Regex("[\\t\\u00a0]+"), " ")
        .trim()
        .replace(Regex(" {2,}"), " ")

    private fun join(lines: List<String>): String = lines
        .filter { it.isNotBlank() }
        .joinToString("\n")
        .take(20_000)

    fun close() {
        chinese.close()
        latin.close()
    }

    companion object {
        private val CJK = Regex("[\\u3400-\\u9fff]")
    }
}
