package com.familyrecipes.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
public class User {
    private Long id;
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    private String email;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String password;
    private String username;
    private String avatar;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

