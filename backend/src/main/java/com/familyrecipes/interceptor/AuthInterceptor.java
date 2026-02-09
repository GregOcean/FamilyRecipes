package com.familyrecipes.interceptor;

import com.familyrecipes.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证拦截器
 * 
 * 开发模式：暂时允许不登录访问，使用默认用户ID
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);
    
    // 开发模式：使用默认用户ID（设置为 1L）
    private static final Long DEFAULT_USER_ID = 1L;
    private static final boolean DEV_MODE = true; // 开发模式开关

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        
        logger.debug("拦截请求: {} {}", method, requestURI);
        
        // OPTIONS请求直接放行
        if ("OPTIONS".equals(method)) {
            logger.debug("OPTIONS请求，直接放行");
            return true;
        }

        // 获取token
        String authHeader = request.getHeader("Authorization");
        logger.debug("Authorization Header: {}", authHeader);
        
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            logger.debug("提取到Token: {}", token.substring(0, Math.min(20, token.length())) + "...");
        }

        // ===== 开发模式：允许不登录访问 =====
        if (DEV_MODE) {
            if (token == null || token.isEmpty()) {
                // 没有 token，使用默认用户ID
                logger.info("⚠️ 开发模式：未提供Token，使用默认用户ID: {}", DEFAULT_USER_ID);
                request.setAttribute("userId", DEFAULT_USER_ID);
                return true;
            } else {
                // 有 token，尝试验证
                try {
                    if (jwtUtil.validateToken(token)) {
                        Long userId = jwtUtil.getUserIdFromToken(token);
                        request.setAttribute("userId", userId);
                        logger.debug("Token验证成功，用户ID: {}", userId);
                        return true;
                    } else {
                        // token 无效，使用默认用户ID
                        logger.info("⚠️ 开发模式：Token无效，使用默认用户ID: {}", DEFAULT_USER_ID);
                        request.setAttribute("userId", DEFAULT_USER_ID);
                        return true;
                    }
                } catch (Exception e) {
                    logger.warn("Token解析失败，使用默认用户ID: {}", e.getMessage());
                    request.setAttribute("userId", DEFAULT_USER_ID);
                    return true;
                }
            }
        }
        // ===== 开发模式结束 =====

        // 生产模式：严格验证token
        if (token == null) {
            logger.warn("请求 {} 没有提供Token", requestURI);
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\"}");
            return false;
        }
        
        if (!jwtUtil.validateToken(token)) {
            logger.warn("请求 {} 的Token无效或已过期", requestURI);
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\"}");
            return false;
        }

        // 将用户ID存入request，方便后续使用
        Long userId = jwtUtil.getUserIdFromToken(token);
        request.setAttribute("userId", userId);
        logger.debug("Token验证成功，用户ID: {}", userId);

        return true;
    }
}

