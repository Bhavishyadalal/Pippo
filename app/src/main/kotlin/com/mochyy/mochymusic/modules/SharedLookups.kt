package com.mochyy.mochymusic.modules

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

/**
 * Coalesce identical concurrent requests without tying the producer to one waiter.
 * Results are not cached here: URL expiry, auth, and retry semantics stay with callers.
 * Inspired by BitChord's shared-request architecture, implemented for Pippo's module API.
 */
internal class SharedLookups<K, V>(
    private val scope: CoroutineScope,
    private val maxPending: Int = 32,
) {
    private val lock = Any()
    private val pending = HashMap<K, Deferred<V>>()

    suspend fun get(key: K, produce: suspend () -> V): V {
        val work = synchronized(lock) {
            pending[key]?.takeUnless { it.isCompleted } ?: run {
                check(pending.size < maxPending || pending.containsKey(key)) { "Too many pending stream lookups" }
                val next = scope.async(start = CoroutineStart.LAZY) { produce() }
                pending[key] = next
                next.invokeOnCompletion {
                    synchronized(lock) {
                        if (pending[key] === next) pending.remove(key)
                    }
                }
                next
            }
        }
        work.start()
        return work.await()
    }
}
