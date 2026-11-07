package com.familyrecipes.controller;
import com.familyrecipes.common.Result;
import com.familyrecipes.model.ChatShareRequest;
import com.familyrecipes.model.Message;
import com.familyrecipes.service.ChatShareService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
@RestController
public class ChatShareController {
    @Autowired ChatShareService service;
    @PostMapping("/api/recipes/{id}/share-chat")
    public Result<Message> share(@PathVariable Long id, @RequestBody ChatShareRequest body, HttpServletRequest request) {
        return Result.success(service.shareRecipe(id, body.getGroupId(), (Long) request.getAttribute("userId"), body.getClientMessageId()));
    }
}
