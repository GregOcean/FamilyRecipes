package com.familyrecipes.ai;

import com.familyrecipes.common.ApiException;
import com.familyrecipes.entity.FridgeItem;
import com.familyrecipes.service.FridgeService;
import com.familyrecipes.service.KitchenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Semaphore;

@Service
public class ReceiptService {
    public record Item(String name, String quantity, String storage, Integer estimated_days) {}
    public record SaveRequest(String requestId, Long kitchenId, List<Item> items) {}
    private final ChatCompletionClient client;
    private final AiRoutes routes;
    private final ObjectMapper json;
    private final KitchenService kitchens;
    private final FridgeService fridge;
    private final JdbcTemplate db;
    private final java.time.Clock clock;
    private final Semaphore slots=new Semaphore(2);
    public ReceiptService(ChatCompletionClient client,AiRoutes routes,ObjectMapper json,KitchenService kitchens,FridgeService fridge,JdbcTemplate db,java.time.Clock clock) {
        this.client=client;this.routes=routes;this.json=json;this.kitchens=kitchens;this.fridge=fridge;this.db=db;this.clock=clock;
    }
    public List<Item> recognize(Long user,Long kitchen,MultipartFile image) {
        kitchens.requireMember(kitchen,user);
        if(!routes.route(AiRoutes.VISION).configured()) throw new ApiException(503,"小票识别尚未配置 / Receipt recognition is not configured");
        if(image.isEmpty() || image.getSize()>8*1024*1024) throw new ApiException(400,"请选择 8MB 以内的图片 / Select an image under 8MB");
        if(!slots.tryAcquire()) throw new ApiException(429,"识别繁忙，请稍后重试 / Please retry shortly");
        try {
            byte[] bytes=image.getBytes();
            String format;
            try(var stream=ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(bytes))) {
                var readers=ImageIO.getImageReaders(stream);
                if(!readers.hasNext())throw new ApiException(400,"请选择 JPG 或 PNG 图片 / Select JPG or PNG");
                var reader=readers.next();
                try {reader.setInput(stream);format=reader.getFormatName().toLowerCase(Locale.ROOT);
                    if(!Set.of("jpeg","png").contains(format) || (long)reader.getWidth(0)*reader.getHeight(0)>25_000_000)throw new ApiException(400,"图片格式或尺寸不支持 / Unsupported image");
                } finally {reader.dispose();}
            }
            String prompt=new ClassPathResource("prompts/receipt-inventory.txt").getContentAsString(StandardCharsets.UTF_8);
            String url="data:image/"+format+";base64,"+Base64.getEncoder().encodeToString(bytes);
            return parse(client.complete(AiRoutes.VISION,List.of(Map.of("role","user","content",List.of(
                Map.of("type","text","text",prompt),Map.of("type","image_url","image_url",Map.of("url",url)))))));
        } catch(ApiException e){throw e;} catch(Exception e){throw new ApiException(502,"识别失败，请重试 / Recognition failed; please retry",e);}
        finally {slots.release();}
    }
    List<Item> parse(String content) throws Exception {
        String raw=content.trim().replaceFirst("^```(?:json)?\\s*","").replaceFirst("\\s*```$","");
        var node=json.readTree(raw);if(node.isObject())node=node.path("items");
        if(!node.isArray() || node.size()>100)throw new IllegalArgumentException("Invalid receipt items");
        var items=new ArrayList<Item>();
        for(var n:node) {
            String name=text(n.get("name"),50);if(name==null)continue;
            String storage=text(n.get("storage"),20);
            if(storage!=null && !Set.of("常温","冷藏","冷冻").contains(storage))storage=null;
            var days=n.get("estimated_days");Integer value=days!=null && days.canConvertToInt()?days.intValue():null;
            if(value!=null && (value<1 || value>3650))value=null;
            items.add(new Item(name,text(n.get("quantity"),50),storage,value));
        }
        return items;
    }
    private String text(com.fasterxml.jackson.databind.JsonNode n,int max){if(n==null || n.isNull() || !n.isValueNode())return null;String s=n.asText().trim();return s.isEmpty()?null:s.substring(0,Math.min(s.length(),max));}

    /** Entire batch and retry receipt commit together, so a lost HTTP response cannot duplicate inventory. */
    @Transactional
    public int save(Long user,SaveRequest request) throws Exception {
        if(request==null || request.requestId()==null || !request.requestId().matches("[a-fA-F0-9-]{36}"))throw new ApiException(400,"Invalid request ID");
        kitchens.requireMember(request.kitchenId(),user);
        if(request.items()==null || request.items().isEmpty() || request.items().size()>100)throw new ApiException(400,"请选择 1–100 项 / Select 1–100 items");
        for(var item:request.items()) {
            if(item==null || item.name()==null || item.name().isBlank() || item.name().length()>50 || item.quantity()==null || item.quantity().isBlank() || item.quantity().length()>50 || item.storage()==null || !Set.of("常温","冷藏","冷冻").contains(item.storage()) || item.estimated_days()==null || item.estimated_days()<1 || item.estimated_days()>3650)
                throw new ApiException(400,"请填写名称、数量、储存方式和天数 / Complete all item fields");
        }
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(request)));
        db.update("INSERT IGNORE INTO receipt_import(user_id,request_id,kitchen_id,payload_hash,item_count) VALUES(?,?,?,?,0)",user,request.requestId(),request.kitchenId(),hash);
        var previous=db.queryForMap("SELECT payload_hash,item_count FROM receipt_import WHERE user_id=? AND request_id=? FOR UPDATE",user,request.requestId());
        if(!hash.equals(previous.get("payload_hash")))throw new ApiException(409,"请求内容已改变 / Retry payload changed");
        int count=((Number)previous.get("item_count")).intValue();if(count>0)return count;
        LocalDate today=LocalDate.now(clock);
        for(var source:request.items()) {
            var ingredient=fridge.getOrCreateIngredient(source.name().trim(),null,null);
            var item=new FridgeItem();item.setUserId(user);item.setKitchenId(request.kitchenId());item.setIngredientId(ingredient.getId());
            item.setAmount(source.quantity().trim());item.setStorageLocation(source.storage());item.setPurchaseDate(today);item.setExpiryDate(today.plusDays(source.estimated_days()));
            fridge.addItem(item);
        }
        count=request.items().size();db.update("UPDATE receipt_import SET item_count=? WHERE user_id=? AND request_id=?",count,user,request.requestId());return count;
    }
}
