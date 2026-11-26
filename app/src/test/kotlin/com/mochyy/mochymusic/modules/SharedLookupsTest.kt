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

    @Test fun abandonedRequestReleasesItsSlot() = runBlocking {
        val shared = SharedLookups<String, Int>(this, maxPending = 1)
        val started = CompletableDeferred<Unit>()
        val stopped = CompletableDeferred<Unit>()
        val request = async {
            shared.get("old") {
                started.complete(Unit)
                try {
                    CompletableDeferred<Int>().await()
                } finally {
                    stopped.complete(Unit)
                }
            }
        }
        started.await()
        request.cancelAndJoin()
        stopped.await()
        assertEquals(5, shared.get("new") { 5 })
    }

    @Test fun fullTableDoesNotRejectIndependentRequest() = runBlocking {
        val shared = SharedLookups<String, Int>(this, maxPending = 1)
        val gate = CompletableDeferred<Int>()
        val started = CompletableDeferred<Unit>()
        val first = async { shared.get("a") { started.complete(Unit); gate.await() } }
        started.await()
        assertEquals(8, shared.get("b") { 8 })
        gate.complete(1)
        assertEquals(1, first.await())
    }

    @Test fun differentKeysDoNotShareResponses() = runBlocking {
        val shared = SharedLookups<String, Int>(this)
        assertEquals(1, shared.get("a") { 1 })
        assertEquals(2, shared.get("b") { 2 })
    }
}
