package com.familyrecipes.controller;

import com.familyrecipes.common.*;
import com.familyrecipes.entity.*;
import com.familyrecipes.service.RecipeWorkflowService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recipes")
public class RecipeWorkflowController {
  @Autowired RecipeWorkflowService service;

  @GetMapping("/browse")
  public Result<PageResult<Recipe>> browse(
      @RequestParam(required = false) Long kitchenId,
      @RequestParam(defaultValue = "discovery") String scope,
      @RequestParam(defaultValue = "all") String origin,
      @RequestParam(defaultValue = "") String keyword,
      @RequestParam(defaultValue = "") String taste,
      @RequestParam(required = false) Integer minutes,
      @RequestParam(required = false) Integer missing,
      @RequestParam(defaultValue = "false") boolean known,
      @RequestParam(defaultValue = "0") String seed,
      @RequestParam(defaultValue = "1") int page,
      HttpServletRequest r) {
    return Result.success(
        service.browse(
            (Long) r.getAttribute("userId"),
            kitchenId,
            scope,
            origin,
            keyword,
            taste,
            minutes,
            missing,
            known,
            seed,
            page));
  }

  @PostMapping("/{id}/adapt")
  public Result<Recipe> adapt(
      @PathVariable Long id, @RequestBody Map<String, Long> b, HttpServletRequest r) {
    return Result.success(service.adapt(id, (Long) r.getAttribute("userId"), b.get("kitchenId")));
  }

  @PutMapping("/{id}/known")
  public Result<Void> known(
      @PathVariable Long id, @RequestBody Map<String, Boolean> b, HttpServletRequest r) {
    service.known(id, (Long) r.getAttribute("userId"), Boolean.TRUE.equals(b.get("known")));
    return Result.success();
  }

  @GetMapping("/{id}/records")
  public Result<List<CookingHistory>> records(@PathVariable Long id, HttpServletRequest r) {
    return Result.success(service.records(id, (Long) r.getAttribute("userId")));
  }

  @PostMapping("/{id}/records")
  public Result<Void> record(
      @PathVariable Long id, @RequestBody CookingHistory h, HttpServletRequest r) {
    service.record(id, (Long) r.getAttribute("userId"), h);
    return Result.success();
  }
}
