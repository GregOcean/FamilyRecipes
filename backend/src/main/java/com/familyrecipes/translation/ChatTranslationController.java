package com.familyrecipes.translation;

import com.familyrecipes.common.*;
import com.familyrecipes.mapper.*;
import com.familyrecipes.model.Message;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/messages/translations")
public class ChatTranslationController {
    private final MessageMapper messages;
    private final GroupChatMapper groups;
    private final TranslationService translations;
    public ChatTranslationController(MessageMapper messages,GroupChatMapper groups,TranslationService translations) {
        this.messages=messages;this.groups=groups;this.translations=translations;
    }
    public record Request(Long groupId,List<Long> ids) {}
    @PostMapping
    public Result<Map<Long,TranslationService.Translation>> translate(@RequestBody Request body,
        @RequestHeader(value="Accept-Language",defaultValue="zh") String language,HttpServletRequest req) {
        Long user=(Long)req.getAttribute("userId");
        if(body.groupId()==null || !groups.isGroupMember(body.groupId(),user)) throw new ApiException(404,"群组不存在或不可访问");
        if(body.ids()==null || body.ids().size()>100 || body.ids().contains(null)) throw new ApiException(400,"每次最多100条消息");
        var result=new LinkedHashMap<Long,TranslationService.Translation>();
        for(Long id:new LinkedHashSet<>(body.ids())) {
            var message=messages.findById(id);
            if(message==null || !body.groupId().equals(message.getGroupId())) throw new ApiException(404,"消息不存在或不可访问");
            if(message.getMessageType()==Message.MessageType.text)
                result.put(id,translations.lookup(message.getContent(),TranslationService.language(language),"chat",true));
        }
        return Result.success(result);
    }
}
