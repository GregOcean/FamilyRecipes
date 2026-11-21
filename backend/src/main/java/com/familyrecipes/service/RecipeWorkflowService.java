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
    return browse(user,kitchen,scope,origin,keyword,taste,minutes,missing,known,seed,page,"");
  }

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
      int page, String cuisine) {
    return browse(user,kitchen,scope,origin,keyword,taste,minutes,missing,known,seed,page,cuisine,"");
  }

  public PageResult<Recipe> browse(
      Long user, Long kitchen, String scope, String origin, String keyword, String taste,
      Integer minutes, Integer missing, boolean known, String seed, int page, String cuisine, String sources) {
    return browse(user,kitchen,scope,origin,keyword,taste,minutes,missing,known,seed,page,cuisine,sources,"","","","");
  }

  public PageResult<Recipe> browse(
      Long user, Long kitchen, String scope, String origin, String keyword, String taste,
      Integer minutes, Integer missing, boolean known, String seed, int page, String cuisine,
      String sources, String tastes, String cuisines, String times, String stocks) {
    // “#标签”前缀表示按标签搜索，标签本身不带 # 存储。
    boolean tagOnly = keyword != null && keyword.trim().startsWith("#");
    if (tagOnly) keyword = keyword.trim().substring(1).trim();
    if (keyword == null) keyword = "";
    var tasteValues=selections(tastes,12,20);
    var cuisineValues=selections(cuisines,100,50);
    var timeValues=selections(times,4,3);
    var stockValues=selections(stocks,2,2);
    if(!Set.of("15","30","60","-60").containsAll(timeValues) || !Set.of("0","-1").containsAll(stockValues))
      throw new ApiException(400,"筛选参数不合法");
    var selectedSources = new LinkedHashSet<String>();
    if(sources == null || sources.length()>80) throw new ApiException(400,"来源筛选不合法");
    if(!sources.isBlank()) {
      selectedSources.addAll(Arrays.asList(sources.split(",",-1)));
      if(!Set.of("created","known","favorites","shared").containsAll(selectedSources))throw new ApiException(400,"来源筛选不合法");
      if(user==null)throw new ApiException(401,"请先登录");
    }
    if (cuisine == null || cuisine.length()>50) throw new ApiException(400,"菜系不合法");
    if (page < 1
        || page > 10000
        || !Set.of("discovery", "mine").contains(scope)
        || !Set.of("all", "created", "favorites", "shared").contains(origin)
        || keyword.length() > 100
        || taste.length() > 20
        || seed.length() > 64
        || minutes != null && minutes < 1 && minutes != -60
        || missing != null && missing < -1) throw new ApiException(400, "筛选参数不合法");
    if (kitchen != null) kitchens.requireMember(kitchen, user);
    if ((missing != null || !stockValues.isEmpty()) && kitchen == null) throw new ApiException(400, "请先选择厨房");
    List<Object> args = new ArrayList<>();
    String where = " WHERE " + RecipeMapper.VISIBLE.replace("#{viewerId}", "?");
    for (int i=0; i<RecipeMapper.VISIBLE.split("#\\{viewerId\\}", -1).length-1; i++) args.add(user);
    String mine =
        "(r.creator_id=? OR EXISTS(SELECT 1 FROM user_favorite f WHERE f.recipe_id=r.id AND"
            + " f.user_id=?) OR EXISTS(SELECT 1 FROM recipe_cook c WHERE c.recipe_id=r.id AND"
            + " c.user_id=?) OR ((r.visibility='group' OR "
            + ("(" + RecipeMapper.MEAL_SHARED + " OR " + RecipeMapper.CHAT_SHARED + ")").replace("#{viewerId}", "?")
            + ") AND r.creator_id<>?))";
    if(!selectedSources.isEmpty()) {
      var predicates=new ArrayList<String>();
      for(String source:selectedSources) {
        switch(source) {
          case "created" -> {predicates.add("r.creator_id=?");args.add(user);}
          case "known" -> {predicates.add("EXISTS(SELECT 1 FROM recipe_cook c WHERE c.recipe_id=r.id AND c.user_id=?)");args.add(user);}
          case "favorites" -> {predicates.add("EXISTS(SELECT 1 FROM user_favorite f WHERE f.recipe_id=r.id AND f.user_id=?)");args.add(user);}
          case "shared" -> {
            predicates.add("((r.visibility='group' OR "+("("+RecipeMapper.MEAL_SHARED+" OR "+RecipeMapper.CHAT_SHARED+")").replace("#{viewerId}","?")+") AND r.creator_id<>?)");
            Collections.addAll(args,user,user,user);
          }
          default -> throw new ApiException(400,"来源筛选不合法");
        }
      }
      where+=" AND ("+String.join(" OR ",predicates)+")";
    }
    // Keep the legacy single-source contract for previously installed clients.
    if (selectedSources.isEmpty() && scope.equals("mine")) {
      where += " AND " + mine;
      Collections.addAll(args, user, user, user, user, user, user);
    }
    if (selectedSources.isEmpty() && origin.equals("created")) {
      where += " AND r.creator_id=?";
      args.add(user);
    } else if (selectedSources.isEmpty() && origin.equals("favorites")) {
      where += " AND EXISTS(SELECT 1 FROM user_favorite f WHERE f.recipe_id=r.id AND f.user_id=?)";
      args.add(user);
    } else if (selectedSources.isEmpty() && origin.equals("shared")) {
      where +=
          " AND (r.visibility='group' OR "
              + ("(" + RecipeMapper.MEAL_SHARED + " OR " + RecipeMapper.CHAT_SHARED + ")").replace("#{viewerId}", "?")
              + ") AND r.creator_id<>?";
      args.add(user);
      args.add(user);
      args.add(user);
    }
    if (selectedSources.isEmpty() && known) {
      where += " AND EXISTS(SELECT 1 FROM recipe_cook c WHERE c.recipe_id=r.id AND c.user_id=?)";
      args.add(user);
    }
    if (tagOnly && !keyword.isBlank()) {
      where += " AND EXISTS(SELECT 1 FROM recipe_tag t WHERE t.recipe_id=r.id AND t.tag_value LIKE ?)";
      args.add("%" + keyword + "%");
    } else if (!keyword.isBlank()) {
      where +=
          " AND (r.name LIKE ? OR r.description LIKE ? OR EXISTS(SELECT 1 FROM recipe_tag t WHERE"
              + " t.recipe_id=r.id AND t.tag_value LIKE ?) OR EXISTS(SELECT 1 FROM"
              + " recipe_ingredient ri JOIN ingredient i ON i.id=ri.ingredient_id WHERE"
              + " ri.recipe_id=r.id AND i.name LIKE ?) OR EXISTS(SELECT 1 FROM content_translations ct "
              + "WHERE ct.source_hash=SHA2(r.name,256) AND ct.kind='content' AND ct.status='ready' AND ct.translated_text LIKE ?) OR EXISTS(SELECT 1 FROM content_translations ct "
              + "WHERE ct.source_hash=SHA2(r.description,256) AND ct.kind='content' AND ct.status='ready' AND ct.translated_text LIKE ?))";
      for (int i = 0; i < 6; i++) args.add("%" + keyword + "%");
    }
    if(!tasteValues.isEmpty()) {
      where += " AND EXISTS(SELECT 1 FROM recipe_tag t WHERE t.recipe_id=r.id AND t.tag_type='taste' AND ("+
          String.join(" OR ",Collections.nCopies(tasteValues.size(),"t.tag_value LIKE ?"))+"))";
      for(String value:tasteValues)args.add("%"+value+"%");
    } else if (!taste.isBlank()) {
      for(String part : taste.equals("酸辣") ? List.of("酸","辣") : List.of(taste)) {
        where += " AND EXISTS(SELECT 1 FROM recipe_tag t WHERE t.recipe_id=r.id AND t.tag_type='taste' AND t.tag_value LIKE ?)";
        args.add("%"+part+"%");
      }
    }
    if(!timeValues.isEmpty()) {
      var durations=new ArrayList<String>();
      for(String value:timeValues) {
        int limit=Integer.parseInt(value);
        durations.add(limit<0?"r.cooking_time>?":"r.cooking_time<=?");
        args.add(Math.abs(limit));
      }
      where += " AND r.cooking_time>0 AND ("+String.join(" OR ",durations)+")";
    } else if (minutes != null) {
      where += minutes == -60 ? " AND r.cooking_time>?" : " AND r.cooking_time>0 AND r.cooking_time<=?";
      args.add(minutes == -60 ? 60 : minutes);
    }
    if (!cuisineValues.isEmpty() || !cuisine.isBlank()) {
      Set<String> values=new LinkedHashSet<>();
      for(String selected:cuisineValues.isEmpty()?List.of(cuisine):cuisineValues)values.addAll(CuisineCatalog.expand(selected));
      where += " AND EXISTS(SELECT 1 FROM recipe_tag t WHERE t.recipe_id=r.id AND t.tag_type='cuisine' AND t.tag_value IN ("+String.join(",",Collections.nCopies(values.size(),"?"))+"))";
      args.addAll(values);
    }
    Integer stockConstraint=missing;
    if(!stockValues.isEmpty())stockConstraint=stockValues.contains("-1")?-1:0;
    if (stockConstraint != null && stockConstraint == -1) {
      where += " AND EXISTS(SELECT 1 FROM recipe_ingredient ri JOIN fridge_item f ON f.ingredient_id=ri.ingredient_id WHERE ri.recipe_id=r.id AND f.kitchen_id=? AND f.status IN ('normal','expiring') AND (f.expiry_date IS NULL OR f.expiry_date>=CURRENT_DATE))";
      args.add(kitchen);
    } else if (stockConstraint != null) {
      where +=
          " AND EXISTS(SELECT 1 FROM recipe_ingredient ri WHERE ri.recipe_id=r.id) AND (SELECT"
              + " COUNT(DISTINCT ri.ingredient_id) FROM recipe_ingredient ri WHERE"
              + " ri.recipe_id=r.id AND NOT EXISTS(SELECT 1 FROM fridge_item f WHERE"
              + " f.ingredient_id=ri.ingredient_id AND f.kitchen_id=? AND f.status IN"
              + " ('normal','expiring') AND (f.expiry_date IS NULL OR f.expiry_date>=CURRENT_DATE)))<=?";
      args.add(kitchen);
      args.add(stockConstraint);
    }
    Long total =
        db.queryForObject("SELECT COUNT(*) FROM recipe r" + where, Long.class, args.toArray());
    String order=" ORDER BY r.id DESC";
    if(tagOnly && !keyword.isBlank()) {
      order=" ORDER BY CASE WHEN EXISTS(SELECT 1 FROM recipe_tag t WHERE t.recipe_id=r.id AND t.tag_value=?) THEN 0 ELSE 1 END,"
          + " COALESCE(r.view_count,0) DESC, COALESCE(r.favorite_count,0) DESC,r.id DESC";
      args.add(keyword);
    } else if(!keyword.isBlank()) {
      order=" ORDER BY CASE WHEN r.name=? OR EXISTS(SELECT 1 FROM content_translations ct WHERE ct.source_hash=SHA2(r.name,256) AND ct.kind='content' AND ct.status='ready' AND ct.translated_text=?) THEN 0 "
          + "WHEN r.name LIKE ? OR EXISTS(SELECT 1 FROM content_translations ct WHERE ct.source_hash=SHA2(r.name,256) AND ct.kind='content' AND ct.status='ready' AND ct.translated_text LIKE ?) THEN 1 "
          + "WHEN EXISTS(SELECT 1 FROM recipe_ingredient ri JOIN ingredient i ON i.id=ri.ingredient_id WHERE ri.recipe_id=r.id AND i.name LIKE ?) THEN 2 ELSE 3 END,"
          + " COALESCE(r.view_count,0) DESC, COALESCE(r.favorite_count,0) DESC,r.id DESC";
      Collections.addAll(args,keyword,keyword,"%"+keyword+"%","%"+keyword+"%","%"+keyword+"%");
    }
    if(scope.equals("discovery") && keyword.isBlank()) {
      String score=user==null ? "MOD(CRC32(CONCAT(r.id,':',#{seed})),100)/50.0" : com.familyrecipes.mapper.RecommendationScore.SQL;
      int viewers=score.split("#\\{viewerId\\}",-1).length-1;
      for(int i=0;i<viewers;i++) args.add(user);
      args.add(seed);
      order=" ORDER BY "+score.replace("#{viewerId}","?").replace("#{seed}","?")+" DESC,r.id DESC";
    }
    args.add((page - 1) * 20);
    var ids =
        db.queryForList(
            "SELECT r.id FROM recipe r" + where + order + " LIMIT 20 OFFSET ?",
            Long.class,
            args.toArray());
    return new PageResult<>(
        browseCards(ids, user), total, page, 20);
  }

  private static Set<String> selections(String csv,int maxItems,int maxLength) {
    if(csv==null || csv.length()>maxItems*(maxLength+1))throw new ApiException(400,"筛选参数不合法");
    if(csv.isBlank())return Set.of();
    var values=new LinkedHashSet<>(Arrays.asList(csv.split(",",-1)));
    if(values.size()>maxItems || values.stream().anyMatch(v->v.isBlank() || v.length()>maxLength))throw new ApiException(400,"筛选参数不合法");
    return values;
  }

  /** List cards do not need steps, media albums or per-recipe detail/translation queries. */
  private List<Recipe> browseCards(List<Long> ids, Long user) {
    if(ids.isEmpty()) return List.of();
    String placeholders=String.join(",",Collections.nCopies(ids.size(),"?"));
    var rows=db.query("SELECT r.id,r.name,r.cover_image,r.cooking_time,r.creator_id,r.visibility "
        + "FROM recipe r WHERE r.id IN ("+placeholders+")",
        org.springframework.jdbc.core.BeanPropertyRowMapper.newInstance(Recipe.class),ids.toArray());
    var tags=db.query("SELECT * FROM recipe_tag WHERE recipe_id IN ("+placeholders+") ORDER BY id",
        org.springframework.jdbc.core.BeanPropertyRowMapper.newInstance(RecipeTag.class),ids.toArray());
    Set<Long> favorites=new HashSet<>();
    if(user!=null) {
      var args=new ArrayList<Object>();args.add(user);args.addAll(ids);
      favorites.addAll(db.queryForList("SELECT recipe_id FROM user_favorite WHERE user_id=? AND recipe_id IN ("+placeholders+")",Long.class,args.toArray()));
    }
    Map<Long,Recipe> byId=new HashMap<>();
    for(Recipe recipe:rows) {
      recipe.setTags(tags.stream().filter(t->recipe.getId().equals(t.getRecipeId())).toList());
      recipe.setIsFavorite(favorites.contains(recipe.getId()));
      byId.put(recipe.getId(),recipe);
    }
    return ids.stream().map(byId::get).filter(Objects::nonNull).toList();
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
    r.setVisibility("public");
    r.setSharedGroupIds("[]");
    r.setKitchenId(kitchen);
    r.setOwnerGroupId(null);
    Recipe adapted = recipes.createRecipe(
        r, r.getTags(), r.getIngredients(), r.getSteps(), List.of(), r.getExternalRecipes());
    return adapted;
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
