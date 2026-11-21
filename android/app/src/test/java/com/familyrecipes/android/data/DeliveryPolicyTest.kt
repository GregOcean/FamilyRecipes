package com.familyrecipes.android.data

import com.familyrecipes.android.data.local.*
import org.junit.Assert.*
import org.junit.Test

class DeliveryPolicyTest {
    @Test fun failedRowsKeepOriginalOrderAfterPersistenceReplacement(){
        val first=PendingMessage("first",1,"first",sequence=10)
        val second=PendingMessage("second",1,"second",sequence=11)
        val replaced=listOf(second,first.copy(error="network",state="retrying",attempts=1))
        assertEquals(listOf("first","second"),DeliveryPolicy.ordered(replaced).map{it.clientId})
    }
    @Test fun permanentAndSessionErrorsNeverAutoRetry(){
        assertEquals("auth",DeliveryPolicy.failureKind(401))
        assertEquals("unavailable",DeliveryPolicy.failureKind(404))
        assertEquals("invalid",DeliveryPolicy.failureKind(400))
        for(state in listOf("failed","auth","unavailable"))assertFalse(DeliveryPolicy.canAutoSend(state,0,100))
        assertTrue(DeliveryPolicy.canAutoSend(null,0,100))
    }
    @Test fun retryWaitsAndBackoffIsBounded(){
        assertEquals("temporary",DeliveryPolicy.failureKind(429))
        assertEquals("temporary",DeliveryPolicy.failureKind(503))
        assertFalse(DeliveryPolicy.canAutoSend("retrying",200,100))
        assertTrue(DeliveryPolicy.canAutoSend("retrying",200,200))
        assertTrue(DeliveryPolicy.retryDelay(2)>DeliveryPolicy.retryDelay(1))
        assertEquals(300_000L,DeliveryPolicy.retryDelay(100))
    }
}
