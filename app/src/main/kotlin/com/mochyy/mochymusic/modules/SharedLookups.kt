package com.mochyy.mochymusic.modules

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

/** Share in-flight work while at least one consumer still needs it. */
internal class SharedLookups<K, V>(
    private val scope: CoroutineScope,
    private val maxPending: Int = 32,
) {
    private class Entry<V>(val work: Deferred<V>, var waiters: Int = 0)
    private val lock = Any()
    private val pending = HashMap<K, Entry<V>>()

    suspend fun get(key: K, produce: suspend () -> V): V {
        val entry = synchronized(lock) {
            val existing = pending[key]?.takeUnless { it.work.isCompleted }
            val selected = existing ?: run {
                // A full speculative table must not reject foreground playback.
                // Unshared calls remain owned by their requesting coroutine.
                if (pending.size >= maxPending && !pending.containsKey(key)) null
                else {
                    val next = Entry(scope.async(start = CoroutineStart.LAZY) { produce() })
                    pending[key] = next
                    next.work.invokeOnCompletion {
                        synchronized(lock) {
                            if (pending[key] === next) pending.remove(key)
                        }
                    }
                    next
                }
            }
            selected?.also { it.waiters++ }
        } ?: return produce()
        try {
            entry.work.start()
            return entry.work.await()
        } finally {
            val cancel = synchronized(lock) {
                entry.waiters--
                if (entry.waiters == 0 && pending[key] === entry) {
                    pending.remove(key)
                    true
                } else false
            }
            if (cancel) entry.work.cancel()
        }
    }
}
