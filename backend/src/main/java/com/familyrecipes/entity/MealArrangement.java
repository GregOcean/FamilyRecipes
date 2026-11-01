package com.familyrecipes.entity;

import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Data
public class MealArrangement {
  private Long id, ownerId, kitchenId, sourceId;
  private String kind, title, meal;
  private LocalDate mealDate;
  private Integer revision;
  private List<Dish> dishes;
  private List<Shopping> shopping;

  @Data
  public static class Dish {
    private Long recipeId;
    private String name;
  }

  @Data
  public static class Shopping {
    private String name;
    private boolean done;
  }
}
