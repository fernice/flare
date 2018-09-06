package de.krall.flare.style

import de.krall.flare.dom.Element
import de.krall.flare.selector.BloomFilter
import de.krall.flare.selector.CountingBloomFilter
import modern.std.None
import modern.std.Option
import modern.std.Some
import modern.std.ifLet
import modern.std.unwrap
import java.util.Stack

class StyleBloom(
        private val filter: BloomFilter,
        private val elements: Stack<PushedElement>,
        private val pushedHashes: Stack<Int>
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
        element.namespace().ifLet { namespace ->
            function(namespace.hashCode())
        }

        function(element.localName().hashCode())

        element.id().ifLet { id ->
            function(id.hashCode())
        }

        for (styleClass in element.classes()) {
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

    fun pop(): Option<Element> {
        val element = if (elements.isEmpty()) {
            return None
        } else {
            elements.pop()
        }

        for (i in 0 until element.hashes) {
            val hash = pushedHashes.pop()
            filter.removeHash(hash)
        }

        return Some(element.element)
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
            val parent = current.traversalParent()

            when (parent) {
                is Some -> {
                    parentsReversed.add(0, parent.value)

                    current = parent.value
                }
                is None -> break@loop
            }
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
            val parent = current.traversalParent()

            when (parent) {
                is Some -> {
                    parentsReversed.add(0, parent.value)

                    if (destination == parent.value) {
                        break@loop
                    } else {
                        current = parent.value
                    }
                }
                is None -> break@loop
            }
        }

        for (parent in parentsReversed) {
            push(parent)
        }
    }

    fun currentParent(): Option<Element> {
        return if (elements.isEmpty()) {
            None
        } else {
            Some(elements.peek().element)
        }
    }

    fun insertParent(element: Element) {
        if (elements.isEmpty()) {
            rebuild(element)
            return
        }

        val traversalParentOption = element.traversalParent()

        val traversalParent = when (traversalParentOption) {
            is Some -> traversalParentOption.value
            is None -> {
                clear()
                return
            }
        }

        // Current parent is present as stack is not empty
        if (currentParent().unwrap() == traversalParent) {
            return
        } else {
            // We have no use for that parent
            pop()
        }

        loop@
        while (true) {
            val currentParent = pop()

            when (currentParent) {
                is Some -> {
                    if (currentParent.value == traversalParent) {
                        rebuildUntil(element, currentParent.value)
                        return
                    }
                }
                is None -> break@loop
            }
        }

        rebuild(element)
    }
}

class PushedElement(val element: Element,
                    val hashes: Int)