package com.familyrecipes.service;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
class WebScrapingServiceTest {
    @ParameterizedTest
    @ValueSource(strings={"http://127.0.0.1/", "https://169.254.169.254/", "file:///etc/passwd", "https://xiachufang.com.evil.example/", "https://xiachufang.com@127.0.0.1/", "https://xiachufang.com:8080/"})
    void disallowsUnsupportedTargetsBeforeConnecting(String url) {
        assertThrows(IOException.class,()->WebScrapingService.validateTarget(url));
    }
}
