package com.familyrecipes.websocket;
import com.familyrecipes.mapper.GroupChatMapper;
import com.familyrecipes.service.MessageService;
import com.familyrecipes.util.JwtUtil;
import com.familyrecipes.model.Message;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.*;
import java.time.LocalDateTime;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ChatWebSocketHandlerTest {
    ChatWebSocketHandler handler;
    MessageService service;
    GroupChatMapper groups;
    JwtUtil jwt;
    WebSocketSession session;
    @BeforeEach void setup() {
        handler=new ChatWebSocketHandler();service=mock(MessageService.class);groups=mock(GroupChatMapper.class);jwt=mock(JwtUtil.class);
        session=mock(WebSocketSession.class);
        Map<String,Object> attributes=new HashMap<>();attributes.put("userId",7L);attributes.put("token","valid");
        when(session.getAttributes()).thenReturn(attributes);when(session.isOpen()).thenReturn(true);
        when(jwt.validateToken("valid")).thenReturn(true);
        ReflectionTestUtils.setField(handler,"messageService",service);ReflectionTestUtils.setField(handler,"groups",groups);ReflectionTestUtils.setField(handler,"jwt",jwt);
        ReflectionTestUtils.setField(handler,"objectMapper",new ObjectMapper().findAndRegisterModules().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
    }
    @Test void nonMemberCannotSubscribe() throws Exception {
        handler.handleTextMessage(session,new TextMessage("{\"command\":\"SUBSCRIBE\",\"destination\":\"/topic/group/2\"}"));
        assertNull(session.getAttributes().get("groupId"));
        var output=ArgumentCaptor.forClass(WebSocketMessage.class);verify(session).sendMessage(output.capture());
        assertTrue(output.getValue().getPayload().toString().contains("ERROR"));
    }
    @Test void senderComesFromHandshakeAndBroadcastUsesAndroidFieldNames() throws Exception {
        when(groups.isGroupMember(2L,7L)).thenReturn(true);
        handler.handleTextMessage(session,new TextMessage("{\"command\":\"SUBSCRIBE\",\"destination\":\"/topic/group/2\"}"));
        Message saved=new Message();saved.setId(9L);saved.setGroupId(2L);saved.setSenderId(7L);saved.setContent("hello");saved.setCreatedAt(LocalDateTime.of(2026,11,1,10,0));
        when(service.sendMessage(2L,7L,"hello",Message.MessageType.text)).thenReturn(saved);
        handler.handleTextMessage(session,new TextMessage("{\"command\":\"SEND\",\"body\":{\"groupId\":2,\"senderId\":999,\"content\":\"hello\"}}"));
        verify(service).sendMessage(2L,7L,"hello",Message.MessageType.text);
        handler.onMessage(saved); // committed-event delivery also covers HTTP sends
        var output=ArgumentCaptor.forClass(WebSocketMessage.class);verify(session,times(2)).sendMessage(output.capture());
        String payload=output.getValue().getPayload().toString();assertTrue(payload.contains("\"sender_id\":7"));assertTrue(payload.contains("\"created_at\":\"2026-11-01T10:00:00\""));
    }
    @Test void expiredSessionCannotSend() throws Exception {
        when(jwt.validateToken("valid")).thenReturn(false);
        handler.handleTextMessage(session,new TextMessage("{\"command\":\"SEND\"}"));
        verify(session).close(CloseStatus.POLICY_VIOLATION);verifyNoInteractions(service);
    }
}
