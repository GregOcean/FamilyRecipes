package com.familyrecipes.controller;
import com.familyrecipes.common.Result;
import com.familyrecipes.service.CatalogAdminService;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired private CatalogAdminService admin;
    @Autowired private com.familyrecipes.service.AdminMonitoringService monitoring;
    private Long check(HttpServletRequest r){Long id=(Long)r.getAttribute("userId");admin.requireAdmin(id);return id;}
    @GetMapping("/recipes") public Result<?> recipes(HttpServletRequest r,@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="false")boolean missing){check(r);return Result.success(admin.recipes(q,page,missing));}
    @GetMapping("/monitor") public Result<?> monitor(HttpServletRequest r)throws Exception{check(r);return Result.success(admin.monitor());}
    @GetMapping("/metrics") public Result<?> metrics(HttpServletRequest r){check(r);return Result.success(monitoring.snapshot());}
    @PostMapping("/tasks/{kind}/{action:pause|resume|retry}") public Result<?> task(HttpServletRequest r,@PathVariable String kind,@PathVariable String action){check(r);monitoring.control(kind,action);return Result.success();}
    @GetMapping("/jobs") public Result<?> jobs(HttpServletRequest r){check(r);return Result.success(admin.jobs());}
    public record Create(@JsonProperty("recipe_ids") @JsonAlias("recipeIds") List<Long> recipeIds){}
    @PostMapping("/jobs") public Result<?> create(HttpServletRequest r,@RequestBody Create body)throws Exception{return Result.success(java.util.Map.of("id",admin.create(body.recipeIds(),check(r))));}
    @GetMapping("/jobs/{id}") public Result<?> result(HttpServletRequest r,@PathVariable String id)throws Exception{check(r);return Result.success(admin.result(id));}
    @PostMapping("/jobs/{id}/{action:pause|retry}") public Result<?> action(HttpServletRequest r,@PathVariable String id,@PathVariable String action){check(r);admin.action(id,action);return Result.success();}
    public record Apply(@JsonProperty("recipe_id") @JsonAlias("recipeId") long recipeId,List<Integer> links,Integer image){}
    @PostMapping("/jobs/{id}/apply") public Result<?> apply(HttpServletRequest r,@PathVariable String id,@RequestBody Apply body)throws Exception{admin.apply(id,body.recipeId(),body.links(),body.image(),check(r));return Result.success();}
}
