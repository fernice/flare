/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style

import org.fernice.flare.selector.PSEUDO_COUNT
import org.fernice.flare.selector.PseudoElement

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
