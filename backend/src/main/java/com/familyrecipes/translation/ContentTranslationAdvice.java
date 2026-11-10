package com.familyrecipes.translation;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import java.util.*;

/** Localize only authorized content responses; never identifiers, usernames or source URLs. */
@RestControllerAdvice
public class ContentTranslationAdvice implements ResponseBodyAdvice<Object> {
    private final ObjectMapper json;
    private final TranslationService translations;
    private static final Set<String> FIELDS=Set.of("name","description","title","notes","caption","amount","category_override_name","source_recipe_name","processing_method","unit","category");
    private static final Set<String> SKIP=Set.of("creator","sender","user","cooks","members","groups","kitchens");
    public ContentTranslationAdvice(ObjectMapper json,TranslationService translations) {this.json=json;this.translations=translations;}
    public boolean supports(MethodParameter p,Class<? extends HttpMessageConverter<?>> c) {return true;}
    public Object beforeBodyWrite(Object body,MethodParameter p,MediaType t,Class<? extends HttpMessageConverter<?>> c,ServerHttpRequest req,ServerHttpResponse res) {
        String path=req.getURI().getPath();
        if(path.equals("/api/recipes/cuisines/tree")) return body;
        if(body==null || !(path.startsWith("/api/recipes") || path.startsWith("/api/fridge") || path.startsWith("/api/inventory/categories") || path.startsWith("/api/meals") || path.startsWith("/api/search"))) return body;
        JsonNode tree=json.valueToTree(body);
        if(tree.path("code").asInt()!=200 || tree.path("data").isNull()) return body;
        boolean authenticated=req instanceof ServletServerHttpRequest s && s.getServletRequest().getAttribute("userId")!=null;
        boolean display=req.getHeaders().getFirst("Accept-Language")!=null && req.getMethod()==HttpMethod.GET && !"true".equals(req.getHeaders().getFirst("X-Content-Original"));
        String language=TranslationService.language(req.getHeaders().getFirst("Accept-Language"));
        localize(tree.path("data"),language,display,authenticated,new int[]{0});
        res.getHeaders().add("Vary","Accept-Language, X-Content-Original");
        return tree;
    }
    private void localize(JsonNode node,String language,boolean display,boolean enqueue,int[] budget) {
        if(node.isArray()) {node.forEach(n->localize(n,language,display,enqueue,budget));return;}
        if(!(node instanceof ObjectNode object)) return;
        var fields=new ArrayList<String>(); object.fieldNames().forEachRemaining(fields::add);
        ObjectNode originals=json.createObjectNode(); ObjectNode statuses=json.createObjectNode();
        for(String key:fields) {
            JsonNode value=object.get(key);
            if(SKIP.contains(key) || (key.equals("category") && !object.has("category_id"))) continue;
            if(key.equals("tag_value") && value.isTextual()) {
                String source=value.asText();
                if("cuisine".equals(object.path("tag_type").asText())) {
                    if(display)object.put("display_value",com.familyrecipes.service.CuisineCatalog.label(source,"en".equals(language)));
                    continue;
                }
                var tr=translations.lookup(source,language,"content",enqueue);
                if(display && "ready".equals(tr.status())) object.put("display_value",tr.text());
                continue;
            }
            if(value.isTextual() && FIELDS.contains(key) && budget[0]++<500) {
                String source=value.asText();
                if(key.equals("name") && object.has("value") && source.matches("[A-Z]{2}")) {
                    if(display) object.put(key,new Locale("",source).getDisplayCountry(Locale.forLanguageTag(language)));
                    continue;
                }
                // Prewarm the other language regardless of this reader's current language.
                String other=TranslationService.sourceLanguage(source).equals("zh")?"en":"zh";
                var translated=translations.lookup(source,other,"content",enqueue);
                if(display && other.equals(language)) {
                    originals.put(key,source); statuses.put(key,translated.status());
                    if("ready".equals(translated.status())) object.put(key,translated.text());
                }
            } else if(value.isContainerNode()) localize(value,language,display,enqueue,budget);
        }
        if(!originals.isEmpty()) {object.set("original_content",originals);object.set("translation_status",statuses);}
    }
}
