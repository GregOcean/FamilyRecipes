package com.familyrecipes.service;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import com.familyrecipes.ai.AiRoutes;
import com.familyrecipes.monitoring.RequestMetrics;
import com.familyrecipes.common.ApiException;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class AdminMonitoringServiceTest {
    @Test void missingDatabaseDoesNotHideMachineAndTrafficMetrics() {
        JdbcTemplate db=mock(JdbcTemplate.class);when(db.queryForObject("SELECT 1",Integer.class)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("secret connection string"));
        var service=new AdminMonitoringService(db,mock(javax.sql.DataSource.class),new RequestMetrics(),new TaskControl(),new AiRoutes());
        ReflectionTestUtils.setField(service,"uploads",System.getProperty("java.io.tmpdir"));
        var data=service.snapshot();assertTrue(data.containsKey("hardware"));assertTrue(data.containsKey("traffic"));assertFalse(data.toString().contains("secret connection string"));
        assertEquals("down",((java.util.Map<?,?>)data.get("database")).get("status"));
    }
    @Test void taskControlsCannotStartUnconfiguredJobs() {
        var controls=new TaskControl();var service=new AdminMonitoringService(mock(JdbcTemplate.class),mock(javax.sql.DataSource.class),new RequestMetrics(),controls,new AiRoutes());
        service.control("translation","pause");assertTrue(controls.isPaused("translation"));
        assertThrows(ApiException.class,()->service.control("translation","resume"));
        assertThrows(ApiException.class,()->service.control("arbitrary-table","retry"));
    }
}
