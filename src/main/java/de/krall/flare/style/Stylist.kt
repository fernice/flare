package de.krall.flare.style

import de.krall.flare.ApplicableDeclarationBlock
import de.krall.flare.dom.Device
import de.krall.flare.dom.Element
import de.krall.flare.font.FontMetricsProvider
import de.krall.flare.selector.AncestorHashes
import de.krall.flare.selector.MatchingContext
import de.krall.flare.selector.PseudoElement
import de.krall.flare.selector.Selector
import de.krall.flare.selector.SelectorMap
import de.krall.flare.std.None
import de.krall.flare.std.Option
import de.krall.flare.std.Some
import de.krall.flare.std.ifLet
import de.krall.flare.std.unwrapOr
import de.krall.flare.style.parser.QuirksMode
import de.krall.flare.style.properties.PropertyDeclarationBlock
import de.krall.flare.style.properties.cascade
import de.krall.flare.style.ruletree.CascadeLevel
import de.krall.flare.style.ruletree.RuleTree
import de.krall.flare.style.ruletree.StyleSource
import de.krall.flare.style.stylesheet.CssRule
import de.krall.flare.style.stylesheet.Origin
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

class Stylist(private val device: Device,
              private val quirksMode: QuirksMode,
              val ruleTree: RuleTree,
              private val stylesheets: DocumentStylesheetList,
              private val cascadeData: DocumentCascadeData) {

    companion object {
        fun new(device: Device, quirksMode: QuirksMode): Stylist {
            return Stylist(
                    device,
                    quirksMode,
                    RuleTree.new(),
                    DocumentStylesheetList.new(),
                    DocumentCascadeData.default()
            )
        }
    }

    fun pushApplicableDeclarations(element: Element,
                                   pseudoElement: Option<PseudoElement>,
                                   styleAttribute: Option<PropertyDeclarationBlock>,
                                   applicableDeclarations: MutableList<ApplicableDeclarationBlock>,
                                   context: MatchingContext) {

        cascadeData.userAgent.normalRules(pseudoElement).ifLet { map ->
            map.getAllMatchingRules(
                    element,
                    applicableDeclarations,
                    context,
                    CascadeLevel.AUTHOR_NORMAL
            )
        }

        cascadeData.user.normalRules(pseudoElement).ifLet { map ->
            map.getAllMatchingRules(
                    element,
                    applicableDeclarations,
                    context,
                    CascadeLevel.AUTHOR_NORMAL
            )
        }

        cascadeData.author.normalRules(pseudoElement).ifLet { map ->
            map.getAllMatchingRules(
                    element,
                    applicableDeclarations,
                    context,
                    CascadeLevel.AUTHOR_NORMAL
            )
        }

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
                inputs.rules.unwrapOr(ruleTree.root()),
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

class DocumentCascadeData(val userAgent: CascadeData,
                          val user: CascadeData,
                          val author: CascadeData,
                          val preOrigin: PerOrigin<CascadeData>) {

    companion object {
        fun default(): DocumentCascadeData {
            return new(
                    CascadeData.new(),
                    CascadeData.new(),
                    CascadeData.new()
            )
        }

        fun new(userAgent: CascadeData,
                user: CascadeData,
                author: CascadeData): DocumentCascadeData {
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

class ElementAndPseudoRules(private val elementMap: SelectorMap,
                            private val pseudoMap: PerPseudoElementMap<SelectorMap>) {

    companion object {
        fun new(): ElementAndPseudoRules {
            return ElementAndPseudoRules(
                    SelectorMap(),
                    PerPseudoElementMap()
            )
        }
    }

    fun insert(rule: Rule, pseudoElement: Option<PseudoElement>, quirksMode: QuirksMode) {
        val map = when (pseudoElement) {
            is Some -> pseudoMap.computeIfAbsent(pseudoElement.value, { SelectorMap() })
            is None -> elementMap
        }

        map.insert(rule)
    }

    fun clear() {
        elementMap.clear()
        for (map in pseudoMap.iter()) {
            if (map is Some) {
                map.value.clear()
            }
        }
    }

    fun rules(pseudoElement: Option<PseudoElement>): Option<SelectorMap> {
        return when (pseudoElement) {
            is Some -> pseudoMap.get(pseudoElement.value)
            is None -> Some(elementMap)
        }
    }
}

class CascadeData(private val normalRules: ElementAndPseudoRules) {

    companion object {
        fun new(): CascadeData {
            return CascadeData(
                    ElementAndPseudoRules.new()
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
        var sourceOrder = 0
        for (rule in stylesheet.rules) {
            when (rule) {
                is CssRule.Style -> {
                    val styleRule = rule.styleRule

                    for (selector in styleRule.selectors) {
                        val pseudoElement = selector.pseudoElement()

                        val indexedRule = Rule(
                                selector,
                                AncestorHashes.new(selector, quirksMode),
                                sourceOrder,
                                styleRule
                        )

                        normalRules.insert(indexedRule, pseudoElement, quirksMode)
                    }
                    sourceOrder++
                }
            }
        }
    }

    fun clear() {
        normalRules.clear()
    }

    fun normalRules(pseudoElement: Option<PseudoElement>): Option<SelectorMap> {
        return normalRules.rules(pseudoElement)
    }
}