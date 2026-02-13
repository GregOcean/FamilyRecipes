package com.familyrecipes.config;

import com.familyrecipes.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类 - 配置静态资源访问和拦截器
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")  // 拦截所有API请求
                .excludePathPatterns(
                        "/api/auth/login",    // 排除登录
                        "/api/auth/register", // 排除注册
                        "/api/auth/refresh",  // 排除刷新token
                        "/api/upload/**",     // 排除文件上传（如果需要）
                        "/api/search/global", // 排除全局搜索
                        "/api/recipes/list",  // 排除菜谱列表
                        "/api/recipes/*/detail", // 排除菜谱详情
                        "/api/config/**"      // 排除配置接口
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源路径
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
        
        // APK文件下载 - 使用文件系统路径
        registry.addResourceHandler("/apk/**")
                .addResourceLocations("file:/opt/familyrecipes/apk/")
                .setCachePeriod(3600); // 缓存1小时
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 根路径重定向到index.html
        registry.addViewController("/").setViewName("forward:/index.html");
    }
}
