package com.familyrecipes.controller;

import com.familyrecipes.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/notifications/favorites")
public class FavoriteNotificationController {
    private final JdbcTemplate db;
    public FavoriteNotificationController(JdbcTemplate db){this.db=db;}
    @GetMapping
    public Result<Map<String,Object>> list(HttpServletRequest request,
        @RequestParam(defaultValue="9223372036854775807") long beforeId) {
        Long user=(Long)request.getAttribute("userId");
        var rows=db.queryForList("""
            SELECT n.id,n.recipe_id AS recipeId,u.username AS actorName,r.name AS recipeName,
                   n.created_at AS createdAt,n.read_at AS readAt
            FROM recipe_favorite_notification n JOIN `user` u ON u.id=n.actor_id
            JOIN recipe r ON r.id=n.recipe_id
            WHERE n.recipient_id=? AND n.id<? ORDER BY n.id DESC LIMIT 51
            """,user,beforeId);
        rows.forEach(row -> row.put("isRead",row.remove("readAt")!=null));
        return Result.success(Map.of("items",rows,"unread",db.queryForObject(
            "SELECT COUNT(*) FROM recipe_favorite_notification WHERE recipient_id=? AND read_at IS NULL",Long.class,user)));
    }
    @PostMapping("/read")
    public Result<Void> read(HttpServletRequest request,@RequestBody Map<String,Long> body) {
        // Only acknowledge the snapshot actually shown; newer arrivals remain unread.
        long through=body.getOrDefault("throughId",0L);
        db.update("UPDATE recipe_favorite_notification SET read_at=NOW() WHERE recipient_id=? AND id<=? AND id>=? AND read_at IS NULL",
            request.getAttribute("userId"),through,body.getOrDefault("fromId",0L));
        return Result.success(null);
    }
}
