package com.familyrecipes.service;

import com.familyrecipes.common.PageResult;
import com.familyrecipes.entity.*;
import com.familyrecipes.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜谱服务
 */
@Service
public class RecipeService {
    
    private static final Logger log = LoggerFactory.getLogger(RecipeService.class);

    @Autowired
    private RecipeMapper recipeMapper;

    @Autowired
    private RecipeTagMapper recipeTagMapper;

    @Autowired
    private RecipeIngredientMapper recipeIngredientMapper;

    @Autowired
    private CookingStepMapper cookingStepMapper;

    @Autowired
    private ExternalRecipeMapper externalRecipeMapper;

    @Autowired
    private RecipeCookMapper recipeCookMapper;

    @Autowired
    private UserFavoriteMapper userFavoriteMapper;
    
    @Autowired
    private UserDislikeMapper userDislikeMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RecipeAccessService access;
    @Autowired private MediaService media;
    @Autowired private KitchenService kitchens;
    @Autowired private RecipeImageMapper images;

    private void validateMedia(Recipe recipe, List<CookingStep> steps, List<ExternalRecipe> links) {
        if(links!=null && links.size()>1) throw new com.familyrecipes.common.ApiException(400,"每篇收藏仅保存一个来源，不同教程请分别收藏");
        if(recipe.getImages()!=null) {
            if(recipe.getImages().size()>30) throw new com.familyrecipes.common.ApiException(400,"最多30张图片");
            int position=0;
            for(RecipeImage image:recipe.getImages()) {
                if(image.getUrl()==null || image.getUrl().length()>500 || image.getCaption()!=null && image.getCaption().length()>1000)
                    throw new com.familyrecipes.common.ApiException(400,"图片信息不合法");
                media.validateCategory(image.getCategory());
                media.requireOwned(image.getUrl(),recipe.getCreatorId());
                image.setPosition(position++);
            }
        }
        media.requireOwned(recipe.getCoverImage(), recipe.getCreatorId());
        if (steps != null) for (CookingStep step : steps) media.requireOwned(step.getImage(), recipe.getCreatorId());
        if (links != null) for (ExternalRecipe link : links) media.requireOwned(link.getThumbnail(), recipe.getCreatorId());
    }

    private java.util.Set<String> mediaUrls(Long id) {
        java.util.Set<String> urls=new java.util.HashSet<>();
        Recipe recipe=recipeMapper.findById(id);
        if(recipe!=null) urls.add(recipe.getCoverImage());
        images.list(id).forEach(image->urls.add(image.getUrl()));
        cookingStepMapper.findByRecipeId(id).forEach(step->urls.add(step.getImage()));
        externalRecipeMapper.findByRecipeId(id).forEach(link->urls.add(link.getThumbnail()));
        urls.remove(null);return urls;
    }

    /**
     * 创建菜谱
     */
    @Transactional
    public Recipe createRecipe(Recipe recipe, List<RecipeTag> tags, 
                               List<RecipeIngredient> ingredients,
                               List<CookingStep> steps,
                               List<Long> cookUserIds,
                               List<ExternalRecipe> externalRecipes) {
        access.validateSharing(recipe);
        if(recipe.getCookingTime()!=null && (recipe.getCookingTime()<1 || recipe.getCookingTime()>10080)) throw new com.familyrecipes.common.ApiException(400,"耗时须为1至10080分钟");
        if(steps!=null && (steps.size()>100 || steps.stream().anyMatch(step->step.getDescription()==null || step.getDescription().isBlank() || step.getDescription().length()>10000))) throw new com.familyrecipes.common.ApiException(400,"请填写每一步的做法，最多100步");
        if(recipe.getId()==null && recipe.getSourceRecipeId()!=null){access.requireRead(recipe.getSourceRecipeId(),recipe.getCreatorId());recipe.setSourceRecipeName(recipeMapper.findById(recipe.getSourceRecipeId()).getName());}
        validateMedia(recipe, steps, externalRecipes);
        // 插入菜谱
        recipeMapper.insert(recipe);
        Long recipeId = recipe.getId();
        
        log.info("创建菜谱成功，ID: {}, 名称: {}", recipeId, recipe.getName());

        // 插入标签
        if (tags != null) {
            log.info("开始插入标签，数量: {}", tags.size());
            for (RecipeTag tag : tags) {
                tag.setRecipeId(recipeId);
                recipeTagMapper.insert(tag);
                log.info("插入标签: type={}, value={}", tag.getTagType(), tag.getTagValue());
            }
        }

        // 插入食材
        if (ingredients != null) {
            for (RecipeIngredient ingredient : ingredients) {
                ingredient.setRecipeId(recipeId);
                recipeIngredientMapper.insert(ingredient);
            }
        }

        // 插入步骤
        if (steps != null) {
            for (CookingStep step : steps) {
                step.setRecipeId(recipeId);
                cookingStepMapper.insert(step);
            }
        }

        // 插入会做的人
        if (cookUserIds != null) {
            for (Long userId : cookUserIds) {
                recipeCookMapper.insert(recipeId, userId, 3);
            }
        }
        
        // 插入外部链接
        if (externalRecipes != null) {
            for (ExternalRecipe externalRecipe : externalRecipes) {
                externalRecipe.setRecipeId(recipeId);
                externalRecipe.setAddedBy(recipe.getCreatorId());
                externalRecipeMapper.insert(externalRecipe);
            }
        }

        if(recipe.getImages()!=null) {
            images.delete(recipeId);
            for(RecipeImage image:recipe.getImages()) images.insert(recipeId,image);
        }
        return getRecipeDetail(recipeId, recipe.getCreatorId());
    }

    /**
     * 更新菜谱
     */
    @Transactional
    public Recipe updateRecipe(Recipe recipe, List<RecipeTag> tags,
                               List<RecipeIngredient> ingredients,
                               List<CookingStep> steps,
                               List<Long> cookUserIds,
                               List<ExternalRecipe> externalRecipes) {
        access.requireEdit(recipe.getId(), recipe.getCreatorId());
        var previousUrls=mediaUrls(recipe.getId());
        access.validateSharing(recipe);
        if(recipe.getCookingTime()!=null && (recipe.getCookingTime()<1 || recipe.getCookingTime()>10080)) throw new com.familyrecipes.common.ApiException(400,"耗时须为1至10080分钟");
        if(steps!=null && (steps.size()>100 || steps.stream().anyMatch(step->step.getDescription()==null || step.getDescription().isBlank() || step.getDescription().length()>10000))) throw new com.familyrecipes.common.ApiException(400,"请填写每一步的做法，最多100步");
        if(recipe.getId()==null && recipe.getSourceRecipeId()!=null){access.requireRead(recipe.getSourceRecipeId(),recipe.getCreatorId());recipe.setSourceRecipeName(recipeMapper.findById(recipe.getSourceRecipeId()).getName());}
        validateMedia(recipe, steps, externalRecipes);
        recipeMapper.update(recipe);
        Long recipeId = recipe.getId();

        // 删除并重新插入标签
        if (tags != null) {
            recipeTagMapper.deleteByRecipeId(recipeId);
            for (RecipeTag tag : tags) {
                tag.setRecipeId(recipeId);
                recipeTagMapper.insert(tag);
            }
        }

        // 删除并重新插入食材
        if (ingredients != null) {
            recipeIngredientMapper.deleteByRecipeId(recipeId);
            for (RecipeIngredient ingredient : ingredients) {
                ingredient.setRecipeId(recipeId);
                recipeIngredientMapper.insert(ingredient);
            }
        }

        // 删除并重新插入步骤
        if (steps != null) {
            cookingStepMapper.deleteByRecipeId(recipeId);
            for (CookingStep step : steps) {
                step.setRecipeId(recipeId);
                cookingStepMapper.insert(step);
            }
        }
        
        if (cookUserIds != null) {
            recipeCookMapper.deleteByRecipeId(recipeId);
            for (Long id : new java.util.LinkedHashSet<>(cookUserIds)) recipeCookMapper.insert(recipeId, id, 3);
        }
        // 删除并重新插入外部链接
        if (externalRecipes != null) {
            externalRecipeMapper.deleteByRecipeId(recipeId);
            for (ExternalRecipe externalRecipe : externalRecipes) {
                externalRecipe.setRecipeId(recipeId);
                externalRecipe.setAddedBy(recipe.getCreatorId());
                externalRecipeMapper.insert(externalRecipe);
            }
        }

        if(recipe.getImages()!=null) {
            images.delete(recipeId);
            for(RecipeImage image:recipe.getImages()) images.insert(recipeId,image);
        }
        previousUrls.forEach(media::retireIfUnlinked);
        return getRecipeDetail(recipeId, recipe.getCreatorId());
    }

    /**
     * 删除菜谱
     */
    @Transactional
    public void deleteRecipe(Long recipeId, Long userId) {
        access.requireEdit(recipeId, userId);
        var urls=mediaUrls(recipeId);
        recipeMapper.delete(recipeId);
        urls.forEach(media::retireIfUnlinked);
    }

    /**
     * 获取菜谱详情
     */
    public Recipe getRecipeDetail(Long recipeId, Long currentUserId) {
        access.requireRead(recipeId, currentUserId);
        Recipe recipe = recipeMapper.findById(recipeId);
        if (recipe == null) {
            return null;
        }

        // 加载关联数据
        recipe.setCreator(userMapper.findById(recipe.getCreatorId()));
        recipe.setImages(images.list(recipeId));
        recipe.setTags(recipeTagMapper.findByRecipeId(recipeId));
        recipe.setIngredients(recipeIngredientMapper.findByRecipeId(recipeId));
        recipe.setSteps(cookingStepMapper.findByRecipeId(recipeId));
        recipe.setExternalRecipes(externalRecipeMapper.findByRecipeId(recipeId));
        recipe.setCooks(recipeCookMapper.findCooksByRecipeId(recipeId));

        // 设置当前用户是否收藏
        if (currentUserId != null) {
            boolean isFavorite = userFavoriteMapper.existsByUserAndRecipe(currentUserId, recipeId) > 0;
            recipe.setIsFavorite(isFavorite);
            recipe.setIsDisliked(userDislikeMapper.existsByUserAndRecipe(currentUserId, recipeId) > 0);
        }



        return recipe;
    }

    public Recipe viewRecipe(Long id, Long userId) {
        Recipe recipe = getRecipeDetail(id, userId);
        recipeMapper.incrementViewCount(id);
        recipe.setViewCount((recipe.getViewCount() == null ? 0 : recipe.getViewCount()) + 1);
        return recipe;
    }

    /**
     * 搜索菜谱
     */
    public PageResult<Recipe> searchRecipes(String keyword, String tagType, String tagValue,
                                           List<Long> ingredientIds, Long creatorId,
                                           Long favoritedByUserId, Long cookedByUserId,
                                           String creatorUsername, String orderBy,
                                           Integer pageNum, Integer pageSize, Long viewerId) {
        return searchRecipes(keyword,tagType,tagValue,ingredientIds,creatorId,favoritedByUserId,cookedByUserId,creatorUsername,orderBy,pageNum,pageSize,viewerId,null);
    }
    public PageResult<Recipe> searchRecipes(String keyword,String tagType,String tagValue,List<Long> ingredientIds,Long creatorId,
        Long favoritedByUserId,Long cookedByUserId,String creatorUsername,String orderBy,Integer pageNum,Integer pageSize,Long viewerId,Long kitchenId) {
        if(kitchenId!=null) kitchens.requireMember(kitchenId,viewerId);

        if (pageNum < 1 || pageSize < 1 || pageSize > 100 || pageNum > 1000000)
            throw new com.familyrecipes.common.ApiException(400, "分页参数不合法");
        Map<String, Object> params = new HashMap<>();
        params.put("viewerId", viewerId);
        params.put("kitchenId", kitchenId);
        params.put("keyword", keyword);
        params.put("tagType", tagType);
        params.put("tagValue", tagValue);
        params.put("ingredientIds", ingredientIds);
        params.put("creatorId", creatorId);
        params.put("favoritedByUserId", favoritedByUserId);
        params.put("cookedByUserId", cookedByUserId);
        params.put("creatorUsername", creatorUsername);
        params.put("orderBy", orderBy);
        params.put("offset", (pageNum - 1) * pageSize);
        params.put("limit", pageSize);

        List<Recipe> recipes = recipeMapper.search(params);
        Long total = recipeMapper.countSearch(params);

        // 加载创建者信息
        for (Recipe recipe : recipes) {
            recipe.setCreator(userMapper.findById(recipe.getCreatorId()));
            recipe.setTags(recipeTagMapper.findByRecipeId(recipe.getId()));
        }

        return new PageResult<>(recipes, total, pageNum, pageSize);
    }

    /**
     * 收藏/取消收藏菜谱
     */
    @Transactional
    public boolean toggleFavorite(Long userId, Long recipeId) {
        recipeMapper.lockById(recipeId);
        access.requireRead(recipeId, userId);
        int exists = userFavoriteMapper.existsByUserAndRecipe(userId, recipeId);
        if (exists > 0) {
            // 取消收藏
            userFavoriteMapper.delete(userId, recipeId);
            recipeMapper.decrementFavoriteCount(recipeId);
            return false;
        } else {
            // 添加收藏
            userFavoriteMapper.insert(userId, recipeId);
            recipeMapper.incrementFavoriteCount(recipeId);
            return true;
        }
    }
    
    /**
     * 差评/取消差评菜谱
     */
    @Transactional
    public boolean toggleDislike(Long userId, Long recipeId) {
        recipeMapper.lockById(recipeId);
        access.requireRead(recipeId, userId);
        int exists = userDislikeMapper.existsByUserAndRecipe(userId, recipeId);
        if (exists > 0) {
            // 取消差评
            userDislikeMapper.delete(userId, recipeId);
            recipeMapper.decrementDislikeCount(recipeId);
            return false;
        } else {
            // 添加差评
            userDislikeMapper.insert(userId, recipeId);
            recipeMapper.incrementDislikeCount(recipeId);
            return true;
        }
    }

    /**
     * 添加外链食谱
     */
    @Transactional
    public void addExternalRecipe(ExternalRecipe externalRecipe) {
        access.requireEdit(externalRecipe.getRecipeId(), externalRecipe.getAddedBy());
        media.requireOwned(externalRecipe.getThumbnail(), externalRecipe.getAddedBy());
        if(!externalRecipeMapper.findByRecipeId(externalRecipe.getRecipeId()).isEmpty())
            throw new com.familyrecipes.common.ApiException(409,"此收藏已有来源，请新建另一篇收藏");
        externalRecipeMapper.insert(externalRecipe);
    }

    /**
     * 记录烹饪（增加烹饪次数）
     */
    @Transactional
    public void recordCooking(Long recipeId, Long userId) {
        access.requireRead(recipeId, userId);
        recipeMapper.incrementCookedCount(recipeId);
    }

    /**
     * 获取最近常做的菜
     */
    public List<Recipe> getRecentlyCooked(int limit, Long viewerId) {
        if (limit < 1 || limit > 100) throw new com.familyrecipes.common.ApiException(400, "数量不合法");
        return recipeMapper.findRecentlyCooked(limit, viewerId);
    }

    /**
     * 获取所有标签值
     */
    public List<String> getTagValues(String tagType, Long viewerId) {
        return recipeMapper.visibleTags(tagType, viewerId);
    }
}

