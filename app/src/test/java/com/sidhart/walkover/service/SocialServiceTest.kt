package com.sidhart.walkover.service

import com.sidhart.walkover.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class SocialServiceTest {

    // Extended Fake to support Social methods
    class FakeSocialFirebaseService : FirebaseService() {
        val users = mutableMapOf<String, User>()
        val friendRequests = mutableListOf<FriendRequest>()
        val friends = mutableMapOf<String, MutableList<Friend>>()

        override suspend fun getUserProfile(userId: String): Result<User?> {
            return Result.success(users[userId])
        }
        
        override suspend fun sendFriendRequest(request: FriendRequest): Result<Unit> {
            friendRequests.add(request)
            return Result.success(Unit)
        }
        
        override suspend fun getFriendRequests(userId: String): Result<List<FriendRequest>> {
            return Result.success(friendRequests.filter { it.toUserId == userId && it.status == RequestStatus.PENDING })
        }
        
        override suspend fun updateFriendRequestStatus(requestId: String, status: RequestStatus): Result<Unit> {
            val req = friendRequests.find { it.id == requestId }
            if (req != null) {
                // In immutable list world, we replace. In mutable list world, we update reference if var? 
                // data class is immutable. So remove and add.
                friendRequests.remove(req)
                friendRequests.add(req.copy(status = status))
                return Result.success(Unit)
            }
            return Result.failure(Exception("Not found"))
        }
        
        override suspend fun addFriendship(user1: Friend, user2: Friend): Result<Unit> {
            friends.getOrPut(user1.userId) { mutableListOf() }.add(user2)
            friends.getOrPut(user2.userId) { mutableListOf() }.add(user1)
            return Result.success(Unit)
        }
        
        override suspend fun getFriends(userId: String): Result<List<Friend>> {
            return Result.success(friends[userId] ?: emptyList())
        }
        
        override suspend fun searchUsers(query: String): Result<List<User>> {
            return Result.success(users.values.filter { it.username.startsWith(query) })
        }
    }

    private fun createService(): Pair<SocialService, FakeSocialFirebaseService> {
        val fakeFirebase = FakeSocialFirebaseService()
        val service = SocialService(fakeFirebase)
        return Pair(service, fakeFirebase)
    }

    @Test
    fun sendFriendRequest_createsPendingRequest() = runBlocking {
        val (service, fakeFirebase) = createService()
        
        // Setup users
        val u1 = User(id = "u1", username = "Alice")
        val u2 = User(id = "u2", username = "Bob")
        fakeFirebase.users["u1"] = u1
        fakeFirebase.users["u2"] = u2
        
        // Send request U1 -> U2
        val result = service.sendFriendRequest(u1, "u2")
        assertTrue(result.isSuccess)
        
        // Check DB
        val requests = fakeFirebase.getFriendRequests("u2").getOrThrow()
        assertEquals(1, requests.size)
        assertEquals("u1", requests[0].fromUserId)
        assertEquals(RequestStatus.PENDING, requests[0].status)
    }
    
    @Test
    fun acceptFriendRequest_createsFriendship() = runBlocking {
        val (service, fakeFirebase) = createService()
        
        // Setup users
        val u1 = User(id = "u1", username = "Alice")
        val u2 = User(id = "u2", username = "Bob")
        fakeFirebase.users["u1"] = u1
        fakeFirebase.users["u2"] = u2
        
        // Create pending request
        val req = FriendRequest(
            id = "req1",
            fromUserId = "u1",
            toUserId = "u2",
            status = RequestStatus.PENDING
        )
        fakeFirebase.friendRequests.add(req)
        
        // Accept
        val result = service.acceptFriendRequest(req)
        assertTrue(result.isSuccess)
        
        // Verify Request Status Updated
        val updatedReq = fakeFirebase.friendRequests.find { it.id == "req1" }
        assertEquals(RequestStatus.ACCEPTED, updatedReq!!.status)
        
        // Verify Friend Lists
        val friends1 = service.getFriends("u1")
        val friends2 = service.getFriends("u2")
        
        assertEquals(1, friends1.size)
        assertEquals("u2", friends1[0].userId)
        
        assertEquals(1, friends2.size)
        assertEquals("u1", friends2[0].userId)
    }
    
    @Test
    fun searchUsers_returnsMatches() = runBlocking {
        val (service, fakeFirebase) = createService()
        
        fakeFirebase.users["u1"] = User(id = "u1", username = "TestUser1")
        fakeFirebase.users["u2"] = User(id = "u2", username = "TestUser2")
        fakeFirebase.users["u3"] = User(id = "u3", username = "OtherUser")
        
        val results = service.searchUsers("Test")
        assertEquals(2, results.size)
        assertTrue(results.any { it.username == "TestUser1" })
        assertTrue(results.any { it.username == "TestUser2" })
    }
    
    @Test
    fun sendFriendRequest_preventSelfAndDuplicates() = runBlocking {
        val (service, fakeFirebase) = createService()
        val u1 = User(id = "u1", username = "Alice")
        
        // Self check
        val selfResult = service.sendFriendRequest(u1, "u1")
        assertTrue(selfResult.isFailure)
        
        // Already friend check
        val u2 = User(id = "u2", username = "Friend")
        fakeFirebase.users["u2"] = u2
        fakeFirebase.addFriendship(
             Friend(userId = "u1"), 
             Friend(userId = "u2")
        )
        
        val duplicateResult = service.sendFriendRequest(u1, "u2")
        assertTrue(duplicateResult.isFailure)
        assertEquals("Already friends", duplicateResult.exceptionOrNull()?.message)
    }
}
