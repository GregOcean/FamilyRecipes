package com.familyrecipes.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket配置 (STOMP - 可选，目前使用RawWebSocketConfig)
 * 如果需要Web端使用STOMP，可以启用此配置
 */
@Configuration
// @EnableWebSocketMessageBroker // 暂时禁用，使用RawWebSocketConfig
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单的内存消息代理，用于将消息发送回客户端
        config.enableSimpleBroker("/topic", "/queue");
        // 定义客户端发送消息的前缀
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册STOMP端点，客户端连接到此端点
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // 允许所有来源
                .withSockJS(); // 启用SockJS fallback选项
    }
}

