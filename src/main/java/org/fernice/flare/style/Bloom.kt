/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style

import org.fernice.flare.dom.Element
import org.fernice.flare.selector.BloomFilter
import org.fernice.flare.selector.CountingBloomFilter
import java.util.Stack

class StyleBloom(
    private val filter: BloomFilter,
    private val elements: Stack<PushedElement>,
    private val pushedHashes: Stack<Int>,
) {

    companion object {
        fun new(): StyleBloom {
            return StyleBloom(
                CountingBloomFilter(),
                Stack(),
                Stack()
            )
        }
    }

    fun filter(): BloomFilter {
        return filter
    }

    private fun forEachHash(element: Element, function: (Int) -> Unit) {
        element.namespace?.let { namespace ->
            function(namespace.hashCode())
        }

        function(element.localName.hashCode())

        element.id?.let { id ->
            function(id.hashCode())
        }

        for (styleClass in element.classes) {
            function(styleClass.hashCode())
        }
    }

    fun push(element: Element) {
        var count = 0
        forEachHash(element) { hash ->
            filter.insertHash(hash)
            pushedHashes.push(hash)
            count++
        }

        elements.push(PushedElement(element, count))
    }

    fun pop(): Element? {
        val element = if (elements.isEmpty()) {
            return null
        } else {
            elements.pop()
        }

        for (i in 0 until element.hashes) {
            val hash = pushedHashes.pop()
            filter.removeHash(hash)
        }

        return element.element
    }

    fun clear() {
        elements.clear()
        filter.clear()
        pushedHashes.clear()
    }

    fun rebuild(element: Element) {
        val parentsReversed = mutableListOf<Element>()

        var current = element

        loop@
        while (true) {
            current = current.traversalParent ?: break

            parentsReversed.add(0, current)
        }

        for (parent in parentsReversed) {
            push(parent)
        }
    }

    fun rebuildUntil(element: Element, destination: Element) {
        val parentsReversed = mutableListOf<Element>()

        var current = element

        loop@
        while (true) {
            current = current.traversalParent ?: break

            parentsReversed.add(0, current)

            if (destination === current) break
        }

        for (parent in parentsReversed) {
            push(parent)
        }
    }

    fun currentParent(): Element? {
        return if (elements.isEmpty()) {
            null
        } else {
            elements.peek().element
        }
    }

    fun insertParent(element: Element) {
        if (elements.isEmpty()) {
            rebuild(element)
            return
        }

        val traversalParent = element.traversalParent ?: return clear()

        // Current parent is present as stack is not empty
        if (currentParent() === traversalParent) {
            return
        } else {
            // We have no use for that parent
            pop()
        }

        loop@
        while (true) {
            val currentParent = pop() ?: break@loop
            if (currentParent === traversalParent) {
                rebuildUntil(element, currentParent)
                return
            }
        }

        rebuild(element)
    }
}

class PushedElement(val element: Element, val hashes: Int)
