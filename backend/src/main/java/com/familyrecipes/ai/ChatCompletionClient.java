package com.familyrecipes.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/** Common transport; each business route owns its prompt, validation and authorization. */
@Service
public class ChatCompletionClient {
    private final AiRoutes routes; private final ObjectMapper json;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    public ChatCompletionClient(AiRoutes routes,ObjectMapper json){this.routes=routes;this.json=json;}
    public String complete(String routeName,List<Map<String,Object>> messages) throws Exception {
        var route=routes.route(routeName);
        if(!route.configured())throw new IllegalStateException("AI route is not configured");
        URI uri=URI.create(route.getUrl());
        if(!"https".equals(uri.getScheme()) && !("http".equals(uri.getScheme()) && Set.of("127.0.0.1","localhost","::1","[::1]").contains(uri.getHost())))
            throw new IllegalArgumentException("AI endpoint must use HTTPS or loopback");
        var body=new LinkedHashMap<String,Object>();
        body.put("model",route.getModel());body.put("messages",messages);body.put("stream",false);
        body.put("temperature",0);body.put("max_tokens",route.getMaxTokens());
        if(!route.getThinkingType().isBlank())body.put("thinking",Map.of("type",route.getThinkingType()));
        if(!route.getReasoningEffort().isBlank())body.put("reasoning_effort",route.getReasoningEffort());
        var request=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(route.getTimeoutSeconds()))
            .header("Content-Type","application/json").header("Authorization","Bearer "+route.getKey())
            .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
        var response=http.send(request,HttpResponse.BodyHandlers.ofString());
        if(response.statusCode()!=200)throw new IllegalStateException("AI provider HTTP "+response.statusCode());
        var choice=json.readTree(response.body()).path("choices").path(0);
        if("length".equals(choice.path("finish_reason").asText()))throw new IllegalStateException("AI output truncated");
        String result=choice.path("message").path("content").asText();
        if(result.isBlank())throw new IllegalStateException("Empty AI response");
        return result;
    }
}
