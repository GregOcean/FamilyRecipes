package com.familyrecipes.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 菜谱评论
 */
@Data
public class RecipeComment {
    private Long id;
    private Long recipeId;
    @JsonIgnore
    private Long userId;
    private String content;
    private LocalDateTime createdAt;

    // Transient: populated for responses
    private User user;
}
