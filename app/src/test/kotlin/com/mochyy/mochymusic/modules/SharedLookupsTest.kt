package com.mochyy.mochymusic.modules

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SharedLookupsTest {
    @Test fun identicalWaitersShareWorkEvenWhenOneCancels() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val shared = SharedLookups<String, Int>(this)
        val first = async(start = CoroutineStart.UNDISPATCHED) {
            shared.get("a") { calls++; gate.await(); 42 }
        }
        val second = async(start = CoroutineStart.UNDISPATCHED) {
            shared.get("a") { calls++; -1 }
        }
        first.cancelAndJoin()
        gate.complete(Unit)
        assertEquals(42, second.await())
        assertEquals(1, calls)
        assertEquals(7, shared.get("a") { calls++; 7 })
        assertEquals(2, calls)
    }

    @Test fun differentKeysDoNotShareResponses() = runBlocking {
        val shared = SharedLookups<String, Int>(this)
        assertEquals(1, shared.get("a") { 1 })
        assertEquals(2, shared.get("b") { 2 })
    }
}
