package com.familyrecipes.util;

import com.familyrecipes.common.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAvailabilityTest {
    @Test void databaseOutageIsUnavailableNotInvalidIdentity() {
        var db=mock(JdbcTemplate.class);var jwt=new JwtUtil();
        ReflectionTestUtils.setField(jwt,"db",db);
        ReflectionTestUtils.setField(jwt,"secret","local-test-secret-at-least-thirty-two-bytes");
        ReflectionTestUtils.setField(jwt,"expiration",60000L);
        when(db.queryForObject(anyString(),eq(Long.class),eq(1L))).thenReturn(0L);
        String token=jwt.generateToken(1L,"local@example.test");
        assertTrue(jwt.validateToken(token));
        when(db.queryForObject(anyString(),eq(Long.class),eq(1L))).thenThrow(new DataAccessResourceFailureException("offline"));
        assertEquals(503,assertThrows(ApiException.class,()->jwt.validateToken(token)).getStatus());
        assertFalse(jwt.validateToken("invalid"));
    }
}
