package com.familyrecipes.service;

import com.familyrecipes.entity.RecipeTag;
import java.util.*;

/** Explicit user/source labels win; absent evidence is not treated as a neutral flavour. */
public final class RecipeClassification {
    public static List<RecipeTag> complete(String name,List<RecipeTag> input) {
        List<RecipeTag> tags=new ArrayList<>(input==null?List.of():input);
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
