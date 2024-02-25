/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.std

import kotlin.reflect.KClass

interface Ordinal<O : Ordinal<O>> {
    val ordinal: Int
}


interface OrdinalUniverse<O : Ordinal<O>> : List<KClass<O>> {
    operator fun contains(element: O): Boolean = contains(element::class)
    fun indexOf(element: O): Int = indexOf(element::class)
}


inline fun <reified O : Ordinal<O>> ordinalUniverse(): OrdinalUniverse<O> {
    return ordinalUniverse { O::class.sealedSubclasses.toTypedArray() as Array<KClass<O>> }
}

inline fun <reified O> enumUniverse(): OrdinalUniverse<O> where O : Ordinal<O>, O : Enum<O> {
    return ordinalUniverse { enumValues<O>().map { it::class }.toTypedArray() as Array<KClass<O>> }
}

fun <O : Ordinal<O>> ordinalUniverse(entriesProvider: () -> Array<out KClass<O>>): OrdinalUniverse<O> {
    return OrdinalUniverseList(entriesProvider)
}

private class OrdinalUniverseList<O : Ordinal<O>>(
    private val entriesProvider: () -> Array<out KClass<O>>,
) : OrdinalUniverse<O>, AbstractList<KClass<O>>() {

    @Volatile
    private var _entries: Array<out KClass<O>>? = null
    private val entries: Array<out KClass<O>>
        get() {
            var entries = _entries
            if (entries == null) {
                entries = entriesProvider()
                _entries = entries
            }
            return entries
        }

    override val size: Int
        get() = entries.size

    override fun get(index: Int): KClass<O> {
        val entries = entries
        checkElementIndex(index, entries.size)
        return entries[index]
    }

    private fun checkElementIndex(index: Int, size: Int) {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException("index: $index, size: $size")
        }
    }
}

class PerOrdinal<O : Ordinal<O>, E : Any>(
    private val universe: OrdinalUniverse<O>,
    private val initializer: (O) -> E,
) : Iterable<PerOrdinal.Entry<O, E>> {

    private val values = Array<Any?>(universe.size) { null }

    fun get(ordinal: O): E {
        check(universe.contains(ordinal)) { "unknown ordinal $ordinal" }

        var value = values[ordinal.ordinal]
        if (value == null) {
            value = initializer.invoke(ordinal)
            values[ordinal.ordinal] = value
        }
        @Suppress("UNCHECKED_CAST")
        return value as E
    }

    fun find(ordinal: O): E? {
        check(universe.contains(ordinal)) { "unknown ordinal $ordinal" }

        @Suppress("UNCHECKED_CAST")
        return values[ordinal.ordinal] as E?
    }

    override fun iterator(): Iterator<Entry<O, E>> = EntryIterator()

    private inner class EntryIterator : Iterator<Entry<O, E>> {

        private var index = 0

        override fun hasNext(): Boolean {
            var index = 0
            while (index < universe.size) {
                if (values[index] != null) return true
                index++
            }
            return false
        }

        override fun next(): Entry<O, E> {
            if (!hasNext()) throw NoSuchElementException()
            val ordinal = universe[index]
            val value = values[index] as E
            return Entry(ordinal, value)
        }
    }

    data class Entry<O : Ordinal<O>, E : Any>(val ordinal: KClass<O>, val value: E)
}
