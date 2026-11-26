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
    public record Item(String name, String quantity, String storage, Integer estimated_days,Long ingredient_id,LocalDate expiry_date) {
        public Item(String name,String quantity,String storage,Integer days){this(name,quantity,storage,days,null,null);}
    }
    public record SaveRequest(String requestId, Long kitchenId, List<Item> items) {}
    private final ChatCompletionClient client;
    private final AiRoutes routes;
    private final ObjectMapper json;
    private final KitchenService kitchens;
    private final FridgeService fridge;
    private final JdbcTemplate db;
    private final java.time.Clock clock;
    @org.springframework.beans.factory.annotation.Autowired private com.familyrecipes.service.IngredientResolutionService resolution;
    private final Semaphore slots=new Semaphore(2);
    public ReceiptService(ChatCompletionClient client,AiRoutes routes,ObjectMapper json,KitchenService kitchens,FridgeService fridge,JdbcTemplate db,java.time.Clock clock) {
        this.client=client;this.routes=routes;this.json=json;this.kitchens=kitchens;this.fridge=fridge;this.db=db;this.clock=clock;
    }
    public List<Item> recognize(Long user,Long kitchen,MultipartFile image) {return recognize(user,kitchen,image,"receipt");}
    public List<Item> recognize(Long user,Long kitchen,MultipartFile image,String mode) {
        if(!Set.of("receipt","item").contains(mode))throw new ApiException(400,"Invalid recognition mode");
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
            String prompt=new ClassPathResource(mode.equals("item")?"prompts/item-inventory.txt":"prompts/receipt-inventory.txt").getContentAsString(StandardCharsets.UTF_8);
            String url="data:image/"+format+";base64,"+Base64.getEncoder().encodeToString(bytes);
            var recognized=parse(client.complete(AiRoutes.VISION,List.of(Map.of("role","user","content",List.of(
                Map.of("type","text","text",prompt),Map.of("type","image_url","image_url",Map.of("url",url)))))));
            return normalize(recognized,user);
        } catch(ApiException e){throw e;} catch(Exception e){throw new ApiException(502,"识别失败，请重试 / Recognition failed; please retry",e);}
        finally {slots.release();}
    }

    /** Text-only path: the phone does OCR locally, so no image or vision quota is used. */
    public List<Item> recognizeText(Long user,Long kitchen,String text,String mode) {
        if(!"receipt".equals(mode))throw new ApiException(400,"文字识别仅支持小票 / Text recognition is only available for receipts");
        kitchens.requireMember(kitchen,user);
        if(!routes.route(AiRoutes.RECEIPT_TEXT).configured()) throw new ApiException(503,"小票文字识别尚未配置 / Receipt text recognition is not configured");
        String body=text==null?"":text.trim();
        if(body.isEmpty() || body.length()>20_000) throw new ApiException(400,"小票文字为空或过长 / Receipt text is empty or too long");
        if(!slots.tryAcquire()) throw new ApiException(429,"识别繁忙，请稍后重试 / Please retry shortly");
        try {
            String prompt=new ClassPathResource("prompts/receipt-text.txt").getContentAsString(StandardCharsets.UTF_8);
            var recognized=parse(client.complete(AiRoutes.RECEIPT_TEXT,List.of(Map.of("role","user","content",prompt+body))));
            return normalize(recognized,user);
        } catch(ApiException e){throw e;} catch(Exception e){throw new ApiException(502,"识别失败，请重试 / Recognition failed; please retry",e);}
        finally {slots.release();}
    }

    private List<Item> normalize(List<Item> recognized,Long user) {
        if(resolution==null)return recognized;
        var normalized=new ArrayList<Item>();
        for(var item:recognized) {
            var found=resolution.match(item.name(),user);
            Integer estimate=found==null?item.estimated_days():com.familyrecipes.service.IngredientShelfLife.estimate(found.getName(),item.storage());
            normalized.add(new Item(item.name(),item.quantity(),item.storage(),estimate!=null?estimate:item.estimated_days(),found==null?null:found.getId(),item.expiry_date()));
        }
        return normalized;
    }
    List<Item> parse(String content) throws Exception {
        String raw=content.trim().replaceFirst("^```(?:json)?\\s*","").replaceFirst("\\s*```$","");
        var node=json.readTree(raw);if(node.isObject())node=node.path("items");
        if(!node.isArray() || node.size()>100)throw new IllegalArgumentException("Invalid receipt items");
        var items=new ArrayList<Item>();
        for(var n:node) {
            String name=text(n.get("name"),50);if(name==null)continue;
            String storage=com.familyrecipes.service.IngredientShelfLife.storage(text(n.get("storage"),20));
            var days=n.get("estimated_days");Integer value=days!=null && days.canConvertToInt()?days.intValue():null;
            if(value!=null && (value<1 || value>3650))value=null;
            LocalDate expiry=null;
            try {var v=text(n.get("expiry_date"),10);if(v!=null)expiry=LocalDate.parse(v);}catch(java.time.DateTimeException ignored){}
            items.add(new Item(name,text(n.get("quantity"),50),storage,value,null,expiry));
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
            if(item==null || item.name()==null || item.name().isBlank() || item.name().length()>50 || item.quantity()!=null && item.quantity().length()>50 || item.storage()!=null && !item.storage().isBlank() && !Set.of("常温","冷藏","冷冻").contains(item.storage()) || item.estimated_days()!=null && (item.estimated_days()<1 || item.estimated_days()>3650))
                throw new ApiException(400,"请检查名称、数量、储存方式和天数 / Check the item fields");
        }
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(request)));
        db.update("INSERT IGNORE INTO receipt_import(user_id,request_id,kitchen_id,payload_hash,item_count) VALUES(?,?,?,?,0)",user,request.requestId(),request.kitchenId(),hash);
        var previous=db.queryForMap("SELECT payload_hash,item_count FROM receipt_import WHERE user_id=? AND request_id=? FOR UPDATE",user,request.requestId());
        if(!hash.equals(previous.get("payload_hash")))throw new ApiException(409,"请求内容已改变 / Retry payload changed");
        int count=((Number)previous.get("item_count")).intValue();if(count>0)return count;
        LocalDate today=LocalDate.now(clock);
        for(var source:request.items()) {
            var ingredient=resolution.resolve(user,new com.familyrecipes.service.IngredientResolutionService.Request(List.of(new com.familyrecipes.service.IngredientResolutionService.Entry(source.name().trim(),source.ingredient_id(),source.quantity())),true,source.storage())).get(0).ingredient();
            var item=new FridgeItem();item.setUserId(user);item.setKitchenId(request.kitchenId());item.setIngredientId(ingredient.getId());
            item.setDisplayName(source.name().trim());item.setAmount(source.quantity()==null?null:source.quantity().trim());item.setStorageLocation(source.storage());item.setPurchaseDate(today);
            item.setExpiryDate(source.expiry_date()!=null?source.expiry_date():source.estimated_days()==null?null:today.plusDays(source.estimated_days()));
            fridge.addItem(item);
        }
        count=request.items().size();db.update("UPDATE receipt_import SET item_count=? WHERE user_id=? AND request_id=?",count,user,request.requestId());return count;
    }
}
