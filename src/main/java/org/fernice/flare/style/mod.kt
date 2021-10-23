/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style

import org.fernice.flare.selector.PSEUDO_COUNT
import org.fernice.flare.selector.PseudoElement
import org.fernice.flare.style.stylesheet.Origin

class PerPseudoElementMap<E> {

    private val entries: Array<Any?> = arrayOfNulls(PSEUDO_COUNT)

    @Suppress("UNCHECKED_CAST")
    fun get(pseudoElement: PseudoElement): E? {
        return entries[pseudoElement.ordinal()] as E?
    }

    fun set(pseudoElement: PseudoElement, value: E) {
        entries[pseudoElement.ordinal()] = value
    }

    @Suppress("UNCHECKED_CAST")
    fun computeIfAbsent(pseudoElement: PseudoElement, insert: () -> E): E {
        var entry = entries[pseudoElement.ordinal()]

        if (entry == null) {
            entry = insert()
            entries[pseudoElement.ordinal()] = entry
        }

        return entry as E
    }

    fun clear() {
        for (i in 0..entries.size) {
            entries[i] = null
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun iterator(): Iterator<E?> {
        return entries.map { it as E? }.iterator()
    }
}

class PerOrigin<E>(
    val userAgent: E,
    val user: E,
    val author: E
) {

    fun get(origin: Origin): E {
        return when (origin) {
            Origin.USER_AGENT -> userAgent
            Origin.USER -> user
            Origin.AUTHOR -> author
        }
    }

    fun iterator(): PerOriginIterator<E> {
        val indices = Origin.values().indices
        return PerOriginIterator(this, indices.iterator())
    }

    fun reversedIterator(): PerOriginIterator<E> {
        val indices = Origin.values().indices.reversed()
        return PerOriginIterator(
            this,
            indices.iterator()
        )
    }
}

class PerOriginIterator<E>(
    private val perOrigin: PerOrigin<E>,
    private val iterator: Iterator<Int>
) : Iterator<E> {

    override fun hasNext(): Boolean = iterator.hasNext()

    override fun next(): E {
        val index = iterator.next()
        return perOrigin.get(Origin.values()[index])
    }
}
