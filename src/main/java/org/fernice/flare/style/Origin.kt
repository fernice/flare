/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style

import org.fernice.std.PerEnumLazy

enum class Origin {

    UserAgent,

    User,

    Author;

    fun selfAndPrevious(): Iterator<Origin> {
        val indices = ordinal downTo 0
        return OriginIterator(indices.iterator())
    }
}

private class OriginIterator(
    private val iterator: Iterator<Int>,
) : Iterator<Origin> {

    override fun hasNext(): Boolean = iterator.hasNext()
    override fun next(): Origin = Origin.entries[iterator.next()]
}

typealias PerOrigin<E> = PerEnumLazy<Origin, E>

fun <E : Any> PerOrigin(initializer: (Origin) -> E): PerOrigin<E> = PerEnumLazy(initializer)
