package com.familyrecipes.android.util

object PasswordRules {
    fun registrationError(password: String): String? = when {
        password.isBlank() -> "请输入密码"
        password.length < 8 -> "密码至少需要8个字符"
        password.toByteArray(Charsets.UTF_8).size > 72 -> "密码过长，最多72字节（中文字符占多个字节）"
        else -> null
    }
}
