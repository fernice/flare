/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style

import org.fernice.flare.dom.Device
import org.fernice.flare.dom.Element
import org.fernice.flare.font.FontMetricsProvider
import org.fernice.flare.selector.AncestorHashes
import org.fernice.flare.selector.PseudoElement
import org.fernice.flare.selector.Selector
import org.fernice.flare.style.parser.QuirksMode
import org.fernice.flare.style.properties.cascade
import org.fernice.flare.style.ruletree.RuleTree
import org.fernice.flare.style.source.StyleRule
import org.fernice.flare.style.source.StyleSource

class Rule(
    val selector: Selector,
    val hashes: AncestorHashes,
    val sourceOrder: Int,
    val styleRule: StyleRule,
) {

    val specificity: Int
        get() = selector.specificity
}

interface StyleCollector {
    fun collect(style: StyleSource)
}

class Stylist(
    quirksMode: QuirksMode,
) {
    val ruleTree = RuleTree()
    val styleRoot = StyleRoot(quirksMode)

    fun cascadeStyleAndVisited(
        device: Device,
        element: Element,
        pseudoElement: PseudoElement?,
        inputs: CascadeInputs,
        previousStyle: ComputedValues?,
        parentStyle: ComputedValues?,
        parentStyleIgnoringFirstLine: ComputedValues?,
        fontMetricsProvider: FontMetricsProvider,
    ): ComputedValues {
        return cascade(
            device,
            element,
            pseudoElement,
            inputs.rules ?: ruleTree.root,
            previousStyle,
            parentStyle,
            parentStyleIgnoringFirstLine,
            fontMetricsProvider,
        )
    }
}

