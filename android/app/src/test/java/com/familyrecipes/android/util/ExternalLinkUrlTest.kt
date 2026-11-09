package com.familyrecipes.android.util
import org.junit.Assert.assertEquals
import org.junit.Test
class ExternalLinkUrlTest {
    @Test fun preservesVideoAndPlaylistParameters() {
        for (url in listOf("https://www.youtube.com/watch?v=abc123&t=42s&list=PL123", "https://youtu.be/abc123?si=some-token&t=42", "https://www.youtube.com/shorts/abc123?feature=share")) {
            assertEquals(url, ExternalLinkUrl.validate(" $url "))
        }
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsNonWebSchemes() {
        ExternalLinkUrl.validate("javascript:alert(1)")
    }
}
