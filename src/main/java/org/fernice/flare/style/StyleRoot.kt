/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style

import org.fernice.flare.dom.Element
import org.fernice.flare.selector.AncestorHashes
import org.fernice.flare.selector.MatchingContext
import org.fernice.flare.selector.PseudoElement
import org.fernice.flare.selector.SelectorMap
import org.fernice.flare.style.parser.QuirksMode
import org.fernice.flare.style.stylesheet.CssRule
import org.fernice.flare.style.stylesheet.Stylesheet
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

class StyleRoot(val quirksMode: QuirksMode) {

    private val lock = ReentrantReadWriteLock()

    private val stylesheets = PerOrigin { mutableListOf<Stylesheet>() }
    private val cascadeData = PerOrigin { CascadeData() }

    fun addStylesheet(stylesheet: Stylesheet) {
        lock.writeLock().withLock {
            stylesheets.get(stylesheet.origin).add(stylesheet)

            cascadeData.get(stylesheet.origin).insertStylesheet(stylesheet, quirksMode)
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

            cascadeData.get(stylesheet.origin).rebuild(collection, quirksMode)
        }
    }

    fun rebuild() {
        lock.writeLock().withLock {
            for (origin in Origin.entries) {
                val collection = stylesheets.get(origin)

                cascadeData.get(origin).rebuild(collection, quirksMode)
            }
        }
    }

    fun contributeMatchingStyles(
        origin: Origin,
        element: Element,
        pseudoElement: PseudoElement?,
        context: MatchingContext,
        collector: StyleCollector,
    ) {
        lock.readLock().withLock {
            cascadeData.get(origin).rules(pseudoElement)?.getAllMatchingRules(
                element,
                context,
                collector,
            )
        }
    }
}

class CascadeData {

    private val rules = ElementAndPseudoRules()
    private var rulesSourceOrder = 0

    fun rebuild(stylesheets: List<Stylesheet>, quirksMode: QuirksMode) {
        clear()
        for (stylesheet in stylesheets) {
            insertStylesheet(stylesheet, quirksMode)
        }
    }

    fun insertStylesheet(stylesheet: Stylesheet, quirksMode: QuirksMode) {
        for (stylesheetRule in stylesheet.rules) {
            when (stylesheetRule) {
                is CssRule.Style -> {
                    val styleRule = stylesheetRule.styleRule

                    for (selector in styleRule.selectors) {
                        val pseudoElement = selector.pseudoElement

                        val rule = Rule(
                            selector,
                            AncestorHashes.fromSelector(selector, quirksMode),
                            rulesSourceOrder,
                            styleRule,
                        )

                        rules.insert(rule, pseudoElement, quirksMode)
                    }
                    rulesSourceOrder++
                }

                else -> {
                }
            }
        }
    }

    fun clear() {
        rules.clear()
        rulesSourceOrder = 0
    }

    fun rules(pseudoElement: PseudoElement?): SelectorMap? {
        return rules.rules(pseudoElement)
    }
}

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
