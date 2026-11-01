package com.familyrecipes.interceptor;

import com.familyrecipes.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Autowired private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equals(request.getMethod())) return true;
        String header = request.getHeader("Authorization");
        if (header != null) {
            if (header.startsWith("Bearer ") && jwtUtil.validateToken(header.substring(7))) {
                request.setAttribute("userId", jwtUtil.getUserIdFromToken(header.substring(7)));
                return true;
            }
            return reject(response);
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean publicRead = "GET".equals(request.getMethod()) && (
            path.equals("/api/recipes/browse") || path.equals("/api/recipes/search") || path.matches("/api/recipes/[0-9]+") ||
            path.equals("/api/recipes/recently-cooked") || path.startsWith("/api/recipes/tags/") ||
            path.equals("/api/search/global") || path.startsWith("/api/config/") ||
            path.startsWith("/api/upload/files/") || path.startsWith("/api/inventory/categories/"));
        return publicRead || reject(response);
    }

    private boolean reject(HttpServletResponse response) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\"}");
        return false;
    }
}
