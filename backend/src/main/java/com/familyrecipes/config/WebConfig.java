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
                .excludePathPatterns("/api/auth/login", "/api/auth/register", "/api/auth/registration-code");
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
