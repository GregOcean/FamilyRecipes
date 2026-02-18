package com.familyrecipes.android.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix

/**
 * 二维码工具类
 */
object QRCodeUtil {
    
    /**
     * 生成二维码
     * @param content 二维码内容
     * @param width 二维码宽度
     * @param height 二维码高度
     * @return 二维码Bitmap
     */
    fun createQRCode(content: String, width: Int = 500, height: Int = 500): Bitmap? {
        try {
            val hints = hashMapOf<EncodeHintType, Any>()
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            hints[EncodeHintType.MARGIN] = 1
            
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                width,
                height,
                hints
            )
            
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (bitMatrix[x, y]) {
                        pixels[y * width + x] = Color.BLACK
                    } else {
                        pixels[y * width + x] = Color.WHITE
                    }
                }
            }
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            return bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * 生成用户名片二维码内容
     * 格式: familyrecipes://add_friend?userId=xxx
     */
    fun generateUserCardContent(userId: Long): String {
        return "familyrecipes://add_friend?userId=$userId"
    }
    
    /**
     * 解析用户名片二维码
     */
    fun parseUserCardContent(content: String): Long? {
        if (!content.startsWith("familyrecipes://add_friend")) {
            return null
        }
        
        return try {
            val uri = android.net.Uri.parse(content)
            uri.getQueryParameter("userId")?.toLongOrNull()
        } catch (e: Exception) {
            null
        }
    }
}

