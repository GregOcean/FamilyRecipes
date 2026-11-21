package com.familyrecipes.util;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;
class ClientAddressTest {
    @Test void trustsOnlyLocalProxyAndNumericAddress() {
        var r=new MockHttpServletRequest();r.setRemoteAddr("127.0.0.1");r.addHeader("X-Real-IP","192.0.2.10");assertEquals("192.0.2.10",ClientAddress.of(r));
        r.setRemoteAddr("192.0.2.20");assertEquals("192.0.2.20",ClientAddress.of(r));
        r.setRemoteAddr("127.0.0.1");r.removeHeader("X-Real-IP");r.addHeader("X-Real-IP","attacker.example");assertEquals("127.0.0.1",ClientAddress.of(r));
    }
    @Test void publicUserDoesNotSerializeEmailOrPassword() throws Exception {
        var u=new com.familyrecipes.entity.User();u.setId(1L);u.setUsername("Alice");u.setEmail("private@example.test");u.setPassword("hash");
        var json=new com.fasterxml.jackson.databind.ObjectMapper();String value=json.writeValueAsString(u);
        assertFalse(value.contains("private@example.test"));assertFalse(value.contains("hash"));assertTrue(value.contains("Alice"));
        String own=json.writeValueAsString(new com.familyrecipes.controller.AuthController.OwnUser(1L,u.getEmail(),u.getUsername(),null));assertTrue(own.contains("private@example.test"));
    }
}
