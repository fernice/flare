package de.krall.flare.style

import de.krall.flare.ApplicableDeclarationBlock
import de.krall.flare.dom.Element
import de.krall.flare.selector.MatchingContext
import de.krall.flare.selector.PseudoElement
import de.krall.flare.std.None
import de.krall.flare.std.Option
import de.krall.flare.std.Some
import de.krall.flare.style.context.StyleContext
import de.krall.flare.style.parser.QuirksMode

class MatchingResult(val declarations: List<ApplicableDeclarationBlock>)

class ResolvedElementStyles(val computedValues: ComputedValues)

class PrimaryStyle(val style: ResolvedElementStyles)

class CascadeInputs(val rules: Option<List<ApplicableDeclarationBlock>>)

class ElementStyleResolver(val element: Element,
                           val context: StyleContext) {

    fun <R> withDefaultParentStyles(run: (Option<ComputedValues>, Option<ComputedValues>) -> R): R {
        val parentElement = element.inheritanceParent()
        val parentData = parentElement.andThen { e -> e.getData() }
        val parentStyle = parentData.map { d -> d.getStyles().primary() }

        return run(parentStyle, parentStyle)
    }

    fun resolvePrimaryStyleWithDefaultParentStyles(): PrimaryStyle {
        return withDefaultParentStyles { parentStyle, layoutStyle ->
            resolvePrimaryStyle(parentStyle, layoutStyle)
        }
    }

    fun resolvePrimaryStyle(parentStyle: Option<ComputedValues>,
                            layoutStyle: Option<ComputedValues>): PrimaryStyle {
        val primaryStyle = matchPrimary()

        return cascadePrimaryStyle(
                CascadeInputs(
                        Some(primaryStyle.declarations)
                ),
                parentStyle,
                layoutStyle
        )
    }

    fun matchPrimary(): MatchingResult {
        val declarations = mutableListOf<ApplicableDeclarationBlock>()

        val bloomFilter = context.bloomFilter.filter()
        val matchingContext = MatchingContext(
                None(),
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

        return MatchingResult(declarations)
    }

    fun cascadePrimaryStyle(inputs: CascadeInputs,
                            parentStyle: Option<ComputedValues>,
                            layoutStyle: Option<ComputedValues>): PrimaryStyle {
        return PrimaryStyle(
                cascadeStyleAndVisited(
                        inputs,
                        parentStyle,
                        layoutStyle,
                        None()
                )
        )
    }

    fun cascadeStyleAndVisited(inputs: CascadeInputs,
                               parentStyle: Option<ComputedValues>,
                               layoutStyle: Option<ComputedValues>,
                               pseudoElement: Option<PseudoElement>): ResolvedElementStyles {
        val values = context.stylist.cascadeStyleAndVisited(
                Some(element),
                pseudoElement,
                inputs,
                parentStyle,
                parentStyle,
                layoutStyle,
                context.fontMetricsProvider
        )

        return ResolvedElementStyles(
                values
        )
    }
}