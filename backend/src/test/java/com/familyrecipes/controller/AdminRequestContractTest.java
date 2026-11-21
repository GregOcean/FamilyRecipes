package com.familyrecipes.controller;

import com.familyrecipes.common.ApiExceptionHandler;
import com.familyrecipes.config.WebConfig;
import com.familyrecipes.interceptor.AuthInterceptor;
import com.familyrecipes.service.AdminMonitoringService;
import com.familyrecipes.service.CatalogAdminService;
import com.familyrecipes.translation.ContentTranslationAdvice;
import com.familyrecipes.translation.TranslationService;
import com.familyrecipes.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Uses Boot's application.yml and real MVC Jackson conversion, not standaloneSetup or DTO mocks. */
@WebMvcTest(AdminController.class)
@ContextConfiguration(classes={AdminController.class,WebConfig.class,AuthInterceptor.class,
    ApiExceptionHandler.class,ContentTranslationAdvice.class})
class AdminRequestContractTest {
    private static final long ADMIN=41L;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean CatalogAdminService admin;
    @MockBean AdminMonitoringService monitoring;
    @MockBean TranslationService translations;
    @MockBean JwtUtil jwt;
    @TempDir Path directory;
    private JdbcTemplate db;

    @BeforeEach void fixture() throws Exception {
        // Protect the exact production configuration that exposed the regression.
        assertEquals(PropertyNamingStrategies.SNAKE_CASE,json.getPropertyNamingStrategy());
        when(jwt.validateToken("test-session")).thenReturn(true);
        when(jwt.getUserIdFromToken("test-session")).thenReturn(ADMIN);
        db=mock(JdbcTemplate.class);
        when(db.queryForObject(anyString(),eq(Integer.class))).thenReturn(0);
        when(db.queryForList(startsWith("SELECT r.id,r.name"),anyLong())).thenAnswer(call -> {
            Map<String,Object> recipe=new HashMap<>();
            recipe.put("id",call.getArgument(1,Long.class));recipe.put("name","Fixture recipe");recipe.put("has_image",0);
            return new ArrayList<>(List.of(recipe));
        });
        // Real create validation and preparation, with isolated disk/DB fixtures. tick() never runs.
        CatalogAdminService creation=new CatalogAdminService();
        ReflectionTestUtils.setField(creation,"adminId",ADMIN);
        ReflectionTestUtils.setField(creation,"db",db);
        ReflectionTestUtils.setField(creation,"json",json);
        ReflectionTestUtils.setField(creation,"directory",directory.toString());
        ReflectionTestUtils.setField(creation,"searchKey","fixture-key-never-sent");
        when(admin.create(any(),eq(ADMIN))).thenAnswer(call -> creation.create(call.getArgument(0),call.getArgument(1)));
    }

    @ParameterizedTest @ValueSource(strings={"recipe_ids","recipeIds"})
    void oneSelectedRecipeReachesServiceWithoutLosingItsId(String property) throws Exception {
        mvc.perform(post("/api/admin/jobs").header("Authorization","Bearer test-session")
            .contentType(MediaType.APPLICATION_JSON).content("{\""+property+"\":[314]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").isString());
        verify(admin).create(List.of(314L),ADMIN);
        verify(db).update(startsWith("INSERT INTO catalog_admin_job"),anyString(),contains("314"),eq(ADMIN));
    }

    @ParameterizedTest @ValueSource(strings={"recipe_id","recipeId"})
    void applyingCandidatesPreservesRecipeIdAndIndexes(String property) throws Exception {
        mvc.perform(post("/api/admin/jobs/fixture-job/apply").header("Authorization","Bearer test-session")
            .contentType(MediaType.APPLICATION_JSON).content("{\""+property+"\":314,\"links\":[0,2],\"image\":1}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        verify(admin).apply("fixture-job",314L,List.of(0,2),1,ADMIN);
    }

    @ParameterizedTest @ValueSource(strings={"recipe_ids","recipeIds"})
    void realServiceRejectsEmptyDuplicateAndOverThirtySelections(String property) throws Exception {
        for(List<Long> ids:List.of(List.<Long>of(),List.of(314L,314L),LongStream.rangeClosed(1,31).boxed().toList())) {
            mvc.perform(post("/api/admin/jobs").header("Authorization","Bearer test-session")
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(property,ids))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        }
        verifyNoInteractions(db);
    }

    @Test void thirtySelectionsRemainAllowed() throws Exception {
        List<Long> ids=LongStream.rangeClosed(1,30).boxed().toList();
        mvc.perform(post("/api/admin/jobs").header("Authorization","Bearer test-session")
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("recipe_ids",ids))))
            .andExpect(status().isOk());
        verify(admin).create(ids,ADMIN);
    }

    @Test void absentSelectionIsRejectedBeforeAnyDatabaseOrSearchWork() throws Exception {
        mvc.perform(post("/api/admin/jobs").header("Authorization","Bearer test-session")
            .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(db);
    }
}
