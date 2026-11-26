package com.familyrecipes.android.ui.fridge

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ReceiptOcrTest {
    @Test
    fun readsDenseChineseReceipt() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.cacheDir, "receipt-ocr-dense-test.jpg")
        val bitmap = Bitmap.createBitmap(3000, 4000, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 48f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val lines = listOf(
            "新鲜超市购物小票",
            "伊利纯牛奶 250ml*12 49.90",
            "蒙牛特仑苏纯牛奶 250ml*12 68.00",
            "海天金标生抽 500ml 9.90",
            "金龙鱼调和油 5L 59.90",
            "东北大米 5kg 39.90",
            "鸡蛋 30枚 25.80",
            "青菜 1.2kg 8.00"
        )
        lines.forEachIndexed { index, line -> canvas.drawText(line, 120f, 260f + index * 110f, paint) }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()

        val ocr = ReceiptOcr(context)
        try {
            val text = ocr.read(file)
            println("DENSE OCR:\n$text")
            assertTrue("OCR result should contain 伊利纯牛奶: $text", text.contains("伊利纯牛奶"))
            assertTrue("OCR result should contain 海天金标生抽: $text", text.contains("海天金标生抽"))
            assertTrue("OCR result should contain 东北大米: $text", text.contains("东北大米"))
            assertTrue("OCR should not inject Latin garbage: $text", !text.contains("A44F3") && !text.contains("4#ut"))
        } finally {
            ocr.close()
            file.delete()
        }
    }

    @Test
    fun readsChineseAndEnglishOnDevice() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.cacheDir, "receipt-ocr-test.jpg")
        val bitmap = Bitmap.createBitmap(1600, 1000, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 84f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        canvas.drawText("牛奶 Milk 250ml", 80f, 220f, paint)
        canvas.drawText("鸡蛋 Eggs 30", 80f, 380f, paint)
        canvas.drawText("青菜 Vegetables 1.2kg", 80f, 540f, paint)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bitmap.recycle()

        val ocr = ReceiptOcr(context)
        try {
            val text = ocr.read(file)
            assertTrue("OCR result should contain 牛奶: $text", text.contains("牛奶"))
            assertTrue("OCR result should contain Milk: $text", text.contains("Milk", ignoreCase = true))
            assertTrue("OCR result should contain Eggs: $text", text.contains("Eggs", ignoreCase = true))
        } finally {
            ocr.close()
            file.delete()
        }
    }

    @Test
    fun readsEnglishOnlyOnDevice() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.cacheDir, "receipt-ocr-english-test.jpg")
        val bitmap = Bitmap.createBitmap(1600, 1000, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 84f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        canvas.drawText("Milk 250ml", 80f, 220f, paint)
        canvas.drawText("Eggs 30", 80f, 380f, paint)
        canvas.drawText("Vegetables 1.2kg", 80f, 540f, paint)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bitmap.recycle()

        val ocr = ReceiptOcr(context)
        try {
            val text = ocr.read(file)
            assertTrue("OCR result should contain Milk: $text", text.contains("Milk", ignoreCase = true))
            assertTrue("OCR result should contain Eggs: $text", text.contains("Eggs", ignoreCase = true))
            assertTrue("OCR result should contain Vegetables: $text", text.contains("Vegetables", ignoreCase = true))
        } finally {
            ocr.close()
            file.delete()
        }
    }
}
