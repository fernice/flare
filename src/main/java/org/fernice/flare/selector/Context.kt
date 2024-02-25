/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.selector

import org.fernice.flare.dom.Element
import org.fernice.flare.style.parser.QuirksMode

enum class VisitedHandlingMode {
    AllLinksUnvisited,
    AllLinksVisitedAndUnvisited,
}

class MatchingContext(
    val bloomFilter: BloomFilter?,
    val quirksMode: QuirksMode,
    var visitedHandling: VisitedHandlingMode,
) {
    var nestingLevel: Int = 0
    var inNegation: Boolean = false
    var relativeSelectorAnchor: Element? = null

    inline fun <R> nest(block: () -> R): R {
        this.nestingLevel += 1
        val result = block()
        this.nestingLevel -= 1
        return result
    }

    inline fun <R> nestForNegation(block: () -> R): R {
        val previousInNegation = inNegation
        this.inNegation = true
        val result = nest(block)
        this.inNegation = previousInNegation
        return result
    }

    inline fun <R> nestForRelativeSelector(
        relativeSelectorAnchor: Element,
        block: () -> R,
    ): R {
        val previousRelativeSelectorAnchor = this.relativeSelectorAnchor
        this.relativeSelectorAnchor = relativeSelectorAnchor
        val result = nest(block)
        this.relativeSelectorAnchor = previousRelativeSelectorAnchor
        return result
    }

    inline fun <R> withVisitedHandling(
        visitedHandling: VisitedHandlingMode,
        block: () -> R,
    ): R {
        val previousVisitedHandling = this.visitedHandling
        this.visitedHandling = visitedHandling
        val result = block()
        this.visitedHandling = previousVisitedHandling
        return result
    }
}
