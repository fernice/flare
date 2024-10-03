/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.std

import java.util.EnumSet
import kotlin.enums.EnumEntries
import kotlin.enums.enumEntries

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified E : Enum<E>, T : Any> PerEnumLazy(noinline initializer: (E) -> T): PerEnumLazy<E, T> {
    return PerEnumLazy(enumEntries<E>(), initializer)
}

class PerEnumLazy<E : Enum<E>, T : Any>(
    private val entries: EnumEntries<E>,
    private val initializer: (E) -> T,
) : Iterable<PerEnumLazy.Entry<E, T>> {

    private val values = Array<Any?>(entries.size) { null }

    fun get(enum: E): T {
        check(entries.contains(enum)) { "unknown enum $enum" }

        var value = values[enum.ordinal]
        if (value == null) {
            value = initializer.invoke(enum)
            values[enum.ordinal] = value
        }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    fun peek(enum: E): T? {
        check(entries.contains(enum)) { "unknown enum $enum" }

        @Suppress("UNCHECKED_CAST")
        return values[enum.ordinal] as T?
    }

    override fun iterator(): Iterator<Entry<E, T>> = EntryIterator()

    private inner class EntryIterator : Iterator<Entry<E, T>> {

        private var index = 0

        override fun hasNext(): Boolean {
            var index = 0
            while (index < entries.size) {
                if (values[index] != null) return true
                index++
            }
            return false
        }

        override fun next(): Entry<E, T> {
            if (!hasNext()) throw NoSuchElementException()
            val enum = entries[index]

            @Suppress("UNCHECKED_CAST")
            val value = values[index] as T
            return Entry(enum, value)
        }
    }

    data class Entry<E : Enum<E>, T : Any>(val enum: E, val value: T)
}


inline fun <reified E : Enum<E>> EnumSet(): EnumSet<E> {
    return EnumSet.noneOf(E::class.java)
}

@Suppress("NOTHING_TO_INLINE")
inline fun <E : Enum<E>> EnumSet<E>.set(enum: E, set: Boolean): Boolean {
    return if (set) {
        add(enum)
    } else {
        remove(enum)
    }
}

fun <E : Enum<E>> EnumSet<E>.with(enum: E): EnumSet<E> {
    val result = EnumSet.copyOf(this)
    result.add(enum)
    return result
}
