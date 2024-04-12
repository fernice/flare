/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style

import org.fernice.flare.dom.Element
import org.fernice.flare.dom.ElementStyles
import org.fernice.flare.selector.MatchingContext
import org.fernice.flare.selector.PseudoElement
import org.fernice.flare.selector.SimpleRuleConditionCache
import org.fernice.flare.selector.VisitedHandlingMode
import org.fernice.flare.style.context.StyleContext
import org.fernice.flare.style.ruletree.RuleNode
import org.fernice.flare.style.source.StyleAttribute
import org.fernice.std.Recycler

class MatchingResult(val ruleNode: RuleNode)

class CascadeInputs(val rules: RuleNode?)

class ElementStyleResolver(val element: Element, val context: StyleContext) {

    private inline fun <R> withDefaultParentStyles(run: (ComputedValues?) -> R): R {
        val parentElement = element.inheritanceParent
        val parentStyle = parentElement?.styles?.primary

        return run(parentStyle)
    }

    fun resolveStyleWithDefaultParentStyles(): ElementStyles {
        return withDefaultParentStyles { parentStyle ->
            resolveStyle(parentStyle)
        }
    }

    fun resolveStyle(parentStyle: ComputedValues?): ElementStyles {
        val previousPrimaryStyle = element.styles?.primary
        val primaryStyle = resolvePrimaryStyle(previousPrimaryStyle, parentStyle)

        val pseudoElements = PerPseudoElementMap<ComputedValues>()

        for (pseudoElement in PseudoElement.entries) {
            // prevent computation for a pseudo-element that doesn't even match
            if (element.hasPseudoElement(pseudoElement)) {
                val previousStyle = element.styles?.pseudos?.get(pseudoElement)
                val pseudoStyle = resolvePseudoStyle(
                    pseudoElement,
                    previousStyle,
                    primaryStyle,
                )

                if (pseudoStyle != null) {
                    pseudoElements.set(pseudoElement, pseudoStyle)
                }
            }
        }

        return ElementStyles(
            primaryStyle,
            pseudoElements
        )
    }

    fun resolvePrimaryStyle(
        previousStyle: ComputedValues?,
        parentStyle: ComputedValues?,
    ): ComputedValues {
        val primaryStyle = matchPrimaryStyle()

        return cascadeStyleAndVisited(
            inputs = CascadeInputs(primaryStyle.ruleNode),
            previousStyle = previousStyle,
            parentStyle = parentStyle,
            pseudoElement = null
        )
    }

    fun matchPrimaryStyle(): MatchingResult {
        return matchStyle(element, element.pseudoElement, element.styleAttribute)
    }

    fun resolvePseudoStyle(
        pseudoElement: PseudoElement,
        previousStyle: ComputedValues?,
        primaryStyle: ComputedValues?,
    ): ComputedValues? {
        val style = matchPseudoStyle(pseudoElement) ?: return null

        return cascadeStyleAndVisited(
            inputs = CascadeInputs(style.ruleNode),
            previousStyle = previousStyle,
            parentStyle = primaryStyle,
            pseudoElement = pseudoElement
        )
    }

    fun matchPseudoStyle(pseudoElement: PseudoElement): MatchingResult? {
        val style = matchStyle(element, pseudoElement, null)
        if (style.ruleNode.parent == null) return null
        return style
    }

    private fun matchStyle(element: Element, pseudoElement: PseudoElement?, styleAttribute: StyleAttribute?): MatchingResult {
        val bloomFilter = context.bloomFilter.filter()
        val matchingContext = MatchingContext(
            context.device,
            bloomFilter,
            QuirksMode.NoQuirks,
            VisitedHandlingMode.AllLinksVisitedAndUnvisited,
            context.ruleConditionCache,
        )

        val rules = ApplicableDeclarationListRecycler.acquire()

        RuleCollector(
            context.styleRoots.asReversedSequence(),
            element,
            pseudoElement,
            styleAttribute,
            matchingContext,
            rules
        ).collectAll()

        val stylist = context.stylist
        val ruleNode = stylist.ruleTree.computedRuleNode(rules.asSequence().map { it.forRuleTree() }.iterator())

        stylist.ruleTree.gc()

        ApplicableDeclarationListRecycler.release(rules)

        return MatchingResult(ruleNode)
    }

    private fun cascadeStyleAndVisited(
        inputs: CascadeInputs,
        previousStyle: ComputedValues?,
        parentStyle: ComputedValues?,
        pseudoElement: PseudoElement?,
    ): ComputedValues {
        return context.stylist.cascadeStyleAndVisited(
            device = context.device,
            element = element,
            pseudoElement = pseudoElement,
            inputs = inputs,
            previousStyle = previousStyle,
            parentStyle = parentStyle,
            parentStyleIgnoringFirstLine = parentStyle,
            fontMetricsProvider = context.fontMetricsProvider
        )
    }
}

private val ApplicableDeclarationListRecycler = Recycler(
    factory = { ArrayList<ApplicableDeclarationBlock>() },
    reset = { it.clear() },
)

