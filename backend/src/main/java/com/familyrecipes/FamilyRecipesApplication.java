package com.familyrecipes;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 家庭菜谱管理系统 - 主启动类
 */
@SpringBootApplication
@MapperScan("com.familyrecipes.mapper")
public class FamilyRecipesApplication {

    public static void main(String[] args) {
        SpringApplication.run(FamilyRecipesApplication.class, args);
        System.out.println("========================================");
        System.out.println("家庭菜谱管理系统启动成功！");
        System.out.println("API文档地址: http://localhost:8080");
        System.out.println("========================================");
    }
}

