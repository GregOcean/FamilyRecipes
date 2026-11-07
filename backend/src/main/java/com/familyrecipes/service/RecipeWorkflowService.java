package com.familyrecipes.service;

import com.familyrecipes.common.*;
import com.familyrecipes.entity.*;
import com.familyrecipes.mapper.RecipeMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecipeWorkflowService {
  @Autowired JdbcTemplate db;
  @Autowired RecipeService recipes;
  @Autowired RecipeAccessService access;
  @Autowired KitchenService kitchens;
  @Autowired MediaService media;
  @Autowired ObjectMapper json;

  public PageResult<Recipe> browse(
      Long user,
      Long kitchen,
      String scope,
      String origin,
      String keyword,
      String taste,
      Integer minutes,
      Integer missing,
      boolean known,
      String seed,
      int page) {
    if (page < 1
        || page > 10000
        || !Set.of("discovery", "mine").contains(scope)
        || !Set.of("all", "created", "favorites", "shared").contains(origin)
        || keyword.length() > 100
        || taste.length() > 20
        || seed.length() > 64
        || minutes != null && minutes < 1
        || missing != null && missing < 0) throw new ApiException(400, "筛选参数不合法");
    if (kitchen != null) kitchens.requireMember(kitchen, user);
    if (missing != null && kitchen == null) throw new ApiException(400, "请先选择厨房");
    List<Object> args = new ArrayList<>();
    String where = " WHERE " + RecipeMapper.VISIBLE.replace("#{viewerId}", "?");
    for (int i=0; i<RecipeMapper.VISIBLE.split("#\\{viewerId\\}", -1).length-1; i++) args.add(user);
    String mine =
        "(r.creator_id=? OR EXISTS(SELECT 1 FROM user_favorite f WHERE f.recipe_id=r.id AND"
            + " f.user_id=?) OR EXISTS(SELECT 1 FROM recipe_cook c WHERE c.recipe_id=r.id AND"
            + " c.user_id=?) OR ((r.visibility='group' OR "
            + ("(" + RecipeMapper.MEAL_SHARED + " OR " + RecipeMapper.CHAT_SHARED + ")").replace("#{viewerId}", "?")
            + ") AND r.creator_id<>?))";
    if (scope.equals("mine")) {
      where += " AND " + mine;
      Collections.addAll(args, user, user, user, user, user, user);
    }
    if (origin.equals("created")) {
      where += " AND r.creator_id=?";
      args.add(user);
    } else if (origin.equals("favorites")) {
      where += " AND EXISTS(SELECT 1 FROM user_favorite f WHERE f.recipe_id=r.id AND f.user_id=?)";
      args.add(user);
    } else if (origin.equals("shared")) {
      where +=
          " AND (r.visibility='group' OR "
              + ("(" + RecipeMapper.MEAL_SHARED + " OR " + RecipeMapper.CHAT_SHARED + ")").replace("#{viewerId}", "?")
              + ") AND r.creator_id<>?";
      args.add(user);
      args.add(user);
      args.add(user);
    }
    if (known) {
      where += " AND EXISTS(SELECT 1 FROM recipe_cook c WHERE c.recipe_id=r.id AND c.user_id=?)";
      args.add(user);
    }
    if (!keyword.isBlank()) {
      where +=
          " AND (r.name LIKE ? OR r.description LIKE ? OR EXISTS(SELECT 1 FROM recipe_tag t WHERE"
              + " t.recipe_id=r.id AND t.tag_value LIKE ?) OR EXISTS(SELECT 1 FROM"
              + " recipe_ingredient ri JOIN ingredient i ON i.id=ri.ingredient_id WHERE"
              + " ri.recipe_id=r.id AND i.name LIKE ?))";
      for (int i = 0; i < 4; i++) args.add("%" + keyword + "%");
    }
    if (!taste.isBlank()) {
      where +=
          " AND EXISTS(SELECT 1 FROM recipe_tag t WHERE t.recipe_id=r.id AND t.tag_type='taste' AND"
              + " t.tag_value LIKE ?)";
      args.add("%" + taste + "%");
    }
    if (minutes != null) {
      where += " AND r.cooking_time<=?";
      args.add(minutes);
    }
    if (missing != null) {
      where +=
          " AND EXISTS(SELECT 1 FROM recipe_ingredient ri WHERE ri.recipe_id=r.id) AND (SELECT"
              + " COUNT(DISTINCT ri.ingredient_id) FROM recipe_ingredient ri WHERE"
              + " ri.recipe_id=r.id AND NOT EXISTS(SELECT 1 FROM fridge_item f WHERE"
              + " f.ingredient_id=ri.ingredient_id AND f.kitchen_id=? AND f.status IN"
              + " ('normal','expiring') AND f.expiry_date>=CURRENT_DATE))<=?";
      args.add(kitchen);
      args.add(missing);
    }
    Long total =
        db.queryForObject("SELECT COUNT(*) FROM recipe r" + where, Long.class, args.toArray());
    String order =
        scope.equals("discovery") && keyword.isBlank()
            ? " ORDER BY ((SELECT COUNT(*) FROM recipe_tag t WHERE t.recipe_id=r.id AND"
                + " EXISTS(SELECT 1 FROM recipe_tag liked JOIN user_favorite f ON"
                + " f.recipe_id=liked.recipe_id WHERE f.user_id=? AND liked.tag_type=t.tag_type"
                + " AND liked.tag_value=t.tag_value)) + LEAST(3,(SELECT COUNT(*) FROM"
                + " cooking_history h WHERE h.recipe_id=r.id AND h.user_id=?)) +"
                + " MOD(CRC32(CONCAT(r.id,?)),100)/25.0) DESC,r.id DESC"
            : " ORDER BY r.id DESC";
    if (scope.equals("discovery") && keyword.isBlank()) {
      args.add(user);
      args.add(user);
      args.add(seed);
    }
    args.add((page - 1) * 20);
    var ids =
        db.queryForList(
            "SELECT r.id FROM recipe r" + where + order + " LIMIT 20 OFFSET ?",
            Long.class,
            args.toArray());
    return new PageResult<>(
        ids.stream().map(id -> recipes.getRecipeDetail(id, user)).toList(), total, page, 20);
  }

  @Transactional
  public Recipe adapt(Long id, Long user, Long kitchen) {
    var r = recipes.getRecipeDetail(id, user);
    kitchens.requireMember(kitchen, user);
    r.setSourceRecipeId(id);
    r.setSourceRecipeName(r.getName());
    r.setName(r.getName().substring(0, Math.min(90, r.getName().length())) + " · 我的做法");
    r.setId(null);
    r.setCreatorId(user);
    r.setVisibility("private");
    r.setSharedGroupIds("[]");
    r.setKitchenId(kitchen);
    r.setOwnerGroupId(null);
    Map<String, String> copies = new HashMap<>();
    java.util.function.Function<String, String> copy =
        url -> url == null ? null : copies.computeIfAbsent(url, u -> media.copyForOwner(u, user));
    r.setCoverImage(copy.apply(r.getCoverImage()));
    r.getImages().forEach(i -> i.setUrl(copy.apply(i.getUrl())));
    r.getSteps().forEach(i -> i.setImage(copy.apply(i.getImage())));
    r.getExternalRecipes().forEach(i -> i.setThumbnail(copy.apply(i.getThumbnail())));
    return recipes.createRecipe(
        r, r.getTags(), r.getIngredients(), r.getSteps(), List.of(), r.getExternalRecipes());
  }

  @Transactional
  public void known(Long id, Long user, boolean known) {
    access.requireRead(id, user);
    if (known)
      db.update(
          "INSERT IGNORE INTO recipe_cook(recipe_id,user_id,skill_level) VALUES(?,?,3)", id, user);
    else db.update("DELETE FROM recipe_cook WHERE recipe_id=? AND user_id=?", id, user);
  }

  public List<CookingHistory> records(Long id, Long user) {
    access.requireRead(id, user);
    return db.query(
        "SELECT * FROM cooking_history WHERE recipe_id=? AND user_id=? ORDER BY cooked_at DESC,id"
            + " DESC",
        (r, n) -> {
          var h = new CookingHistory();
          h.setId(r.getLong("id"));
          h.setRecipeId(id);
          h.setUserId(user);
          h.setCookedAt(r.getTimestamp("cooked_at").toLocalDateTime());
          h.setNotes(r.getString("notes"));
          h.setImages(r.getString("images"));
          return h;
        },
        id,
        user);
  }

  @Transactional
  public void record(Long id, Long user, CookingHistory h) {
    access.requireRead(id, user);
    if (h.getNotes() == null
        || h.getNotes().isBlank()
        || h.getNotes().length() > 10000
        || h.getCookedAt() == null) throw new ApiException(400, "请填写做菜日期和心得");
    try {
      String[] images =
          json.readValue(h.getImages() == null ? "[]" : h.getImages(), String[].class);
      if (images.length > 20) throw new ApiException(400, "最多20张图片");
      for (String url : images) media.requireOwned(url, user);
      db.update(
          "INSERT INTO cooking_history(recipe_id,user_id,cooked_at,notes,images) VALUES(?,?,?,?,?)",
          id,
          user,
          h.getCookedAt(),
          h.getNotes(),
          json.writeValueAsString(images));
    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      throw new ApiException(400, "记录格式不正确");
    }
  }
}
