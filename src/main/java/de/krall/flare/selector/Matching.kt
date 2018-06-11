package de.krall.flare.selector

import de.krall.flare.dom.Element
import de.krall.flare.std.*

fun matchesSelector(selector: Selector,
                    hashes: Option<AncestorHashes>,
                    element: Element,
                    context: MatchingContext): Boolean {

    hashes.ifLet { h ->
        context.bloomFilter.ifLet { bf ->
            if (!mayMatch(h, bf)) {
                return false
            }
        }
    }

    return matchesComplexSelector(selector.iter(), element, context)
}

private fun mayMatch(hashes: AncestorHashes, bloomFilter: BloomFilter): Boolean {
    for (i in 0..3) {
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

private fun matchesComplexSelector(iter: SelectorIter, element: Element, context: MatchingContext): Boolean {
    val result = matchesComplexSelectorInternal(iter, element, context)

    return when (result) {
        MatchResult.MATCHED -> true
        else -> false
    }
}

private fun matchesComplexSelectorInternal(iter: SelectorIter, element: Element, context: MatchingContext): MatchResult {
    val matchesCompoundSelector = matchesCompoundSelector(iter, element, context)

    if (!matchesCompoundSelector) {
        return MatchResult.NOT_MATCHED_RESTART_FROM_CLOSET_LATER_SIBLING
    }

    val nextCombinator = iter.nextSequence()

    val combinator = when (nextCombinator) {
        is Some -> nextCombinator.value
        is None -> return MatchResult.MATCHED
    }

    val candidateNotFound = when (combinator) {
        is Combinator.NextSibling,
        is Combinator.LaterSibling -> {
            MatchResult.NOT_MATCHED_RESTART_FROM_CLOSEST_DESCENDANT
        }
        is Combinator.Descendant,
        is Combinator.Child,
        is Combinator.PseudoElement -> {
            MatchResult.NOT_MATCHED_GLOBALLY
        }
    }

    var nextElement = nextElementForCombinator(element, combinator)

    while (true) {
        val innerElement = when (nextElement) {
            is Some -> nextElement.value
            is None -> return candidateNotFound
        }

        val result = matchesComplexSelectorInternal(iter.clone(), innerElement, context)

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

        nextElement = nextElementForCombinator(innerElement, combinator)
    }
}

private fun nextElementForCombinator(element: Element, combinator: Combinator): Option<Element> {
    return when (combinator) {
        is Combinator.NextSibling, is Combinator.LaterSibling -> element.previousSibling()
        is Combinator.Child, is Combinator.Descendant -> element.parent()
        is Combinator.PseudoElement -> element.owner()
    }
}

private fun matchesLocalName(element: Element, localName: String, localNameLower: String): Boolean {
    return element.localName() == localName
}

private fun matchesCompoundSelector(iter: SelectorIter, element: Element, context: MatchingContext): Boolean {
    var nextSelector = iter.next()

    nextSelector.ifLet { s ->
        if (s is Component.LocalName) {
            if (!matchesLocalName(element, s.localName, s.localNameLower)) {
                return false
            }

            nextSelector = iter.next()
        }
    }

    nextSelector.ifLet { s ->
        if (s is Component.ID) {
            if (!element.hasID(s.id)) {
                return false
            }

            nextSelector = iter.next()
        }
    }

    nextSelector.ifLet { s ->
        if (s is Component.ID) {
            if (!element.hasID(s.id)) {
                return false
            }

            nextSelector = iter.next()
        }
    }

    loop@
    while (true) {
        val selector = when (nextSelector) {
            is Some -> nextSelector.unwrap()
            is None -> break@loop
        }

        if (selector is Component.Class) {
            if (!element.hasClass(selector.styleClass)) {
                return false
            }

            nextSelector = iter.next()
        } else {
            break@loop
        }
    }

    loop@
    while (true) {
        val selector = when (nextSelector) {
            is Some -> nextSelector.unwrap()
            is None -> return true
        }

        if (!matchesSimpleSelector(selector, element, context)) {
            return false
        }

        nextSelector = iter.next()
    }
}

private fun matchesSimpleSelector(selector: Component, element: Element, context: MatchingContext): Boolean {
    return when (selector) {
        is Component.Combinator -> throw IllegalStateException("unreachable")
        is Component.PseudoElement -> {
            element.matchPseudoElement(selector.pseudoElement)
        }
        is Component.LocalName -> {
            matchesLocalName(element, selector.localName, selector.localNameLower)
        }
        is Component.ExplicitUniversalType, is Component.ExplicitAnyNamespace -> true
        is Component.DefaultNamespace -> {
            val namespace = element.namespace()
            when (namespace) {
                is Some -> namespace.value == selector.namespace
                is None -> false
            }
        }
        is Component.Namespace -> {
            val namespace = element.namespace()
            when (namespace) {
                is Some -> namespace.value == selector.namespace
                is None -> false
            }
        }
        is Component.ExplicitNoNamespace -> {
            element.namespace().isNone()
        }
        is Component.ID -> {
            element.hasID(selector.id)
        }
        is Component.Class -> {
            element.hasClass(selector.styleClass)
        }
        is Component.AttributeOther,
        is Component.AttributeInNoNamespaceExists,
        is Component.AttributeInNoNamespace -> throw IllegalStateException("unsupported")
        is Component.NonTSPseudoClass -> {
            element.matchNonTSPseudoClass(selector.pseudoClass)
        }
        is Component.FirstChild -> {
            matchesFirstChild(element)
        }
        is Component.LastChild -> {
            matchesLastChild(element)
        }
        is Component.OnlyChild -> {
            matchesOnlyChild(element)
        }
        is Component.Root -> {
            element.isRoot()
        }
        is Component.Empty -> {
            element.isEmpty()
        }
        is Component.Host,
        is Component.Scope -> {
            throw IllegalStateException("unsupported")
        }
        is Component.NthChild -> {
            matchesGenericNthChild(element, selector.nth.a, selector.nth.b, false, false)
        }
        is Component.NthLastChild -> {
            matchesGenericNthChild(element, selector.nth.a, selector.nth.b, false, true)
        }
        is Component.NthOfType -> {
            matchesGenericNthChild(element, selector.nth.a, selector.nth.b, true, false)
        }
        is Component.NthLastOfType -> {
            matchesGenericNthChild(element, selector.nth.a, selector.nth.b, true, true)
        }
        is Component.FirstOfType -> {
            matchesGenericNthChild(element, 1, 0, true, false)
        }
        is Component.LastOfType -> {
            matchesGenericNthChild(element, 1, 0, true, true)
        }
        is Component.OnlyType -> {
            matchesGenericNthChild(element, 0, 1, true, false) &&
                    matchesGenericNthChild(element, 0, 1, true, true)
        }
        is Component.Negation -> {
            val iter = selector.iter()

            var nextSelector = iter.next()

            loop@
            while (true) {
                val innerSelector = when (nextSelector) {
                    is Some -> nextSelector.unwrap()
                    is None -> break@loop
                }

                if (matchesSimpleSelector(innerSelector, element, context)) {
                    return false
                }

                nextSelector = iter.next()
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
    fun next(element: Element): Option<Element> {
        return if (fromEnd) {
            element.nextSibling()
        } else {
            element.previousSibling()
        }
    }

    var index = 1
    var current = element

    loop@
    while (true) {
        val next = next(current)

        current = when (next) {
            is Some -> next.value
            is None -> break@loop
        }

        if (!ofType || isSameType(element, current)) {
            index++
        }
    }

    return index
}

private fun isSameType(element: Element, other: Element): Boolean {
    return element.localName() == other.localName() && element.namespace() == other.namespace()
}

private fun matchesFirstChild(element: Element): Boolean {
    return element.previousSibling().isNone()
}

private fun matchesLastChild(element: Element): Boolean {
    return element.nextSibling().isNone()
}

private fun matchesOnlyChild(element: Element): Boolean {
    return element.previousSibling().isNone() && element.nextSibling().isNone()
}