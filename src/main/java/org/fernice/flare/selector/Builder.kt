/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.selector

import org.fernice.flare.debugAssert
import org.fernice.flare.std.iter.Iter
import org.fernice.flare.std.iter.iter
import org.fernice.flare.std.atMost
import fernice.std.None
import fernice.std.Some

class SelectorBuilder {

    private val simpleSelectors = mutableListOf<Component>()
    private val combinators = mutableListOf<Pair<Combinator, Int>>()
    private var currentLength = 0

    fun pushSimpleSelector(component: Component) {
        debugAssert(component !is Component.Combinator, "What is he doing here?")

        simpleSelectors.add(component)
        currentLength += 1
    }

    fun pushCombinator(combinator: Combinator) {
        combinators.add(Pair(combinator, currentLength))
        currentLength = 0
    }

    fun isEmpty(): Boolean {
        return simpleSelectors.isEmpty() && combinators.isEmpty()
    }

    fun hasDanglingCombinator(): Boolean {
        return !combinators.isEmpty() && currentLength == 0
    }

    fun build(hasPseudoElement: Boolean): Selector {
        val specificity = if (hasPseudoElement) {
            specificity(simpleSelectors.iter()) or PSEUDO_ELEMENT_BIT
        } else {
            specificity(simpleSelectors.iter()) and PSEUDO_ELEMENT_BIT.inv()
        }

        return buildWithSpecificityAndFlags(SpecificityAndFlags(specificity))
    }

    fun buildWithSpecificityAndFlags(specificityAndFlags: SpecificityAndFlags): Selector {
        val selector = mutableListOf<Component>()

        val combinatorIter = combinators.reversed().iter()

        var upper = simpleSelectors.size
        var lower = upper - currentLength

        loop@
        do {
            selector.addAll(simpleSelectors.subList(lower, upper))

            when (val next = combinatorIter.next()) {
                is Some -> {
                    val (combinator, length) = next.value

                    upper = lower
                    lower -= length

                    selector.add(Component.Combinator(combinator))
                }
                is None -> break@loop
            }
        } while (true)

        return Selector(specificityAndFlags, selector)
    }
}

private class Specificity {

    var idSelectors = 0
    var classSelectors = 0
    var elementSelectors = 0
}

private const val MAX_10_BIT = (1 shl 10) - 1

private fun Specificity.into(): Int {
    return (this.idSelectors.atMost(MAX_10_BIT) shl 20) or
            (this.classSelectors.atMost(MAX_10_BIT) shl 10) or
            (this.elementSelectors.atMost(MAX_10_BIT))
}

private fun specificity(iter: Iter<Component>): Int {
    fun simpleSelectorSpecificity(simpleSelector: Component, specificity: Specificity) {
        when (simpleSelector) {
            is Component.Combinator -> throw IllegalStateException("unreachable")

            is Component.LocalName, is Component.PseudoElement -> {
                specificity.elementSelectors += 1
            }

            is Component.ID -> {
                specificity.idSelectors += 1
            }

            is Component.Class,
            is Component.AttributeInNoNamespace,
            is Component.AttributeInNoNamespaceExists,
            is Component.AttributeOther,
            is Component.FirstChild,
            is Component.LastChild,
            is Component.OnlyChild,
            is Component.Root,
            is Component.Empty,
            is Component.Scope,
            is Component.Host,
            is Component.NthChild,
            is Component.NthLastChild,
            is Component.NthOfType,
            is Component.NthLastOfType,
            is Component.FirstOfType,
            is Component.LastOfType,
            is Component.OnlyOfType,
            is Component.NonTSPseudoClass -> {
                specificity.classSelectors += 1
            }

            is Component.Negation -> {
                for (selector in simpleSelector.iter()) {
                    simpleSelectorSpecificity(selector, specificity)
                }
            }

            else -> {
            }
        }
    }

    val specificity = Specificity()
    for (simpleSelector in iter) {
        simpleSelectorSpecificity(simpleSelector, specificity)
    }
    return specificity.into()
}


private const val PSEUDO_ELEMENT_BIT = 1 shl 31

class SpecificityAndFlags(private val bits: Int) {

    fun specificity(): Int {
        return bits and PSEUDO_ELEMENT_BIT.inv()
    }

    fun hasPseudoElement(): Boolean {
        return (bits and PSEUDO_ELEMENT_BIT) != 0
    }
}
