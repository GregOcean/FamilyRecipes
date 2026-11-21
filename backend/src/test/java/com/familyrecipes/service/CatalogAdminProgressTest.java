package com.familyrecipes.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class CatalogAdminProgressTest {
    private final ObjectMapper json=new ObjectMapper();
    @Test void reportsCompletedFailedCurrentAndPendingWithoutGuessing() throws Exception {
        var recipes=json.readTree("[{\"id\":1,\"name\":\"鸡蛋\"},{\"id\":2,\"name\":\"豆腐\"},{\"id\":3},{\"id\":4}]");
        var report=json.readTree("{\"jobs\":[{\"id\":1,\"status\":\"done\",\"result\":{\"images\":[{}],\"link_candidates\":[{},{}]}},{\"id\":2,\"status\":\"running\"},{\"id\":3,\"status\":\"failed\",\"result\":{\"error\":\"TimeoutError\"}}],\"activity\":{\"recipe_id\":2,\"stage\":\"查找图片\"},\"requests_by_kind\":{\"search\":5}}");
        var progress=CatalogAdminService.summarize(recipes,report,"running");
        assertEquals(1,progress.get("completed"));assertEquals(1,progress.get("failed"));assertEquals(1,progress.get("running"));assertEquals(5,progress.get("searchRequests"));
        var items=(List<Map<String,Object>>)progress.get("items");assertEquals("查找图片",items.get(1).get("stage"));assertEquals("pending",items.get(3).get("status"));assertEquals(2,items.get(0).get("links"));
        var paused=CatalogAdminService.summarize(recipes,report,"paused");assertEquals(0,paused.get("running"));assertEquals("paused",((List<Map<String,Object>>)paused.get("items")).get(1).get("status"));
    }
    @Test void missingProgressIsPendingNotComplete() throws Exception {
        var p=CatalogAdminService.summarize(json.readTree("[{\"id\":1}]"),json.createObjectNode(),"queued");
        assertEquals(0,p.get("completed"));assertEquals(0,p.get("failed"));assertEquals(1,p.get("total"));
    }
}
