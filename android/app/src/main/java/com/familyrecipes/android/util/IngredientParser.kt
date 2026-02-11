package com.familyrecipes.android.util

import com.familyrecipes.android.data.local.ConfigManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * 食材信息解析器
 * 从文本中智能识别：食材名称、数量、过期时间
 */
object IngredientParser {

    /**
     * 解析结果
     */
    data class ParseResult(
        val ingredientName: String?,      // 食材名称
        val amount: String?,              // 数量
        val expiryDays: Int,              // 过期天数
        val storageLocation: String = com.familyrecipes.android.data.local.ConfigManager.getDefaultStorageLocation(), // 存储位置，从配置获取默认值
        val notes: String                 // 完整备注
    )

    /**
     * 常见食材的默认保质期（天数）
     */
    private val DEFAULT_SHELF_LIFE = mapOf(
        // 肉类
        "猪肉" to 3, "牛肉" to 3, "羊肉" to 3, "鸡肉" to 3, "鸭肉" to 3,
        "鱼" to 2, "虾" to 2, "蟹" to 2, "贝类" to 1,
        
        // 蛋奶
        "鸡蛋" to 30, "鸭蛋" to 30, "鹌鹑蛋" to 30,
        "牛奶" to 7, "酸奶" to 7, "奶酪" to 14, "黄油" to 30,
        
        // 蔬菜
        "白菜" to 7, "青菜" to 3, "生菜" to 3, "菠菜" to 3,
        "西红柿" to 7, "黄瓜" to 5, "茄子" to 5, "辣椒" to 7,
        "土豆" to 14, "红薯" to 14, "南瓜" to 14, "冬瓜" to 14,
        "萝卜" to 10, "胡萝卜" to 10, "洋葱" to 14, "大蒜" to 30,
        
        // 水果
        "苹果" to 7, "香蕉" to 5, "橙子" to 10, "葡萄" to 5,
        "草莓" to 3, "樱桃" to 3, "蓝莓" to 5, "芒果" to 5,
        "西瓜" to 7, "哈密瓜" to 7, "猕猴桃" to 7,
        
        // 豆制品
        "豆腐" to 3, "豆浆" to 2, "豆干" to 5,
        
        // 其他
        "面包" to 3, "馒头" to 3, "包子" to 3,
        "米饭" to 2, "面条" to 3
    )

    /**
     * 数量单位词
     */
    private val AMOUNT_UNITS = listOf(
        "个", "只", "条", "根", "片", "块", "颗", "粒", "枚",
        "袋", "盒", "包", "瓶", "罐", "桶", "箱",
        "斤", "两", "克", "千克", "公斤", "kg", "g",
        "升", "毫升", "ml", "l"
    )

    /**
     * 时间关键词
     */
    private val TIME_KEYWORDS = listOf(
        "过期", "到期", "保质期", "吃完", "用完", "坏", "不新鲜"
    )

    /**
     * 解析输入文本
     */
    fun parse(input: String): ParseResult {
        val text = input.trim()
        
        if (text.isEmpty()) {
            return ParseResult(null, null, 7, "冰箱", text)
        }

        // 1. 提取过期时间信息
        val expiryDays = extractExpiryDays(text)
        
        // 2. 提取数量信息
        val amount = extractAmount(text)
        
        // 3. 提取食材名称
        val ingredientName = extractIngredientName(text, amount)
        
        // 4. 如果没有识别出食材名，用第一个词作为食材名
        val finalName = ingredientName ?: text.split(Regex("[，,\\s]")).firstOrNull()?.trim()
        
        // 5. 根据食材类型设置默认保质期（如果没有明确时间）
        val finalExpiryDays = if (expiryDays == -1) {
            getDefaultShelfLife(finalName)
        } else {
            expiryDays
        }

        return ParseResult(
            ingredientName = finalName,
            amount = amount,
            expiryDays = finalExpiryDays,
            storageLocation = ConfigManager.getDefaultStorageLocation(),  // 从配置获取默认存储位置
            notes = text
        )
    }

    /**
     * 提取过期天数
     * 支持格式：
     * - "两周后过期" -> 14天
     * - "7天内要吃完" -> 7天
     * - "还有5天过期" -> 5天
     * - "三个月" -> 90天
     */
    private fun extractExpiryDays(text: String): Int {
        // 匹配 "X天"
        val dayPattern = Regex("(\\d+)\\s*天")
        dayPattern.find(text)?.let {
            return it.groupValues[1].toInt()
        }

        // 匹配 "X周"
        val weekPattern = Regex("(\\d+|一|两|三|四|五|六|七|八|九|十)\\s*周")
        weekPattern.find(text)?.let {
            val weeks = chineseNumberToInt(it.groupValues[1])
            return weeks * 7
        }

        // 匹配 "X个月"
        val monthPattern = Regex("(\\d+|一|两|三|四|五|六|七|八|九|十)\\s*个?月")
        monthPattern.find(text)?.let {
            val months = chineseNumberToInt(it.groupValues[1])
            return months * 30
        }

        // 如果包含时间关键词但没有具体天数，返回-1表示需要使用默认值
        return if (TIME_KEYWORDS.any { text.contains(it) }) -1 else -1
    }

    /**
     * 提取数量信息
     * 支持格式：
     * - "一袋"
     * - "2盒"
     * - "500克"
     */
    private fun extractAmount(text: String): String? {
        // 匹配 "数字+单位" 或 "中文数字+单位"
        val pattern = Regex("(\\d+\\.?\\d*|一|两|三|四|五|六|七|八|九|十|几)\\s*(${AMOUNT_UNITS.joinToString("|")})")
        pattern.find(text)?.let {
            return it.value
        }
        return null
    }

    /**
     * 提取食材名称
     * 策略：
     * 1. 去除数量和时间信息
     * 2. 取第一个有意义的词
     * 3. 匹配已知食材库
     */
    private fun extractIngredientName(text: String, amount: String?): String? {
        var cleanText = text
        
        // 移除数量信息
        amount?.let {
            cleanText = cleanText.replace(it, "")
        }
        
        // 移除时间相关描述
        TIME_KEYWORDS.forEach { keyword ->
            val index = cleanText.indexOf(keyword)
            if (index > 0) {
                cleanText = cleanText.substring(0, index)
            }
        }
        
        // 移除标点符号
        cleanText = cleanText.replace(Regex("[，,、。！？\\s]+"), "")
        
        // 如果清理后为空，返回null
        if (cleanText.isEmpty()) return null
        
        // 尝试匹配已知食材
        for ((ingredient, _) in DEFAULT_SHELF_LIFE) {
            if (cleanText.contains(ingredient)) {
                return ingredient
            }
        }
        
        // 如果没有匹配，返回清理后的文本（限制长度）
        return if (cleanText.length <= 10) cleanText else cleanText.substring(0, 10)
    }

    /**
     * 根据食材名称获取默认保质期
     */
    private fun getDefaultShelfLife(ingredientName: String?): Int {
        if (ingredientName == null) return 7
        
        // 精确匹配
        DEFAULT_SHELF_LIFE[ingredientName]?.let { return it }
        
        // 模糊匹配
        for ((name, days) in DEFAULT_SHELF_LIFE) {
            if (ingredientName.contains(name) || name.contains(ingredientName)) {
                return days
            }
        }
        
        // 默认7天
        return 7
    }

    /**
     * 中文数字转阿拉伯数字
     */
    private fun chineseNumberToInt(chinese: String): Int {
        return when (chinese) {
            "一" -> 1
            "两", "二" -> 2
            "三" -> 3
            "四" -> 4
            "五" -> 5
            "六" -> 6
            "七" -> 7
            "八" -> 8
            "九" -> 9
            "十" -> 10
            else -> chinese.toIntOrNull() ?: 1
        }
    }

    /**
     * 计算过期日期
     */
    fun calculateExpiryDate(days: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, days)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }
}

