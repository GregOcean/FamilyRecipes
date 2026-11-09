package com.familyrecipes.android.data

import com.familyrecipes.android.data.remote.ApiService
import com.familyrecipes.android.ui.design.Ui
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class FriendInvitationResponseTest {
    @Test fun invitationAndBothResponsesAcceptSuccessfulNullData() = runBlocking {
        val paths = mutableListOf<String>()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            paths.add(chain.request().url.encodedPath)
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK")
                .body("""{"code":200,"message":"success","data":null}"""
                    .toResponseBody("application/json".toMediaType())).build()
        }.build()
        val api = Retrofit.Builder().baseUrl("http://localhost/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build().create(ApiService::class.java)
        Ui.ok(api.addFriend(mapOf("friendId" to 2L)))
        Ui.ok(api.respondFriend(2L, mapOf("accept" to true)))
        Ui.ok(api.respondFriend(3L, mapOf("accept" to false)))
        Ui.ok(api.addGroupMembers(5L,mapOf("userIds" to listOf(2L,3L))))
        assertEquals(4, paths.size)
    }
}
