package com.familyrecipes.service;

import com.familyrecipes.common.ApiException;
import com.familyrecipes.entity.MealArrangement;
import com.familyrecipes.mapper.GroupChatMapper;
import com.familyrecipes.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MealService {
  @Autowired JdbcTemplate db;
  @Autowired ObjectMapper json;
  @Autowired KitchenService kitchens;
  @Autowired RecipeAccessService recipes;
  @Autowired GroupChatMapper groups;
  @Autowired MessageService messages;

  private String encode(Object x) {
    try {
      return json.writeValueAsString(x);
    } catch (Exception e) {
      throw new ApiException(400, "内容格式不正确");
    }
  }

  private MealArrangement row(java.sql.ResultSet r, int n) throws java.sql.SQLException {
    var m = new MealArrangement();
    m.setId(r.getLong("id"));
    m.setOwnerId(r.getLong("owner_id"));
    m.setKitchenId(r.getLong("kitchen_id"));
    m.setKind(r.getString("kind"));
    m.setTitle(r.getString("title"));
    m.setMeal(r.getString("meal"));
    m.setRevision(r.getInt("revision"));
    m.setSourceId((Long) r.getObject("source_id"));
    var date = r.getDate("meal_date");
    m.setMealDate(date == null ? null : date.toLocalDate());
    try {
      m.setDishes(
          Arrays.asList(json.readValue(r.getString("dishes"), MealArrangement.Dish[].class)));
      m.setShopping(
          Arrays.asList(json.readValue(r.getString("shopping"), MealArrangement.Shopping[].class)));
    } catch (Exception e) {
      throw new java.sql.SQLException(e);
    }
    return m;
  }

  public List<MealArrangement> list(Long user, Long kitchen) {
    kitchens.requireMember(kitchen, user);
    return db.query(
        "SELECT m.* FROM meal_arrangement m WHERE m.kitchen_id=? AND (m.owner_id=? OR EXISTS(SELECT"
            + " 1 FROM meal_share s JOIN group_member g ON g.group_id=s.group_id WHERE"
            + " s.meal_id=m.id AND g.user_id=?)) ORDER BY m.updated_at DESC,m.id DESC",
        this::row,
        kitchen,
        user,
        user);
  }

  public MealArrangement get(Long id, Long user) {
    var rows =
        db.query(
            "SELECT m.* FROM meal_arrangement m WHERE m.id=? AND (m.owner_id=? OR EXISTS(SELECT 1"
                + " FROM meal_share s JOIN group_member g ON g.group_id=s.group_id WHERE"
                + " s.meal_id=m.id AND g.user_id=?))",
            this::row,
            id,
            user,
            user);
    if (rows.isEmpty()) throw new ApiException(404, "菜单不存在或不可访问");
    return rows.get(0);
  }

  @Transactional
  public MealArrangement save(MealArrangement m, Long user) {
    kitchens.requireMember(m.getKitchenId(), user);
    if (!Set.of("order", "prep").contains(Objects.toString(m.getKind(), ""))
        || m.getTitle() == null
        || m.getTitle().isBlank()
        || m.getTitle().length() > 100) throw new ApiException(400, "请填写菜单名称和类型");
    if (m.getMeal() != null && !Set.of("", "早餐", "午餐", "晚餐", "加餐").contains(m.getMeal()))
      throw new ApiException(400, "餐次不合法");
    if (m.getDishes() == null
        || m.getDishes().size() > 100
        || m.getShopping() == null
        || m.getShopping().size() > 200) throw new ApiException(400, "菜单项目过多");
    if (m.getSourceId() != null) get(m.getSourceId(), user);
    for (var d : m.getDishes()) {
      if (d.getName() == null || d.getName().isBlank() || d.getName().length() > 100)
        throw new ApiException(400, "菜名须为1至100字");
      if (d.getRecipeId() != null) recipes.requireRead(d.getRecipeId(), user);
    }
    for (var d : m.getShopping())
      if (d.getName() == null || d.getName().isBlank() || d.getName().length() > 200)
        throw new ApiException(400, "采购项目须为1至200字");
    if (m.getId() == null) {
      var key = new org.springframework.jdbc.support.GeneratedKeyHolder();
      db.update(
          c -> {
            var p =
                c.prepareStatement(
                    "INSERT INTO"
                        + " meal_arrangement(owner_id,kitchen_id,kind,title,meal_date,meal,dishes,shopping,source_id)"
                        + " VALUES(?,?,?,?,?,?,?,?,?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS);
            p.setObject(1, user);
            p.setObject(2, m.getKitchenId());
            p.setString(3, m.getKind());
            p.setString(4, m.getTitle());
            p.setObject(5, m.getMealDate());
            p.setString(6, m.getMeal());
            p.setString(7, encode(m.getDishes()));
            p.setString(8, encode(m.getShopping()));
            p.setObject(9, m.getSourceId());
            return p;
          },
          key);
      m.setId(key.getKey().longValue());
    } else {
      var old = get(m.getId(), user);
      if (!user.equals(old.getOwnerId())) throw new ApiException(403, "仅创建者可以编辑，请创建自己的备菜");
      if (!Objects.equals(old.getKitchenId(), m.getKitchenId())
          || !old.getKind().equals(m.getKind())) throw new ApiException(400, "不能更换已保存菜单的厨房或类型");
      if (db.update(
              "UPDATE meal_arrangement SET"
                  + " title=?,meal_date=?,meal=?,dishes=?,shopping=?,revision=revision+1 WHERE id=?"
                  + " AND owner_id=? AND revision=?",
              m.getTitle(),
              m.getMealDate(),
              m.getMeal(),
              encode(m.getDishes()),
              encode(m.getShopping()),
              m.getId(),
              user,
              m.getRevision())
          != 1) throw new ApiException(409, "菜单已更新，请重新打开后编辑");
    }
    return get(m.getId(), user);
  }

  @Transactional
  public Message share(Long id, Long group, Long user) {
    return share(id, group, user, null);
  }

  @Transactional
  public Message share(Long id, Long group, Long user, String clientId) {
    var m = get(id, user);
    if (!user.equals(m.getOwnerId())) throw new ApiException(403, "只有创建者可以分享菜单");
    if (group == null || groups.lockById(group) == null || !groups.isGroupMember(group, user)) throw new ApiException(404, "群组不存在");
    var previous = messages.findSharedMessage(user, clientId, group, "familyrecipes://meal/" + id);
    if (previous != null) return previous;
    for (var dish : m.getDishes()) {
      if (dish.getRecipeId() != null) recipes.requireRead(dish.getRecipeId(), user);
    }
    db.update("INSERT IGNORE INTO meal_share(meal_id,group_id) VALUES(?,?)", id, group);
    return messages.sendMessage(
        group,
        user,
        (m.getKind().equals("order") ? "点菜" : "备菜")
            + "："
            + m.getTitle()
            + "\n"
            + (m.getMealDate() == null ? "" : m.getMealDate() + " ")
            + Objects.toString(m.getMeal(), "")
            + "\n"
            + m.getDishes().stream().limit(10)
                .map(MealArrangement.Dish::getName)
                .reduce((a, b) -> a + "、" + b)
                .orElse("")
            + "\nfamilyrecipes://meal/"
            + id,
        Message.MessageType.text, clientId);
  }
}
