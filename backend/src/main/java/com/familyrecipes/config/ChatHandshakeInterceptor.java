package com.familyrecipes.config;

import com.familyrecipes.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.*;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import java.util.Map;

@Component
public class ChatHandshakeInterceptor implements HandshakeInterceptor {
    @Autowired private JwtUtil jwt;
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler handler, Map<String,Object> attributes) {
        String auth = request.getHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ") || !jwt.validateToken(auth.substring(7))) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        String token = auth.substring(7);
        attributes.put("userId", jwt.getUserIdFromToken(token));
        attributes.put("token", token);
        return true;
    }
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler handler, Exception exception) { }
}
