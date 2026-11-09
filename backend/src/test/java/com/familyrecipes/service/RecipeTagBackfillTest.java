package com.familyrecipes.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.familyrecipes.entity.RecipeTag;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class RecipeTagBackfillTest {
    final ObjectMapper json=new ObjectMapper();
    @Test void acceptsOnlyBoundedKnownLabels() throws Exception {
        assertEquals("甜",RecipeTagBackfill.parse("```json\n{\"taste\":[\"甜\"],\"cuisine\":[\"日式\"]}\n```",json).path("taste").get(0).asText());
        for(String text:List.of("{}","{\"taste\":[\"待确认\"],\"cuisine\":[]}","{\"taste\":[\"甜\",\"甜\"],\"cuisine\":[]}","{\"taste\":[],\"cuisine\":[\"日本\"]}"))
            assertThrows(Exception.class,()->RecipeTagBackfill.parse(text,json));
        assertTrue(RecipeTagBackfill.parse("{\"taste\":[],\"cuisine\":[]}",json).path("taste").isEmpty());
    }
    @Test void explicitLabelRemovesPlaceholderButKeepsOtherTypes() {
        RecipeTag pending=new RecipeTag();pending.setTagType("taste");pending.setTagValue("待确认");
        RecipeTag sweet=new RecipeTag();sweet.setTagType("taste");sweet.setTagValue("甜");
        var result=RecipeClassification.complete("",List.of(pending,sweet));
        assertEquals(2,result.size());assertEquals("甜",result.get(0).getTagValue());
        assertEquals("cuisine",result.get(1).getTagType());
    }
}
