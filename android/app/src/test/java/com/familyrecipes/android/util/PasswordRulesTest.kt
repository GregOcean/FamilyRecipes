package com.familyrecipes.android.util

import org.junit.Assert.*
import org.junit.Test

class PasswordRulesTest {
    @Test fun rejectsShortPasswordsBeforeSendingRequest() {
        assertEquals("密码至少需要8个字符", PasswordRules.registrationError("123"))
        assertNotNull(PasswordRules.registrationError("       "))
        assertNull(PasswordRules.registrationError("local-test-123"))
    }

    @Test fun respectsBcryptUtf8ByteLimit() {
        assertNull(PasswordRules.registrationError("中".repeat(24)))
        assertNotNull(PasswordRules.registrationError("中".repeat(25)))
        assertNotNull(PasswordRules.registrationError("a".repeat(73)))
    }
}
