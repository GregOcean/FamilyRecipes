package com.familyrecipes.mapper;

/** Shared by browse and the legacy recommendation endpoint. Tag affinity is capped. */
public final class RecommendationScore {
    public static final String SQL =
        "(6*EXISTS(SELECT 1 FROM recipe_cook c WHERE c.user_id=#{viewerId} AND c.recipe_id=r.id) "
        + "+5*EXISTS(SELECT 1 FROM user_favorite f WHERE f.user_id=#{viewerId} AND f.recipe_id=r.id) "
        + "+2*EXISTS(SELECT 1 FROM recipe_view_history v WHERE v.user_id=#{viewerId} AND v.recipe_id=r.id AND v.viewed_at>DATE_SUB(NOW(),INTERVAL 30 DAY)) "
        + "+LEAST(4,(SELECT COUNT(*) FROM recipe_tag t JOIN (SELECT DISTINCT liked.tag_type,liked.tag_value FROM recipe_tag liked JOIN (SELECT recipe_id FROM user_favorite WHERE user_id=#{viewerId} UNION SELECT recipe_id FROM recipe_cook WHERE user_id=#{viewerId}) preferred ON preferred.recipe_id=liked.recipe_id WHERE liked.tag_type IN ('taste','cuisine') AND liked.tag_value!='待确认') affinity ON affinity.tag_type=t.tag_type AND affinity.tag_value=t.tag_value WHERE t.recipe_id=r.id)) "
        + "+LEAST(2,(SELECT COUNT(*) FROM recipe_tag t JOIN (SELECT DISTINCT seen.tag_type,seen.tag_value FROM recipe_view_history v JOIN recipe_tag seen ON seen.recipe_id=v.recipe_id WHERE v.user_id=#{viewerId} AND v.viewed_at>DATE_SUB(NOW(),INTERVAL 30 DAY) AND seen.tag_type IN ('taste','cuisine') AND seen.tag_value!='待确认') affinity ON affinity.tag_type=t.tag_type AND affinity.tag_value=t.tag_value WHERE t.recipe_id=r.id)) "
        + "+LEAST(2,(SELECT COUNT(*) FROM cooking_history h WHERE h.user_id=#{viewerId} AND h.recipe_id=r.id)) "
        + "-2*EXISTS(SELECT 1 FROM user_dislike d WHERE d.user_id=#{viewerId} AND d.recipe_id=r.id) "
        + "+MOD(CRC32(CONCAT(r.id,':',#{seed})),100)/50.0)";
    private RecommendationScore() {}
}
