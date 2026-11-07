package com.familyrecipes.controller;

import com.familyrecipes.common.Result;
import com.familyrecipes.entity.MealArrangement;
import com.familyrecipes.service.MealService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meals")
public class MealController {
  @Autowired MealService service;

  @GetMapping
  public Result<List<MealArrangement>> list(@RequestParam Long kitchenId, HttpServletRequest r) {
    return Result.success(service.list((Long) r.getAttribute("userId"), kitchenId));
  }

  @GetMapping("/{id}")
  public Result<MealArrangement> get(@PathVariable Long id, HttpServletRequest r) {
    return Result.success(service.get(id, (Long) r.getAttribute("userId")));
  }

  @PostMapping
  public Result<MealArrangement> save(@RequestBody MealArrangement m, HttpServletRequest r) {
    return Result.success(service.save(m, (Long) r.getAttribute("userId")));
  }

  @PostMapping("/{id}/share")
  public Result<com.familyrecipes.model.Message> share(
      @PathVariable Long id, @RequestBody com.familyrecipes.model.ChatShareRequest body, HttpServletRequest r) {
    return Result.success(service.share(id, body.getGroupId(), (Long) r.getAttribute("userId"), body.getClientMessageId()));
  }
}
