package com.familyrecipes.controller;

import com.familyrecipes.common.*;
import com.familyrecipes.entity.*;
import com.familyrecipes.service.*;
import com.familyrecipes.translation.TranslationService;
import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/catalog")
public class CatalogImportController {
    private final CuisineRefinementService refinement;
    private final JdbcTemplate db; private final ObjectMapper json; private final RecipeService recipes; private final FridgeService fridge;
    @Value("${app.catalog.admin-user-id:0}") private long admin;
    public CatalogImportController(JdbcTemplate db,ObjectMapper json,RecipeService recipes,FridgeService fridge,CuisineRefinementService refinement) {this.refinement=refinement;this.db=db;this.json=json;this.recipes=recipes;this.fridge=fridge;}
    private void requireAdmin(HttpServletRequest r) {if(admin<=0 || !Long.valueOf(admin).equals(r.getAttribute("userId")))throw new ApiException(403,"仅数据管理员可执行");}
    @GetMapping("/status") public Result<Map<String,Object>> status(HttpServletRequest r) {
        requireAdmin(r);return Result.success(Map.of("classification",db.queryForList("SELECT status,COUNT(*) AS count FROM recipe_classification GROUP BY status"),"translations",db.queryForList("SELECT status,COUNT(*) AS count FROM content_translations GROUP BY status"),"imported",db.queryForObject("SELECT COUNT(*) FROM recipe_provenance",Long.class)));
    }
    @PostMapping("/translations/retry") public Result<Integer> retry(HttpServletRequest r) {
        requireAdmin(r);return Result.success(db.update("UPDATE content_translations SET status='pending',attempts=0,next_attempt_at=NOW() WHERE status='failed'"));
    }
    @PostMapping("/classification/retry") public Result<Integer> retryClassification(HttpServletRequest r) {
        requireAdmin(r);return Result.success(db.update("DELETE FROM recipe_classification WHERE status IN ('failed','needs_review')"));
    }
    @GetMapping("/cuisine-refinement") public Result<Map<String,Object>> refinementStatus(HttpServletRequest r) {
        requireAdmin(r);return Result.success(refinement.audit());
    }
    @PostMapping("/cuisine-refinement") public Result<Map<String,Object>> enqueueRefinement(
            @RequestParam(defaultValue="5") int limit,HttpServletRequest r) {
        requireAdmin(r);return Result.success(Map.of("queued",refinement.enqueue(admin,limit),"taxonomy_version",CuisineRefinementService.VERSION));
    }
    @GetMapping("/imported") public Result<List<Map<String,Object>>> imported(HttpServletRequest r) {
        requireAdmin(r);return Result.success(db.queryForList("SELECT source_identity,recipe_id FROM recipe_provenance ORDER BY recipe_id"));
    }
    @GetMapping("/sources") public Result<List<Map<String,Object>>> sources(@RequestParam(defaultValue="0") long afterId) {
        if(afterId<0)throw new ApiException(400,"游标错误");
        return Result.success(db.queryForList("SELECT p.recipe_id,r.name,p.metadata FROM recipe_provenance p JOIN recipe r ON r.id=p.recipe_id WHERE r.visibility='public' AND p.recipe_id>? ORDER BY p.recipe_id LIMIT 100",afterId));
    }
    @PostMapping("/import") @Transactional
    public Result<Long> importRecipe(@RequestBody JsonNode body,HttpServletRequest request) throws Exception {
        requireAdmin(request);
        JsonNode provenance=body.path("provenance");
        for(String field:List.of("source","source_id","source_url","author","license","license_url"))
            if(provenance.path(field).asText().isBlank())throw new ApiException(400,"缺少来源元数据: "+field);
        String identity=TranslationService.digest(provenance.path("source").asText()+"\n"+provenance.path("source_id").asText());
        // Serialize imports for this administrator; unique identity also guards repeated requests.
        db.queryForObject("SELECT id FROM user WHERE id=? FOR UPDATE",Long.class,admin);
        var existing=db.queryForList("SELECT recipe_id FROM recipe_provenance WHERE source_identity=?",Long.class,identity);
        if(!existing.isEmpty())return Result.success(existing.get(0));
        Recipe recipe=json.treeToValue(body.path("recipe"),Recipe.class);
        recipe.setId(null);recipe.setCreatorId(admin);recipe.setVisibility("public");recipe.setKitchenId(null);recipe.setOwnerGroupId(null);recipe.setSharedGroupIds("[]");recipe.setSourceRecipeId(null);
        List<RecipeIngredient> ingredients=new ArrayList<>();
        if(body.path("ingredients").size()>100)throw new ApiException(400,"食材过多");
        for(JsonNode item:body.path("ingredients")) {
            String name=item.path("name").asText();if(name.isBlank() || name.length()>50)throw new ApiException(400,"食材名称无效");
            RecipeIngredient ingredient=new RecipeIngredient();ingredient.setIngredientId(fridge.getOrCreateIngredient(name,null).getId());ingredient.setAmount(item.path("amount").asText());ingredient.setIsMain(item.path("is_main").asBoolean(false));ingredients.add(ingredient);
        }
        List<CookingStep> steps=new ArrayList<>();for(JsonNode node:body.path("steps"))steps.add(json.treeToValue(node,CookingStep.class));
        if(steps.isEmpty())throw new ApiException(400,"菜谱缺少步骤");
        List<RecipeTag> tags=new ArrayList<>();for(JsonNode node:body.path("tags"))tags.add(json.treeToValue(node,RecipeTag.class));
        Recipe saved=recipes.createRecipe(recipe,tags,ingredients,steps,List.of(),List.of());
        db.update("INSERT INTO recipe_provenance(recipe_id,source_identity,metadata) VALUES(?,?,?)",saved.getId(),identity,json.writeValueAsString(provenance));
        return Result.success(saved.getId());
    }
}
