/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style

import org.fernice.flare.dom.Device
import org.fernice.flare.dom.Element
import org.fernice.flare.selector.*
import org.fernice.flare.style.ruletree.CascadeLevel
import org.fernice.flare.style.source.StyleRule
import org.fernice.flare.style.stylesheet.*
import org.fernice.std.debug
import org.fernice.std.truncate
import org.fernice.std.unused
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

class StyleRoot(val device: Device, val quirksMode: QuirksMode) {

    private val lock = ReentrantReadWriteLock()

    private val stylesheets = PerOrigin { mutableListOf<Stylesheet>() }
    private val cascadeData = PerOrigin { CascadeData() }

    fun addStylesheet(stylesheet: Stylesheet) {
        lock.writeLock().withLock {
            stylesheets.get(stylesheet.origin).add(stylesheet)

            cascadeData.get(stylesheet.origin).addStylesheet(stylesheet, device, quirksMode)
        }
    }

    /**
     * Remove the stylesheet from the pool.
     * This operation is rather expensive as optimization for inserting and especially matching prevent this from being a simple
     * remove. The style origin has to be rebuilt completely in order to remove a single.
     */
    fun removeStylesheet(stylesheet: Stylesheet) {
        lock.writeLock().withLock {
            stylesheets.get(stylesheet.origin).remove(stylesheet)

            // This is an optimization to just calling rebuild() reducing this
            // operation down to the origin modified
            val collection = stylesheets.get(stylesheet.origin)

            cascadeData.get(stylesheet.origin).rebuild(collection, device, quirksMode)
        }
    }

    fun rebuild() {
        lock.writeLock().withLock {
            for (origin in Origin.entries) {
                val collection = stylesheets.get(origin)

                cascadeData.get(origin).rebuild(collection, device, quirksMode)
            }
        }
    }

    fun <R> readCascadeData(origin: Origin, block: (CascadeData) -> R): R {
        return lock.readLock().withLock {
            block(cascadeData.get(origin))
        }
    }
}

class LayerId(val value: Int) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LayerOrder) return false

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value
    }

    override fun toString(): String = "LayerId[$value]"

    companion object {
        val Root = LayerId(0)
    }
}

class ContainerConditionId(val value: Int) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LayerOrder) return false

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value
    }

    override fun toString(): String = "ContainerConditionId[$value]"

    companion object {
        val None = ContainerConditionId(0)
    }
}

class CascadeData {

    private val normalRules = ElementAndPseudoRules()
    private var rulesSourceOrder = 0

    private var numberOfDeclarations = 0
    private var numberOfSelectors = 0

    fun rebuild(
        stylesheets: List<Stylesheet>,
        device: Device,
        quirksMode: QuirksMode,
    ) {
        clear()
        for (stylesheet in stylesheets) {
            addStylesheet(
                stylesheet,
                device,
                quirksMode
            )
        }
    }

    fun layerOrderFor(layerId: LayerId): LayerOrder {
        unused(layerId)
        return LayerOrder.Root
    }

    fun containerConditionMatches(
        containerConditionId: ContainerConditionId,
        device: Device,
        element: Element,
    ): Boolean {
        unused(containerConditionId)
        unused(device)
        unused(element)
        return false
    }

    fun normalRules(pseudoElement: PseudoElement?): SelectorMap? {
        return normalRules.rules(pseudoElement)
    }

    fun addStylesheet(
        stylesheet: Stylesheet,
        device: Device,
        quirksMode: QuirksMode,
    ) {
        val containingRuleState = ContainingRuleState.initial()
        addRuleList(
            stylesheet.rules.iterator(),
            parentCondition = null,
            device,
            quirksMode,
            stylesheet,
            containingRuleState
        )
    }

    private fun addRuleList(
        rules: Iterator<CssRule>,
        parentCondition: RuleCondition?,
        device: Device,
        quirksMode: QuirksMode,
        stylesheet: Stylesheet,
        containingRuleState: ContainingRuleState,
    ) {
        for (rule in rules) {
            var handled = true

            var selectorsForNestedRules: MutableList<Selector>? = null

            when (rule) {
                is CssRule.Style -> {
                    val styleRule = rule.styleRule

                    numberOfDeclarations += styleRule.declarations.size

                    val ancestorSelectors = containingRuleState.ancestorSelectorLists.lastOrNull()
                    val hasNestedRules = styleRule.rules != null
                    if (hasNestedRules) {
                        selectorsForNestedRules = mutableListOf()
                    }

                    for (selector in styleRule.selectors) {
                        numberOfSelectors += 1

                        val pseudoElement = selector.pseudoElement

                        val selector = when {
                            ancestorSelectors != null -> selector.replaceParent(ancestorSelectors.selectors)
                            else -> selector
                        }

                        selectorsForNestedRules?.add(selector)

                        val ancestorHashes = AncestorHashes.fromSelector(selector, quirksMode)

                        val rule = Rule(
                            parentCondition,
                            selector,
                            ancestorHashes,
                            rulesSourceOrder,
                            LayerId.Root,
                            ContainerConditionId.None,
                            styleRule,
                        )

                        normalRules.insert(rule, pseudoElement, quirksMode)
                    }

                    rulesSourceOrder++
                    handled = !hasNestedRules
                }
                // is CssRule.KeyFrames -> {}
                // is CssRule.Property -> {}
                // is CssRule.FontFace -> {}
                // is CssRule.FontFeatureValues -> {}
                // is CssRule.FontPaletteValues -> {}
                // is CssRule.CounterStyle -> {}
                // is CssRule.Page -> {}

                else -> handled = false
            }

            if (handled) {
                debug {
                    val (children, effective) = RulesIterator.children(
                        rule,
                        device,
                        quirksMode,
                        PotentiallyEffectiveRules
                    )
                    assert(children == null)
                    assert(effective == Effective.True)
                }
                continue
            }

            val (children, effective) = RulesIterator.children(
                rule,
                device,
                quirksMode,
                PotentiallyEffectiveRules,
            )

            if (effective == Effective.False) continue

            val savedContainingRuleState = containingRuleState.save()
            when (rule) {
                // is CssRule.Import -> {}
                is CssRule.Media -> {

                }
                // is CssRule.LayerBlock -> {}
                // is CssRule.LayerStatement -> {}
                is CssRule.Style -> {
                    if (selectorsForNestedRules != null) {
                        containingRuleState.ancestorSelectorLists.add(SelectorList(selectorsForNestedRules))
                    }
                }
                // is CssRule.Container -> {}

                else -> {
                    // others rules are not inserted
                }
            }

            if (children != null) {
                val condition = when (effective) {
                    is Effective.Indeterminable -> when {
                        parentCondition != null -> parentCondition.derive(effective.condition)
                        else -> RuleCondition(effective.condition)
                    }

                    else -> parentCondition
                }

                addRuleList(
                    children,
                    condition,
                    device,
                    quirksMode,
                    stylesheet,
                    containingRuleState
                )
            }

            containingRuleState.restore(savedContainingRuleState)
        }
    }

    fun clear() {
        normalRules.clear()
        rulesSourceOrder = 0
        numberOfDeclarations = 0
        numberOfSelectors = 0
    }
}

private class ContainingRuleState(
    var layerName: MutableList<String>,
    var layerId: LayerId,
    var containerConditionId: ContainerConditionId,
    val ancestorSelectorLists: MutableList<SelectorList>,
) {

    fun save(): SavedContainingRuleState {
        return SavedContainingRuleState(
            layerName.size,
            layerId,
            containerConditionId,
            ancestorSelectorLists.size
        )
    }

    fun restore(saved: SavedContainingRuleState) {
        assert(layerName.size >= saved.layerNameSize)
        assert(ancestorSelectorLists.size >= saved.ancestorSelectorListsSize)
        layerName.truncate(saved.layerNameSize)
        layerId = saved.layerId
        containerConditionId = saved.containerConditionId
        ancestorSelectorLists.truncate(saved.ancestorSelectorListsSize)
    }

    companion object {
        fun initial(): ContainingRuleState {
            return ContainingRuleState(
                layerName = mutableListOf(),
                layerId = LayerId.Root,
                containerConditionId = ContainerConditionId.None,
                ancestorSelectorLists = mutableListOf(),
            )
        }
    }
}

private class SavedContainingRuleState(
    val layerNameSize: Int,
    val layerId: LayerId,
    val containerConditionId: ContainerConditionId,
    val ancestorSelectorListsSize: Int,
)

class ElementAndPseudoRules {

    private val elementMap = SelectorMap()
    private val pseudoElementsMap = PerPseudoElementMap<SelectorMap>()

    fun insert(rule: Rule, pseudoElement: PseudoElement?, quirksMode: QuirksMode) {
        val map = if (pseudoElement != null) {
            pseudoElementsMap.computeIfAbsent(pseudoElement) { SelectorMap() }
        } else {
            elementMap
        }

        map.insert(rule)
    }

    fun clear() {
        elementMap.clear()
        for (map in pseudoElementsMap.iterator()) {
            map?.clear()
        }
    }

    fun rules(pseudoElement: PseudoElement?): SelectorMap? {
        return if (pseudoElement != null) {
            pseudoElementsMap.get(pseudoElement)
        } else {
            elementMap
        }
    }
}

class Rule(
    val condition: RuleCondition?,
    val selector: Selector,
    val hashes: AncestorHashes,
    val sourceOrder: Int,
    val layerId: LayerId,
    val containerConditionId: ContainerConditionId,
    val styleRule: StyleRule,
) {

    val specificity: Int
        get() = selector.specificity

    fun toApplicableDeclarationBlock(
        level: CascadeLevel,
        cascadeData: CascadeData,
    ): ApplicableDeclarationBlock {
        return ApplicableDeclarationBlock.from(
            styleRule,
            sourceOrder,
            specificity,
            level,
            cascadeData.layerOrderFor(layerId),
        )
    }
}

