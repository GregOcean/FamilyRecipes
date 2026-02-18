package com.familyrecipes.mapper;

import com.familyrecipes.entity.Ingredient;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 食材Mapper
 */
@Mapper
public interface IngredientMapper {

    @Select("SELECT * FROM ingredient WHERE id = #{id}")
    Ingredient findById(Long id);

    @Select("SELECT * FROM ingredient WHERE name = #{name}")
    Ingredient findByName(String name);

    @Insert("INSERT INTO ingredient(name, category, unit) VALUES(#{name}, #{category}, #{unit})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Ingredient ingredient);

    @Select("SELECT * FROM ingredient WHERE name LIKE CONCAT('%', #{keyword}, '%') LIMIT 20")
    List<Ingredient> searchByName(String keyword);
    
    // 搜索食材（精确匹配优先，然后模糊匹配）
    @Select("SELECT * FROM ingredient WHERE name LIKE CONCAT('%', #{keyword}, '%') " +
            "ORDER BY CASE " +
            "  WHEN name = #{keyword} THEN 0 " +  // 完全匹配优先
            "  WHEN name LIKE CONCAT(#{keyword}, '%') THEN 1 " +  // 开头匹配其次
            "  ELSE 2 " +  // 包含匹配最后
            "END, name")
    List<Ingredient> searchIngredients(String keyword);

    @Select("SELECT * FROM ingredient ORDER BY name")
    List<Ingredient> findAll();
}

