package com.familyrecipes.monitoring;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class RequestMetricsTest {
    @Test void windowExpiresButProcessCountersRemain() {
        var clock=new AtomicLong(100_000);var metrics=new RequestMetrics(clock::get);
        metrics.started();metrics.finished("GET /api/recipes/{id}",200,10,20,100);
        metrics.started();metrics.finished("GET /api/recipes/{id}",503,90,0,40);
        var snapshot=metrics.snapshot();assertEquals(2,snapshot.get("requests"));assertEquals(1L,snapshot.get("serverErrors"));
        assertEquals(90.0,snapshot.get("p95Millis"));assertEquals(0,snapshot.get("active"));
        clock.addAndGet(60_001);snapshot=metrics.snapshot();assertEquals(0,snapshot.get("requests"));assertEquals(2L,snapshot.get("totalSinceStart"));
    }
    @Test void filterCountsWriterAndNeverRecordsRawUrlOrQuery() throws Exception {
        var metrics=new RequestMetrics();var filter=new RequestMetricsFilter(metrics);
        var req=new org.springframework.mock.web.MockHttpServletRequest("GET","/api/recipes/123");req.setQueryString("private=secret");
        var res=new org.springframework.mock.web.MockHttpServletResponse();
        filter.doFilter(req,res,(request,response)->{
            request.setAttribute(org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,"/api/recipes/{id}");
            response.getWriter().write("ok");
        });
        assertEquals("ok",res.getContentAsString());assertEquals(2L,metrics.snapshot().get("responseBytesSinceStart"));
        String routes=metrics.snapshot().get("routes").toString();assertTrue(routes.contains("{id}"));assertFalse(routes.contains("secret"));assertFalse(routes.contains("123"));
    }
}
