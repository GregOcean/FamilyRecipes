package com.familyrecipes.android.data

import com.familyrecipes.android.data.model.*
import com.familyrecipes.android.data.remote.ApiService
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ChatContentTest {
    @Test fun sameNamesStillMentionOnlyTheSelectedIdentity() {
        val content=ChatContent.mention(7,"小王")+" "+ChatContent.mention(8,"小王")
        assertTrue(ChatContent.mentions(content,7));assertTrue(ChatContent.mentions(content,8))
        assertFalse(ChatContent.mentions(content,9));assertEquals("@小王 @小王",ChatContent.plain(content))
        assertFalse(ChatContent.mentions("@小王",7))
    }
    @Test fun malformedIdsAndLabelsCannotCreateAnotherMention() {
        assertFalse(ChatContent.mentions("@[x](familyrecipes://user/99999999999999999999999999)",7))
        assertEquals("@名字  下一行",ChatContent.plain(ChatContent.mention(7,"名字]\n下一行")))
        assertNull(ChatContent.link("familyrecipes://recipe/99999999999999999999999999"))
        assertEquals(ChatContent.Link("meal",42),ChatContent.link("点菜：晚餐\nfamilyrecipes://meal/42"))
    }
    @Test fun retrofitCanSerializeSendAndShareRequestsWithoutWildcardFailure() = runBlocking {
        val bodies=mutableListOf<String>()
        val client=OkHttpClient.Builder().addInterceptor { chain ->
            val buffer=okio.Buffer();chain.request().body!!.writeTo(buffer);bodies.add(buffer.readUtf8())
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("{\"code\":200,\"data\":null}".toResponseBody("application/json".toMediaType())).build()
        }.build()
        val api=Retrofit.Builder().baseUrl("http://localhost/").client(client).addConverterFactory(GsonConverterFactory.create()).build().create(ApiService::class.java)
        api.sendMessage(SendMessageRequest(2,"hi","text","client-id"))
        api.shareMeal(3,ChatShareRequest(2,"share-id"))
        api.shareRecipeChat(4,ChatShareRequest(2,"recipe-id"))
        assertTrue(bodies[0].contains("\"group_id\":2"));assertTrue(bodies[0].contains("\"client_message_id\":\"client-id\""))
        assertTrue(bodies[1].contains("\"groupId\":2"));assertTrue(bodies[2].contains("recipe-id"))
    }
}
