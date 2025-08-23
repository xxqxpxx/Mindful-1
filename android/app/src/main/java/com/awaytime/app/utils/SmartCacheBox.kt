package com.awaytime.app.utils

/**
 * A generic time-aware Least Recently Used (LRU) cache.
 * Adapted from Mindful's SmartCacheBox implementation.
 *
 * It stores key-value pairs with a fixed capacity and evicts:
 * - The least recently used items when capacity is exceeded.
 * - Entries older than a specified time-to-live.
 */
class SmartCacheBox<K, V>(
    private val maxSize: Int,
    private val maxAgeMs: Long,
) {

    private data class TimedValue<V>(val value: V, var timestamp: Long)

    private val cache = LinkedHashMap<K, TimedValue<V>>()

    /**
     * Inserts or updates an entry in the cache.
     */
    @Synchronized
    fun put(key: K, value: V) {
        cache[key] = TimedValue(value, now())
        evictExpired()
        evictOverflow()
    }

    /**
     * Returns the value associated with the specified key if present and not expired.
     */
    @Synchronized
    fun get(key: K): V? = cache[key]?.value

    /**
     * Checks whether the cache contains the specified key and that it is not expired.
     */
    @Synchronized
    fun contains(key: K): Boolean = get(key) != null

    /**
     * Removes the mapping for a key from this cache if present.
     */
    @Synchronized
    fun remove(key: K) {
        cache.remove(key)
    }

    /**
     * Removes all entries from the cache.
     */
    @Synchronized
    fun clear() {
        cache.clear()
    }

    /**
     * Returns the number of currently valid (non-expired) items in the cache.
     */
    @Synchronized
    fun size(): Int {
        return cache.size
    }

    /**
     * Returns the current system time in milliseconds.
     */
    private fun now(): Long = System.currentTimeMillis()

    /**
     * Removes all expired entries based on the TTL.
     */
    private fun evictExpired() {
        val iterator = cache.entries.iterator()
        val currentTime = now()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (currentTime - entry.value.timestamp > maxAgeMs) {
                iterator.remove()
            }
        }
    }

    /**
     * Removes least recently used entries if the cache exceeds its size limit.
     */
    private fun evictOverflow() {
        while (cache.size > maxSize) {
            val iterator = cache.entries.iterator()
            if (iterator.hasNext()) {
                iterator.next()
                iterator.remove()
            }
        }
    }
}