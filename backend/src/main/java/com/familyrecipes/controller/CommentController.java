package com.familyrecipes.controller;

import com.familyrecipes.common.ApiException;
import com.familyrecipes.common.Result;
import com.familyrecipes.entity.RecipeComment;
import com.familyrecipes.entity.User;
import com.familyrecipes.mapper.RecipeCommentMapper;
import com.familyrecipes.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜谱评论控制器
 */
@RestController
@RequestMapping("/api/recipes/{recipeId}/comments")
public class CommentController {

    @Autowired
    private RecipeCommentMapper commentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private com.familyrecipes.mapper.RecipeMapper recipeMapper;

    @Autowired
    private com.familyrecipes.service.RecipeAccessService accessService;

    /**
     * 获取菜谱评论列表
     */
    @GetMapping
    public Result<List<RecipeComment>> listComments(@PathVariable Long recipeId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        // 公开菜谱允许未登录查看评论；需要验证菜谱可见性
        if (userId != null) {
            accessService.requireRead(recipeId, userId);
        } else {
            // 未登录用户只能查看公开菜谱的评论
            com.familyrecipes.entity.Recipe recipe = recipeMapper.findById(recipeId);
            if (recipe == null || !"public".equals(recipe.getVisibility())) {
                throw new ApiException(403, "无权查看此菜谱");
            }
        }
        List<RecipeComment> comments = commentMapper.findByRecipeId(recipeId);
        return Result.success(comments);
    }

    /**
     * 发表评论
     */
    @PostMapping
    public Result<RecipeComment> addComment(@PathVariable Long recipeId,
            @jakarta.validation.Valid @RequestBody CommentRequest body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new ApiException(401, "请先登录");
        }
        accessService.requireRead(recipeId, userId);
        String content = body.getContent().trim();
        if (content.isEmpty()) {
            throw new ApiException(400, "评论内容不能为空");
        }
        RecipeComment comment = new RecipeComment();
        comment.setRecipeId(recipeId);
        comment.setUserId(userId);
        comment.setContent(content);
        commentMapper.insert(comment);

        // Populate user info for response
        User user = userMapper.findById(userId);
        comment.setUser(user);
        if (user != null) user.setPassword(null);
        return Result.success(comment);
    }

    /**
     * 删除评论（仅评论者本人）
     */
    @DeleteMapping("/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long recipeId, @PathVariable Long commentId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new ApiException(401, "请先登录");
        }
        int deleted = commentMapper.deleteByIdAndUser(commentId, userId);
        if (deleted == 0) {
            throw new ApiException(404, "评论不存在或无权删除");
        }
        return Result.success();
    }

    @Data
    static class CommentRequest {
        @NotBlank(message = "评论内容不能为空")
        @Size(max = 500, message = "评论最多500个字符")
        private String content;
    }
}
