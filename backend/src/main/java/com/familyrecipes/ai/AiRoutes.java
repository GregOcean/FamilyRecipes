package com.familyrecipes.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.*;

/** Independent model routes. Credentials deliberately have no generated toString(). */
@Component
@ConfigurationProperties(prefix="app.ai")
public class AiRoutes {
    public static final String TRANSLATION="translation", VISION="vision";
    private Map<String,Route> routes=new HashMap<>();
    public Map<String,Route> getRoutes(){return routes;}
    public void setRoutes(Map<String,Route> value){routes=value;}
    public Route route(String name) {
        if(!Set.of(TRANSLATION,VISION).contains(name))throw new IllegalArgumentException("Unknown AI route");
        return routes.computeIfAbsent(name,k->new Route());
    }
    public static class Route {
        private String url="",key="",model="",reasoningEffort="",thinkingType="";
        private int timeoutSeconds=120,maxTokens=8192;
        public boolean configured(){return !url.isBlank()&&!key.isBlank()&&!model.isBlank();}
        public String getUrl(){return url;} public void setUrl(String v){url=v;}
        public String getKey(){return key;} public void setKey(String v){key=v;}
        public String getModel(){return model;} public void setModel(String v){model=v;}
        public String getReasoningEffort(){return reasoningEffort;} public void setReasoningEffort(String v){reasoningEffort=v;}
        public String getThinkingType(){return thinkingType;}
        public void setThinkingType(String v){
            if(!Set.of("","enabled","disabled").contains(v))throw new IllegalArgumentException("Invalid thinking type");
            thinkingType=v;
        }
        public int getTimeoutSeconds(){return timeoutSeconds;} public void setTimeoutSeconds(int v){timeoutSeconds=v;}
        public int getMaxTokens(){return maxTokens;} public void setMaxTokens(int v){maxTokens=v;}
    }
}
