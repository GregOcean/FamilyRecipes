package com.familyrecipes.service;

import com.familyrecipes.entity.RecipeTag;
import com.familyrecipes.common.ApiException;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RecipeEditorValidationTest {
    private RecipeTag tag(String type,String value) {var tag=new RecipeTag();tag.setTagType(type);tag.setTagValue(value);return tag;}
    @Test void editedCuisineLabelsRemainIndividuallyFilterable() {
        var result=RecipeClassification.complete("菜",List.of(tag("cuisine","家常菜、东北菜"),tag("cuisine","家常菜"),tag("taste","酸, 辣")));
        assertEquals(List.of("家常菜","东北菜"),result.stream().filter(t->t.getTagType().equals("cuisine")).map(RecipeTag::getTagValue).toList());
        assertEquals(List.of("酸","辣"),result.stream().filter(t->t.getTagType().equals("taste")).map(RecipeTag::getTagValue).toList());
    }
    @Test void localDeviceUrisCannotBecomePublishedImages() {
        var media=new MediaService();
        for(String url:List.of("file:///data/user/0/photos/a.jpg","content://photos/1","/relative/photo.png","javascript:alert(1)","https://user:password@example.com/image.jpg"))
            assertThrows(ApiException.class,()->media.requireOwned(url,1L));
        assertDoesNotThrow(()->media.requireOwned("https://upload.wikimedia.org/example.jpg",1L));
        assertDoesNotThrow(()->media.requireOwned(null,1L));
    }
}
