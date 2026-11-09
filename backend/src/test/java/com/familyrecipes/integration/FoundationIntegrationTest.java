package com.familyrecipes.integration;

import com.familyrecipes.util.JwtUtil;
import com.familyrecipes.model.Message;
import org.springframework.http.MediaType;
import com.familyrecipes.mapper.*;
import com.familyrecipes.service.*;
import com.familyrecipes.entity.*;
import com.familyrecipes.common.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Uses an isolated MySQL initialized with schema.sql and data.sql; every test rolls back its rows. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named="FOUNDATION_DB_TEST", matches="true")
class FoundationIntegrationTest {
    @TempDir static Path images;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("file.upload-dir", images::toString);
        registry.add("app.ai.routes.translation.model", () -> "");
        registry.add("file.base-url", () -> "http://localhost/api/upload/files/");
    }
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @Autowired JwtUtil jwt;
    @Autowired RecipeService recipes;
    @Autowired RecipeMapper recipeMapper;
    @Autowired FridgeService fridge;
    @Autowired FridgeItemMapper fridgeMapper;
    @Autowired MediaService media;
    @Autowired MessageService messages;
    @Autowired MessageMapper messageMapper;
    @Autowired ObjectMapper json;
    @Autowired Clock clock;
    @Autowired UserService users;
    @org.springframework.boot.test.mock.mockito.MockBean VerificationMailSender mail;
    @Autowired EmailVerificationService emailCodes;
    private final Map<String,String> deliveredCodes=new HashMap<>();
    private String codeFor(String email) {
        emailCodes.sendRegistrationCode(email,"test-"+owner);
        return deliveredCodes.get(email.toLowerCase(java.util.Locale.ROOT));
    }
    @Autowired GroupChatService groups;
    @Autowired RecommendationMapper recommendations;
    @Autowired KitchenService kitchens;
    long owner, outsider, group, kitchen;
    String ownerAuth, otherAuth;
    @BeforeEach void fixtures() {
        org.mockito.Mockito.reset(mail);
        deliveredCodes.clear();
        org.mockito.Mockito.doAnswer(call -> {
            deliveredCodes.put(call.getArgument(0),call.getArgument(1));return null;
        }).when(mail).send(org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString());
        owner = user("owner"); outsider = user("outsider");
        ownerAuth = "Bearer " + jwt.generateToken(owner, "owner@test.example");
        otherAuth = "Bearer " + jwt.generateToken(outsider, "outsider@test.example");
        db.update("INSERT INTO group_chat(name,creator_id,manager_id,member_count) VALUES('test',?,?,1)", owner, owner);
        group = db.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        db.update("INSERT INTO group_member(group_id,user_id,role) VALUES(?,?,'manager')", group, owner);
        kitchen=kitchens.create(group,"城区厨房",owner).getId();
    }
    long user(String name) {
        db.update("INSERT INTO user(email,password,username) VALUES(?, 'never-serialize-this',?)", UUID.randomUUID()+"@test.example", name);
        return db.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
    Recipe recipe(String visibility) {
        Recipe r = new Recipe(); r.setName("test dish"); r.setCreatorId(owner); r.setVisibility(visibility);
        if (visibility.equals("group")) r.setSharedGroupIds("["+group+"]");
        // Legacy private fixtures exercise pre-migration read permissions, not new writes.
        if(visibility.equals("private"))r.setVisibility("public");
        var saved=recipes.createRecipe(r, null, null, null, null, null);
        if(visibility.equals("private")) {db.update("UPDATE recipe SET visibility='private' WHERE id=?",saved.getId());saved.setVisibility("private");}
        return saved;
    }

    @Autowired FriendshipService friendshipService;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Test void tagBackfillRejectsStaleContentAndBoundsFailures() throws Exception {
        db.update("INSERT IGNORE INTO recipe_classification(recipe_id,status) SELECT id,'complete' FROM recipe");
        var r=recipe("public");
        var client=org.mockito.Mockito.mock(com.familyrecipes.ai.ChatCompletionClient.class);
        org.mockito.Mockito.when(client.complete(org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyList()))
            .thenAnswer(call->{db.update("UPDATE recipe SET description='changed during request' WHERE id=?",r.getId());return "{\"taste\":[\"甜\"],\"cuisine\":[\"日式\"]}";});
        var worker=new RecipeTagBackfill(db,new ObjectMapper(),client,new com.familyrecipes.ai.AiRoutes(),transactionManager);
        worker.processOne();
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM recipe_classification WHERE recipe_id=?",Integer.class,r.getId()));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM recipe_tag WHERE recipe_id=? AND tag_value='甜'",Integer.class,r.getId()));
        org.mockito.Mockito.when(client.complete(org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyList())).thenReturn("bad JSON");
        for(int i=0;i<4;i++) {
            db.update("UPDATE recipe_classification SET next_attempt_at=NOW() WHERE recipe_id=?",r.getId());
            worker.processOne();
        }
        assertEquals(3,db.queryForObject("SELECT attempts FROM recipe_classification WHERE recipe_id=?",Integer.class,r.getId()));
        assertEquals("failed",db.queryForObject("SELECT status FROM recipe_classification WHERE recipe_id=?",String.class,r.getId()));
    }

    @Test void tagBackfillWritesMissingTagsPreservesExplicitAndDoesNotRepeat() throws Exception {
        db.update("INSERT IGNORE INTO recipe_classification(recipe_id,status) SELECT id,'complete' FROM recipe");
        var r=recipe("public");
        db.update("DELETE FROM recipe_tag WHERE recipe_id=?",r.getId());
        db.update("INSERT INTO recipe_tag(recipe_id,tag_type,tag_value) VALUES(?,'cuisine','粤菜')",r.getId());
        var client=org.mockito.Mockito.mock(com.familyrecipes.ai.ChatCompletionClient.class);
        org.mockito.Mockito.when(client.complete(org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyList()))
            .thenReturn("{\"taste\":[\"甜\"],\"cuisine\":[\"日式\"],\"evidence\":\"honey\"}");
        var worker=new RecipeTagBackfill(db,new ObjectMapper(),client,new com.familyrecipes.ai.AiRoutes(),transactionManager);
        worker.processOne();worker.processOne();
        assertEquals(List.of("甜","粤菜"),db.queryForList("SELECT tag_value FROM recipe_tag WHERE recipe_id=? ORDER BY tag_value",String.class,r.getId()).stream().sorted().toList());
        assertEquals("complete",db.queryForObject("SELECT status FROM recipe_classification WHERE recipe_id=?",String.class,r.getId()));
        org.mockito.Mockito.verify(client,org.mockito.Mockito.times(1)).complete(org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyList());
    }

    @Test void friendSearchIsMinimalAndInvitationsRequireRecipientAcceptance() throws Exception {
        var matches=friendshipService.search(owner,Long.toString(outsider));
        assertEquals(1,matches.size());assertFalse(matches.get(0).containsKey("email"));
        friendshipService.addFriend(owner,outsider);
        assertFalse(friendshipService.isFriend(owner,outsider));
        assertThrows(ApiException.class,()->friendshipService.addFriend(owner,outsider));
        assertThrows(ApiException.class,()->friendshipService.respond(owner,outsider,true));
        friendshipService.respond(outsider,owner,true);
        assertTrue(friendshipService.isFriend(owner,outsider));
        assertTrue(friendshipService.isFriend(outsider,owner));
        assertTrue(friendshipService.invitations(owner).isEmpty());
    }
    @Test void savingAdaptationCopiesOnlyOnSaveAndPreservesSource() {
        var original=recipe("public");long before=db.queryForObject("SELECT COUNT(*) FROM recipe",Long.class);
        var draft=recipes.getRecipeDetail(original.getId(),outsider);
        assertEquals(before,db.queryForObject("SELECT COUNT(*) FROM recipe",Long.class));
        draft.setId(null);draft.setSourceRecipeId(original.getId());draft.setCreatorId(outsider);
        draft.setName("My changed version");draft.setVisibility("public");
        var saved=recipes.createRecipe(draft,draft.getTags(),draft.getIngredients(),draft.getSteps(),List.of(),draft.getExternalRecipes());
        assertEquals(before+1,db.queryForObject("SELECT COUNT(*) FROM recipe",Long.class));
        assertEquals(original.getId(),saved.getSourceRecipeId());
        assertEquals("test dish",recipes.getRecipeDetail(original.getId(),owner).getName());
    }
    @Test void recommendationPrefersKnownAndFavoriteButDoesNotExcludeDisliked() {
        var known=recipe("public");var disliked=recipe("public");
        db.update("INSERT INTO recipe_cook(recipe_id,user_id,skill_level) VALUES(?,?,3)",known.getId(),owner);
        db.update("INSERT INTO user_favorite(recipe_id,user_id) VALUES(?,?)",known.getId(),owner);
        db.update("INSERT INTO user_dislike(recipe_id,user_id) VALUES(?,?)",disliked.getId(),owner);
        var ranked=recommendations.recommend(owner,null,"discovery","stable",LocalDate.now(clock));
        assertTrue(ranked.indexOf(known.getId())<ranked.indexOf(disliked.getId()));assertTrue(ranked.contains(disliked.getId()));
        recipes.viewRecipe(known.getId(),owner);recipes.viewRecipe(known.getId(),owner);
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM recipe_view_history WHERE user_id=? AND recipe_id=?",Integer.class,owner,known.getId()));
        var page=workflows.browse(owner,null,"discovery","all","","",null,null,false,"stable",1,"");
        assertNotNull(page);
    }
    @Test void publicRecipeMigrationIsRepeatableAndPreservesGroups() throws Exception {
        var legacy=recipe("private");var restricted=recipe("group");
        String sql=new String(new org.springframework.core.io.ClassPathResource("migrations/2026-11-08-public-recipes.sql").getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        // Avoid DDL's implicit commit in this rollback-isolated test; check the data migration twice.
        String update=sql.substring(sql.indexOf("UPDATE"),sql.indexOf(';'));
        db.update(update);db.update(update);
        assertEquals("public",recipeMapper.findById(legacy.getId()).getVisibility());
        assertEquals("group",recipeMapper.findById(restricted.getId()).getVisibility());
    }
    @Test void newRecipeDefaultsToPublicAndPrivateWritesAreRejected() {
        Recipe r = new Recipe(); r.setName("默认公开"); r.setCreatorId(owner);
        var saved = recipes.createRecipe(r,null,null,null,null,null);
        assertEquals("public",saved.getVisibility());
        assertNotNull(recipes.getRecipeDetail(saved.getId(),outsider));
        r.setId(null);r.setVisibility("private");
        assertThrows(ApiException.class,()->recipes.createRecipe(r,null,null,null,null,null));
        saved.setVisibility("private");
        assertThrows(ApiException.class,()->recipes.updateRecipe(saved,null,null,null,null,null));
    }
    @Test void externalLinksKeepNotesAndUserOrderAcrossEdits() {
        Recipe r = new Recipe(); r.setName("多来源菜谱"); r.setCreatorId(owner); r.setVisibility("public");
        ExternalRecipe a = new ExternalRecipe(); a.setTitle("视频"); a.setUrl("https://youtu.be/demo");
        a.setNotes("孩子吃可以省略辣椒"); a.setSource("wrong"); a.setSortOrder(99);
        ExternalRecipe b = new ExternalRecipe(); b.setTitle("步骤"); b.setUrl("https://www.instagram.com/p/demo");
        b.setNotes("第3步可省略");
        r = recipes.createRecipe(r,null,null,null,null,List.of(a,b));
        assertEquals("YouTube",r.getExternalRecipes().get(0).getSource());
        assertEquals("孩子吃可以省略辣椒",r.getExternalRecipes().get(0).getNotes());
        assertEquals(0,r.getExternalRecipes().get(0).getSortOrder());
        b.setNotes("更新备注");
        r = recipes.updateRecipe(r,null,null,null,null,List.of(b,a));
        var saved = recipes.getRecipeDetail(r.getId(),owner).getExternalRecipes();
        assertEquals(List.of("步骤","视频"), saved.stream().map(ExternalRecipe::getTitle).toList());
        assertEquals("更新备注",saved.get(0).getNotes());
        assertEquals(List.of(0,1),saved.stream().map(ExternalRecipe::getSortOrder).toList());
        ExternalRecipe c = new ExternalRecipe(); c.setRecipeId(r.getId()); c.setAddedBy(owner);
        c.setTitle("另一种做法"); c.setUrl("https://fb.watch/demo");
        recipes.addExternalRecipe(c);
        assertEquals(2,recipes.getRecipeDetail(r.getId(),owner).getExternalRecipes().get(2).getSortOrder());
        c.setAddedBy(outsider);
        assertThrows(ApiException.class,()->recipes.addExternalRecipe(c));
        c.setAddedBy(owner); c.setUrl("javascript:alert(1)");
        assertThrows(ApiException.class,()->recipes.addExternalRecipe(c));
    }
    @Test void passwordsAreHashedAndValidationIsEnforced() throws Exception {
        String email=UUID.randomUUID()+"@test.example";
        User user=users.register(email,"test-password-123","new user",codeFor(email));
        var initial = kitchens.list(user.getId());
        assertEquals(1, initial.size());
        assertEquals("我的厨房", initial.get(0).getName());
        assertEquals(1, groups.getGroupDetail(initial.get(0).getGroupId(), user.getId()).getMemberCount());
        String stored=db.queryForObject("SELECT password FROM user WHERE id=?",String.class,user.getId());
        assertTrue(stored.startsWith("$2a$"));assertNotEquals("test-password-123",stored);
        assertEquals(user.getId(),jwt.getUserIdFromToken(users.login(email,"test-password-123")));
        assertThrows(ApiException.class,()->users.login(email,"wrong-password"));
        mvc.perform(post("/api/auth/register").contentType("application/json").content("{\"email\":\"invalid\",\"password\":\"short\",\"username\":\"\"}")).andExpect(status().isBadRequest());
    }
    @Test void soloKitchenInitializationIsIdempotentAndPrivate() {
        var initial = kitchens.list(outsider);
        assertEquals(1, initial.size());
        assertEquals(initial.get(0).getId(), kitchens.list(outsider).get(0).getId());
        assertThrows(ApiException.class, () -> kitchens.requireMember(initial.get(0).getId(), owner));
        var second = kitchens.create(initial.get(0).getGroupId(), "另一处厨房", outsider);
        assertEquals(initial.get(0).getGroupId(), second.getGroupId());
        assertEquals(2, kitchens.list(outsider).size());
        // Existing shared kitchens remain intact; visiting does not create another household.
        assertEquals(kitchen, kitchens.list(owner).get(0).getId());
        assertEquals(1, kitchens.list(owner).size());
    }
    @Test void kitchenCanBeCreatedWithoutPreexistingGroup() {
        var created = kitchens.create(null, "一个人的厨房", outsider);
        assertEquals(outsider, groups.getGroupDetail(created.getGroupId(), outsider).getManagerId());
        assertEquals(10, groups.getGroupDetail(created.getGroupId(), outsider).getMaxMembers());
        assertEquals(created.getId(), kitchens.list(outsider).get(0).getId());
    }
    @Test void stockClassificationOverrideNeverChangesTheSharedIngredient() {
        var milk = fridge.previewIngredient("牛奶");
        long drinks = db.queryForObject("SELECT id FROM inventory_categories WHERE name='饮品'",Long.class);
        FridgeItem item = new FridgeItem(); item.setUserId(owner); item.setKitchenId(kitchen);
        item.setIngredientId(milk.getId()); item.setExpiryDate(LocalDate.now(clock).plusDays(7));
        item.setCategoryOverrideId(drinks);
        item = fridge.addItem(item);
        var listed = fridge.getUserFridgeItems(owner,kitchen).get(0);
        assertEquals(drinks, listed.getCategoryOverrideId());
        assertEquals("饮品", listed.getCategoryOverrideName());
        assertEquals("蛋奶", listed.getIngredient().getCategory());
        assertEquals("蛋奶", fridge.previewIngredient("牛奶").getCategory());
        item.setUserId(owner); item.setCategoryOverrideId(null); fridge.updateItem(item);
        assertNull(fridge.getUserFridgeItems(owner,kitchen).get(0).getCategoryOverrideId());
        item.setUserId(outsider); item.setCategoryOverrideId(drinks);
        FridgeItem forbidden = item;
        assertThrows(ApiException.class, () -> fridge.updateItem(forbidden));
    }
    @Test void inventoryTaxonomyUsesOneCategoryAndPreviewDoesNotCreateRows() throws Exception {
        int before = db.queryForObject("SELECT COUNT(*) FROM ingredient", Integer.class);
        var milk = fridge.previewIngredient("牛奶");
        assertNotNull(milk.getId()); assertEquals("蛋奶", milk.getCategory());
        assertEquals("蔬菜菌菇", fridge.previewIngredient("西红柿").getCategory());
        assertEquals("番茄", fridge.previewIngredient("西红柿").getName());
        assertEquals("饮品", fridge.previewIngredient("豆浆").getCategory());
        assertEquals("熟食速食", fridge.previewIngredient("鱼丸").getCategory());
        assertEquals("米面主食", fridge.previewIngredient("饺子皮").getCategory());
        assertEquals("调味料", fridge.previewIngredient("豆豉").getCategory());
        var unknown = fridge.previewIngredient("验收新物品");
        assertNull(unknown.getId()); assertEquals("待分类", unknown.getCategory());
        assertEquals(before, db.queryForObject("SELECT COUNT(*) FROM ingredient", Integer.class));
        long chosen = db.queryForObject("SELECT id FROM inventory_categories WHERE name='饮品'", Long.class);
        var created = fridge.getOrCreateIngredient("验收新物品", "ignored", chosen);
        assertEquals("饮品", created.getCategory()); assertEquals(chosen, created.getCategoryId());
        // Adding stock must not reclassify a shared, existing ingredient.
        assertEquals("蛋奶", fridge.getOrCreateIngredient("牛奶", "ignored", chosen).getCategory());
        assertThrows(ApiException.class, () -> fridge.getOrCreateIngredient("无效分类物品", null, -1L));
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM ingredient i LEFT JOIN inventory_categories c ON c.id=i.category_id WHERE c.id IS NULL OR i.category<>c.name", Integer.class));
        mvc.perform(get("/api/fridge/ingredients/preview").param("name","牛奶").header("Authorization",ownerAuth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.category").value("蛋奶"));
    }
    @Test void authNeverFallsBackToUserOne() throws Exception {
        mvc.perform(get("/api/fridge/items")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/recipes/search").header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/recipes/search")).andExpect(status().isOk());
        mvc.perform(post("/api/upload/image")).andExpect(status().isUnauthorized());
    }
    @Test void privateAndGroupRecipesAreFilteredBeforePaginationAndCounting() throws Exception {
        Recipe privateRecipe = recipe("private"); Recipe shared = recipe("group"); Recipe publicRecipe = recipe("public");
        assertFalse(recipeMapper.isVisible(privateRecipe.getId(), outsider));
        assertFalse(recipeMapper.isVisible(shared.getId(), outsider));
        assertTrue(recipeMapper.isVisible(publicRecipe.getId(), null));
        Map<String,Object> query = new HashMap<>(); query.put("keyword", "test dish"); query.put("viewerId", outsider); query.put("offset",0);query.put("limit",20);
        assertEquals(1L, recipeMapper.countSearch(query));
        assertEquals(publicRecipe.getId(), recipeMapper.search(query).get(0).getId());
        assertEquals(1, recipeMapper.searchByKeyword("test", outsider).size());
        mvc.perform(get("/api/recipes/"+privateRecipe.getId()).header("Authorization",otherAuth)).andExpect(status().isNotFound());
        mvc.perform(get("/api/recipes/"+publicRecipe.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.data.creator.password").doesNotExist());
        db.update("INSERT INTO group_member(group_id,user_id) VALUES(?,?)",group,outsider);
        assertTrue(recipeMapper.isVisible(shared.getId(), outsider));
        db.update("DELETE FROM group_member WHERE group_id=? AND user_id=?",group,outsider);
        assertFalse(recipeMapper.isVisible(shared.getId(), outsider));
    }
    @Test void omittedCollectionsSurviveButExplicitEmptyClearsAndOtherUsersCannotEdit() throws Exception {
        Recipe r=recipe("public");
        db.update("INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(?,1,'preserve')",r.getId());
        String payload=json.writeValueAsString(Map.of("recipe",Map.of("name","edited","visibility","public")));
        mvc.perform(put("/api/recipes/"+r.getId()).header("Authorization",otherAuth).contentType("application/json").content(payload)).andExpect(status().isNotFound());
        mvc.perform(put("/api/recipes/"+r.getId()).header("Authorization",ownerAuth).contentType("application/json").content(payload)).andExpect(status().isOk());
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM cooking_step WHERE recipe_id=?",Integer.class,r.getId()));
        payload=json.writeValueAsString(Map.of("recipe",Map.of("name","edited","visibility","public"),"steps",List.of()));
        mvc.perform(put("/api/recipes/"+r.getId()).header("Authorization",ownerAuth).contentType("application/json").content(payload)).andExpect(status().isOk());
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM cooking_step WHERE recipe_id=?",Integer.class,r.getId()));
    }
    @Test void expiringItemsRemainVisibleAfterStatusRefreshAndConsumptionIsScoped() throws Exception {
        db.update("INSERT INTO ingredient(name,category_id) VALUES('apple',1)");
        long ingredient=db.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        FridgeItem item=new FridgeItem();item.setUserId(owner);item.setKitchenId(kitchen);item.setIngredientId(ingredient);item.setExpiryDate(LocalDate.now(clock).plusDays(1));
        item=fridge.addItem(item);
        assertEquals("expiring",item.getStatus());
        assertEquals("apple",fridge.getUserFridgeItems(owner,kitchen).get(0).getIngredient().getName());
        assertEquals(1L,fridge.getUserFridgeItems(owner,kitchen).get(0).getIngredient().getCategoryId());
        assertEquals(1,fridge.getExpiringItems(owner).size());
        mvc.perform(post("/api/fridge/items/"+item.getId()+"/consume").header("Authorization",otherAuth)).andExpect(status().isNotFound());
        assertEquals("expiring",fridgeMapper.findById(item.getId()).getStatus());
        fridge.markAsConsumed(item.getId(),owner);
        assertTrue(fridge.getExpiringItems(owner).isEmpty());
    }
    @Test void imagesAreDecodedOwnedAndFollowRecipeVisibility() throws Exception {
        assertThrows(ApiException.class,()->media.upload(new MockMultipartFile("file","fake.png","image/png","not an image".getBytes()),owner));
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",bytes);
        String url=media.upload(new MockMultipartFile("file","picture.exe","image/png",bytes.toByteArray()),owner);
        String filename=url.substring(url.lastIndexOf('/')+1);
        assertNotNull(media.read(filename,owner));
        assertThrows(ApiException.class,()->media.read(filename,outsider));
        assertThrows(ApiException.class,()->media.requireOwned(url,outsider));
        Recipe r=recipe("public"); db.update("UPDATE recipe SET cover_image=? WHERE id=?",url,r.getId());
        mvc.perform(get("/api/upload/files/"+filename)).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        db.update("UPDATE recipe SET visibility='private' WHERE id=?",r.getId());
        assertThrows(ApiException.class,()->media.read(filename,null));
        assertThrows(ApiException.class,()->media.read("../schema.sql",owner));
    }
    @Test void messageReadsAreScopedAndReadPositionCannotRewind() {
        var first=messages.sendMessage(group,owner,"one",null);
        var second=messages.sendMessage(group,owner,"two",null);
        assertNotNull(first.getCreatedAt());assertEquals("owner",first.getSender().getUsername());
        assertThrows(ApiException.class,()->messages.getUnreadCount(group,outsider));
        assertThrows(ApiException.class,()->messages.sendMessage(group,outsider,"forged",null));
        messages.markAsRead(group,owner,second.getId());messages.markAsRead(group,owner,first.getId());
        assertEquals(second.getId(),messageMapper.getReadPosition(owner,group).getLastReadMessageId());
        assertEquals(second.getId(),messages.getMessages(group,owner,1,20).get(0).getId());
    }

    @Test void kitchensShareMembersButNeverInventoryAndLabelsAreNotPermissions() throws Exception {
        long second=kitchens.create(group,"老家厨房",owner).getId();
        assertNotEquals(kitchen,second);
        assertThrows(ApiException.class,()->kitchens.create(group,"forged",outsider));
        db.update("INSERT INTO ingredient(name) VALUES('共享苹果')");
        long ingredient=db.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        FridgeItem item=new FridgeItem();item.setUserId(owner);item.setKitchenId(kitchen);item.setIngredientId(ingredient);
        item.setExpiryDate(LocalDate.now(clock).plusDays(2));item.setOwnerUserId(owner);item=fridge.addItem(item);
        db.update("INSERT INTO group_member(group_id,user_id) VALUES(?,?)",group,outsider);
        assertEquals(1,fridge.getUserFridgeItems(outsider,kitchen).size());
        assertTrue(fridge.getUserFridgeItems(outsider,second).isEmpty());
        item.setUserId(outsider);item.setAmount("2个");item.setKitchenId(second);
        FridgeItem updated=fridge.updateItem(item);
        assertEquals(kitchen,updated.getKitchenId());assertEquals(owner,updated.getUserId());
        assertEquals(owner,updated.getOwnerUserId());assertEquals("2个",updated.getAmount());
        fridge.markAsConsumed(item.getId(),outsider);
        assertEquals("consumed",fridgeMapper.findById(item.getId()).getStatus());
        db.update("DELETE FROM group_member WHERE group_id=? AND user_id=?",group,outsider);
        assertThrows(ApiException.class,()->fridge.getUserFridgeItems(outsider,kitchen));
    }
    @Test void catchupHasNoTotalLimitAndRetriesAreIdempotent() throws Exception {
        String client=UUID.randomUUID().toString();
        var first=messages.sendMessage(group,owner,"hello",null,client);
        assertEquals(first.getId(),messages.sendMessage(group,owner,"hello",null,client).getId());
        assertThrows(ApiException.class,()->messages.sendMessage(group,owner,"changed",null,client));
        for(int i=0;i<420;i++) messages.sendMessage(group,owner,"message "+i,null,UUID.randomUUID().toString());
        long cursor=0;Set<Long> received=new HashSet<>();
        while(true) {
            var page=messages.catchUp(group,owner,cursor,200);if(page.isEmpty())break;
            assertTrue(page.get(0).getId()>cursor);page.forEach(m->assertTrue(received.add(m.getId())));
            cursor=page.get(page.size()-1).getId();
        }
        assertEquals(421,received.size());
        assertThrows(ApiException.class,()->messages.catchUp(group,outsider,0L,200));
        mvc.perform(post("/api/messages/send").header("Authorization",ownerAuth).contentType("application/json")
            .content(json.writeValueAsString(Map.of("group_id",group,"content","hello","client_message_id",client,"message_type","text"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(first.getId()));
    }
    @Test void galleryMetadataAndTrashRespectReferencesAndRetainTombstones() throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",bytes);
        String url=media.upload(new MockMultipartFile("file","shot.png","image/png",bytes.toByteArray()),owner,"screenshot");
        String filename=url.substring(url.lastIndexOf('/')+1);
        Recipe r=new Recipe();r.setName("截图笔记");r.setCreatorId(owner);r.setVisibility("group");r.setKitchenId(kitchen);
        RecipeImage image=new RecipeImage();image.setUrl(url);image.setCategory("screenshot");image.setCaption("我自己的步骤记录");r.setImages(List.of(image));
        r=recipes.createRecipe(r,null,null,null,null,null);
        assertEquals("我自己的步骤记录",r.getImages().get(0).getCaption());
        assertThrows(ApiException.class,()->media.changeTrash(filename,owner,false));
        db.update("INSERT INTO group_member(group_id,user_id) VALUES(?,?)",group,outsider);
        assertNotNull(media.read(filename,outsider));
        recipes.deleteRecipe(r.getId(),owner);media.changeTrash(filename,owner,false);
        assertEquals(1,media.trash(owner).size());assertThrows(ApiException.class,()->media.read(filename,owner));
        media.changeTrash(filename,owner,true);assertNotNull(media.read(filename,owner));
        media.changeTrash(filename,owner,false);db.update("UPDATE media_file SET purge_after=NOW()-INTERVAL 1 DAY WHERE filename=?",filename);
        media.cleanTrash();assertFalse(java.nio.file.Files.exists(images.resolve(filename)));
        assertNotNull(db.queryForObject("SELECT purged_at FROM media_file WHERE filename=?",java.sql.Timestamp.class,filename));
        assertThrows(ApiException.class,()->media.changeTrash(filename,owner,true));
    }
    @Test void onlyTextLinksAndOwnedStickersCanBeSent() throws Exception {
        assertThrows(ApiException.class,()->messages.sendMessage(group,owner,"https://example.com/video",com.familyrecipes.model.Message.MessageType.image));
        assertThrows(ApiException.class,()->messages.sendMessage(group,owner,"https://example.com/image",com.familyrecipes.model.Message.MessageType.sticker));
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",bytes);
        String url=media.upload(new MockMultipartFile("file","smile.png","image/png",bytes.toByteArray()),owner,"sticker");
        var sticker=messages.sendMessage(group,owner,url,com.familyrecipes.model.Message.MessageType.sticker);
        assertEquals("sticker",sticker.getMessageType().name());
        db.update("INSERT INTO group_member(group_id,user_id) VALUES(?,?)",group,outsider);
        assertNotNull(media.read(url.substring(url.lastIndexOf('/')+1),outsider));
    }

    @Test void tenMembersAndMultipleNamedKitchensAreServerRules() throws Exception {
        var created=groups.createGroup(owner,"family",null,100,List.of());
        assertEquals(10,created.getMaxMembers());
        assertEquals(1,kitchens.list(owner).stream().filter(k->k.getGroupId().equals(created.getId())).count());
        List<Long> invited=new ArrayList<>();
        for(int i=0;i<9;i++) {
            long id=user("member"+i);invited.add(id);
            db.update("INSERT INTO friendship(user_id,friend_id,status) VALUES(?,?,'accepted')",owner,id);
        }
        groups.addGroupMember(created.getId(),owner,invited);
        long extra=user("extra");db.update("INSERT INTO friendship(user_id,friend_id,status) VALUES(?,?,'accepted')",owner,extra);
        assertThrows(ApiException.class,()->groups.addGroupMember(created.getId(),owner,List.of(extra)));
        assertEquals(10,db.queryForObject("SELECT COUNT(*) FROM group_member WHERE group_id=?",Integer.class,created.getId()));
        mvc.perform(get("/api/kitchens").header("Authorization",ownerAuth)).andExpect(status().isOk());
    }
    @Test void starterLibraryAndRecommendationsAreUsableAndVisibilityScoped() throws Exception {
        assertEquals(423,db.queryForObject("SELECT COUNT(*) FROM ingredient",Integer.class));
        assertEquals(10,db.queryForObject("SELECT COUNT(*) FROM recipe WHERE creator_id IS NULL AND visibility='public'",Integer.class));
        assertEquals("番茄",db.queryForObject("SELECT name FROM ingredient WHERE JSON_CONTAINS(aliases,JSON_QUOTE('西红柿'))",String.class));
        var discovery=recommendations.recommend(owner,null,"discovery","seed",LocalDate.now(clock));
        assertFalse(discovery.isEmpty());
        mvc.perform(get("/api/recipes/recommendations").header("Authorization",ownerAuth)).andExpect(status().isOk());
        var privateRecipe=recipe("private");
        assertFalse(recommendations.recommend(outsider,null,"discovery","seed",LocalDate.now(clock)).contains(privateRecipe.getId()));
        assertTrue(recommendations.recommend(owner,kitchen,"inventory","seed",LocalDate.now(clock)).isEmpty());
        long ingredient=db.queryForObject("SELECT id FROM ingredient WHERE name='番茄'",Long.class);
        db.update("INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount) VALUES(?,?,'1个')",privateRecipe.getId(),ingredient);
        db.update("INSERT INTO recipe_cook(recipe_id,user_id,skill_level) VALUES(?,?,3)",privateRecipe.getId(),owner);
        FridgeItem item=new FridgeItem();item.setKitchenId(kitchen);item.setUserId(owner);item.setIngredientId(ingredient);item.setExpiryDate(LocalDate.now(clock).plusDays(1));fridge.addItem(item);
        assertTrue(recommendations.recommend(owner,kitchen,"inventory","seed",LocalDate.now(clock)).contains(privateRecipe.getId()));
        long second=kitchens.create(group,"另一地点",owner).getId();
        assertTrue(recommendations.recommend(owner,second,"inventory","seed",LocalDate.now(clock)).isEmpty());
        mvc.perform(get("/api/recipes/search").param("kitchenId",Long.toString(kitchen)).header("Authorization",otherAuth)).andExpect(status().isNotFound());
    }

    @Test void registrationRequiresDeliveredCodeAndConsumesItAtomically() throws Exception {
        String email=UUID.randomUUID()+"@example.com";
        assertThrows(ApiException.class,()->users.register(email,"test-password-123","tester","123456"));
        String code=codeFor(email);
        String stored=db.queryForObject("SELECT code_hash FROM registration_email_code WHERE email=?",String.class,email);
        assertNotEquals(code,stored);assertEquals(64,stored.length());
        String payload=json.writeValueAsString(Map.of("email",email.toUpperCase(java.util.Locale.ROOT),"password","test-password-123","username","tester","code",code));
        mvc.perform(post("/api/auth/register").contentType("application/json").content(payload)).andExpect(status().isOk());
        assertNotNull(users.getUserByEmail(email));
        mvc.perform(post("/api/auth/register").contentType("application/json").content(payload)).andExpect(status().isBadRequest());
        assertTrue(db.queryForObject("SELECT consumed FROM registration_email_code WHERE email=?",Boolean.class,email));
    }
    @Test void wrongAttemptsExpiryAndResendInvalidateCodes() {
        String email=UUID.randomUUID()+"@example.com";
        String first=codeFor(email);
        String wrong=first.equals("000000")?"000001":"000000";
        for(int i=0;i<5;i++) assertThrows(ApiException.class,()->users.register(email,"test-password-123","tester",wrong));
        assertEquals(5,db.queryForObject("SELECT attempts FROM registration_email_code WHERE email=?",Integer.class,email));
        assertThrows(ApiException.class,()->users.register(email,"test-password-123","tester",first));
        assertEquals(429,assertThrows(ApiException.class,()->codeFor(email)).getStatus());
        db.update("UPDATE registration_email_code SET issued_at=0 WHERE email=?",email);
        String next=codeFor(email);
        assertEquals(0,db.queryForObject("SELECT attempts FROM registration_email_code WHERE email=?",Integer.class,email));
        db.update("UPDATE registration_email_code SET expires_at=0 WHERE email=?",email);
        assertThrows(ApiException.class,()->users.register(email,"test-password-123","tester",next));
        assertNull(users.getUserByEmail(email));
    }
    @Test void smtpFailureNeverActivatesCodeAndStillLimitsSending() {
        String email=UUID.randomUUID()+"@example.com";
        org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("test failure")).when(mail)
            .send(org.mockito.ArgumentMatchers.eq(email),org.mockito.ArgumentMatchers.anyString());
        assertEquals(503,assertThrows(ApiException.class,()->codeFor(email)).getStatus());
        assertFalse(db.queryForObject("SELECT active FROM registration_email_code WHERE email=?",Boolean.class,email));
        assertEquals(429,assertThrows(ApiException.class,()->codeFor(email)).getStatus());
    }
    @Test void registrationCodeEndpointIsPublicAndEmailQuotaIsEnforced() throws Exception {
        String email=UUID.randomUUID()+"@example.com";
        mvc.perform(post("/api/auth/registration-code").contentType("application/json").content(json.writeValueAsString(Map.of("email",email))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.retry_after").value(60)).andExpect(jsonPath("$.data.code").doesNotExist());
        for(int i=1;i<5;i++) { db.update("UPDATE registration_email_code SET issued_at=0 WHERE email=?",email);codeFor(email); }
        db.update("UPDATE registration_email_code SET issued_at=0 WHERE email=?",email);
        assertEquals(429,assertThrows(ApiException.class,()->codeFor(email)).getStatus());
    }
    @Autowired RecipeWorkflowService workflows;
    @Autowired MealService meals;
    @Test void mealsArePrivateUntilExplicitSharingAndRevisionChecked() throws Exception {
        var m=new MealArrangement();m.setKitchenId(kitchen);m.setKind("order");m.setTitle("周末点菜");
        var privateRecipe=recipe("private");
        var image=new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB);var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);
        String imageUrl=media.upload(new MockMultipartFile("file","shared.png","image/png",out.toByteArray()),owner);
        privateRecipe.setCoverImage(imageUrl);privateRecipe.setVisibility("public");recipes.updateRecipe(privateRecipe,List.of(),List.of(),List.of(),null,null);
        db.update("UPDATE recipe SET visibility='private' WHERE id=?",privateRecipe.getId());
        String filename=imageUrl.substring(imageUrl.lastIndexOf('/')+1);
        assertThrows(ApiException.class,()->media.read(filename,outsider));
        var dish=new MealArrangement.Dish();dish.setName("手写菜名");dish.setRecipeId(privateRecipe.getId());m.setDishes(List.of(dish));m.setShopping(List.of());
        m=meals.save(m,owner);Long id=m.getId();
        assertThrows(ApiException.class,()->meals.get(id,outsider));
        db.update("INSERT INTO group_member(group_id,user_id,role) VALUES(?,?,'member')",group,outsider);
        assertThrows(ApiException.class,()->meals.get(id,outsider));
        meals.share(id,group,owner);assertEquals("周末点菜",meals.get(id,outsider).getTitle());
        var shared=meals.get(id,outsider);assertThrows(ApiException.class,()->meals.save(shared,outsider));
        var prep=meals.get(id,outsider);prep.setId(null);prep.setSourceId(id);prep.setKind("prep");
        var savedPrep=meals.save(prep,outsider);
        assertEquals("手写菜名",savedPrep.getDishes().get(0).getName());
        assertEquals(privateRecipe.getId(),savedPrep.getDishes().get(0).getRecipeId());
        assertEquals(privateRecipe.getId(),recipes.getRecipeDetail(privateRecipe.getId(),outsider).getId());
        assertTrue(java.nio.file.Files.exists(media.read(filename,outsider)));
        var editAttempt=recipes.getRecipeDetail(privateRecipe.getId(),outsider);editAttempt.setCreatorId(outsider);
        assertThrows(ApiException.class,()->recipes.updateRecipe(editAttempt,List.of(),List.of(),List.of(),null,null));
        assertThrows(ApiException.class,()->recipes.deleteRecipe(privateRecipe.getId(),outsider));
        assertEquals(1,workflows.browse(outsider,kitchen,"mine","shared","","",null,null,false,"x",1).getTotal());
        var stale=meals.get(id,owner);m.setTitle("新菜单");meals.save(m,owner);assertThrows(ApiException.class,()->meals.save(stale,owner));
        db.update("DELETE FROM group_member WHERE group_id=? AND user_id=?",group,outsider);
        assertThrows(ApiException.class,()->meals.get(id,outsider));
        assertThrows(ApiException.class,()->recipes.getRecipeDetail(privateRecipe.getId(),outsider));
        assertThrows(ApiException.class,()->media.read(filename,outsider));
    }
    @Test void inventoryStatesNeverCountDiscardedAsConsumedAndRestoreRecalculatesExpiry() {
        var item=new FridgeItem();item.setUserId(owner);item.setKitchenId(kitchen);item.setIngredientId(fridge.previewIngredient("牛奶").getId());item.setExpiryDate(LocalDate.now(clock).minusDays(1));
        Long id=fridge.addItem(item).getId();fridge.changeStatus(id,owner,"discarded");
        assertTrue(fridge.getUserFridgeItems(owner,kitchen).isEmpty());assertTrue(fridge.getConsumedItems(owner,kitchen).isEmpty());
        fridge.changeStatus(id,owner,"normal");assertEquals("expired",fridge.getUserFridgeItems(owner,kitchen).get(0).getStatus());
        fridge.changeStatus(id,owner,"consumed");assertEquals(1,fridge.getConsumedItems(owner,kitchen).size());
        assertThrows(ApiException.class,()->fridge.changeStatus(id,outsider,"normal"));
    }
    @Test void cuisineFilterCombinesWithVisibility() {
        var publicRecipe=recipe("public");
        var privateRecipe=recipe("group");
        String probe="cuisine-probe-"+UUID.randomUUID();
        for (var r : List.of(publicRecipe,privateRecipe)) {
            r.setName(probe);
            var cuisine=new RecipeTag();cuisine.setTagType("cuisine");cuisine.setTagValue("川菜");
            recipes.updateRecipe(r,List.of(cuisine),null,null,null,null);
        }
        assertEquals(1,workflows.browse(outsider,null,"discovery","all",probe,"",null,null,false,"x",1,"川菜").getTotal());
        assertEquals(0,workflows.browse(outsider,null,"discovery","all",probe,"",null,null,false,"x",1,"意大利菜").getTotal());
    }
    @Test void adaptationCopiesContentAndImagesAndKeepsSourceIndependent() throws Exception {
        var image=new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB);var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);
        String url=media.upload(new MockMultipartFile("file","test.png","image/png",out.toByteArray()),owner);
        var original=new Recipe();original.setName("原始做法");original.setCreatorId(owner);original.setVisibility("public");original.setCoverImage(url);
        var step=new CookingStep();step.setStepNumber(1);step.setDescription("原始步骤");step.setImage(url);
        original=recipes.createRecipe(original,List.of(),List.of(),List.of(step),List.of(),List.of());
        var otherKitchen=kitchens.list(outsider).get(0);
        var copy=workflows.adapt(original.getId(),outsider,otherKitchen.getId());
        assertEquals(original.getId(),copy.getSourceRecipeId());assertEquals("public",copy.getVisibility());assertNotEquals(url,copy.getCoverImage());
        media.requireOwned(copy.getCoverImage(),outsider);
        copy.setName("自己的版本");recipes.updateRecipe(copy,copy.getTags(),copy.getIngredients(),copy.getSteps(),null,copy.getExternalRecipes());
        assertEquals("原始做法",recipes.getRecipeDetail(original.getId(),owner).getName());
        recipes.deleteRecipe(original.getId(),owner);assertEquals("原始步骤",recipes.getRecipeDetail(copy.getId(),outsider).getSteps().get(0).getDescription());
        assertTrue(java.nio.file.Files.exists(media.read(copy.getCoverImage().substring(copy.getCoverImage().lastIndexOf('/')+1),outsider)));
    }
    @Test void browseCombinesTasteKnownInventoryAndSearchBeforePagination() {
        var r=recipe("private");var tag=new RecipeTag();tag.setTagType("taste");tag.setTagValue("酸辣");
        var ingredient=new RecipeIngredient();ingredient.setIngredientId(fridge.previewIngredient("牛奶").getId());ingredient.setAmount("1瓶");
        r.setCookingTime(15);r.setVisibility("public");recipes.updateRecipe(r,List.of(tag),List.of(ingredient),List.of(),null,null);
        db.update("UPDATE recipe SET visibility='private' WHERE id=?",r.getId());
        workflows.known(r.getId(),owner,true);
        assertEquals(0,workflows.browse(owner,kitchen,"mine","all","test","酸",30,0,true,"x",1).getTotal());
        var item=new FridgeItem();item.setUserId(owner);item.setKitchenId(kitchen);item.setIngredientId(ingredient.getIngredientId());item.setExpiryDate(LocalDate.now(clock).plusDays(5));fridge.addItem(item);
        assertEquals(1,workflows.browse(owner,kitchen,"mine","all","test","酸",30,0,true,"x",1).getTotal());
        assertEquals(0,workflows.browse(outsider,null,"discovery","all","test","酸",30,null,false,"x",1).getTotal());
    }

    @Autowired ChatShareService chatShares;
    @Test void chatSharingGrantsReadOnlyAccessAndRetriesDoNotDuplicate() throws Exception {
        Recipe privateRecipe=recipe("private");
        assertFalse(recipeMapper.isVisible(privateRecipe.getId(),outsider));
        assertThrows(ApiException.class,()->chatShares.shareRecipe(privateRecipe.getId(),group,outsider,UUID.randomUUID().toString()));
        db.update("INSERT INTO group_member(group_id,user_id,role) VALUES(?,?,'member')",group,outsider);
        String client=UUID.randomUUID().toString();
        var sent=chatShares.shareRecipe(privateRecipe.getId(),group,owner,client);
        assertEquals(client,sent.getClientMessageId());
        assertEquals(sent.getId(),chatShares.shareRecipe(privateRecipe.getId(),group,owner,client).getId());
        assertTrue(recipeMapper.isVisible(privateRecipe.getId(),outsider));
        assertEquals(1, workflows.browse(outsider,kitchen,"mine","shared","","",null,null,false,"chat",1).getTotal());
        BufferedImage avatar = new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream imageBytes = new ByteArrayOutputStream(); ImageIO.write(avatar,"png",imageBytes);
        String avatarUrl = media.upload(new MockMultipartFile("file","avatar.png","image/png",imageBytes.toByteArray()),owner);
        db.update("UPDATE user SET avatar=? WHERE id=?",avatarUrl,owner);
        String filename=avatarUrl.substring(avatarUrl.lastIndexOf('/')+1);
        assertTrue(java.nio.file.Files.exists(media.read(filename,outsider)));
        assertThrows(ApiException.class,()->recipes.deleteRecipe(privateRecipe.getId(),outsider));
        mvc.perform(post("/api/recipes/"+privateRecipe.getId()+"/share-chat").header("Authorization",ownerAuth)
            .contentType("application/json").content(json.writeValueAsString(Map.of("groupId",group,"clientMessageId",client))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(sent.getId()));
        db.update("DELETE FROM group_member WHERE group_id=? AND user_id=?",group,outsider);
        assertFalse(recipeMapper.isVisible(privateRecipe.getId(),outsider));
        assertThrows(ApiException.class,()->media.read(filename,outsider));
    }
    @Test void mentionsValidateGroupMembershipAndPreserveSenderIdentity() {
        String content="@[同名成员](familyrecipes://user/"+outsider+") 一起吃饭";
        assertThrows(ApiException.class,()->messages.sendMessage(group,owner,content,null));
        db.update("INSERT INTO group_member(group_id,user_id,role) VALUES(?,?,'member')",group,outsider);
        var sent=messages.sendMessage(group,owner,content,null,UUID.randomUUID().toString());
        assertEquals("owner",sent.getSender().getUsername());
        assertEquals(content,messages.catchUp(group,outsider,0L,200).get(0).getContent());
        assertThrows(ApiException.class,()->messages.sendMessage(group,owner,"@[x](familyrecipes://user/9999999999999999999999)",null));
    }
    @Test void mealShareRetriesReturnOriginalMessageAfterEditing() {
        var meal=new MealArrangement();meal.setKitchenId(kitchen);meal.setKind("prep");meal.setTitle("备菜");
        meal.setDishes(List.of());meal.setShopping(List.of());meal=meals.save(meal,owner);
        String client=UUID.randomUUID().toString();
        var sent=meals.share(meal.getId(),group,owner,client);
        meal.setTitle("后来改名");meals.save(meal,owner);
        assertEquals(sent.getId(),meals.share(meal.getId(),group,owner,client).getId());
    }

    @Test void searchRanksNamesBeforeIngredientsThenDescriptionAndPopularity() {
        String term="番茄";
        var exact=recipe("public");var popular=recipe("public");var favorite=recipe("public");var plain=recipe("public");var ingredient=recipe("public");var description=recipe("public");
        db.update("UPDATE recipe SET name=?,view_count=0 WHERE id=?",term,exact.getId());
        db.update("UPDATE recipe SET name=?,view_count=10,favorite_count=0 WHERE id=?",term+"炒饭",popular.getId());
        db.update("UPDATE recipe SET name=?,view_count=5,favorite_count=8 WHERE id=?",term+"汤",favorite.getId());
        db.update("UPDATE recipe SET name=?,view_count=5,favorite_count=0 WHERE id=?",term+"炖肉",plain.getId());
        db.update("UPDATE recipe SET view_count=9999 WHERE id=?",ingredient.getId());
        db.update("INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount) SELECT ?,id,'1个' FROM ingredient WHERE name=?",ingredient.getId(),term);
        db.update("UPDATE recipe SET description=?,view_count=99999 WHERE id=?",term,description.getId());
        var ids=workflows.browse(owner,null,"mine","created",term,"",null,null,false,"0",1).getList().stream().map(Recipe::getId).toList();
        assertEquals(List.of(exact.getId(),popular.getId(),favorite.getId(),plain.getId(),ingredient.getId(),description.getId()),ids);
    }
    @Test void groupInvitesAllowMembersAndRequireAcceptedFriendshipAndGrantKitchenMembership() {
        assertThrows(ApiException.class,()->groups.addGroupMember(group,owner,List.of(outsider)));
        friendshipService.addFriend(owner,outsider);
        assertThrows(ApiException.class,()->groups.addGroupMember(group,owner,List.of(outsider)));
        friendshipService.respond(outsider,owner,true);
        groups.addGroupMember(group,owner,List.of(outsider));
        assertNotNull(kitchens.requireMember(kitchen,outsider));
        long newcomer=user("newcomer");
        assertThrows(ApiException.class,()->groups.addGroupMember(group,newcomer,List.of(owner)));
        friendshipService.addFriend(outsider,newcomer);friendshipService.respond(newcomer,outsider,true);
        groups.addGroupMember(group,outsider,List.of(newcomer));
        assertNotNull(kitchens.requireMember(kitchen,newcomer));
        int count=db.queryForObject("SELECT COUNT(*) FROM group_member WHERE group_id=?",Integer.class,group);
        groups.addGroupMember(group,owner,List.of(outsider));
        assertEquals(count,db.queryForObject("SELECT COUNT(*) FROM group_member WHERE group_id=?",Integer.class,group));
    }

    @Test void translatedSearchWithLargeCatalogCompletesWithoutDetailFanout() {
        var recipeRows=new ArrayList<Object[]>();
        var translationRows=new ArrayList<Object[]>();
        String prefix="perf-"+UUID.randomUUID();
        for(int i=0;i<3000;i++)recipeRows.add(new Object[]{prefix+i,owner,"public"});
        db.batchUpdate("INSERT INTO recipe(name,creator_id,visibility) VALUES(?,?,?)",recipeRows);
        for(int i=0;i<12000;i++) {
            String source=prefix+i;
            translationRows.add(new Object[]{com.familyrecipes.translation.TranslationService.digest(source),source,"translated "+source});
        }
        db.batchUpdate("INSERT INTO content_translations(source_hash,source_text,translated_text,target_language,engine,kind,status) VALUES(?,?,?,'zh','perf','content','ready')",translationRows);
        long start=System.nanoTime();
        var result=assertTimeout(Duration.ofSeconds(8),()->workflows.browse(null,null,"discovery","all","translated "+prefix+"2999","",null,null,false,"0",1));
        assertEquals(1,result.getTotal());
        System.out.println("Catalog search benchmark: 3000 recipes / 12000 translations, "+((System.nanoTime()-start)/1_000_000)+" ms");
    }

    @Test void browseCardsStayLightweightAndSearchTranslatedDescriptionsWithoutDuplicates() {
        var r=recipe("public");
        String description="独特描述"+UUID.randomUUID();
        String term="translated-probe-"+UUID.randomUUID();
        db.update("UPDATE recipe SET description=? WHERE id=?",description,r.getId());
        db.update("INSERT INTO user_favorite(recipe_id,user_id) VALUES(?,?)",r.getId(),owner);
        String hash=com.familyrecipes.translation.TranslationService.digest(description);
        for(String engine:List.of("test-a","test-b"))
            db.update("INSERT INTO content_translations(source_hash,target_language,engine,source_text,kind,status,translated_text) VALUES(?,'en',?,?,'content','ready',?)",hash,engine,description,term);
        var page=workflows.browse(owner,null,"discovery","all",term,"",null,null,false,"0",1);
        assertEquals(1,page.getTotal());
        assertEquals(1,page.getList().size());
        var card=page.getList().get(0);
        assertEquals(r.getId(),card.getId());
        assertEquals(true,card.getIsFavorite());
        assertNotNull(card.getTags());
        assertNull(card.getSteps());assertNull(card.getDescription());assertNull(card.getImages());
        assertEquals(description,recipes.getRecipeDetail(r.getId(),owner).getDescription());
        db.update("UPDATE recipe SET visibility='private' WHERE id=?",r.getId());
        assertEquals(0,workflows.browse(outsider,null,"discovery","all",term,"",null,null,false,"0",1).getTotal());
    }

    @Test void translationsPreserveOriginalsAndInvalidateOnEdit() throws Exception {
        Recipe r=recipe("public");
        db.update("UPDATE recipe SET name='清蒸鱼' WHERE id=?",r.getId());
        String hash=com.familyrecipes.translation.TranslationService.digest("清蒸鱼");
        db.update("INSERT INTO content_translations(source_hash,target_language,engine,source_text,kind,status,translated_text) VALUES(?,'en','content:llm::v1','清蒸鱼','content','ready','Steamed fish')",hash);
        mvc.perform(get("/api/recipes/"+r.getId()).header("Accept-Language","en"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("Steamed fish"))
            .andExpect(jsonPath("$.data.original_content.name").value("清蒸鱼"));
        mvc.perform(get("/api/recipes/"+r.getId()))
            .andExpect(jsonPath("$.data.name").value("清蒸鱼"));
        mvc.perform(get("/api/recipes/"+r.getId()).header("Accept-Language","en").header("X-Content-Original","true"))
            .andExpect(jsonPath("$.data.name").value("清蒸鱼"));
        assertEquals(1,workflows.browse(null,null,"discovery","all","Steamed fish","",null,null,false,"0",1).getTotal());
        assertTrue(recipeMapper.searchByKeyword("Steamed fish",null).stream().anyMatch(item->item.getId().equals(r.getId())));
        db.update("UPDATE recipe SET visibility='private' WHERE id=?",r.getId());
        assertEquals(0,workflows.browse(outsider,null,"discovery","all","Steamed fish","",null,null,false,"0",1).getTotal());
        db.update("UPDATE recipe SET visibility='public',name='红烧鱼' WHERE id=?",r.getId());
        mvc.perform(get("/api/recipes/"+r.getId()).header("Accept-Language","en"))
            .andExpect(jsonPath("$.data.name").value("红烧鱼"));
        assertEquals("红烧鱼",db.queryForObject("SELECT name FROM recipe WHERE id=?",String.class,r.getId()));
    }

    @Test void chatTranslationsRequireMembershipAndNeverAcceptArbitraryText() throws Exception {
        var m=messages.sendMessage(group,owner,"不要放花生",Message.MessageType.text);
        String hash=com.familyrecipes.translation.TranslationService.digest(m.getContent());
        db.update("INSERT INTO content_translations(source_hash,target_language,engine,source_text,kind,status,translated_text) VALUES(?,'en','chat:llm::v1',?,'chat','ready','Do not add peanuts')",hash,m.getContent());
        String payload="{\"group_id\":"+group+",\"ids\":["+m.getId()+"]}";
        mvc.perform(post("/api/messages/translations").header("Authorization",ownerAuth).header("Accept-Language","en").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data."+m.getId()+".text").value("Do not add peanuts"));
        mvc.perform(post("/api/messages/translations").header("Authorization",otherAuth).header("Accept-Language","en").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isNotFound());
        assertEquals("不要放花生",messageMapper.findById(m.getId()).getContent());
    }

    @Autowired com.familyrecipes.controller.CatalogImportController catalog;
    @Test void catalogImportIsAdminOnlyAndIdempotent() throws Exception {
        org.springframework.test.util.ReflectionTestUtils.setField(catalog,"admin",owner);
        String payload="{\"provenance\":{\"source\":\"test\",\"source_id\":\"one\",\"source_url\":\"https://example.com\",\"author\":\"Test\",\"license\":\"CC0\",\"license_url\":\"https://creativecommons.org/publicdomain/zero/1.0/\"},\"recipe\":{\"name\":\"Test imported recipe\"},\"steps\":[{\"step_number\":1,\"description\":\"Cook\"}],\"ingredients\":[],\"tags\":[]}";
        try {
            mvc.perform(post("/api/catalog/import").header("Authorization",otherAuth).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isForbidden());
            String first=mvc.perform(post("/api/catalog/import").header("Authorization",ownerAuth).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            String second=mvc.perform(post("/api/catalog/import").header("Authorization",ownerAuth).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertEquals(json.readTree(first).get("data"),json.readTree(second).get("data"));
            long id=json.readTree(first).get("data").asLong();
            assertEquals("public",recipeMapper.findById(id).getVisibility());
            var adapted=workflows.adapt(id,owner,kitchen);
            assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM recipe_provenance WHERE recipe_id=?",Integer.class,adapted.getId()));
            mvc.perform(put("/api/recipes/"+id).header("Authorization",otherAuth).contentType(MediaType.APPLICATION_JSON).content("{\"recipe\":{\"name\":\"Changed\"}}"))
                .andExpect(status().isNotFound());
        } finally { org.springframework.test.util.ReflectionTestUtils.setField(catalog,"admin",0L); }
    }

    @Autowired com.familyrecipes.translation.TranslationService translationService;
    @Autowired com.familyrecipes.ai.AiRoutes aiRoutes;
    @Test void workerUsesConfiguredProviderAndDurableCache() throws Exception {
        var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        java.util.concurrent.atomic.AtomicInteger calls=new java.util.concurrent.atomic.AtomicInteger();
        server.createContext("/translate",exchange->{
            calls.incrementAndGet();
            byte[] bytes="{\"choices\":[{\"message\":{\"content\":\"Steamed fish\"}}]}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });server.start();
        org.springframework.test.util.ReflectionTestUtils.setField(translationService,"enabled",true);
        var oldRoute=aiRoutes.getRoutes().get("translation");
        var route=new com.familyrecipes.ai.AiRoutes.Route();
        route.setUrl("http://127.0.0.1:"+server.getAddress().getPort()+"/translate");route.setKey("test-only");route.setModel("fixture-model");
        aiRoutes.getRoutes().put("translation",route);
        try {
            assertEquals("pending",translationService.lookup("测试清蒸鱼","en","content",true).status());
            translationService.lookup("测试清蒸鱼","en","content",true);
            translationService.work();
            var result=translationService.lookup("测试清蒸鱼","en","content",true);
            assertEquals("ready",result.status());assertEquals("Steamed fish",result.text());assertEquals(1,calls.get());
            assertEquals("pending",translationService.lookup("修改后的清蒸鱼","en","content",true).status());
        } finally {
            server.stop(0);
            org.springframework.test.util.ReflectionTestUtils.setField(translationService,"enabled",false);
            aiRoutes.getRoutes().put("translation",oldRoute);
        }
    }

    @Test void receiptImportIsAtomicIdempotentAndMemberOnly() throws Exception {
        String id=UUID.randomUUID().toString();
        String body=json.writeValueAsString(Map.of("request_id",id,"kitchen_id",kitchen,"items",List.of(
            Map.of("name","receipt milk","quantity","2 cartons","storage","冷藏","estimated_days",7),
            Map.of("name","receipt rice","quantity","1kg","storage","常温","estimated_days",180))));
        mvc.perform(post("/api/receipts/import").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/receipts/import").header("Authorization",otherAuth).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNotFound());
        for(int i=0;i<2;i++)mvc.perform(post("/api/receipts/import").header("Authorization",ownerAuth).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.data").value(2));
        assertEquals(2,db.queryForObject("SELECT COUNT(*) FROM fridge_item WHERE kitchen_id=?",Integer.class,kitchen));
        mvc.perform(post("/api/receipts/import").header("Authorization",ownerAuth).contentType(MediaType.APPLICATION_JSON).content(body.replace("2 cartons","3 cartons"))).andExpect(status().isConflict());
        String invalid=body.replace(id,UUID.randomUUID().toString()).replace("180","0");
        mvc.perform(post("/api/receipts/import").header("Authorization",ownerAuth).contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
        assertEquals(2,db.queryForObject("SELECT COUNT(*) FROM fridge_item WHERE kitchen_id=?",Integer.class,kitchen));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM receipt_import WHERE user_id=?",Integer.class,owner));
    }
    @Test void receiptRecognitionRequiresMembershipAndConfiguration() throws Exception {
        var image=new MockMultipartFile("image","receipt.png","image/png",new byte[]{1});
        mvc.perform(multipart("/api/receipts/recognize").file(image).param("kitchenId",String.valueOf(kitchen))).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/receipts/recognize").file(image).param("kitchenId",String.valueOf(kitchen)).header("Authorization",otherAuth)).andExpect(status().isNotFound());
        mvc.perform(multipart("/api/receipts/recognize").file(image).param("kitchenId",String.valueOf(kitchen)).header("Authorization",ownerAuth)).andExpect(status().isServiceUnavailable());
    }

    @Test void receiptImagePassesThroughProviderAndReturnsEditableFields() throws Exception {
        var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        var captured=new java.util.concurrent.atomic.AtomicReference<String>();
        server.createContext("/chat",exchange->{
            captured.set(new String(exchange.getRequestBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
            String output="[{\"name\":\"牛奶\",\"quantity\":\"2盒\",\"storage\":\"冷藏\",\"estimated_days\":null}]";
            byte[] bytes=json.writeValueAsBytes(Map.of("choices",List.of(Map.of("finish_reason","stop","message",Map.of("content",output)))));
            exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });server.start();
        var old=aiRoutes.getRoutes().get("vision");var route=new com.familyrecipes.ai.AiRoutes.Route();
        route.setUrl("http://127.0.0.1:"+server.getAddress().getPort()+"/chat");route.setKey("test-only");route.setModel("glm-4.6v-flash");route.setThinkingType("disabled");
        aiRoutes.getRoutes().put("vision",route);
        try {
            var bytes=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(10,20,BufferedImage.TYPE_INT_RGB),"png",bytes);
            mvc.perform(multipart("/api/receipts/recognize").file(new MockMultipartFile("image","test.png","image/png",bytes.toByteArray())).param("kitchenId",String.valueOf(kitchen)).header("Authorization",ownerAuth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].name").value("牛奶")).andExpect(jsonPath("$.data[0].quantity").value("2盒"));
            var request=json.readTree(captured.get());assertEquals("disabled",request.path("thinking").path("type").asText());
            assertTrue(request.path("messages").get(0).path("content").get(1).path("image_url").path("url").asText().startsWith("data:image/png;base64,"));
            mvc.perform(multipart("/api/receipts/recognize").file(new MockMultipartFile("image","bad.png","image/png",new byte[]{1})).param("kitchenId",String.valueOf(kitchen)).header("Authorization",ownerAuth)).andExpect(status().isBadRequest());
        }finally{aiRoutes.getRoutes().put("vision",old);server.stop(0);}
    }
}
