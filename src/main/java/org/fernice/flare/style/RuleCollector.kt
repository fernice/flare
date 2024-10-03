/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style

import org.fernice.flare.dom.Element
import org.fernice.flare.selector.MatchingContext
import org.fernice.flare.selector.PseudoElement
import org.fernice.flare.selector.SelectorMap
import org.fernice.flare.style.ruletree.CascadeLevel
import org.fernice.flare.style.source.StyleAttribute

class RuleCollector(
    private val styleRoots: Sequence<StyleRoot>,
    private val element: Element,
    private val pseudoElement: PseudoElement?,
    private val styleAttribute: StyleAttribute?,
    private val matchingContext: MatchingContext,
    private val rules: ApplicableDeclarationList,
) {

    private var inSortScope = false

    private inline fun sortScope(block: () -> Unit) {
        assert(!inSortScope) { "sort-scopes cannot be nested" }
        inSortScope = true
        val previousSize = rules.size
        block()
        val size = rules.size
        if (size != previousSize) {
            rules.subList(previousSize, size).sortWith(ApplicableDeclarationBlockComparator)
        }
        inSortScope = false
    }

    private fun collectUserAgentRules() {
        collectStyleRootsRules(Origin.UserAgent)
    }

    private fun collectUserRules() {
        collectStyleRootsRules(Origin.User)
    }

    private fun collectAuthorRules() {
        collectStyleRootsRules(Origin.Author)
    }

    private fun collectStyleRootsRules(origin: Origin) {
        for (styleRoot in styleRoots) {
            collectStyleRootRules(styleRoot, origin)
        }
    }

    private fun collectStyleRootRules(styleRoot: StyleRoot, origin: Origin) {
        val cascadeLevel = when (origin) {
            Origin.UserAgent -> CascadeLevel.UserAgentNormal
            Origin.User -> CascadeLevel.UserNormal
            Origin.Author -> CascadeLevel.AuthorNormal
        }

        styleRoot.readCascadeData(origin) read@{ cascadeData ->
            val map = cascadeData.normalRules(pseudoElement) ?: return@read

            sortScope {
                collectRulesInSelectorMap(map, cascadeLevel, cascadeData)
            }
        }
    }

    private fun collectRulesInSelectorMap(
        map: SelectorMap,
        cascadeLevel: CascadeLevel,
        cascadeData: CascadeData,
    ) {
        assert(inSortScope) { "cannot collect rules without sort-scope" }
        map.getAllMatchingRules(
            element,
            matchingContext,
            cascadeLevel,
            cascadeData,
            rules,
        )
    }

    private fun collectStyleAttribute() {
        styleAttribute?.let { styleAttribute ->
            rules.add(ApplicableDeclarationBlock.fromStyleAttribute(styleAttribute))
        }
    }

    fun collectAll() {
        collectUserAgentRules()
        collectUserRules()
        collectAuthorRules()
        collectStyleAttribute()
    }
}

private object ApplicableDeclarationBlockComparator : Comparator<ApplicableDeclarationBlock> {

    override fun compare(o1: ApplicableDeclarationBlock, o2: ApplicableDeclarationBlock): Int {
        var c = o1.layerOrder.compareTo(o2.layerOrder)
        if (c != 0) return c

        c = o1.specificity.compareTo(o2.specificity)
        if (c != 0) return c

        return o1.sourceOrder.compareTo(o2.sourceOrder)
    }
}
