package com.familyrecipes.service;

import com.familyrecipes.common.ApiException;
import com.familyrecipes.entity.Ingredient;
import com.familyrecipes.mapper.IngredientMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;

/** Display labels and supermarket product names never become identity keys. */
@Service
public class IngredientResolutionService {
    public record Entry(String text,Long ingredientId,String amount) {}
    public record Request(List<Entry> entries,boolean createUnmatched,String storage) {}
    public record Resolution(Ingredient ingredient,String name,String amount,String storage,Integer estimatedDays,
                             LocalDate expiryDate,boolean matched,String matchKind,List<Ingredient> candidates) {}
    public record EstimateRequest(Long ingredientId,String name,String storage) {}
    public record Estimate(Integer estimatedDays,String storage) {}
    private final IngredientMapper ingredients;private final JdbcTemplate db;private final ObjectMapper json;
    private final InventoryCategoryService categories;private final Clock clock;
    public IngredientResolutionService(IngredientMapper ingredients,JdbcTemplate db,ObjectMapper json,InventoryCategoryService categories,Clock clock){
        this.ingredients=ingredients;this.db=db;this.json=json;this.categories=categories;this.clock=clock;
    }
    private static final Map<String,String> ENGLISH=Map.ofEntries(Map.entry("milk","牛奶"),Map.entry("full cream milk","牛奶"),Map.entry("whole milk","牛奶"),Map.entry("fresh milk","牛奶"),Map.entry("egg","鸡蛋"),Map.entry("eggs","鸡蛋"),Map.entry("tomato","番茄"),Map.entry("tomatoes","番茄"),Map.entry("carrot","胡萝卜"),Map.entry("carrots","胡萝卜"),Map.entry("chicken breast","鸡胸肉"),Map.entry("chicken","鸡肉"),Map.entry("rice","大米"),Map.entry("potato","土豆"),Map.entry("potatoes","土豆"),Map.entry("onion","洋葱"),Map.entry("tofu","豆腐"),Map.entry("soy sauce","酱油"));
    private static final Set<String> BRANDS=Set.of("fairprice","meiji","magnolia","dutch lady","nestle","nestlé","yili","mengniu","伊利","蒙牛","光明","明治","金典","特仑苏");
    private static String normalize(String value){return java.text.Normalizer.normalize(value,java.text.Normalizer.Form.NFKC).trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);}
    public Ingredient exact(String name) {
        if(name==null||name.isBlank())return null;
        Ingredient found=ingredients.findByName(name.trim());if(found!=null && !Boolean.FALSE.equals(found.getCatalog()))return found;
        String english=ENGLISH.get(normalize(name));if(english!=null){found=ingredients.findByName(english);if(found!=null)return found;}
        // Only a unique ready translated label can resolve; never pick an arbitrary substring result.
        var ids=db.queryForList("SELECT DISTINCT i.id FROM ingredient i JOIN content_translations t ON t.source_hash=SHA2(i.name,256) WHERE i.is_catalog=1 AND t.kind='content' AND t.status='ready' AND LOWER(TRIM(t.translated_text))=? LIMIT 2",Long.class,normalize(name));
        if(ids.size()==1)return ingredients.findById(ids.get(0));
        return null;
    }
    public Ingredient match(String name,Long user) {
        Ingredient found=exact(name);if(found!=null)return found;
        if(user!=null){var ids=db.queryForList("SELECT ingredient_id FROM ingredient_user_alias WHERE user_id=? AND alias_text=?",Long.class,user,normalize(name));if(!ids.isEmpty())return ingredients.findById(ids.get(0));}
        // Remove recognized brand tokens only; do not turn chocolate milk or fish balls into raw milk/fish.
        String value=normalize(name);
        for(String brand:BRANDS) if(value.startsWith(brand) && (brand.matches(".*[\\p{IsHan}].*") || value.length()==brand.length() || value.charAt(brand.length())==' ')) {
            String rest=value.substring(brand.length()).trim();Ingredient candidate=exact(rest);if(candidate!=null)return candidate;
        }
        return null;
    }
    public Ingredient unresolved(String name) { return unresolved(name,null); }
    public Ingredient unresolved(String name,Long categoryId) {
        if(categoryId!=null && categories.getCategoryById(categoryId)==null)throw new ApiException(400,"分类不存在 / Category not found");
        Ingredient found=ingredients.findByName(name);if(found!=null)return found;
        Ingredient draft=new Ingredient();draft.setName(name);draft.setCatalog(false);draft.setCategoryId(categoryId!=null?categoryId:categories.smartMatchCategory(name));
        draft.setCategory(categories.getCategoryById(draft.getCategoryId()).getName());
        try{ingredients.insert(draft);}catch(org.springframework.dao.DuplicateKeyException e){return ingredients.findByName(name);}
        return draft;
    }
    @Transactional public List<Resolution> resolve(Long user,Request request) {
        if(request==null||request.entries()==null||request.entries().isEmpty()||request.entries().size()>100)throw new ApiException(400,"请选择 1–100 项 / Select 1–100 entries");
        var result=new ArrayList<Resolution>();
        for(Entry entry:request.entries()) {
            if(entry==null||entry.text()==null||entry.text().isBlank()||entry.text().length()>500)throw new ApiException(400,"食材信息须为 1–500 字 / Enter 1–500 characters");
            var parsed=IngredientTextParser.parse(entry.text(),LocalDate.now(clock));
            String name=parsed.name();if(name.isBlank()||name.length()>50)throw new ApiException(400,"请分开填写食材，每个名称不超过 50 字 / Separate foods; each name must be at most 50 characters");
            Ingredient selected=entry.ingredientId()==null?null:ingredients.findById(entry.ingredientId());
            if(entry.ingredientId()!=null&&selected==null)throw new ApiException(400,"食材不存在 / Food not found");
            Ingredient found=selected!=null?selected:match(name,user);
            boolean matched=found!=null&&!Boolean.FALSE.equals(found.getCatalog());
            List<Ingredient> candidates=matched?List.of():suggest(name);
            if(found==null&&request.createUnmatched())found=unresolved(name);
            if(request.createUnmatched() && selected!=null && user!=null && matched && !normalize(name).equals(normalize(selected.getName())))
                db.update("INSERT INTO ingredient_user_alias(user_id,alias_text,ingredient_id) VALUES(?,?,?) ON DUPLICATE KEY UPDATE ingredient_id=VALUES(ingredient_id)",user,normalize(name),selected.getId());
            String storage=IngredientShelfLife.storage(request.storage());
            Integer days=parsed.days()!=null?parsed.days():matched?IngredientShelfLife.estimate(found.getName(),storage):null;
            result.add(new Resolution(found,name,entry.amount()!=null?entry.amount():parsed.amount(),storage,days,parsed.expiryDate(),matched,selected!=null?"selected":matched?"catalog":"unresolved",candidates));
        }
        return result;
    }
    private List<Ingredient> suggest(String name) {
        String value=normalize(name);
        return ingredients.findAll().stream().filter(i->!Boolean.FALSE.equals(i.getCatalog())).filter(i->{
            if(value.contains(normalize(i.getName())) || normalize(i.getName()).contains(value))return true;
            return ENGLISH.entrySet().stream().anyMatch(e->e.getValue().equals(i.getName()) && value.matches(".*\\b"+java.util.regex.Pattern.quote(e.getKey())+"\\b.*"));
        }).sorted(Comparator.comparingInt((Ingredient i)->i.getName().length()).reversed()).limit(5).toList();
    }
    public Estimate estimate(EstimateRequest r){Ingredient i=r.ingredientId()==null?match(r.name(),null):ingredients.findById(r.ingredientId());String storage=IngredientShelfLife.storage(r.storage());return new Estimate(i==null?null:IngredientShelfLife.estimate(i.getName(),storage),storage);}
}
