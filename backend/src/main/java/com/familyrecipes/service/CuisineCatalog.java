package com.familyrecipes.service;

import com.fasterxml.jackson.databind.*;
import org.springframework.core.io.ClassPathResource;
import java.util.*;

/** Curated hierarchy, stable filter keys and non-destructive aliases for existing imports. */
public final class CuisineCatalog {
    private static final JsonNode ROOT;
    private static final Map<String,JsonNode> NODES=new LinkedHashMap<>();
    private static final Map<String,String> CANONICAL=new HashMap<>();
    static {
        try(var in=new ClassPathResource("cuisines.json").getInputStream()) {
            ROOT=new ObjectMapper().readTree(in);
            index(ROOT);
        } catch(Exception e){throw new ExceptionInInitializerError(e);}
    }
    private static void index(JsonNode nodes) {
        for(var n:nodes) {
            String key=n.path("value").asText();NODES.put(key,n);CANONICAL.put(key,key);
            for(var alias:n.path("aliases"))CANONICAL.put(alias.asText(),key);
            index(n.path("children"));
        }
    }
    public static boolean isKnown(String value) {return CANONICAL.containsKey(value);}
    public static List<String> labels() {return NODES.values().stream().filter(n->n.path("children").isEmpty()).map(n->n.path("value").asText()).toList();}
    public static String label(String value,boolean english) {
        var node=NODES.get(CANONICAL.get(value));return node==null?value:node.path(english?"en":"name").asText();
    }
    public static List<String> expand(String selected) {
        var node=NODES.get(CANONICAL.get(selected));
        if(node==null)return List.of(selected); // Keep historical custom tags searchable.
        Set<String> values=new LinkedHashSet<>();collect(node,values);return List.copyOf(values);
    }
    private static void collect(JsonNode node,Set<String> values) {
        values.add(node.path("value").asText());node.path("aliases").forEach(a->values.add(a.asText()));
        node.path("children").forEach(child->collect(child,values));
    }
    public static List<Map<String,Object>> tree(boolean english) {return display(ROOT,english);}
    private static List<Map<String,Object>> display(JsonNode nodes,boolean english) {
        List<Map<String,Object>> result=new ArrayList<>();
        for(var n:nodes) result.add(Map.of("value",n.path("value").asText(),"name",n.path(english?"en":"name").asText(),"aliases",expand(n.path("value").asText()),"children",display(n.path("children"),english)));
        return result;
    }
    private CuisineCatalog(){}
}
