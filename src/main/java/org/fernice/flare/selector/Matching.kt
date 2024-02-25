/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.selector

import org.fernice.flare.dom.Element
import kotlin.math.cos

/**
 * Checks if the [selector] matches the [element]. Performs a fast reject if both the [AncestorHashes]
 * of the selector and the [BloomFilter] of the element's parent is given. See [mayMatch] for the premise
 * of fast rejecting.
 */
internal fun matchesSelector(
    selector: Selector,
    hashes: AncestorHashes?,
    element: Element,
    context: MatchingContext,
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

private fun matchesComplexSelectors(
    selectors: List<Selector>,
    element: Element,
    context: MatchingContext,
): Boolean {
    for (selector in selectors) {
        if (matchesComplexSelector(selector.iterator(), element, context)) {
            return true
        }
    }
    return false
}

private fun matchesComplexSelector(
    iterator: SelectorIterator,
    element: Element,
    context: MatchingContext,
): Boolean {
    return when (matchesComplexSelectorInternal(iterator, element, context)) {
        MatchResult.MATCHED -> true
        else -> false
    }
}

private fun matchesComplexSelectorInternal(
    iterator: SelectorIterator,
    element: Element,
    context: MatchingContext,
): MatchResult {
    val matchesCompoundSelector = matchesCompoundSelector(iterator, element, context)

    if (!matchesCompoundSelector) return MatchResult.NOT_MATCHED_RESTART_FROM_CLOSET_LATER_SIBLING

    if (!iterator.hasNextSequence()) return MatchResult.MATCHED
    val combinator = iterator.nextSequence()

    val candidateNotFound = when (combinator) {
        Combinator.NextSibling,
        Combinator.LaterSibling,
        -> MatchResult.NOT_MATCHED_RESTART_FROM_CLOSEST_DESCENDANT

        Combinator.Descendant,
        Combinator.Child,
        Combinator.Part,
        Combinator.SlotAssignment,
        Combinator.PseudoElement,
        -> MatchResult.NOT_MATCHED_GLOBALLY
    }

    var visitedHandling = when {
        combinator.isSibling() -> VisitedHandlingMode.AllLinksUnvisited
        else -> context.visitedHandling
    }

    var nextElement = element
    while (true) {
        if (nextElement.isLink()) {
            visitedHandling = VisitedHandlingMode.AllLinksUnvisited
        }

        nextElement = nextElementForCombinator(nextElement, combinator)
            ?: return candidateNotFound

        val result = context.withVisitedHandling(visitedHandling) {
            matchesComplexSelectorInternal(iterator.clone(), nextElement, context)
        }

        when {
            result == MatchResult.MATCHED ||
                    result == MatchResult.NOT_MATCHED_GLOBALLY ||
                    combinator == Combinator.NextSibling -> {
                return result
            }

            combinator == Combinator.PseudoElement ||
                    combinator == Combinator.Child -> {
                return MatchResult.NOT_MATCHED_RESTART_FROM_CLOSEST_DESCENDANT
            }

            result == MatchResult.NOT_MATCHED_RESTART_FROM_CLOSEST_DESCENDANT &&
                    combinator == Combinator.LaterSibling -> {
                return result
            }
        }

    }
}

private fun nextElementForCombinator(element: Element, combinator: Combinator): Element? {
    return when (combinator) {
        Combinator.NextSibling, Combinator.LaterSibling -> element.previousSibling
        Combinator.Child, Combinator.Descendant -> element.parent
        Combinator.Part -> null
        Combinator.SlotAssignment -> null
        Combinator.PseudoElement -> element.owner
    }
}

private fun matchesRelativeSelectors(
    selectors: List<RelativeSelector>,
    element: Element,
    context: MatchingContext,
): Boolean {
    for ((selector, matchHint) in selectors) {
        var (traverseSubtree, traverseSiblings, nextElement) = when (matchHint) {
            RelativeSelectorMatchHint.InChild -> Triple(false, true, element.firstChild)
            RelativeSelectorMatchHint.InSubtree -> Triple(true, true, element.firstChild)
            RelativeSelectorMatchHint.InSibling -> Triple(false, true, element.nextSibling)
            RelativeSelectorMatchHint.InSiblingSubtree -> Triple(true, true, element.nextSibling)
            RelativeSelectorMatchHint.InNextSibling -> Triple(false, false, element.nextSibling)
            RelativeSelectorMatchHint.InNextSiblingSubtree -> Triple(true, false, element.nextSibling)
        }
        while (nextElement != null) {
            if (matchesComplexSelector(selector.iterator(), nextElement, context)) return true
            if (traverseSubtree && matchesRelativeSelectorSubtree(selector, element, context)) return true
            if (!traverseSiblings) break
            nextElement = nextElement.nextSibling
        }
    }

    return false
}

private fun matchesRelativeSelectorSubtree(
    selector: Selector,
    element: Element,
    context: MatchingContext,
): Boolean {
    var nextElement = element.firstChild
    while (nextElement != null) {
        if (matchesComplexSelector(selector.iterator(), element, context)) return true
        if (matchesRelativeSelectorSubtree(selector, element, context)) return true
        nextElement = element.nextSibling
    }
    return false
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

    while (nextSelector is Component.Class) {
        if (!element.hasClass(nextSelector.styleClass)) return false

        nextSelector = iterator.nextOrNull() ?: return true
    }

    while (true) {
        if (!matchesSimpleSelector(nextSelector, element, context)) return false

        nextSelector = iterator.nextOrNull() ?: return true
    }
}

private fun matchesSimpleSelector(component: Component, element: Element, context: MatchingContext): Boolean {
    return when (component) {
        is Component.ID -> element.hasID(component.id)
        is Component.Class -> element.hasClass(component.styleClass)
        is Component.LocalName -> matchesLocalName(element, component.localName, component.localNameLower)

        is Component.DefaultNamespace -> element.namespace == component.namespace
        is Component.Namespace -> element.namespace == component.namespace
        is Component.ExplicitNoNamespace -> element.namespace == null
        is Component.ExplicitUniversalType,
        is Component.ExplicitAnyNamespace,
        -> true

        is Component.Part -> false
        is Component.Slotted -> false

        is Component.NonTSPseudoClass -> element.matchNonTSPseudoClass(component.pseudoClass)
        is Component.NonTSFPseudoClass -> element.matchNonTSFPseudoClass(component.pseudoClass)
        is Component.PseudoElement -> element.matchPseudoElement(component.pseudoElement)

        is Component.Root -> element.isRoot()
        is Component.Empty -> element.isEmpty()
        is Component.Host -> false

        is Component.Nth -> {
            val data = component.data
            if (!data.isFunction) {
                when (data.type) {
                    NthType.Forward -> matchesFirstChild(element)
                    NthType.Backward -> matchesLastChild(element)
                    NthType.Only -> matchesOnlyChild(element)
                }
            } else {
                matchesGenericNthChild(element, data.a, data.b, ofType = false, fromEnd = data.type == NthType.Backward)
            }
        }

        is Component.NthOfType -> {
            val data = component.data
            if (!data.isFunction) {
                when (data.type) {
                    NthType.Forward -> matchesGenericNthChild(element, 0, 1, ofType = true, fromEnd = false)
                    NthType.Backward -> matchesGenericNthChild(element, 0, 1, ofType = true, fromEnd = true)
                    NthType.Only -> matchesGenericNthChild(element, 0, 1, ofType = true, fromEnd = false) &&
                            matchesGenericNthChild(element, 0, 1, ofType = true, fromEnd = true)
                }
            } else {
                matchesGenericNthChild(element, data.a, data.b, ofType = true, fromEnd = data.type == NthType.Backward)
            }
        }

        is Component.Is -> context.nest { matchesComplexSelectors(component.selectors, element, context) }
        is Component.Where -> context.nest { matchesComplexSelectors(component.selectors, element, context) }

        is Component.Negation -> context.nestForNegation { !matchesComplexSelectors(component.selectors, element, context) }

        is Component.Has -> context.nestForRelativeSelector(element) { matchesRelativeSelectors(component.selectors, element, context) }

        is Component.Scope -> element.isRoot()

        is Component.AttributeOther,
        is Component.AttributeInNoNamespaceExists,
        is Component.AttributeInNoNamespace,
        -> false

        is Component.RelativeSelectorAnchor -> {
            val anchor = context.relativeSelectorAnchor ?: error("relative selector outside of relative context")

            element === anchor
        }

        is Component.Combinator -> error("combinator in simple selector")
        is Component.ParentSelector -> error("not replaced & in selector")
    }
}

private fun matchesLocalName(element: Element, localName: String, localNameLower: String): Boolean {
    return element.localName == localName
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
