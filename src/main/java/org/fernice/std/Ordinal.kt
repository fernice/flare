/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.std

import java.util.Collections

interface Ordinal {
    val ordinal: Int
}

class OrdinalUniverse<O : Ordinal>(
    vararg values: O,
) : Iterable<O> {
    init {
        val continuous = values.asSequence().withIndex().all { (index, ordinal) -> index == ordinal.ordinal }
        require(continuous) { "universe of the ordinal must be continuous starting at 0" }
    }

    val values: List<O> = Collections.unmodifiableList(arrayListOf(*values))

    val size: Int
        get() = values.size

    operator fun contains(ordinal: O): Boolean = values.contains(ordinal)

    operator fun get(index: Int): O = values[index]

    override fun iterator(): Iterator<O> = values.iterator()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OrdinalUniverse<*>) return false

        if (values != other.values) return false

        return true
    }

    override fun hashCode(): Int {
        return values.hashCode()
    }

    override fun toString(): String = values.toString()
}

class PerOrdinal<O : Ordinal, E : Any>(
    val universe: OrdinalUniverse<O>,
    private val initializer: ((O) -> E)? = null,
) : Iterable<PerOrdinal.Entry<O, E>> {

    private val values = Array<Any?>(universe.size) { null }

    @Suppress("UNCHECKED_CAST")
    fun get(ordinal: O): E {
        check(universe.contains(ordinal)) { "unknown ordinal $ordinal" }

        var value = values[ordinal.ordinal] as E?
        if (value == null && initializer != null) {
            value = initializer.invoke(ordinal)
            values[ordinal.ordinal] = value
        }
        if (value == null) error("")
        return value
    }

    @Suppress("UNCHECKED_CAST")
    fun find(ordinal: O): E? {
        check(universe.contains(ordinal)) { "unknown ordinal $ordinal" }

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

    data class Entry<O : Ordinal, E : Any>(val ordinal: O, val value: E)
}


