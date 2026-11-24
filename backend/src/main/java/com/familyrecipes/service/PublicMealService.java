package com.familyrecipes.service;

import com.familyrecipes.common.ApiException;
import com.familyrecipes.entity.MealArrangement;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 面向未注册家人的只读菜单页，以及采购项勾选。 */
@Service
public class PublicMealService {
  private static final SecureRandom RANDOM = new SecureRandom();

  @Autowired JdbcTemplate db;
  @Autowired ObjectMapper json;
  @Autowired MealService meals;

  public static class PublicShare {
    public String token;
    public String path;
    public PublicShare(String token) {
      this.token = token;
      this.path = "/m/" + token;
    }
  }

  public static class PublicMeal {
    public String title, kind, meal, kitchenName;
    public LocalDate mealDate;
    public List<MealArrangement.Dish> dishes;
    public List<MealArrangement.Shopping> shopping;
  }

  public static class CheckRequest {
    public Integer index;
    public Boolean done;
  }

  @Transactional
  public PublicShare share(Long mealId, Long userId) {
    var meal = meals.get(mealId, userId);
    if (!userId.equals(meal.getOwnerId())) throw new ApiException(403, "只有创建者可以分享菜单");
    var existing =
        db.query(
            "SELECT public_token FROM meal_arrangement WHERE id=? AND public_token IS NOT NULL",
            (rs, n) -> rs.getString(1),
            mealId);
    if (!existing.isEmpty()) return new PublicShare(existing.get(0));
    for (int attempt = 0; attempt < 5; attempt++) {
      var token = randomToken();
      try {
        if (db.update(
                "UPDATE meal_arrangement SET public_token=? WHERE id=? AND owner_id=? AND"
                    + " public_token IS NULL",
                token,
                mealId,
                userId)
            == 1) return new PublicShare(token);
      } catch (org.springframework.dao.DuplicateKeyException ignored) {
        // Extremely unlikely collision, retry with a new token.
      }
      var current =
          db.query(
              "SELECT public_token FROM meal_arrangement WHERE id=?",
              (rs, n) -> rs.getString(1),
              mealId);
      if (!current.isEmpty() && current.get(0) != null) return new PublicShare(current.get(0));
    }
    throw new ApiException(500, "分享链接生成失败，请稍后重试");
  }

  public PublicMeal view(String token) {
    var rows =
        db.query(
            "SELECT m.*,k.name AS kitchen_name FROM meal_arrangement m LEFT JOIN kitchen k ON"
                + " k.id=m.kitchen_id WHERE m.public_token=?",
            (rs, n) -> {
              var view = new PublicMeal();
              view.title = rs.getString("title");
              view.kind = rs.getString("kind");
              view.meal = rs.getString("meal");
              view.kitchenName = rs.getString("kitchen_name");
              var date = rs.getDate("meal_date");
              view.mealDate = date == null ? null : date.toLocalDate();
              view.dishes = readDishes(rs.getString("dishes"));
              view.shopping = readShopping(rs.getString("shopping"));
              return view;
            },
            token);
    if (rows.isEmpty()) throw new ApiException(404, "分享链接不存在或已失效");
    return rows.get(0);
  }

  @Transactional
  public PublicMeal check(String token, CheckRequest body) {
    if (body == null
        || body.index == null
        || body.index < 0
        || body.index > 199
        || body.done == null) throw new ApiException(400, "采购项不合法");
    var shopping = view(token).shopping;
    if (body.index >= shopping.size()) throw new ApiException(400, "采购项不存在");
    shopping.get(body.index).setDone(body.done);
    try {
      db.update(
          "UPDATE meal_arrangement SET shopping=? WHERE public_token=?",
          json.writeValueAsString(shopping),
          token);
    } catch (Exception e) {
      throw new ApiException(500, "采购清单保存失败");
    }
    return view(token);
  }

  private List<MealArrangement.Dish> readDishes(String raw) {
    try {
      return raw == null
          ? List.of()
          : Arrays.asList(json.readValue(raw, MealArrangement.Dish[].class));
    } catch (Exception e) {
      return List.of();
    }
  }

  private List<MealArrangement.Shopping> readShopping(String raw) {
    try {
      return raw == null
          ? List.of()
          : Arrays.asList(json.readValue(raw, MealArrangement.Shopping[].class));
    } catch (Exception e) {
      return List.of();
    }
  }

  private String randomToken() {
    var bytes = new byte[16];
    RANDOM.nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }
}
