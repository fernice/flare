package de.krall.flare.style

import de.krall.flare.ApplicableDeclarationBlock
import de.krall.flare.dom.Device
import de.krall.flare.dom.Element
import de.krall.flare.font.FontMetricsProvider
import de.krall.flare.selector.*
import de.krall.flare.std.Option
import de.krall.flare.std.ifLet
import de.krall.flare.style.parser.QuirksMode
import de.krall.flare.style.properties.PropertyDeclarationBlock
import de.krall.flare.style.properties.cascade
import de.krall.flare.style.ruletree.CascadeLevel
import de.krall.flare.style.ruletree.StyleSource
import de.krall.flare.style.stylesheet.CssRule
import de.krall.flare.style.stylesheet.StyleRule
import de.krall.flare.style.stylesheet.Stylesheet

class Rule(val selector: Selector,
           val hashes: AncestorHashes,
           val sourceOrder: Int,
           val styleRule: StyleRule) {

    fun specificity(): Int {
        return selector.specificity()
    }

    fun toApplicableDeclaration(cascadeLevel: CascadeLevel): ApplicableDeclarationBlock {
        return ApplicableDeclarationBlock.new(
                StyleSource.fromRule(styleRule),
                sourceOrder,
                cascadeLevel,
                specificity()
        )
    }
}

class Stylist(val device: Device,
              val quirksMode: QuirksMode,
              private val styleMap: SelectorMap) {

    companion object {
        fun new(device: Device, quirksMode: QuirksMode): Stylist {
            return Stylist(
                    device,
                    quirksMode,
                    SelectorMap()
            )
        }
    }

    fun pushApplicableDeclarations(element: Element,
                                   pseudoElement: Option<PseudoElement>,
                                   styleAttribute: Option<PropertyDeclarationBlock>,
                                   applicableDeclarations: MutableList<ApplicableDeclarationBlock>,
                                   context: MatchingContext) {

        styleMap.getAllMatchingRules(
                element,
                applicableDeclarations,
                context,
                CascadeLevel.AUTHOR_NORMAL
        )

        styleAttribute.ifLet { block ->
            applicableDeclarations.add(
                    ApplicableDeclarationBlock.fromDeclarations(
                            block,
                            CascadeLevel.STYLE_ATTRIBUTE_NORMAL
                    )
            )
        }
    }

    fun cascadeStyleAndVisited(element: Option<Element>,
                               pseudoElement: Option<PseudoElement>,
                               inputs: CascadeInputs,
                               parentStyle: Option<ComputedValues>,
                               parentStyleIgnoringFirstLine: Option<ComputedValues>,
                               layoutStyle: Option<ComputedValues>,
                               fontMetricsProvider: FontMetricsProvider): ComputedValues {
        return cascade(
                device,
                element,
                pseudoElement,
                inputs.rules.expect("currently only supplier"),
                parentStyle,
                parentStyleIgnoringFirstLine,
                layoutStyle,
                fontMetricsProvider
        )
    }

    fun insertStyleheet(stylesheet: Stylesheet) {
        var sourceOrder = 0
        for (rule in stylesheet.rules) {
            when (rule) {
                is CssRule.Style -> {
                    val styleRule = rule.styleRule

                    for (selector in styleRule.selectors) {
                        val indexedRule = Rule(
                                selector,
                                AncestorHashes.new(selector, quirksMode),
                                sourceOrder,
                                styleRule
                        )

                        styleMap.insert(indexedRule)
                    }
                }
            }

            sourceOrder++
        }
    }
}