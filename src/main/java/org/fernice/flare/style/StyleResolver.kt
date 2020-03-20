/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style

import org.fernice.flare.ApplicableDeclarationBlock
import org.fernice.flare.dom.Element
import org.fernice.flare.selector.MatchingContext
import org.fernice.flare.selector.PseudoElement
import org.fernice.flare.style.context.StyleContext
import org.fernice.flare.style.parser.QuirksMode
import org.fernice.flare.style.ruletree.RuleNode

class ResolvedStyle(val style: ComputedValues)

class MatchingResult(val ruleNode: RuleNode)

class PrimaryStyle(val style: ResolvedStyle) {

    fun style(): ComputedValues {
        return style.style
    }
}

class CascadeInputs(val rules: RuleNode?)

class ResolvedElementStyles(val primary: PrimaryStyle, val pseudos: PerPseudoElementMap<ComputedValues>)

class ElementStyleResolver(val element: Element, val context: StyleContext) {

    inline fun <R> withDefaultParentStyles(run: (ComputedValues?, ComputedValues?) -> R): R {
        val parentElement = element.inheritanceParent
        val parentData = parentElement?.getDataOrNull()
        val parentStyle = parentData?.styles?.primary()

        return run(parentStyle, parentStyle)
    }

    fun resolvePrimaryStyleWithDefaultParentStyles(): ResolvedElementStyles {
        return withDefaultParentStyles { parentStyle, layoutStyle ->
            resolveStyle(parentStyle, layoutStyle)
        }
    }

    fun resolveStyle(parentStyle: ComputedValues?, layoutStyle: ComputedValues?): ResolvedElementStyles {
        val primaryStyle = resolvePrimaryStyle(parentStyle, layoutStyle)

        val pseudoElements = PerPseudoElementMap<ComputedValues>()

        PseudoElement.forEachEagerCascadedPseudoElement { pseudo ->
            val pseudoStyle = resolvePseudoStyle(
                pseudo,
                primaryStyle,
                primaryStyle.style()
            )

            if (pseudoStyle != null) {
                pseudoElements.set(pseudo, pseudoStyle.style)
            }
        }

        return ResolvedElementStyles(
            primaryStyle,
            pseudoElements
        )
    }

    fun resolvePrimaryStyle(
        parentStyle: ComputedValues?,
        layoutStyle: ComputedValues?
    ): PrimaryStyle {
        val primaryStyle = matchPrimary()

        return cascadePrimaryStyle(
            CascadeInputs(
                primaryStyle.ruleNode
            ),
            parentStyle,
            layoutStyle
        )
    }

    fun matchPrimary(): MatchingResult {
        val declarations = mutableListOf<ApplicableDeclarationBlock>()

        val bloomFilter = context.bloomFilter.filter()
        val matchingContext = MatchingContext(
            bloomFilter,
            QuirksMode.NO_QUIRKS
        )

        val stylist = context.stylist

        stylist.pushApplicableDeclarations(
            element,
            element.pseudoElement,
            element.styleAttribute,
            declarations,
            matchingContext
        )

        val ruleNode = stylist.ruleTree.computedRuleNode(declarations)

        return MatchingResult(ruleNode)
    }

    fun cascadePrimaryStyle(
        inputs: CascadeInputs,
        parentStyle: ComputedValues?,
        layoutStyle: ComputedValues?
    ): PrimaryStyle {
        return PrimaryStyle(
            cascadeStyleAndVisited(
                inputs,
                parentStyle,
                layoutStyle,
                pseudoElement = null
            )
        )
    }

    fun resolvePseudoStyle(
        pseudo: PseudoElement,
        primaryStyle: PrimaryStyle,
        layoutParentStyle: ComputedValues?
    ): ResolvedStyle? {
        val style = matchPseudo(pseudo) ?: return null
        return cascadeStyleAndVisited(
            CascadeInputs(
                style
            ),
            primaryStyle.style(),
            layoutParentStyle,
            pseudo
        )

    }

    fun matchPseudo(pseudo: PseudoElement): RuleNode? {
        val declarations = mutableListOf<ApplicableDeclarationBlock>()

        val bloomFilter = context.bloomFilter.filter()
        val matchingContext = MatchingContext(
            bloomFilter,
            QuirksMode.NO_QUIRKS
        )

        val stylist = context.stylist

        stylist.pushApplicableDeclarations(
            element,
            pseudo,
            null,
            declarations,
            matchingContext
        )

        if (declarations.isEmpty()) {
            return null
        }

        return stylist.ruleTree.computedRuleNode(declarations)
    }

    fun cascadeStyleAndVisited(
        inputs: CascadeInputs,
        parentStyle: ComputedValues?,
        layoutStyle: ComputedValues?,
        pseudoElement: PseudoElement?
    ): ResolvedStyle {
        val values = context.stylist.cascadeStyleAndVisited(
            context.device,
            element,
            pseudoElement,
            inputs,
            parentStyle,
            parentStyle,
            layoutStyle,
            context.fontMetricsProvider
        )

        return ResolvedStyle(
            values
        )
    }
}
