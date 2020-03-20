/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.selector

import org.fernice.flare.dom.Element

/**
 * Checks if the [selector] matches the [element]. Performs a fast reject if both the [AncestorHashes]
 * of the selector and the [BloomFilter] of the element's parent is given. See [mayMatch] for the premise
 * of fast rejecting.
 */
fun matchesSelector(
    selector: Selector,
    hashes: AncestorHashes?,
    element: Element,
    context: MatchingContext
): Boolean {

    if (hashes != null) {
        context.bloomFilter?.let { bloomFilter ->
            if (!mayMatch(hashes, bloomFilter)) {
                return false
            }
        }
    }

    return matchesComplexSelector(selector.iterator(), element, context)
}

/**
 * Performs a fast reject if the any of the [AncestorHashes] is not contained in the bloom filter. This work on
 * the premise that the AncestorHashes are an excerpt of all relevant components in a selector that match a parent
 * and are hashable. In combination with a [BloomFilter], filled with all corresponding hashes of all parents,
 * a selector can be fast rejected if a ancestor hash is not contained the BloomFilter as that would be the
 * requirement for them to match. This optimization does only work for selector that do have a parental combinator.
 */
private fun mayMatch(hashes: AncestorHashes, bloomFilter: BloomFilter): Boolean {
    for (i in 0 until 3) {
        val hash = hashes.packedHashes[i]
        if (hash == 0) {
            return true
        }

        if (!bloomFilter.mightContainHash(hash and HASH_BLOOM_MASK)) {
            return false
        }
    }

    val fourth = hashes.fourthHash()
    return fourth == 0 || bloomFilter.mightContainHash(fourth)
}

private enum class MatchResult {

    MATCHED,

    NOT_MATCHED_GLOBALLY,

    NOT_MATCHED_RESTART_FROM_CLOSEST_DESCENDANT,

    NOT_MATCHED_RESTART_FROM_CLOSET_LATER_SIBLING
}

private fun matchesComplexSelector(
    iterator: SelectorIterator,
    element: Element,
    context: MatchingContext
): Boolean {
    return when (matchesComplexSelectorInternal(iterator, element, context)) {
        MatchResult.MATCHED -> true
        else -> false
    }
}

private fun matchesComplexSelectorInternal(
    iterator: SelectorIterator,
    element: Element,
    context: MatchingContext
): MatchResult {
    val matchesCompoundSelector = matchesCompoundSelector(iterator, element, context)

    if (!matchesCompoundSelector) return MatchResult.NOT_MATCHED_RESTART_FROM_CLOSET_LATER_SIBLING

    if (!iterator.hasNextSequence()) return MatchResult.MATCHED
    val combinator = iterator.nextSequence()

    val candidateNotFound = when (combinator) {
        is Combinator.NextSibling,
        is Combinator.LaterSibling -> MatchResult.NOT_MATCHED_RESTART_FROM_CLOSEST_DESCENDANT
        is Combinator.Descendant,
        is Combinator.Child,
        is Combinator.PseudoElement -> MatchResult.NOT_MATCHED_GLOBALLY
    }

    var nextElement = nextElementForCombinator(element, combinator)

    while (true) {
        if (nextElement == null) return candidateNotFound

        val result = matchesComplexSelectorInternal(iterator.clone(), nextElement, context)

        when {
            result == MatchResult.MATCHED ||
                    result == MatchResult.NOT_MATCHED_GLOBALLY ||
                    combinator is Combinator.NextSibling -> {
                return result
            }
            combinator is Combinator.PseudoElement ||
                    combinator is Combinator.Child -> {
                return MatchResult.NOT_MATCHED_RESTART_FROM_CLOSEST_DESCENDANT
            }
            result == MatchResult.NOT_MATCHED_RESTART_FROM_CLOSEST_DESCENDANT &&
                    combinator is Combinator.LaterSibling -> {
                return result
            }
        }

        nextElement = nextElementForCombinator(nextElement, combinator)
    }
}

private fun nextElementForCombinator(element: Element, combinator: Combinator): Element? {
    return when (combinator) {
        is Combinator.NextSibling, is Combinator.LaterSibling -> element.previousSibling
        is Combinator.Child, is Combinator.Descendant -> element.parent
        is Combinator.PseudoElement -> element.owner
    }
}

private fun matchesLocalName(element: Element, localName: String, localNameLower: String): Boolean {
    return element.localName == localName
}

private fun matchesCompoundSelector(iterator: SelectorIterator, element: Element, context: MatchingContext): Boolean {
    var nextSelector = iterator.nextOrNull() ?: return true

    if (nextSelector is Component.LocalName) {
        if (!matchesLocalName(element, nextSelector.localName, nextSelector.localNameLower)) return false

        nextSelector = iterator.nextOrNull() ?: return true
    }

    if (nextSelector is Component.ID) {
        if (!element.hasID(nextSelector.id)) return false

        nextSelector = iterator.nextOrNull() ?: return true
    }

    while (true) {
        if (nextSelector is Component.Class) {
            if (!element.hasClass(nextSelector.styleClass)) return false

            nextSelector = iterator.nextOrNull() ?: return true
            continue
        }
        break
    }

    while (true) {
        if (!matchesSimpleSelector(nextSelector, element, context)) return false

        nextSelector = iterator.nextOrNull() ?: return true
    }
}

private fun matchesSimpleSelector(selector: Component, element: Element, context: MatchingContext): Boolean {
    return when (selector) {
        is Component.Combinator -> throw IllegalStateException("unreachable")
        is Component.PseudoElement -> element.matchPseudoElement(selector.pseudoElement)
        is Component.LocalName -> matchesLocalName(element, selector.localName, selector.localNameLower)
        is Component.ExplicitUniversalType, is Component.ExplicitAnyNamespace -> true
        is Component.DefaultNamespace -> element.namespace == selector.namespace
        is Component.Namespace -> element.namespace == selector.namespace
        is Component.ExplicitNoNamespace -> element.namespace == null
        is Component.ID -> element.hasID(selector.id)
        is Component.Class -> element.hasClass(selector.styleClass)
        is Component.AttributeOther,
        is Component.AttributeInNoNamespaceExists,
        is Component.AttributeInNoNamespace -> false
        is Component.NonTSPseudoClass -> element.matchNonTSPseudoClass(selector.pseudoClass)
        is Component.FirstChild -> matchesFirstChild(element)
        is Component.LastChild -> matchesLastChild(element)
        is Component.OnlyChild -> matchesOnlyChild(element)
        is Component.Root -> element.isRoot()
        is Component.Empty -> element.isEmpty()
        is Component.Host,
        is Component.Scope -> false
        is Component.NthChild -> matchesGenericNthChild(element, selector.nth.a, selector.nth.b, ofType = false, fromEnd = false)
        is Component.NthLastChild -> {
            matchesGenericNthChild(element, selector.nth.a, selector.nth.b, ofType = false, fromEnd = true)
        }
        is Component.NthOfType -> {
            matchesGenericNthChild(element, selector.nth.a, selector.nth.b, ofType = true, fromEnd = false)
        }
        is Component.NthLastOfType -> {
            matchesGenericNthChild(element, selector.nth.a, selector.nth.b, ofType = true, fromEnd = true)
        }
        is Component.FirstOfType -> {
            matchesGenericNthChild(element, 1, 0, ofType = true, fromEnd = false)
        }
        is Component.LastOfType -> {
            matchesGenericNthChild(element, 1, 0, ofType = true, fromEnd = true)
        }
        is Component.OnlyOfType -> {
            matchesGenericNthChild(element, 0, 1, ofType = true, fromEnd = false) &&
                    matchesGenericNthChild(element, 0, 1, ofType = true, fromEnd = true)
        }
        is Component.Negation -> {
            val iterator = selector.iterator()
            while (iterator.hasNext()) {
                val nextSelector = iterator.next()

                if (matchesSimpleSelector(nextSelector, element, context)) return false
            }
            true
        }
    }
}

private fun matchesGenericNthChild(element: Element, a: Int, b: Int, ofType: Boolean, fromEnd: Boolean): Boolean {
    val index = nthChildIndex(element, ofType, fromEnd)

    val an = index - b

    if (an < 0) {
        return false
    }

    if (a == 0) {
        return false
    }

    val n = an / a

    return n >= 0 && n * a == an
}

private fun nthChildIndex(element: Element, ofType: Boolean, fromEnd: Boolean): Int {
    fun next(element: Element): Element? {
        return if (fromEnd) {
            element.nextSibling
        } else {
            element.previousSibling
        }
    }

    var index = 1
    var current = element

    loop@
    while (true) {
        current = next(current) ?: break

        if (!ofType || isSameType(element, current)) {
            index++
        }
    }

    return index
}

private fun isSameType(element: Element, other: Element): Boolean {
    return element.localName == other.localName && element.namespace == other.namespace
}

private fun matchesFirstChild(element: Element): Boolean {
    return element.previousSibling == null
}

private fun matchesLastChild(element: Element): Boolean {
    return element.nextSibling == null
}

private fun matchesOnlyChild(element: Element): Boolean {
    return element.previousSibling == null && element.nextSibling == null
}

private fun <E : Any> Iterator<E>.nextOrNull(): E? = if (hasNext()) next() else null
