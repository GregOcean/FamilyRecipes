package com.familyrecipes.translation;

import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.*;

/** Bounded cyclic scan catches existing content and all non-HTTP writes without a message broker. */
@Service
public class TranslationBackfill {
    private final JdbcTemplate db; private final TranslationService translations; private final ObjectMapper json;
    @Value("${app.translation.enabled:false}") private boolean enabled;
    private final Map<String,Long> cursors=new HashMap<>();
    private static final Map<String,String> TABLES=Map.ofEntries(
        Map.entry("recipe","name,description"),Map.entry("ingredient","name,processing_method"),
        Map.entry("cooking_step","description"),Map.entry("external_recipe","title,notes"),
        Map.entry("recipe_ingredient","amount"),Map.entry("fridge_item","amount,notes"),
        Map.entry("inventory_categories","name"),Map.entry("recipe_tag","tag_value"),
        Map.entry("cooking_history","notes"),Map.entry("recipe_image","caption"),Map.entry("meal_arrangement","title,dishes,shopping"));
    public TranslationBackfill(JdbcTemplate db,TranslationService translations,ObjectMapper json) {this.db=db;this.translations=translations;this.json=json;}
    @Scheduled(fixedDelay=30000,initialDelay=15000)
    public void scan() {
        if(!enabled) return;
        for(var table:TABLES.entrySet()) {
            long cursor=cursors.getOrDefault(table.getKey(),0L);
            var rows=db.queryForList("SELECT id,"+table.getValue()+" FROM "+table.getKey()+" WHERE id>? ORDER BY id LIMIT 20",cursor);
            for(var row:rows) {
                for(String field:table.getValue().split(",")) {
                    Object value=row.get(field); if(value==null)continue;
                    if(field.equals("dishes") || field.equals("shopping")) {
                        try { for(JsonNode entry:json.readTree(value.toString())) enqueue(entry.path("name").asText()); }
                        catch(Exception ignored) { /* Existing malformed legacy rows do not block other content. */ }
                    } else enqueue(value.toString());
                }
            }
            cursors.put(table.getKey(),rows.size()<20?0L:((Number)rows.get(rows.size()-1).get("id")).longValue());
        }
    }
    private void enqueue(String text) {translations.lookup(text,TranslationService.sourceLanguage(text).equals("zh")?"en":"zh","content",true);}
}
