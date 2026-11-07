package com.familyrecipes.model;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
@Data
public class ChatShareRequest {
    @JsonAlias("groupId") private Long groupId;
    @JsonAlias("clientMessageId") private String clientMessageId;
}
