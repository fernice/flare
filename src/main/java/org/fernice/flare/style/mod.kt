/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style

import org.fernice.flare.selector.PSEUDO_COUNT
import org.fernice.flare.selector.PseudoElement
import org.fernice.flare.std.iter.Iter
import org.fernice.flare.std.iter.iter
import org.fernice.flare.style.stylesheet.Origin
import fernice.std.None
import fernice.std.Option
import fernice.std.Some
import fernice.std.unwrap

class PerPseudoElementMap<E> {

    private val entries: Array<Option<E>> = Array(PSEUDO_COUNT) { None }

    fun get(pseudoElement: PseudoElement): Option<E> {
        return entries[pseudoElement.ordinal()]
    }

    fun set(pseudoElement: PseudoElement, value: E) {
        entries[pseudoElement.ordinal()] = Some(value)
    }

    fun computeIfAbsent(pseudoElement: PseudoElement, insert: () -> E): E {
        var entry = entries[pseudoElement.ordinal()]

        if (entry.isNone()) {
            entry = Some(insert())
            entries[pseudoElement.ordinal()] = entry
        }

        return entry.unwrap()
    }

    fun clear() {
        for (i in 0..entries.size) {
            entries[i] = None
        }
    }

    fun iter(): Iter<Option<E>> {
        return entries.iter()
    }
}

class PerOrigin<E>(val userAgent: E,
                   val user: E,
                   val author: E) {

    fun get(origin: Origin): E {
        return when (origin) {
            Origin.USER_AGENT -> userAgent
            Origin.USER -> user
            Origin.AUTHOR -> author
        }
    }

    fun iter(): PerOriginIter<E> {
        return PerOriginIter(
                this,
                0,
                false
        )
    }

    fun iterReversed(): PerOriginIter<E> {
        return PerOriginIter(
                this,
                0,
                false
        )
    }
}

class PerOriginIter<E>(private val perOrigin: PerOrigin<E>,
                       private var index: Int,
                       private val reversed: Boolean) : Iter<E> {

    override fun next(): Option<E> {
        if (reversed) {
            if (index == 0) {
                return None
            }
        } else {
            if (index == 3) {
                return None
            }
        }

        val value = perOrigin.get(Origin.values()[index])

        if (reversed) {
            index--
        } else {
            index++
        }

        return Some(value)
    }

    override fun clone(): PerOriginIter<E> {
        return PerOriginIter(
                perOrigin,
                index,
                reversed
        )
    }
}
