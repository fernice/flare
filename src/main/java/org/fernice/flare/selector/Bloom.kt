/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.selector

const val HASH_BLOOM_MASK = 0x00ffffff

private const val KEY_SIZE = 12
private const val KEY_MASK = (1 shl KEY_SIZE) - 1

private const val ARRAY_SIZE = 1 shl KEY_SIZE

sealed class BloomFilter(private val storage: BloomStorage) {

    fun insertHash(hash: Int) {
        storage.adjustFirstSlot(hash, true)
        storage.adjustSecondSlot(hash, true)
    }

    fun removeHash(hash: Int) {
        storage.adjustFirstSlot(hash, false)
        storage.adjustSecondSlot(hash, false)
    }

    fun mightContainHash(hash: Int): Boolean {
        return !storage.isFirstSlotEmpty(hash) && !storage.isSecondSlotEmpty(hash)
    }

    fun clear() {
        storage.clear()
    }
}

class CountingBloomFilter : BloomFilter(CountingBloomStorage())

class NonCountingBloomFilter : BloomFilter(NonCountingBloomStorage())

private fun hash1(hash: Int): Int {
    return hash and KEY_SIZE
}

private fun hash2(hash: Int): Int {
    return (hash shr KEY_SIZE) and KEY_MASK
}

interface BloomStorage {

    fun isSlotEmpty(index: Int): Boolean

    fun adjustSlot(index: Int, increment: Boolean)

    fun clear()

    fun isFirstSlotEmpty(hash: Int): Boolean {
        return isSlotEmpty(indexFirstSlot(hash))
    }

    fun isSecondSlotEmpty(hash: Int): Boolean {
        return isSlotEmpty(indexSecondSlot(hash))
    }

    fun adjustFirstSlot(hash: Int, increment: Boolean) {
        adjustSlot(indexFirstSlot(hash), increment)
    }

    fun adjustSecondSlot(hash: Int, increment: Boolean) {
        adjustSlot(indexSecondSlot(hash), increment)
    }

    fun indexFirstSlot(hash: Int): Int {
        return hash1(hash)
    }

    fun indexSecondSlot(hash: Int): Int {
        return hash2(hash)
    }
}

class CountingBloomStorage : BloomStorage {

    private val counters = ByteArray(ARRAY_SIZE)

    override fun isSlotEmpty(index: Int): Boolean {
        return counters[index] == (0x00).toByte()
    }

    override fun adjustSlot(index: Int, increment: Boolean) {
        val count = counters[index]

        if (count != (0xff).toByte()) {
            counters[index] = if (increment) {
                count.inc()
            } else {
                count.dec()
            }
        }
    }

    override fun clear() {
        for (i in counters.indices) {
            counters[i] = 0
        }
    }
}

class NonCountingBloomStorage : BloomStorage {

    private val flags = IntArray(ARRAY_SIZE / 32)

    override fun isSlotEmpty(index: Int): Boolean {
        val bit = 1 shl (index % 32)
        val bits = flags[index / 32]

        return (bits and bit) != 0
    }

    override fun adjustSlot(index: Int, increment: Boolean) {
        val bit = 1 shl (index % 32)
        val bits = flags[index / 32]

        if (increment) {
            flags[index / 32] = bits or bit
        }
    }

    override fun clear() {
        for (i in flags.indices) {
            flags[i] = 0
        }
    }
}

