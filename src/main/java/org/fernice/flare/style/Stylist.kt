/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style

import org.fernice.flare.ApplicableDeclarationBlock
import org.fernice.flare.dom.Device
import org.fernice.flare.dom.Element
import org.fernice.flare.font.FontMetricsProvider
import org.fernice.flare.selector.AncestorHashes
import org.fernice.flare.selector.MatchingContext
import org.fernice.flare.selector.PseudoElement
import org.fernice.flare.selector.Selector
import org.fernice.flare.selector.SelectorMap
import org.fernice.flare.style.parser.QuirksMode
import org.fernice.flare.style.properties.PropertyDeclarationBlock
import org.fernice.flare.style.properties.cascade
import org.fernice.flare.style.ruletree.CascadeLevel
import org.fernice.flare.style.ruletree.RuleTree
import org.fernice.flare.style.ruletree.StyleSource
import org.fernice.flare.style.stylesheet.CssRule
import org.fernice.flare.style.stylesheet.Origin
import org.fernice.flare.style.stylesheet.StyleRule
import org.fernice.flare.style.stylesheet.Stylesheet

class Rule(
    val selector: Selector,
    val hashes: AncestorHashes,
    val sourceOrder: Int,
    val styleRule: StyleRule
) {

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

class Stylist(
    private val quirksMode: QuirksMode,
    val ruleTree: RuleTree,
    private val stylesheets: DocumentStylesheetList,
    private val cascadeData: DocumentCascadeData
) {

    companion object {
        fun new(quirksMode: QuirksMode): Stylist {
            return Stylist(
                quirksMode,
                RuleTree.new(),
                DocumentStylesheetList.new(),
                DocumentCascadeData.default()
            )
        }
    }

    fun pushApplicableDeclarations(
        element: Element,
        pseudoElement: PseudoElement?,
        styleAttribute: PropertyDeclarationBlock?,
        applicableDeclarations: MutableList<ApplicableDeclarationBlock>,
        context: MatchingContext
    ) {

        cascadeData.userAgent.normalRules(pseudoElement)?.getAllMatchingRules(
            element,
            applicableDeclarations,
            context,
            CascadeLevel.USER_AGENT_NORMAL
        )

        cascadeData.user.normalRules(pseudoElement)?.getAllMatchingRules(
            element,
            applicableDeclarations,
            context,
            CascadeLevel.USER_NORMAL
        )

        cascadeData.author.normalRules(pseudoElement)?.getAllMatchingRules(
            element,
            applicableDeclarations,
            context,
            CascadeLevel.AUTHOR_NORMAL
        )

        if (styleAttribute != null) {
            applicableDeclarations.add(
                ApplicableDeclarationBlock.fromDeclarations(
                    styleAttribute,
                    CascadeLevel.STYLE_ATTRIBUTE_NORMAL
                )
            )
        }
    }

    fun cascadeStyleAndVisited(
        device: Device,
        element: Element?,
        pseudoElement: PseudoElement?,
        inputs: CascadeInputs,
        parentStyle: ComputedValues?,
        parentStyleIgnoringFirstLine: ComputedValues?,
        layoutStyle: ComputedValues?,
        fontMetricsProvider: FontMetricsProvider
    ): ComputedValues {
        return cascade(
            device,
            element,
            pseudoElement,
            inputs.rules ?: ruleTree.root(),
            parentStyle,
            parentStyleIgnoringFirstLine,
            layoutStyle,
            fontMetricsProvider
        )
    }

    fun addStylesheet(stylesheet: Stylesheet) {
        stylesheets.perOrigin
            .get(stylesheet.origin)
            .add(stylesheet)

        cascadeData.preOrigin
            .get(stylesheet.origin)
            .insertStylesheet(stylesheet, quirksMode)
    }

    /**
     * Remove the stylesheet from the pool.
     * This operation is rather expensive as optimization for inserting and especially matching prevent this from being a simple
     * remove. The style origin has to be rebuild completely in order to remove a single.
     */
    fun removeStylesheet(stylesheet: Stylesheet) {
        stylesheets.perOrigin
            .get(stylesheet.origin)
            .remove(stylesheet)

        // This is an optimization to just calling rebuild() reducing this
        // operation down to the origin modified
        val collection = stylesheets.perOrigin
            .get(stylesheet.origin)

        cascadeData.preOrigin
            .get(stylesheet.origin)
            .rebuild(collection, quirksMode)
    }

    fun rebuild() {
        for (origin in Origin.values()) {
            val collection = stylesheets.perOrigin
                .get(origin)

            cascadeData.preOrigin
                .get(origin)
                .rebuild(collection, quirksMode)
        }
    }
}

class DocumentStylesheetList(val perOrigin: PerOrigin<MutableList<Stylesheet>>) {

    companion object {
        fun new(): DocumentStylesheetList {
            return DocumentStylesheetList(
                PerOrigin(
                    mutableListOf(),
                    mutableListOf(),
                    mutableListOf()
                )
            )
        }
    }
}

class DocumentCascadeData(
    val userAgent: CascadeData,
    val user: CascadeData,
    val author: CascadeData,
    val preOrigin: PerOrigin<CascadeData>
) {

    companion object {
        fun default(): DocumentCascadeData {
            return new(
                CascadeData.new(),
                CascadeData.new(),
                CascadeData.new()
            )
        }

        fun new(
            userAgent: CascadeData,
            user: CascadeData,
            author: CascadeData
        ): DocumentCascadeData {
            return DocumentCascadeData(
                userAgent,
                user,
                author,
                PerOrigin(
                    userAgent,
                    user,
                    author
                )
            )
        }
    }
}

class ElementAndPseudoRules(
    private val elementMap: SelectorMap,
    private val pseudoMap: PerPseudoElementMap<SelectorMap>
) {

    companion object {
        fun new(): ElementAndPseudoRules {
            return ElementAndPseudoRules(
                SelectorMap(),
                PerPseudoElementMap()
            )
        }
    }

    fun insert(rule: Rule, pseudoElement: PseudoElement?, quirksMode: QuirksMode) {
        val map = if (pseudoElement != null) {
            pseudoMap.computeIfAbsent(pseudoElement) { SelectorMap() }
        } else {
            elementMap
        }

        map.insert(rule)
    }

    fun clear() {
        elementMap.clear()
        for (map in pseudoMap.iterator()) {
            map?.clear()
        }
    }

    fun rules(pseudoElement: PseudoElement?): SelectorMap? {
        return if (pseudoElement != null) {
            pseudoMap.get(pseudoElement)
        } else {
            elementMap
        }
    }
}

class CascadeData(
    private val normalRules: ElementAndPseudoRules,
    private var rulesSourceOrder: Int
) {

    companion object {
        fun new(): CascadeData {
            return CascadeData(
                normalRules = ElementAndPseudoRules.new(),
                rulesSourceOrder = 0
            )
        }
    }

    fun rebuild(stylesheets: List<Stylesheet>, quirksMode: QuirksMode) {
        clear()

        for (stylesheet in stylesheets) {
            insertStylesheet(stylesheet, quirksMode)
        }
    }

    fun insertStylesheet(stylesheet: Stylesheet, quirksMode: QuirksMode) {
        for (rule in stylesheet.rules) {
            when (rule) {
                is CssRule.Style -> {
                    val styleRule = rule.styleRule

                    for (selector in styleRule.selectors) {
                        val pseudoElement = selector.pseudoElement()

                        val indexedRule = Rule(
                            selector,
                            AncestorHashes.new(selector, quirksMode),
                            rulesSourceOrder,
                            styleRule
                        )

                        normalRules.insert(indexedRule, pseudoElement, quirksMode)
                    }
                    rulesSourceOrder++
                }
                else -> {
                }
            }
        }
    }

    fun clear() {
        normalRules.clear()
        rulesSourceOrder = 0
    }

    fun normalRules(pseudoElement: PseudoElement?): SelectorMap? {
        return normalRules.rules(pseudoElement)
    }
}
