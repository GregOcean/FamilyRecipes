package com.familyrecipes.service;

import com.familyrecipes.entity.RecipeTag;
import java.util.*;

/** Explicit user/source labels win; absent evidence is not treated as a neutral flavour. */
public final class RecipeClassification {
    public static List<RecipeTag> complete(String name,List<RecipeTag> input) {
        Map<String,RecipeTag> unique=new LinkedHashMap<>();
        for(RecipeTag original:input==null?List.<RecipeTag>of():input) {
            if(original==null || original.getTagType()==null || original.getTagValue()==null) continue;
            String type=original.getTagType().trim();
            String[] values=Set.of("cuisine","taste").contains(type)
                ? original.getTagValue().split("[、，,;；\\n]+") : new String[]{original.getTagValue()};
            for(String value:values) {
                value=value.trim().replaceFirst("^#", "").trim();
                if(value.isEmpty()) continue;
                RecipeTag tag=new RecipeTag();tag.setTagType(type);tag.setTagValue(value);
                unique.putIfAbsent(type+":"+value.toLowerCase(Locale.ROOT),tag);
            }
        }
        List<RecipeTag> tags=new ArrayList<>(unique.values());
        for(String type:List.of("taste","cuisine")) {
            if(tags.stream().anyMatch(t->type.equals(t.getTagType()) && t.getTagValue()!=null && !t.getTagValue().isBlank() && !"待确认".equals(t.getTagValue().trim())))
                tags.removeIf(t->type.equals(t.getTagType()) && (t.getTagValue()==null || t.getTagValue().isBlank() || "待确认".equals(t.getTagValue().trim())));
            if(tags.stream().noneMatch(t->type.equals(t.getTagType()) && t.getTagValue()!=null && !t.getTagValue().isBlank())) {
                RecipeTag tag=new RecipeTag();tag.setTagType(type);tag.setTagValue("待确认");tags.add(tag);
            }
        }
        return tags;
    }
    private RecipeClassification() {}
}
