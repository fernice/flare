/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.std

fun <T> List<T>.asReversedSequence(): Sequence<T> {
    return ReversedListSequence(this)
}

private class ReversedListSequence<T>(
    private val list: List<T>,
) : Sequence<T> {
    override fun iterator(): Iterator<T> {
        return object : Iterator<T> {
            private val iterator = list.listIterator(list.size)

            override fun hasNext(): Boolean = iterator.hasPrevious()

            override fun next(): T = iterator.previous()
        }
    }
}


fun <E> List<E>.resized(): List<E> {
    return ArrayList(this)
}

fun <E> MutableCollection<E>.drain(): List<E> {
    val list = toList()
    clear()
    return list
}

fun <E> MutableList<E>.truncate(size: Int) {
    if (this.size > size){
        subList(size, this.size).clear()
    }
}
