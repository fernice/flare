package de.krall.flare.style

import de.krall.flare.ApplicableDeclarationBlock
import de.krall.flare.dom.Element
import de.krall.flare.selector.MatchingContext
import de.krall.flare.selector.PseudoElement
import de.krall.flare.style.context.StyleContext
import de.krall.flare.style.parser.QuirksMode
import de.krall.flare.style.ruletree.RuleNode
import modern.std.None
import modern.std.Option
import modern.std.Some

class ResolvedStyle(val style: ComputedValues)

class MatchingResult(val ruleNode: RuleNode)

class PrimaryStyle(val style: ResolvedStyle) {

    fun style(): ComputedValues {
        return style.style
    }
}

class CascadeInputs(val rules: Option<RuleNode>)

class ResolvedElementStyles(val primary: PrimaryStyle,
                            val pseudos: PerPseudoElementMap<ComputedValues>)

class ElementStyleResolver(val element: Element,
                           val context: StyleContext) {

    fun <R> withDefaultParentStyles(run: (Option<ComputedValues>, Option<ComputedValues>) -> R): R {
        val parentElement = element.inheritanceParent()
        val parentData = parentElement.andThen { e -> e.getData() }
        val parentStyle = parentData.map { d -> d.styles.primary() }

        return run(parentStyle, parentStyle)
    }

    fun resolvePrimaryStyleWithDefaultParentStyles(): ResolvedElementStyles {
        return withDefaultParentStyles { parentStyle, layoutStyle ->
            resolveStyle(parentStyle, layoutStyle)
        }
    }

    fun resolveStyle(parentStyle: Option<ComputedValues>, layoutStyle: Option<ComputedValues>): ResolvedElementStyles {
        val primaryStyle = resolvePrimaryStyle(parentStyle, layoutStyle)

        val pseudoElements = PerPseudoElementMap<ComputedValues>()

        PseudoElement.forEachEagerCascadedPseudoElement { pseudo ->
            val pseudoStyle = resolvePseudoStyle(
                    pseudo,
                    primaryStyle,
                    Some(primaryStyle.style())
            )

            if (pseudoStyle is Some) {
                pseudoElements.set(pseudo, pseudoStyle.value.style)
            }
        }

        return ResolvedElementStyles(
                primaryStyle,
                pseudoElements
        )
    }

    fun resolvePrimaryStyle(
            parentStyle: Option<ComputedValues>,
            layoutStyle: Option<ComputedValues>
    ): PrimaryStyle {
        val primaryStyle = matchPrimary()

        return cascadePrimaryStyle(
                CascadeInputs(
                        Some(primaryStyle.ruleNode)
                ),
                parentStyle,
                layoutStyle
        )
    }

    fun matchPrimary(): MatchingResult {
        val declarations = mutableListOf<ApplicableDeclarationBlock>()

        val bloomFilter = context.bloomFilter.filter()
        val matchingContext = MatchingContext(
                Some(bloomFilter),
                QuirksMode.NO_QUIRKS
        )

        val stylist = context.stylist

        stylist.pushApplicableDeclarations(
                element,
                element.pseudoElement(),
                element.styleAttribute(),
                declarations,
                matchingContext
        )

        val ruleNode = stylist.ruleTree.computedRuleNode(declarations)

        return MatchingResult(ruleNode)
    }

    fun cascadePrimaryStyle(
            inputs: CascadeInputs,
            parentStyle: Option<ComputedValues>,
            layoutStyle: Option<ComputedValues>
    ): PrimaryStyle {
        return PrimaryStyle(
                cascadeStyleAndVisited(
                        inputs,
                        parentStyle,
                        layoutStyle,
                        None
                )
        )
    }

    fun resolvePseudoStyle(
            pseudo: PseudoElement,
            primaryStyle: PrimaryStyle,
            layoutParentStyle: Option<ComputedValues>
    ): Option<ResolvedStyle> {
        val matchedStyle = matchPseudo(pseudo)

        val style = when (matchedStyle) {
            is Some -> matchedStyle.value
            is None -> return None
        }

        return Some(cascadeStyleAndVisited(
                CascadeInputs(
                        Some(style)
                ),
                Some(primaryStyle.style()),
                layoutParentStyle,
                Some(pseudo)
        ))
    }

    fun matchPseudo(pseudo: PseudoElement): Option<RuleNode> {
        val declarations = mutableListOf<ApplicableDeclarationBlock>()

        val bloomFilter = context.bloomFilter.filter()
        val matchingContext = MatchingContext(
                Some(bloomFilter),
                QuirksMode.NO_QUIRKS
        )

        val stylist = context.stylist

        stylist.pushApplicableDeclarations(
                element,
                Some(pseudo),
                None,
                declarations,
                matchingContext
        )

        if (declarations.isEmpty()) {
            return None
        }

        val ruleNode = stylist.ruleTree.computedRuleNode(declarations)

        return Some(ruleNode)
    }

    fun cascadeStyleAndVisited(
            inputs: CascadeInputs,
            parentStyle: Option<ComputedValues>,
            layoutStyle: Option<ComputedValues>,
            pseudoElement: Option<PseudoElement>
    ): ResolvedStyle {
        val values = context.stylist.cascadeStyleAndVisited(
                context.device,
                Some(element),
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