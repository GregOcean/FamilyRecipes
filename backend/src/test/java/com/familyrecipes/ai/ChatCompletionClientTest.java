package com.familyrecipes.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChatCompletionClientTest {
    @Test void visionDisablesThinkingWithoutChangingTranslationRoute() throws Exception {
        var json=new ObjectMapper();var captured=new AtomicReference<String>();
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/chat",exchange->{
            captured.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            byte[] bytes="{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"[]\"}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });server.start();
        try {
            var routes=new AiRoutes();var vision=routes.route(AiRoutes.VISION);
            vision.setUrl("http://127.0.0.1:"+server.getAddress().getPort()+"/chat");
            vision.setKey("test-only");vision.setModel("glm-4.6v-flash");vision.setThinkingType("disabled");vision.setMaxTokens(2048);vision.setTimeoutSeconds(45);
            assertEquals("[]",new ChatCompletionClient(routes,json).complete(AiRoutes.VISION,List.of(Map.of("role","user","content","receipt"))));
            var request=json.readTree(captured.get());
            assertEquals("disabled",request.path("thinking").path("type").asText());
            assertEquals(2048,request.path("max_tokens").asInt());
            assertEquals("",routes.route(AiRoutes.TRANSLATION).getThinkingType());
        }finally{server.stop(0);}
    }
}
