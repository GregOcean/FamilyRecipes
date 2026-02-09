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

    @Insert("INSERT INTO ingredient(name, category) VALUES(#{name}, #{category})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Ingredient ingredient);

    @Select("SELECT * FROM ingredient WHERE name LIKE CONCAT('%', #{keyword}, '%') LIMIT 20")
    List<Ingredient> searchByName(String keyword);

    @Select("SELECT * FROM ingredient ORDER BY name")
    List<Ingredient> findAll();
}

