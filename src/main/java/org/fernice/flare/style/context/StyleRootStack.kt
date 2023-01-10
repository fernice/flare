/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.context

import org.fernice.flare.dom.Element
import org.fernice.flare.style.StyleRoot
import java.util.LinkedList

class StyleRootStack(styleRoot: StyleRoot) : Iterable<StyleRoot> {

    private val layers = LinkedList<Layer>()
    private val styleRoots = LinkedList<StyleRoot>()

    init {
        styleRoots.push(styleRoot)
    }

    fun insert(element: Element) {
        if (peek() === element) return

        val parent = element.traversalParent

        while (!isEmpty() && peek() !== parent) {
            pop()
        }

        if (peek() === parent) {
            push(element)
        } else {
            val reversed = LinkedList<Element>()

            var current: Element? = element
            while (current != null) {
                reversed.addFirst(current)
                current = current.traversalParent
            }

            for (entry in reversed) {
                push(entry)
            }
        }
    }

    private fun push(element: Element) {
        val styleRoot = element.styleRoot

        layers.push(Layer(element, styleRoot))
        if (styleRoot != null) {
            styleRoots.push(styleRoot)
        }
    }

    private fun pop() {
        val layer = layers.pop()
        if (layer.styleRoot != null) {
            styleRoots.pop()
        }
    }

    private fun peek(): Element? {
        return layers.peek()?.element
    }

    private fun isEmpty(): Boolean {
        return layers.isEmpty()
    }

    private fun clear() {
        repeat(layers.size) {
            pop()
        }
    }

    override fun iterator(): Iterator<StyleRoot> {
        return styleRoots.iterator()
    }

    fun reversedIterator(): Iterator<StyleRoot> {
        return object : Iterator<StyleRoot> {
            private val iterator = styleRoots.listIterator(styleRoots.size)
            override fun hasNext(): Boolean = iterator.hasPrevious()
            override fun next(): StyleRoot = iterator.previous()
        }
    }

    private class Layer(val element: Element, val styleRoot: StyleRoot?)
}

