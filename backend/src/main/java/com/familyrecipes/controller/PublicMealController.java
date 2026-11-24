package com.familyrecipes.controller;

import com.familyrecipes.common.Result;
import com.familyrecipes.service.PublicMealService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PublicMealController {
  @Autowired PublicMealService service;

  @PostMapping("/meals/{id}/public-share")
  public Result<PublicMealService.PublicShare> share(
      @PathVariable Long id, HttpServletRequest request) {
    return Result.success(service.share(id, (Long) request.getAttribute("userId")));
  }

  @GetMapping("/public/meals/{token}")
  public Result<PublicMealService.PublicMeal> view(@PathVariable String token) {
    return Result.success(service.view(token));
  }

  @PostMapping("/public/meals/{token}/check")
  public Result<PublicMealService.PublicMeal> check(
      @PathVariable String token, @RequestBody PublicMealService.CheckRequest body) {
    return Result.success(service.check(token, body));
  }
}
