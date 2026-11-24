package com.familyrecipes.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** 把免登录菜单短链交给静态页，由前端读取 URL 中的 token。 */
@Controller
public class PublicMealPageController {
  @GetMapping("/m/{token}")
  public String page() {
    return "forward:/public-meal.html";
  }
}
