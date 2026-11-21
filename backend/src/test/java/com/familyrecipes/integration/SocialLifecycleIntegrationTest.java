package com.familyrecipes.integration;

import com.familyrecipes.common.ApiException;
import com.familyrecipes.mapper.GroupChatMapper;
import com.familyrecipes.mapper.MessageMapper;
import com.familyrecipes.model.Message;
import com.familyrecipes.service.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"app.ai.routes.translation.model=","app.catalog.admin-user-id=0"})
@Transactional
@EnabledIfEnvironmentVariable(named="FOUNDATION_DB_TEST",matches="true")
class SocialLifecycleIntegrationTest {
    @Autowired JdbcTemplate db;
    @Autowired GroupChatService groups;
    @Autowired GroupChatMapper groupMapper;
    @Autowired FriendshipService friends;
    @Autowired MessageService messages;
    @Autowired MessageMapper messageMapper;
    @Autowired KitchenService kitchens;
    private long owner,member,other,group;
    private long user(){
        String id=UUID.randomUUID().toString();
        db.update("INSERT INTO user(username,email,password) VALUES(?,?,?)",id,id+"@test.example","test-only");
        return db.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
    }
    @BeforeEach void setup(){owner=user();member=user();other=user();group=groups.createGroup(owner,"test group","",10,List.of()).getId();}
    private void befriend(long a,long b){friends.addFriend(a,b);friends.respond(b,a,true);}
    @Test void messageTimePreservesDatabaseInstantAndIncludesOffsetInJson() throws Exception {
        var message=messages.sendMessage(group,owner,"time contract",Message.MessageType.text);
        Long seconds=db.queryForObject("SELECT UNIX_TIMESTAMP(created_at) FROM message WHERE id=?",Long.class,message.getId());
        assertEquals(seconds.longValue(),message.getCreatedAt().getEpochSecond());
        var json=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules()
            .setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE)
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        assertEquals(message.getCreatedAt().toString(),json.readTree(json.writeValueAsString(message)).get("created_at").asText());
    }
    @Test void englishFirstUseCreatesEnglishDefaultsButNeverRenamesCustomContent(){
        var previous=org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        var request=new org.springframework.mock.web.MockHttpServletRequest();
        request.addHeader("Accept-Language","en-US");
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(new org.springframework.web.context.request.ServletRequestAttributes(request));
        try {
            var kitchen=kitchens.list(other).get(0);
            assertEquals("My kitchen",kitchen.getName());
            assertEquals("My kitchen group",groupMapper.getGroupById(kitchen.getGroupId()).getName());
            kitchens.rename(kitchen.getId(),"My own name",other);
            assertEquals("My own name",kitchens.list(other).get(0).getName());
            long created=groups.createGroup(other,"Friends","",10,List.of()).getId();
            assertEquals("Home kitchen",kitchens.list(other).stream().filter(k->k.getGroupId().equals(created)).findFirst().orElseThrow().getName());
        }finally {
            org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(previous);
        }
    }
    @Test void ownershipTransferIsAtomicAndKeepsExactlyOneManager(){
        befriend(owner,member);groups.addGroupMember(group,owner,List.of(member));
        assertThrows(ApiException.class,()->groups.transferManager(group,member,owner));
        assertThrows(ApiException.class,()->groups.transferManager(group,owner,other));
        groups.transferManager(group,owner,member);
        assertEquals(member,groupMapper.getGroupById(group).getManagerId());
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM group_member WHERE group_id=? AND role='manager'",Integer.class,group));
        assertThrows(ApiException.class,()->kitchens.create(group,"denied",owner));
        assertNotNull(kitchens.create(group,"new kitchen",member));
        groups.leaveGroup(group,owner);assertFalse(groupMapper.isGroupMember(group,owner));
    }
    @Test void joiningDoesNotCountPreJoinHistoryAndUpdateRejectsBlankNames(){
        messages.sendMessage(group,owner,"old message",Message.MessageType.text);
        long previous=messageMapper.getLastMessage(group).getId();
        befriend(owner,member);groups.addGroupMember(group,owner,List.of(member));
        assertEquals(previous,messageMapper.getReadPosition(member,group).getLastReadMessageId());
        assertEquals(1,messages.getUnreadCount(group,member)); // Only the joining system event.
        assertThrows(ApiException.class,()->groups.updateGroup(group,owner,"  ",""));
        assertThrows(ApiException.class,()->groups.updateGroup(group,owner,"x".repeat(101),""));
        groups.updateGroup(group,owner," renamed ","");assertEquals("renamed",groupMapper.getGroupById(group).getName());
        groups.removeGroupMember(group,owner,member);
        assertThrows(ApiException.class,()->messages.history(group,member,Long.MAX_VALUE,50));
    }
    @Test void relationshipSearchAndPendingCancellationHaveSeparateSemantics(){
        assertEquals("none",friends.search(owner,""+member).get(0).get("relationship"));
        friends.addFriend(owner,member);
        assertEquals("outgoing",friends.search(owner,""+member).get(0).get("relationship"));
        assertEquals("incoming",friends.search(member,""+owner).get(0).get("relationship"));
        assertThrows(ApiException.class,()->friends.cancelInvitation(member,owner));
        friends.respond(member,owner,true);
        assertThrows(ApiException.class,()->friends.cancelInvitation(owner,member));
        assertTrue(friends.isFriend(owner,member));
        assertEquals("accepted",friends.search(owner,""+member).get(0).get("relationship"));
        friends.addFriend(owner,other);friends.cancelInvitation(owner,other);
        assertTrue(friends.invitations(owner).isEmpty());
    }
    @Test void historyUsesStableCursorWhileNewMessagesArrive(){
        for(int i=0;i<65;i++)messages.sendMessage(group,owner,"msg "+i,Message.MessageType.text);
        var latest=messages.history(group,owner,Long.MAX_VALUE,50);
        assertEquals(50,latest.size());
        long oldest=latest.get(latest.size()-1).getId();
        messages.sendMessage(group,owner,"concurrent new",Message.MessageType.text);
        var older=messages.history(group,owner,oldest,50);
        assertEquals(16,older.size()); // 15 older messages plus the creation system event.
        assertTrue(older.stream().allMatch(m->m.getId()<oldest));
        assertThrows(ApiException.class,()->messages.history(group,other,Long.MAX_VALUE,50));
    }
}
