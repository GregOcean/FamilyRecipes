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
