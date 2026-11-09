package com.familyrecipes.android.data

import com.familyrecipes.android.data.local.FriendInvitations
import com.familyrecipes.android.data.model.FriendInvitation
import org.junit.Assert.*
import org.junit.Test

class FriendInvitationPolicyTest {
    private val items=listOf(FriendInvitation(10,2,1,"A",null),FriendInvitation(11,1,3,"B",null))
    @Test fun outgoingInvitationsNeverCountAsIncomingOrNotify() {
        assertEquals(listOf(10L),FriendInvitations.incoming(items,1).map { it.id })
        assertEquals(listOf(10L),FriendInvitations.unseen(items,1,emptySet()).map { it.id })
    }
    @Test fun seenRequestsDoNotAlertAgainAndNewRequestsStillDo() {
        assertTrue(FriendInvitations.unseen(items,1,setOf(10L)).isEmpty())
        assertEquals(listOf(12L),FriendInvitations.unseen(items+FriendInvitation(12,4,1,"C",null),1,setOf(10L)).map { it.id })
    }
    @Test fun accountSwitchFiltersForTheNewRecipient() {
        assertEquals(listOf(11L),FriendInvitations.incoming(items,3).map { it.id })
        assertTrue(FriendInvitations.incoming(items,4).isEmpty())
    }
}
