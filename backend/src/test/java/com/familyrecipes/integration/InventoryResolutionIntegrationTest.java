package com.familyrecipes.integration;
import com.familyrecipes.entity.*;
import com.familyrecipes.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named="FOUNDATION_DB_TEST", matches="true")
class InventoryResolutionIntegrationTest {
    @Autowired JdbcTemplate db;@Autowired IngredientResolutionService resolver;@Autowired FridgeService fridge;@Autowired KitchenService kitchens;
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired com.familyrecipes.util.JwtUtil jwt;
    @Autowired com.familyrecipes.ai.AiRoutes routes;
    @org.springframework.beans.factory.annotation.Value("${app.translation.version:v1}") String translationVersion;
    long user,kitchen;
    @BeforeEach void setup(){db.update("INSERT INTO user(email,password,username) VALUES(?,?,?)",UUID.randomUUID()+"@test.example","irrelevant","inventory-test");user=db.queryForObject("SELECT LAST_INSERT_ID()",Long.class);kitchen=kitchens.list(user).get(0).getId();}
    private IngredientResolutionService.Resolution resolve(String text,Long id,boolean create){return resolver.resolve(user,new IngredientResolutionService.Request(List.of(new IngredientResolutionService.Entry(text,id,null)),create,"冷藏")).get(0);}
    @Test void englishBrandsAndAliasesShareCanonicalIdWithoutLosingProductName(){
        var canonical=resolve("牛奶",null,false);var brand=resolve("FairPrice Full Cream Milk 2 cartons",null,false);
        assertTrue(brand.matched());assertEquals(canonical.ingredient().getId(),brand.ingredient().getId());
        assertEquals("FairPrice Full Cream Milk",brand.name());assertEquals("2 cartons",brand.amount());
        assertEquals(resolve("番茄",null,false).ingredient().getId(),resolve("西红柿",null,false).ingredient().getId());
    }
    @Test void ambiguousFoodsRemainUnclassifiedUntilAnExplicitPersonalChoice(){
        var draft=resolve("测试品牌巧克力风味饮料",null,true);assertFalse(draft.matched());assertFalse(draft.ingredient().getCatalog());
        assertEquals("测试品牌巧克力风味饮料",draft.ingredient().getName());
        long milk=resolve("牛奶",null,false).ingredient().getId();
        resolve("只预览的包装缩写",milk,false);
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM ingredient_user_alias WHERE user_id=? AND alias_text='只预览的包装缩写'",Integer.class,user));
        resolve("测试包装缩写",milk,true);assertEquals(milk,resolve("测试包装缩写",null,false).ingredient().getId());
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM ingredient WHERE name='测试包装缩写'",Integer.class));
    }
    @Test void inventoryRetryIsIdempotentAndUnknownExpiryPersists(){
        var item=new FridgeItem();item.setUserId(user);item.setKitchenId(kitchen);item.setIngredientId(resolve("鸡胸肉",null,false).ingredient().getId());
        item.setDisplayName("品牌鸡胸肉");item.setClientMutationId(UUID.randomUUID().toString());item.setAmount("300g");item.setStorageLocation("冷藏");
        var first=fridge.addItem(item);var second=fridge.addItem(item);assertEquals(first.getId(),second.getId());assertNull(first.getExpiryDate());
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM fridge_item WHERE kitchen_id=?",Integer.class,kitchen));
        item.setAmount("400g");assertThrows(com.familyrecipes.common.ApiException.class,()->fridge.addItem(item));
    }
    @Test void englishResolutionLocalizesLabelWithoutChangingRawNameOrIdentity() throws Exception {
        String hash=com.familyrecipes.translation.TranslationService.digest("牛奶");
        String engine="content:llm:"+routes.route(com.familyrecipes.ai.AiRoutes.TRANSLATION).getModel()+":"+translationVersion;
        db.update("INSERT INTO content_translations(source_hash,target_language,engine,source_text,kind,status,translated_text) VALUES(?,'en',?,'牛奶','content','ready','Milk') ON DUPLICATE KEY UPDATE status='ready',translated_text='Milk'",hash,engine);
        long milk=resolve("牛奶",null,false).ingredient().getId();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/fridge/ingredients/resolve")
            .header("Authorization","Bearer "+jwt.generateToken(user,"inventory-test@test.example")).header("Accept-Language","en")
            .contentType("application/json").content("{\"entries\":[{\"text\":\"Meiji milk 2 bottles\"}],\"create_unmatched\":false}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data[0].name").value("Meiji milk"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data[0].ingredient.id").value(milk))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data[0].ingredient.name").value("Milk"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data[0].ingredient.original_content.name").value("牛奶"));
        assertEquals("牛奶",db.queryForObject("SELECT name FROM ingredient WHERE id=?",String.class,milk));
    }
    @Test void parsedRecipeNameResolvesWithoutQuantityInItsIdentity(){
        var row=resolve("鸡胸肉300克",null,true);
        assertEquals("鸡胸肉",row.ingredient().getName());assertEquals("300克",row.amount());
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM ingredient WHERE name='鸡胸肉300克'",Integer.class));
    }
}
